package com.alexcloud.cloud.client.controllers;

import com.alexcloud.cloud.client.ClientNetwork;
import com.alexcloud.cloud.common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    @FXML
    VBox clientPanel, serverPanel;

    private ClientPanelController clientPC;
    private ServerPanelController serverPC;

    public void btnDownloadAction(ActionEvent actionEvent) {
        clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) 35);
        System.out.println("Оправили сигнальный байт 35");
        ClientNetwork.getInstance().getCurrentChannel().writeAndFlush(buf);
        ClientNetwork.getInstance().setOnReceivedCallback(() -> {
            clientPC.updateFileList(Paths.get(clientPC.getCurrentPath()));
        });
    }

    public void menuItemExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnLoadAction(ActionEvent actionEvent) throws IOException {
        clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        serverPC = (ServerPanelController) serverPanel.getProperties().get("serverCtrl");

        if (clientPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Ни один файл не выбран!");
            alert.showAndWait();
            return;
        }
        Path srsPath = Paths.get(clientPC.getCurrentPath(), clientPC.getSelectedFilename());

        FileSender.sendFile(srsPath, ClientNetwork.getInstance().getCurrentChannel(), channelFuture -> {
            if (!channelFuture.isSuccess()) {
                channelFuture.cause().printStackTrace();
                ClientNetwork.getInstance().stop();
            }
            if (channelFuture.isSuccess()) {
                System.out.println("Файл успешно передан!");
                System.out.println("Спим");
                Thread.sleep(2000);
                System.out.println("Проснулись");
                serverPC.updateFileList(Paths.get("./server-storage/Client1"));
                System.out.println("Обновились");
            }
        });
    }
}
