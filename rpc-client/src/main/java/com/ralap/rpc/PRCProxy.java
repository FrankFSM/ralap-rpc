package com.ralap.rpc;

import com.ralap.rpc.commen.RpcRequest;
import com.ralap.rpc.commen.RpcResponse;
import com.ralap.rpc.registry.ServiceDiscover;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * 客户端代理
 * 用于通过代理，模拟调用相关方法
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

    /**
     * 创建代理类
     *
     * @param interfaceClass 接口对象
     * 1. 创建动态代理
     * 2. 封装request对象
     * 3. 通过服务发现，获取服务地址
     * 4. 通过框架client发送请求
     * 5. 接收结果返回
     * @param <T> 结果类型
     * @return 结果
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        return (T) Proxy
                .newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass},
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
                                return response.getResult();
                            }
                        });
    }
}
