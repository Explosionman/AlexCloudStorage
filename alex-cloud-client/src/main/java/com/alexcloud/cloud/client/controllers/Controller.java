package com.alexcloud.cloud.client.controllers;

import com.alexcloud.cloud.client.ClientHandler;
import com.alexcloud.cloud.client.ClientNetwork;
import com.alexcloud.cloud.client.Main;
import com.alexcloud.cloud.common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    private final byte SIGNAL_BYTE_UPDATE = 45;
    private String selectedFile;
    public static final String ROOT_PATH = "./client-storage/";

    @FXML
    VBox clientPanel, serverPanel;

    private ClientPanelController clientPC;
    private ServerPanelController serverPC;

    public void btnDownloadAction(ActionEvent actionEvent) {
        clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        serverPC = (ServerPanelController) serverPanel.getProperties().get("serverCtrl");
        selectedFile = serverPC.getSelectedFilename();
        byte[] filenameBytes = selectedFile.getBytes(StandardCharsets.UTF_8);

        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
        buf.writeByte((byte) 35);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        System.out.println("Оправили сигнальный байт 35 и данные запрашиваемом файле");

        ClientNetwork.getInstance().getCurrentChannel().writeAndFlush(buf);
        ClientNetwork.getInstance().setOnReceivedCallback(() -> {
            clientPC.updateFileList(Paths.get(clientPC.getCurrentPath()));
        });
    }

    public void menuItemExitAction(ActionEvent actionEvent) {
        ClientNetwork.getInstance().stop();
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
            }
            if (channelFuture.isSuccess()) {
                System.out.println("Файл успешно передан!");
                System.out.println("Спим");
                Thread.sleep(1000);
                System.out.println("Проснулись");
                ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                buf.writeByte(SIGNAL_BYTE_UPDATE);
                ClientNetwork.getInstance().getCurrentChannel().writeAndFlush(buf);
            }
            ClientNetwork.getInstance().setOnFileListReceivedCallback(() -> {
                serverPC.updateFileList(ClientHandler.fileList);
            });
        });
    }
}
