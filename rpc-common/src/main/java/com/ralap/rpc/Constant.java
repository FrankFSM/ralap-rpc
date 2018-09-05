package com.ralap.rpc;

/**
 * 常量池
 *
 * @author: ralap
 * @date: created at 2018/9/5 13:59
 */
public class Constant {

    /**
     * zk 注册路径
     */
    public static final String REGISTRY_ZK_PATH = "/registry";

    /**
     * zk 注册节点
     */
    public static final String REGISTRY_ZK_DATA = REGISTRY_ZK_PATH + "/data";

    /**
     * zk 超时时间
     */
    public static final int REGISTRY_ZK_SESSION_TIMEOUT = 5 * 1000;


}
