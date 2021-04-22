package com.alexcloud.cloud.client;

import com.alexcloud.cloud.client.callbacks.FileListReceivedCallback;
import com.alexcloud.cloud.client.callbacks.FileReceivedCallback;
import com.alexcloud.cloud.client.callbacks.UserAlreadyExistsCallback;
import com.alexcloud.cloud.client.callbacks.UserSuccessfullyCreatedCallback;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CountDownLatch;

public class ClientNetwork {
    private static ClientNetwork ourInstance = new ClientNetwork();

    public static ClientNetwork getInstance() {
        return ourInstance;
    }

    final String HOST = "localhost";
    final int PORT = 8189;

    private ClientNetwork() {
    }

    private Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setOnReceivedCallback(FileReceivedCallback onReceivedFileReceivedCallback) {
        currentChannel.pipeline().get(ClientHandler.class).setReceivedCallback(onReceivedFileReceivedCallback);
    }

    public void setOnFileListReceivedCallback(FileListReceivedCallback onFileListReceivedCallback) {
        currentChannel.pipeline().get(ClientHandler.class).setFileListReceivedCallback(onFileListReceivedCallback);
    }

    public void setOnUserAlreadyExistsCallback(UserAlreadyExistsCallback onUserAlreadyExistsCallback) {
        currentChannel.pipeline().get(ClientHandler.class).setUserAlreadyExistsCallback(onUserAlreadyExistsCallback);
    }

    public void setOnUserSuccessfullyCreatedCallback(UserSuccessfullyCreatedCallback onUserSuccessfullyCreatedCallback) {
        currentChannel.pipeline().get(ClientHandler.class).setUserSuccessfullyCreatedCallback(onUserSuccessfullyCreatedCallback);
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
                            currentChannel = channel;
                            currentChannel.pipeline().addLast(new ClientHandler());
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
