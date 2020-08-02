package com.alexcloud.cloud.client;

import com.alexcloud.cloud.common.FileSender;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class TestClientWithoutFX {

    public static void start() throws InterruptedException {
        System.out.println("Начало работы клиента");
        CountDownLatch cdl = new CountDownLatch(1);
        System.out.println("Создан КДЛ, далее новый поток для соединения");
        new Thread(() -> ClientNetwork.getInstance().start(cdl)).start();
        System.out.println("Создан поток, переход к await");
        cdl.await();
    }
//        FileSender.sendFile(Paths.get("1.txt"), ClientNetwork.getInstance().getCurrentChannel(), channelFuture -> {
//            if (!channelFuture.isSuccess()) {
//                channelFuture.cause().printStackTrace();
//                ClientNetwork.getInstance().stop();
//            }
//            if (channelFuture.isSuccess()) {
//                System.out.println("Файл успешно передан!");
//                ClientNetwork.getInstance().stop();
//            }
//        });
}
