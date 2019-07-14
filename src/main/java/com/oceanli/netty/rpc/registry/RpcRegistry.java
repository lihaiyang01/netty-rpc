package com.oceanli.netty.rpc.registry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RpcRegistry {

    private int port;

    public RpcRegistry(int port) {
        this.port = port;
    }

    public void start() {

        //主线程池初始化
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //子线程池初始化
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //客户端初始化处理
                        @Override
                        protected void initChannel(SocketChannel client) throws Exception {
                            //在netty中，把所有业务逻辑的处理全部归总在一个队列中
                            //这个队列包含了各种各样的处理逻辑，对这些逻辑在netty中有一个封装
                            //封装成一个对象,无锁化串行任务队列, pipeline
                            ChannelPipeline pipeline = client.pipeline();
                            //自定义编码器
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0 ,4));
                            //自定义解码器
                            pipeline.addLast(new LengthFieldPrepender(4));
                            //实参处理
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
                            //执行属于自己的逻辑
                            //1.注册：给每个对象起一个名字，对外提供服务名字
                            //2.服务位置做一个登记
                            pipeline.addLast(new RpcRegistryHandler());

                        }
                    })
                    //针对主线程的配置 分配线程最大数量128
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //针对子线程的配置 保持长链接
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            //启动服务器
            ChannelFuture f = server.bind(this.port).sync();
            System.out.println("GP RPC Registry start listen at " + this.port);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        new RpcRegistry(8080).start();
    }
}
