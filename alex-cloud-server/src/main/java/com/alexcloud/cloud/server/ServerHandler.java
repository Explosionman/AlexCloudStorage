package com.alexcloud.cloud.server;

import com.alexcloud.cloud.common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private final byte SIGNAL_BYTE_CREATE_USER = 5;
    private final byte SIGNAL_BYTE_CREATE_USER_OK = 7;
    private final byte SIGNAL_BYTE_CREATE_USER_FAILED = 9;
    private final byte SIGNAL_BYTE_AUTH_FAILED = 10;
    private final byte SIGNAL_BYTE_AUTH = 15;
    private final byte SIGNAL_BYTE_IN = 25;
    private final byte SIGNAL_BYTE_OUT = 35;
    private final byte SIGNAL_BYTE_UPDATE = 45;
    private static final String ROOT_PATH = "./server-storage/";
    private String confirmedName;
    private String confirmedRootName;
    private StringBuffer sb = new StringBuffer();

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
                    nextLength = buf.readInt();
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    String name = new String(fileName, StandardCharsets.UTF_8);
                    System.out.println("Это имя файла, который запросил клиент" + name);

                    FileSender.sendFile(Paths.get(ROOT_PATH + confirmedRootName + new String(fileName, StandardCharsets.UTF_8)), ctx.channel(), channelFuture -> {
                        if (!channelFuture.isSuccess()) {
                            channelFuture.cause().printStackTrace();
                        }
                        if (channelFuture.isSuccess()) {
                            System.out.println("Файл успешно передан!");
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
                    System.out.println("Проверка данных введенных пользователем");
                    confirmedName = SqlClient.getNickname(accName, pwd);
                    if (confirmedName != null) {
                        String fileList = getFileList(ROOT_PATH + confirmedName + "//");
                        System.out.println("Список файлов в папке " + confirmedName + ": " + fileList);
                        authAnswer(ctx.channel(), fileList);
                    } else {
                        authFailedAnswer(ctx.channel());
                    }
                    confirmedRootName = confirmedName + "//";
                } else if (readAtTheMoment == SIGNAL_BYTE_UPDATE) {
                    updateFileList(ctx.channel(), getFileList(ROOT_PATH + confirmedName + "//"));
                } else if (readAtTheMoment == SIGNAL_BYTE_CREATE_USER) {
                    System.out.println("Получен сигнальный байт для регистрации");
                    nextLength = buf.readInt();
                    byte[] accPass = new byte[nextLength];
                    buf.readBytes(accPass);
                    String[] dataBeforeRegister = new String(accPass, StandardCharsets.UTF_8).split("SEPARATOR");
                    System.out.println("Получена строка для регистрации: " + Arrays.toString(dataBeforeRegister));
                    String accName = dataBeforeRegister[0];
                    String pwd = dataBeforeRegister[1];
                    System.out.println("Проверка данных введенных пользователем");
                    if (SqlClient.checkUserExists(accName) == false) {
                        SqlClient.createUser(accName, pwd);
                        makeNewDir(accName);
                        System.out.println("Пользователь успешно создан!");
                        System.out.println("Отправлен соответствующий сигнал!");
                        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1);
                        buffer.writeByte(SIGNAL_BYTE_CREATE_USER_OK);
                        ctx.writeAndFlush(buffer);
                    } else {
                        System.out.println("Такой пользователь уже существует!");
                        System.out.println("Отправлен соответствующий сигнал!");
                        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1);
                        buffer.writeByte(SIGNAL_BYTE_CREATE_USER_FAILED);
                        ctx.writeAndFlush(buffer);
                    }
                    currentState = State.FILE_LENGTH;
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
                    out = new BufferedOutputStream(new FileOutputStream(ROOT_PATH + confirmedRootName + new String(fileName, StandardCharsets.UTF_8)));
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

    public void authAnswer(Channel channel, String fileList) {
        byte[] fileListBytes = fileList.getBytes(StandardCharsets.UTF_8);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + fileListBytes.length);
        buffer.writeByte(SIGNAL_BYTE_AUTH);
        buffer.writeInt(fileListBytes.length);
        buffer.writeBytes(fileListBytes);
        channel.writeAndFlush(buffer);
        System.out.println("Записали и отправили сигнальный байт  " + SIGNAL_BYTE_AUTH + "  - успешная проверка + список файлов");
    }

    public void authFailedAnswer(Channel channel) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1);
        byte signalByte = SIGNAL_BYTE_AUTH_FAILED;
        buffer.writeByte(signalByte);
        channel.writeAndFlush(buffer);
        System.out.println("Записали и отправили сигнальный байт " + signalByte + " - введены некорректные данные");
    }

    public void updateFileList(Channel channel, String fileList) {
        byte[] fileListBytes = fileList.getBytes(StandardCharsets.UTF_8);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + fileListBytes.length);
        buffer.writeByte(SIGNAL_BYTE_UPDATE);
        buffer.writeInt(fileListBytes.length);
        buffer.writeBytes(fileListBytes);
        channel.writeAndFlush(buffer);
        System.out.println("Записали и отправили сигнальный байт  " + SIGNAL_BYTE_UPDATE + "  - успешная проверка + список файлов");
    }


    public String getFileList(String pathToFile) {
        String filePath;
        File filesDir = new File(pathToFile);
        sb.setLength(0);
        File[] dir = filesDir.listFiles();
        for (int i = 0; i < dir.length; i++) {
            if (dir[i].isFile()) {
                filePath = pathToFile + "//" + dir[i].getName();
                try {
                    sb.append(dir[i].getName()).append("SEPARATOR").append(Files.size(Paths.get(filePath))).append("NEXT_FILE");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public void makeNewDir(String userName) {
        File f = new File(ROOT_PATH + userName);
        f.mkdir();
        String data = "Спасибо, что используете Alex`s cloud storage!";
        try {
            FileOutputStream out = new FileOutputStream(ROOT_PATH + userName + "//" + "welcome.txt");
            out.write(data.getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
