package com.oceanli.netty.rpc.provider;

import com.oceanli.netty.rpc.api.IRpcHelloService;

public class RpcHelloServiceImpl implements IRpcHelloService {
    @Override
    public String sayHello(String name) {
        return "hello, " + name;
    }
}
