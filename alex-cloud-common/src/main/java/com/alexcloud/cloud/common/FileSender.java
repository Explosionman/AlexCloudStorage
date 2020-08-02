package com.alexcloud.cloud.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {
    private static final byte SIGNAL_BYTE = 25;

    public static void sendFile(Path srcPath, Channel channel, ChannelFutureListener finishListener) throws IOException {
        System.out.println("Создаём регион");
        FileRegion region = new DefaultFileRegion(srcPath.toFile(), 0, Files.size(srcPath));
        byte[] filenameBytes = srcPath.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);

        buf.writeByte(SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(Files.size(srcPath));
        System.out.println("метка перед Flush");
        channel.writeAndFlush(buf);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        System.out.println("Transfer закончен");

        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }
}
