package com.alexcloud.cloud.client;

import com.alexcloud.cloud.client.callbacks.AuthCallback;
import com.alexcloud.cloud.client.callbacks.AuthFailedCallback;
import com.alexcloud.cloud.client.callbacks.FileReceivedCallback;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private FileReceivedCallback receivedCallback;
    private AuthCallback authCallback;
    private AuthFailedCallback authFailedCallback;

    public void setAuthCallback(AuthCallback authCallback) {
        this.authCallback = authCallback;
    }

    public void setAuthFailedCallback(AuthFailedCallback authFailedCallback) {
        this.authFailedCallback = authFailedCallback;
    }

    public void setReceivedCallback(FileReceivedCallback receivedCallback) {
        this.receivedCallback = receivedCallback;
    }

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private final byte SIGNAL_BYTE_IN = 25;
    private final byte SIGNAL_BYTE_AUTH_OK = 15;
    private final byte SIGNAL_BYTE_AUTH_FAILED = 10;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("работает channelRead");
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readAtTheMoment = buf.readByte();
                if (readAtTheMoment == SIGNAL_BYTE_IN) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    //Ответ от сервера, что пароль/логин не прошли проверку
                } else if (readAtTheMoment == SIGNAL_BYTE_AUTH_FAILED) {
                    setAuthFailedCallback(authFailedCallback);
                } else if (readAtTheMoment == SIGNAL_BYTE_AUTH_OK) {
                    setAuthCallback(authCallback);
                } else {
                    System.out.println("Произошла хрень в IDLE -> NAME_LENGTH");
                }
            }
            if (currentState == State.NAME_LENGTH) {
                //Длина имени файла будет в интовой (что будет больше 4 байт)
                if (buf.readableBytes() >= 4) {
                    System.out.println("Получаем длину названия файла");
                    nextLength = buf.readInt();
                    currentState = State.NAME;

                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("Получен файл: " + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream("./client-storage/" + new String(fileName, StandardCharsets.UTF_8)));
                    currentState = State.FILE_LENGTH;
                }
            }
            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    System.out.println("Получена длина файла " + fileLength);
                    fileLength = buf.readLong();
                    currentState = State.FILE;
                }
            }
            if (currentState == State.FILE) {
                if (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        System.out.println("Файл получен");
                        try {
                            Thread.sleep(2000);
                            receivedCallback.callback();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        out.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
