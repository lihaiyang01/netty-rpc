package com.oceanli.netty.rpc.consumer.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RpcConsumerHandler extends ChannelInboundHandlerAdapter {
    private Object response;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        this.response = msg;
        System.out.println("客户端获取结果: " + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        System.out.println("client is exception ");
    }

    public Object getResponse() {
        return response;
    }
}
