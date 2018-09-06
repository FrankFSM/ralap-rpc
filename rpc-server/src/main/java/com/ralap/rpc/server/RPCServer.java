package com.ralap.rpc.server;

import com.ralap.rpc.commen.RpcDecoder;
import com.ralap.rpc.commen.RpcEncoder;
import com.ralap.rpc.commen.RpcRequest;
import com.ralap.rpc.commen.RpcResponse;
import com.ralap.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

/**
 * @author: ralap
 * @date: created at 2018/9/6 9:59
 */
@Slf4j
public class RPCServer implements ApplicationContextAware, InitializingBean {

    private String serverAddress;
    private ServiceRegistry serviceRegistry;

    private Map<String, Object> handleMap = new HashMap<String, Object>();


    public RPCServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 收集所有标注了RPCService主键的类
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        //获取所有添加了注解的类
        Map<String, Object> beans = applicationContext
                .getBeansWithAnnotation(RPCService.class);
        if (!CollectionUtils.isEmpty(beans)) {
            for (Object bean : beans.values()) {
                handleMap.put(bean.getClass().getName(), bean);
            }
        }
    }

    /**
     * 启动netty服务，绑定流水线handler
     * 1. 接收请求 反序列化到request中；
     * 2. 根据request，反射调用对象的方法，接收返回结果
     * 3. 将结果序列化封装到response中返回到客户端中
     */
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel)
                                throws Exception {
                            channel.pipeline()
                                    .addLast(new RpcDecoder(RpcRequest.class))// 注册解码 IN-1
                                    .addLast(new RpcEncoder(RpcResponse.class))// 注册编码 OUT
                                    .addLast(new RPCHandler(handleMap));//注册RpcHandler IN-2
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            log.debug("server started on port {}", port);

            if (serviceRegistry != null) {
                serviceRegistry.registry(serverAddress);
            }

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


    }
}
