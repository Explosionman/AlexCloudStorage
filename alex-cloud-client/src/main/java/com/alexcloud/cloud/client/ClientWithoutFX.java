package com.alexcloud.cloud.client;

import java.util.concurrent.CountDownLatch;

public class ClientWithoutFX {

    public static void start() throws InterruptedException {
        System.out.println("Начало работы клиента");
        CountDownLatch cdl = new CountDownLatch(1);
        System.out.println("Создан КДЛ, далее новый поток для соединения");
        new Thread(() -> ClientNetwork.getInstance().start(cdl)).start();
        System.out.println("Создан поток, переход к await");
        cdl.await();
    }
}
