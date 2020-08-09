package com.alexcloud.cloud.client.controllers;

import com.alexcloud.cloud.client.FileInfo;
import com.alexcloud.cloud.client.Main;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ServerPanelController implements Initializable {
    @FXML
    TableView<FileInfo> serverFilesTable;

    @FXML
    VBox clientPanel;

    @FXML
    TextField pathFiled;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(30);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        filenameColumn.setPrefWidth(200);

        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("Размер");
        sizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        sizeColumn.setPrefWidth(120);

        sizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
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

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        serverFilesTable.getColumns().addAll(fileTypeColumn, filenameColumn, sizeColumn, fileDateColumn);
        serverFilesTable.getSortOrder().add(fileTypeColumn);

//

//        serverFilesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (event.getClickCount() == 2) {
//                    String root = "./server-storage/";
//                    Path path = Paths.get(root + (serverFilesTable.getSelectionModel().getSelectedItem().getFileName()));
//                    if (Files.isDirectory(path)) {
//                        updateFileList(path);
//                    }
//                }
//            }
//        });


        updateFileList(Paths.get(Controller.ROOT_PATH, Main.clientName));
    }

    public void updateFileList(Path path) {
        try {
            pathFiled.setText(Main.clientName + " storage");
            serverFilesTable.getItems().clear();
            serverFilesTable.getItems().addAll(Files.list(Paths.get(Controller.ROOT_PATH, Main.clientName)).map(FileInfo::new).collect(Collectors.toList()));
            serverFilesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
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

    public void btnDownloadAction(ActionEvent actionEvent) {

    }
}

