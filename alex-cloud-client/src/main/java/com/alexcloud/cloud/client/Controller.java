package com.alexcloud.cloud.client;

import com.alexcloud.cloud.common.FileSender;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {

    @FXML
    TextField login, password;

    @FXML
    VBox clientPanel, serverPanel;

    public void menuItemExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnLoadAction(ActionEvent actionEvent) throws InterruptedException, IOException {
        TestClientWithoutFX.start();
        ClientPanelController clientPC = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        ServerPanelController serverPC = (ServerPanelController) serverPanel.getProperties().get("serverCtrl");

        if (clientPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Ни один файл не выбран!");
            alert.showAndWait();
            return;
        }
        Path srsPath = Paths.get(clientPC.getCurrentPath(), clientPC.getSelectedFilename());
        Path dstPath = Paths.get(serverPC.getCurrentPath());

        FileSender.sendFile(srsPath, ClientNetwork.getInstance().getCurrentChannel(), channelFuture -> {
            if (!channelFuture.isSuccess()) {
                channelFuture.cause().printStackTrace();
                ClientNetwork.getInstance().stop();
            }
            if (channelFuture.isSuccess()) {
                System.out.println("Файл успешно передан!");
                serverPC.updateFileList(Paths.get("./server-storage/Client1/"));
                ClientNetwork.getInstance().stop();
            }
        });
    }

//    public void btnLoginAction(ActionEvent actionEvent) throws IOException {
//        if (login.getText().equals("Alex") && password.getText().equals("aaa")) Main.setMainScene();
//    }
}
