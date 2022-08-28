package com.sec.chatting.server;

import com.sec.chatting.message.Request;
import com.sec.chatting.message.Response;
import com.sec.chatting.protocol.MagicCodec;
import com.sec.chatting.protocol.RequestDecoder;
import com.sec.chatting.protocol.ResponseEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {

    //账号：密码
    static Map<String, String> users = new HashMap<>(8);

    static {
        users.put("u1", "u1");
        users.put("u2", "u2");
        users.put("u3", "u3");
        users.put("u4", "u4");
        users.put("u5", "u5");
        users.put("u6", "u6");
    }

    //用户：通道
    static Map<String, Channel> utc = new HashMap<>(8);

    //通道：用户
    static Map<Channel, String> ctu = new HashMap<>(8);

    //组名：用户名集合
    static Map<String, Set<String>> groups = new HashMap<>();


    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new MagicCodec())
                                .addLast(new ResponseEncoder())
                                .addLast(new RequestDecoder())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        removeChannel();
                                        super.channelInactive(ctx);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        removeChannel();
//                                        super.exceptionCaught(ctx, cause);
                                    }

                                    private void removeChannel() {
                                        utc.remove(ctu.get(ch));
                                        ctu.remove(ch);
                                        ch.close();
                                    }

                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        Request request = (Request) msg;
                                        switch (request.getType()) {
                                            case LOGIN: {
                                                String username = request.getParameters()[0];
                                                String password = request.getParameters()[1];
                                                if (users.containsKey(username)) {
                                                    if (users.get(username).equals(password)) {
                                                        utc.put(username, ch);
                                                        ctu.put(ch, username);
                                                        ctx.writeAndFlush(new Response(true, "登录成功！"));
                                                        break;
                                                    }
                                                }
                                                ctx.writeAndFlush(new Response(false, "用户名或密码错误！"));
                                                break;
                                            }
                                            case SEND_ONE: {
                                                String to = request.getParameters()[0];
                                                String content = request.getParameters()[1];
                                                if (utc.containsKey(to)) {
                                                    utc.get(to).writeAndFlush(new Response(true, ctu.get(ch) + ":" + content));
                                                    ctx.writeAndFlush(new Response(true, "发送成功！"));
                                                } else {
                                                    ctx.writeAndFlush(new Response(false, "该用户不在线上！"));
                                                }
                                                break;
                                            }
                                            case SEND_GROUP: {
                                                String groupName = request.getParameters()[0];
                                                String content = request.getParameters()[1];
                                                if (groups.containsKey(groupName)) {
                                                    Set<String> users = groups.get(groupName);
                                                    users.forEach(user -> {
                                                        if (utc.containsKey(user)) {
                                                            if (!utc.get(user).equals(ch)) {
                                                                utc.get(user).writeAndFlush(new Response(true,
                                                                        groupName + "组消息：" + ctu.get(ch) + ":" + content));
                                                            }
                                                        }
                                                    });
                                                    ctx.writeAndFlush(new Response(true, "发送成功！"));
                                                } else {
                                                    ctx.writeAndFlush(new Response(false, "该组不存在！"));
                                                }
                                                break;
                                            }
                                            case JOIN_GROUP: {
                                                String groupName = request.getParameters()[0];
                                                if (groups.containsKey(groupName)) {
                                                    groups.get(groupName).add(ctu.get(ch));
                                                    ctx.writeAndFlush(new Response(true, "加入成功！"));
                                                    groups.get(groupName).forEach(user -> {
                                                        if (utc.containsKey(user)) {
                                                            utc.get(user).writeAndFlush(new Response(true, ctu.get(ch) + "加入群聊" + groupName));
                                                        }
                                                    });
                                                } else {
                                                    ctx.writeAndFlush(new Response(false, "该组不存在！"));
                                                }
                                                break;
                                            }
                                            case QUIT_GROUP: {
                                                String groupName = request.getParameters()[0];
                                                if (groups.containsKey(groupName)) {
                                                    groups.get(groupName).remove(ctu.get(ch));
                                                    ctx.writeAndFlush(new Response(true, "退出成功！"));
                                                    groups.get(groupName).forEach(user -> {
                                                        if (utc.containsKey(user)) {
                                                            utc.get(user).writeAndFlush(new Response(true, ctu.get(ch) + "退出群聊" + groupName));
                                                        }
                                                    });
                                                } else {
                                                    ctx.writeAndFlush(new Response(false, "该组不存在！"));
                                                }
                                                break;
                                            }
                                            case CREATE_GROUP: {
                                                String groupName = request.getParameters()[0];
                                                if (!groups.containsKey(groupName)) {
                                                    Set<String> users = new HashSet<>();
                                                    for (int i = 1; i < request.getParameters().length; i++) {
                                                        if (utc.containsKey(request.getParameters()[i])) {
                                                            users.add(request.getParameters()[i]);
                                                        }
                                                    }
                                                    users.add(ctu.get(ch));
                                                    groups.put(groupName, users);
                                                    ctx.writeAndFlush(new Response(true, "创建成功！"));
                                                } else {
                                                    ctx.writeAndFlush(new Response(false, "该组已经存在！"));
                                                }
                                                break;
                                            }
                                            case EXIT: {
                                                removeChannel();
                                                break;
                                            }
                                            default:
                                                break;
                                        }
                                    }
                                });
                    }
                }).bind(9999);

    }
}
