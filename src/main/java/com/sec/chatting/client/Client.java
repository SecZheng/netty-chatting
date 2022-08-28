package com.sec.chatting.client;

import com.sec.chatting.message.Request;
import com.sec.chatting.message.RequestType;
import com.sec.chatting.message.Response;
import com.sec.chatting.protocol.MagicCodec;
import com.sec.chatting.protocol.RequestEncoder;
import com.sec.chatting.protocol.ResponseDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class Client {
    public static void main(String[] args) {
        Thread main = Thread.currentThread();
        Scanner scanner = new Scanner(System.in);
        AtomicBoolean login = new AtomicBoolean(false);
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channel = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new MagicCodec())
                                .addLast(new ResponseDecoder())
                                .addLast(new RequestEncoder())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        Response response = (Response) msg;
                                        System.out.println(response.getMessage());
                                        if (main.isAlive()) {
                                            if (response.isSuccess()) {
                                                login.set(true);
                                                printMenuAndSendMessage(ch);
                                            }
                                            LockSupport.unpark(main);
                                        }
                                    }

                                    void printMenuAndSendMessage(NioSocketChannel ch) {
                                        new Thread(() -> {
                                            System.out.println("-------------");
                                            System.out.println("send username message");
                                            System.out.println("gsend groupName message");
                                            System.out.println("create groupName u1 u2 ...");
                                            System.out.println("join groupName");
                                            System.out.println("quit groupName");
                                            System.out.println("exit");
                                            System.out.println("-------------");
                                            while (true) {
                                                Request request = Request.create(scanner.nextLine());
                                                ch.writeAndFlush(request);
                                                if (request.getType() == RequestType.EXIT) {
                                                    group.shutdownGracefully();
                                                    break;
                                                }
                                            }
                                        }, "sendThread").start();
                                    }
                                });
                    }
                }).connect("localhost", 9999);

        try {
            Channel ch = channel.sync().channel();
            while (!login.get()) {
                System.out.println("正在登录·····");
                System.out.println("请输入用户名：");
                String username = scanner.nextLine();
                System.out.println("请输入密码：");
                String password = scanner.nextLine();
                ch.writeAndFlush(new Request(RequestType.LOGIN, username, password));
                LockSupport.park(main);
            }
        } catch (Exception e) {
            try {
                System.out.println("服务端异常，3秒后自动关闭");
                group.shutdownGracefully().sync();
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
            }
        }
    }
}
