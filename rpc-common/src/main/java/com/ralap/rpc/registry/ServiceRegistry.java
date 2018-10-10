package com.ralap.rpc.registry;

import com.ralap.rpc.Constant;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * 服务注册
 * 为服务端提供服务注册功能
 *
 * @author: ralap
 * @date: created at 2018/9/5 14:12
 */
@Slf4j
public class ServiceRegistry {


    /*注册地址*/
    private String registryAddress;
    /*锁初始化（控制执行流程）*/
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * 初始化服务注册
     *
     * @param registryAddress zk地址
     */
    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    /**
     * 提供服务注册
     */
    public void registry(String data) {
        if (data != null) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                //注册创建服务地址
                createNode(zk, data);
            }
        }
    }

    /**
     * 建立zk连接
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            //建立连接
            zk = new ZooKeeper(registryAddress, Constant.REGISTRY_ZK_SESSION_TIMEOUT, even -> {
                if (even.getState() == KeeperState.SyncConnected) {
                    //建立连接成功，放行
                    latch.countDown();
                }
            });
            //等待
            latch.await();
        } catch (Exception e) {
            log.error("zk连接失败", e);
        }
        return zk;
    }

    /**
     * 创建节点，保存服务信息
     *
     * @param data 服务信息
     */
    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            //创建父路径
            if (zk.exists(Constant.REGISTRY_ZK_PATH, null) == null) {
                zk.create(Constant.REGISTRY_ZK_PATH, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
            //添加注册信息，临时有序节点
            String path = zk.create(Constant.REGISTRY_ZK_DATA, bytes, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("创建节点{}", path);
        } catch (Exception e) {
            log.error("创建节点异常", e);
        }
    }

}
