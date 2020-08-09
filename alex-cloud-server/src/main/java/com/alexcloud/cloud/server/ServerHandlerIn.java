package com.alexcloud.cloud.server;

import com.alexcloud.cloud.common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class ServerHandlerIn extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private final byte SIGNAL_BYTE_AUTH = 15;
    private final byte SIGNAL_BYTE_IN = 25;
    private final byte SIGNAL_BYTE_OUT = 35;

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
                //Если получаем сигнал на приём файла
                if (readAtTheMoment == SIGNAL_BYTE_IN) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;

                    //Если получаем сигнал на отдачу файла
                } else if (readAtTheMoment == SIGNAL_BYTE_OUT) {
                    FileSender.sendFile(Paths.get("./server-storage/Client1/11.txt"), ctx.channel(), channelFuture -> {
                        if (!channelFuture.isSuccess()) {
                            channelFuture.cause().printStackTrace();
                        }
                        if (channelFuture.isSuccess()) {
                            System.out.println("Файл успешно передан!");
                            System.out.println("Спим");
                            Thread.sleep(2000);
                            System.out.println("Проснулись");
                            System.out.println("Нормально обновились");
                        }
                    });

                    //Исли получаем байт на авторизацию
                } else if (readAtTheMoment == SIGNAL_BYTE_AUTH) {
                    System.out.println("Получен байт авторизации");
                    receivedFileLength = 0L;
                    nextLength = buf.readInt();
                    byte[] accPass = new byte[nextLength];
                    buf.readBytes(accPass);
                    String[] checkBeforeEnter = new String(accPass, StandardCharsets.UTF_8).split("Auth");
                    String accName = checkBeforeEnter[0];
                    String pwd = checkBeforeEnter[1];
                    String confirmedName = SqlClient.getNickname(accName, pwd);


                } else {
                    System.out.println("Произошла неведомая хрень в IDLE -> NAME_LENGTH");
                }
            }
            if (currentState == State.NAME_LENGTH) {
                //Длина имени файла будет в интовой (что будет больше 4 байт)
                if (buf.readableBytes() >= 4) {
                    System.out.println("Получаем длину названия");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("Получен файл: " + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream("./server-storage/Client1/" + new String(fileName, StandardCharsets.UTF_8)));
                    currentState = State.FILE_LENGTH;
                }
            }
            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("Получена длина файла " + fileLength);
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

    public void authAnswer(String confirmedName, Channel channel) {
        byte signalByte = 15;
        if (confirmedName == null) signalByte = 10;
        //Если клиент есть в базе и пароль совпал, передаём клиенту название его папки, которая равна его логину
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1);
        buffer.writeByte(signalByte);
        System.out.println("Записали сигнальный байт " + signalByte + " , 15 - успешная проверка, 10 - введены некорректные данные");
        channel.writeAndFlush(buffer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
