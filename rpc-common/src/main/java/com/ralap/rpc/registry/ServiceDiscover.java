package com.ralap.rpc.registry;

import com.ralap.rpc.Constant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * @author: ralap
 * @date: created at 2018/9/5 15:27
 */
@Slf4j
public class ServiceDiscover {


    /*注册地址*/
    private String registryAddress;

    private CountDownLatch latch = new CountDownLatch(1);
    private volatile List<String> dataList = new ArrayList<>();


    public ServiceDiscover(String registryAddress) {
        this.registryAddress = registryAddress;
        ZooKeeper zk = connectServer();
        if (zk != null) {
            watchNode(zk);
        }
    }


    /**
     * 发现服务地址信息
     */
    public String discover() {
        String data = null;
        //获取服务个数
        int serverCount = this.dataList.size();
        if (serverCount > 0) {
            if (serverCount == 1) {
                data = this.dataList.get(0);
            } else {
                data = this.dataList.get(ThreadLocalRandom.current().nextInt(serverCount));
            }
        } else {
            log.info("没有节点信息");
        }
        log.info("节点信息{}", data);
        return data;
    }


    /**
     * 创建zk连接
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.REGISTRY_ZK_SESSION_TIMEOUT, even -> {
                if (even.getState() == KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            log.error("zk连接失败", e);
        }
        return zk;
    }

    /**
     * 监听节点变化
     */
    private void watchNode(ZooKeeper zk) {
        try {
            List<String> childrenList = zk.getChildren(Constant.REGISTRY_ZK_PATH, even -> {
                if (even.getType() == EventType.NodeChildrenChanged) {
                    watchNode(zk);
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String children : childrenList) {
                byte[] bytes = zk.getData(Constant.REGISTRY_ZK_PATH + "/" + children, false, null);
                dataList.add(new String(bytes));
            }
            log.info("发现节点{}", dataList);
            this.dataList = dataList;
        } catch (Exception e) {
            log.error("监听异常", e);
        }
    }


}
