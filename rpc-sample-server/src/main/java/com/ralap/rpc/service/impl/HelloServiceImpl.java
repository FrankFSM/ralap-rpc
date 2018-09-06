package com.ralap.rpc.service.impl;

import com.ralap.rpc.server.RPCService;
import com.ralap.rpc.HelloService;

/**
 * @author: ralap
 * @date: created at 2018/9/6 14:57
 */
@RPCService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    public String say(String name) {
        return "Hello -------->【" + name + "】";
    }
}
