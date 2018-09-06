package com.ralap.rpc;

import com.ralap.rpc.commen.RpcDecoder;
import com.ralap.rpc.commen.RpcEncoder;
import com.ralap.rpc.commen.RpcRequest;
import com.ralap.rpc.commen.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端通讯
 * 1. 通过netty发送请求对象，接收结果
 * @author: ralap
 * @date: created at 2018/9/6 14:15
 */
@Slf4j
public class RPCClient extends SimpleChannelInboundHandler<RpcResponse> {

    /*服务地址*/
    private String host;
    /*服务端口*/
    private int port;
    private RpcResponse response;

    public RPCClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private Object obj = new Object();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response)
            throws Exception {
        this.response = response;
        //获取到结果唤起等待线程
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel)
                                throws Exception {
                            // 向pipeline中添加编码、解码、业务处理的handler
                            channel.pipeline()
                                    .addLast(new RpcEncoder(RpcRequest.class))  //OUT - 1
                                    .addLast(new RpcDecoder(RpcResponse.class)) //IN - 1
                                    .addLast(RPCClient.this);                   //IN - 2
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);
            // 链接服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            //将request对象写入outbundle处理后发出（即RpcEncoder编码器）
            future.channel().writeAndFlush(request).sync();

            // 用线程等待的方式决定是否关闭连接
            // 其意义是：先在此阻塞，等待获取到服务端的返回后，被唤醒，从而关闭网络连接
            synchronized (obj) {
                obj.wait();
            }
            if (response != null) {
                future.channel().closeFuture().sync();
            }
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught exception", cause);
        ctx.close();
    }
}
