package com.alexcloud.cloud.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class ClientNetwork {
    private static ClientNetwork ourInstance = new ClientNetwork();

    public static ClientNetwork getInstance() {
        return ourInstance;
    }

    final String HOST = "localhost";
    final int PORT = 8189;

    private ClientNetwork() {
        System.out.println("Конструктор");
    }

    private Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
        System.out.println("Начало работы метода start в ClientNetwork");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            // channel.pipeline().addLast();
                            currentChannel = channel;
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect(HOST, PORT).sync();
            System.out.println("Клиент подключился к серверу");
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        currentChannel.close();
    }
}
