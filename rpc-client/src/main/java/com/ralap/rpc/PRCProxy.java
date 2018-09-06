package com.ralap.rpc;

import com.ralap.rpc.commen.RpcRequest;
import com.ralap.rpc.commen.RpcResponse;
import com.ralap.rpc.registry.ServiceDiscover;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author: ralap
 * @date: created at 2018/9/6 14:28
 */
public class PRCProxy {

    /*服务地址*/
    private String serverAddress;
    private ServiceDiscover serviceDiscover;

    public PRCProxy(ServiceDiscover serviceDiscover) {
        this.serviceDiscover = serviceDiscover;
    }


    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass},
                (proxy, method, args) -> {

                    RpcRequest request = new RpcRequest();
                    request.setRequestId(UUID.randomUUID().toString());
                    request.setClassName(method.getDeclaringClass().getName());
                    request.setMethodName(method.getName());
                    request.setParameters(args);
                    request.setParameterTypes(method.getParameterTypes());
                    if (serviceDiscover != null) {
                        serverAddress = serviceDiscover.discover();
                    }
                    String[] array = serverAddress.split(":");
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    RPCClient client = new RPCClient(host, port);
                    RpcResponse response = client.send(request);
                    if (response.isError()) {
                        throw response.getError();
                    } else {
                        return response;
                    }
                });
    }
}
