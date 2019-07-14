package com.oceanli.netty.rpc.registry;

import com.oceanli.netty.rpc.protocol.InvokerProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcRegistryHandler extends ChannelInboundHandlerAdapter {

    List<String> classNames = new ArrayList<>();
    Map<String, Object> registryMap = new ConcurrentHashMap<>();

    //1.根据一个包名将所有符合条件的 class全部扫描出来，放到一个容器中

    public RpcRegistryHandler() {

        scanClass("com.oceanli.netty.rpc.provider");
        registryService();
    }

    private void registryService() {
        //2.给每个class起一个唯一的名称，作为服务名称，放到一个容器中
        if (classNames == null || classNames.size() == 0) {
            return;
        }
        for (String className: classNames) {
            try {

                Class<?> clazz = Class.forName(className);
                Class<?> its = clazz.getInterfaces()[0];
                registryMap.put(its.getName(), clazz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scanClass(String packageName) {

        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File f: file.listFiles()) {
            if (f.isDirectory()) {
                scanClass(packageName + "." + f.getName());
            }
            String className = packageName + "." + f.getName().replace(".class", "");
            classNames.add(className);
        }

    }






    //有客户端连接上的时候会回调
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //3.当有一个链接过来时，就会获取协议内容 InvokerProtocol对象
        //4.要去注册号的容器里找到符合条件的服务
        Object result = null;
        if (msg instanceof InvokerProtocol) {
            InvokerProtocol request =  (InvokerProtocol) msg;
            if (registryMap.containsKey(request.getClassName())) {
                //5.通过远程调用provider得到返回结果，并回复给客户端
                Object service = registryMap.get(request.getClassName());
                String methodName = request.getMethodName();
                Method method = service.getClass().getMethod(methodName, request.getParams());
                result = method.invoke(service, request.getValues());
            }
            ctx.write(result);
            ctx.flush();
            ctx.close();
            System.out.println("客户端请求参数: " + msg + ", 服务端计算结果: " + result);
        }
    }

    //连接发生异常时回调
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
