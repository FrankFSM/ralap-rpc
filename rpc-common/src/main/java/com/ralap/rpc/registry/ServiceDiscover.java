package com.ralap.rpc.registry;

import com.ralap.rpc.Constant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

/**
 * 服务发现
 * 为客户端提供服务发现功能
 *
 * @author: ralap
 * @date: created at 2018/9/5 15:27
 */
@Slf4j
public class ServiceDiscover {


    /*注册地址*/
    private String registryAddress;
    /*锁初始化（控制创建连接后才能做后续操作）*/
    private CountDownLatch latch = new CountDownLatch(1);
    /*服务器地址*/
    private volatile List<String> dataList = new ArrayList<>();

    /**
     * 初始化服务发现
     * 1.与zk监理连接
     * 2.获取监听服务变化
     *
     * @param registryAddress zk地址
     */
    public ServiceDiscover(String registryAddress) {
        this.registryAddress = registryAddress;
        ZooKeeper zk = connectServer();
        if (zk != null) {
            watchNode(zk);
        }
    }


    /**
     * 获取发现服务地址信息
     * 如果为集群，随机获取
     */
    public String discover() {
        String data = null;
        //获取服务个数
        int serverCount = this.dataList.size();
        if (serverCount > 0) {
            if (serverCount == 1) {
                data = this.dataList.get(0);
            } else {
                //随机获取
                data = this.dataList.get(ThreadLocalRandom.current().nextInt(serverCount));
            }
        } else {
            log.info("没有节点信息");
        }
        log.info("节点信息{}", data);
        return data;
    }


    /**
     * 建立zk连接
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            //家里连接
            zk = new ZooKeeper(registryAddress, Constant.REGISTRY_ZK_SESSION_TIMEOUT, even -> {
                if (even.getState() == KeeperState.SyncConnected) {
                    //建立成功放行
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
            //获取服务节点，监听变化
            List<String> childrenList = zk.getChildren(Constant.REGISTRY_ZK_PATH, even -> {
                if (even.getType() == EventType.NodeChildrenChanged) {
                    //重置服务信息
                    watchNode(zk);
                }
            });
            dataList = new ArrayList<>();
            //获取服务器地址
            for (String children : childrenList) {
                byte[] bytes = zk.getData(Constant.REGISTRY_ZK_PATH + "/" + children, false, null);
                dataList.add(new String(bytes));
            }
            log.info("发现节点{}", dataList);
        } catch (Exception e) {
            log.error("监听异常", e);
        }
    }


}
