package com.alexcloud.cloud.client;

import com.alexcloud.cloud.client.callbacks.FileListReceivedCallback;
import com.alexcloud.cloud.client.callbacks.FileReceivedCallback;
import com.alexcloud.cloud.client.callbacks.UserAlreadyExistsCallback;
import com.alexcloud.cloud.client.callbacks.UserSuccessfullyCreatedCallback;
import com.alexcloud.cloud.client.controllers.ClientPanelController;
import com.alexcloud.cloud.client.controllers.Controller;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private FileReceivedCallback receivedCallback;
    private FileListReceivedCallback fileListReceivedCallback;
    private UserSuccessfullyCreatedCallback userSuccessfullyCreatedCallback;
    private UserAlreadyExistsCallback userAlreadyExistsCallback;
    public static String fileList;

    public void setReceivedCallback(FileReceivedCallback receivedCallback) {
        this.receivedCallback = receivedCallback;
    }

    public void setFileListReceivedCallback(FileListReceivedCallback fileListReceivedCallback) {
        this.fileListReceivedCallback = fileListReceivedCallback;
    }

    public void setUserAlreadyExistsCallback(UserAlreadyExistsCallback userAlreadyExistsCallback) {
        this.userAlreadyExistsCallback = userAlreadyExistsCallback;
    }

    public void setUserSuccessfullyCreatedCallback(UserSuccessfullyCreatedCallback userSuccessfullyCreatedCallback) {
        this.userSuccessfullyCreatedCallback = userSuccessfullyCreatedCallback;
    }

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private final byte SIGNAL_BYTE_CREATE_USER_OK = 7;
    private final byte SIGNAL_BYTE_CREATE_USER_FAILED = 9;
    private final byte SIGNAL_BYTE_IN = 25;
    private final byte SIGNAL_BYTE_AUTH_OK = 15;
    private final byte SIGNAL_BYTE_AUTH_FAILED = 10;
    private final byte SIGNAL_BYTE_UPDATE = 45;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("работает channelRead  (Хэндлер)");
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readAtTheMoment = buf.readByte();
                if (readAtTheMoment == SIGNAL_BYTE_IN) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    //Ответ от сервера, что пароль/логин не прошли проверку
                } else if (readAtTheMoment == SIGNAL_BYTE_AUTH_FAILED) {
                    System.out.println("Аутентификация провалена!!! (Хэндлер)");
                    Main.authFailed = true;
                    currentState = State.IDLE;
                } else if (readAtTheMoment == SIGNAL_BYTE_AUTH_OK) {
                    System.out.println("Аутентификация успешно пойдена (Хэндлер)");
                    nextLength = buf.readInt();
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    fileList = new String(fileName, StandardCharsets.UTF_8);
                    System.out.println("Получен fileList: " + fileList);
                    Main.authOK = true;
                    currentState = State.IDLE;
                } else if (readAtTheMoment == SIGNAL_BYTE_UPDATE) {
                    nextLength = buf.readInt();
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    fileList = new String(fileName, StandardCharsets.UTF_8);
                    System.out.println("Получен fileList: " + fileList);
                    fileListReceivedCallback.callback();
                    currentState = State.IDLE;
                } else if (readAtTheMoment == SIGNAL_BYTE_CREATE_USER_FAILED) {
                    System.out.println("Получен сигнал о том, что такой пользователь уже существует");
                    userAlreadyExistsCallback.callback();
                } else if (readAtTheMoment == SIGNAL_BYTE_CREATE_USER_OK) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            userSuccessfullyCreatedCallback.callback();
                        }
                    });

                } else {
                    System.out.println("Произошла хрень в IDLE -> NAME_LENGTH (Хэндлер)");
                }
            }
            if (currentState == State.NAME_LENGTH) {
                //Длина имени файла будет интовой (что будет больше 4 байт)
                if (buf.readableBytes() >= 4) {
                    System.out.println("Получаем длину названия файла (Хэндлер)");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("Получен файл: " + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(ClientPanelController.actualClientPath + "//" + new String(fileName, StandardCharsets.UTF_8)));
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
