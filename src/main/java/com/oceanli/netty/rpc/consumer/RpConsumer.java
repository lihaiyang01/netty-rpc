package com.oceanli.netty.rpc.consumer;

import com.oceanli.netty.rpc.api.IRpcService;
import com.oceanli.netty.rpc.consumer.proxy.RpcProxy;

public class RpConsumer {

    public static void main(String[] args) {

        IRpcService rpcService = RpcProxy.create(IRpcService.class);
        System.out.println("8 + 2 = " + rpcService.add(8, 2));
        System.out.println("8 - 2 = " + rpcService.sub(8, 2));
        System.out.println("8 * 2 = " + rpcService.mult(8, 2));
        System.out.println("8 / 2 = " + rpcService.div(8, 2));
    }
}
