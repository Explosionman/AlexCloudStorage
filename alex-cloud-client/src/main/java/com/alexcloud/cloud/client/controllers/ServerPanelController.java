package com.alexcloud.cloud.client.controllers;

import com.alexcloud.cloud.client.ClientHandler;
import com.alexcloud.cloud.client.Main;
import com.alexcloud.cloud.client.filesInfo.ServerFileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ServerPanelController implements Initializable {

    List<ServerFileInfo> serverFiles = new ArrayList<>();

    @FXML
    TableView<ServerFileInfo> serverFilesTable;

    @FXML
    TextField pathFiled;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        TableColumn<ServerFileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        filenameColumn.setPrefWidth(200);

        TableColumn<ServerFileInfo, Long> sizeColumn = new TableColumn<>("Размер");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        sizeColumn.setPrefWidth(120);

        sizeColumn.setCellFactory(column -> {
            return new TableCell<ServerFileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        pathFiled.setText(Main.clientName + " storage");
        serverFilesTable.getColumns().addAll(filenameColumn, sizeColumn);
        serverFilesTable.getSortOrder().add(filenameColumn);
        serverFiles.clear();
        updateFileList(ClientHandler.fileList);
    }

    public void updateFileList(String fileList) {
            System.out.println("Работает updateFileList со списком файлов: " + fileList);
            String[] list = fileList.split("NEXT_FILE");
            System.out.println("В массиве List данные: " + Arrays.toString(list));
            String[] files;
            serverFiles.clear();
            serverFilesTable.getItems().clear();

            pathFiled.setText(Main.clientName + " storage");
            for (int i = 0; i < list.length; i++) {
                files = list[i].split("SEPARATOR");
                System.out.println("В массиве files данные: " + Arrays.toString(files));
                serverFiles.add(new ServerFileInfo(files[0], Long.valueOf(files[1])));
            }
            serverFilesTable.getItems().addAll(serverFiles);
            serverFilesTable.sort();
    }


    public String getSelectedFilename() {
        if (!serverFilesTable.isFocused()) {
            return null;
        }
        return serverFilesTable.getSelectionModel().getSelectedItem().getFileName();
    }

    public String getCurrentPath() {
        return Controller.ROOT_PATH + Main.clientName;
    }
}

