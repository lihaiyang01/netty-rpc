package com.oceanli.netty.rpc.consumer.proxy;

import com.oceanli.netty.rpc.protocol.InvokerProtocol;
import com.oceanli.netty.rpc.registry.RpcRegistryHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxy {

    public static <T> T create(Class<T> clazz) {

        MethodProxy methodProxy = new MethodProxy(clazz);
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, methodProxy);
    }

    private static class MethodProxy implements InvocationHandler {
        Class clazz;

        public MethodProxy(Class clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }
            return rpcInvoker(proxy, method, args);
        }

        private Object rpcInvoker(Object proxy, Method method, Object[] args) {

            InvokerProtocol invokerProtocol = new InvokerProtocol();
            invokerProtocol.setClassName(clazz.getName());
            invokerProtocol.setMethodName(method.getName());
            invokerProtocol.setParams(method.getParameterTypes());
            invokerProtocol.setValues(args);
            //发起网络请求
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            final RpcConsumerHandler rpcConsumerHandler = new RpcConsumerHandler();

            try {


                Bootstrap client = new Bootstrap();
                client.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {

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
                                pipeline.addLast(rpcConsumerHandler);
                            }
                        });
                ChannelFuture f = client.connect("localhost", 8080).sync();
                f.channel().writeAndFlush(invokerProtocol).sync();
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
            return rpcConsumerHandler.getResponse();
        }
    }
}
