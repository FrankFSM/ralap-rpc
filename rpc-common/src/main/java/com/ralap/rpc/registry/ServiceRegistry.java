package com.ralap.rpc.registry;

import com.ralap.rpc.Constant;
import com.sun.xml.internal.ws.util.StringUtils;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.jboss.netty.util.internal.StringUtil;

/**
 * 服务注册
 *
 * @author: ralap
 * @date: created at 2018/9/5 14:12
 */
@Slf4j
public class ServiceRegistry {


    /*注册地址*/
    private String registryAddress;

    private CountDownLatch latch = new CountDownLatch(1);


    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void registry(String data) {
        if (data != null) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                createNode(zk, data);
            }
        }
    }


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

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            if (zk.exists(Constant.REGISTRY_ZK_PATH, null) == null) {
                zk.create(Constant.REGISTRY_ZK_PATH, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
            String path = zk.create(Constant.REGISTRY_ZK_DATA, bytes, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);

            log.info("创建节点{}", path);
        } catch (Exception e) {
            log.error("创建节点异常", e);
        }
    }


}
