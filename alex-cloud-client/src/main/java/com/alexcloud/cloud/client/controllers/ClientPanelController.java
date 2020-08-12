package com.alexcloud.cloud.client.controllers;

import com.alexcloud.cloud.client.filesInfo.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientPanelController implements Initializable {

    public static String actualClientPath;

    @FXML
    TableView<FileInfo> clientFilesTable;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField pathFiled;

    Path rootPath = null;

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

        clientFilesTable.getColumns().addAll(fileTypeColumn, filenameColumn, sizeColumn, fileDateColumn);
        clientFilesTable.getSortOrder().add(fileTypeColumn);

        disksBox.getItems().clear();

        int i = 0;
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            if (i < 1) {
                rootPath = p.normalize();
                i++;
            }
            disksBox.getItems().add(p.toString());
        }

        disksBox.getSelectionModel().select(0);

        clientFilesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathFiled.getText()).resolve(clientFilesTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        actualClientPath = path.toString();
                        updateFileList(path);
                    }
                }
            }
        });
        System.out.println("Первый найденный диск из списка: " + rootPath.toString());
        updateFileList(Paths.get(rootPath.toString()));
    }

    public void updateFileList(Path path) {
        try {
            pathFiled.setText(path.normalize().toAbsolutePath().toString());
            clientFilesTable.getItems().clear();
            clientFilesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientFilesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnUpPathAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathFiled.getText()).getParent();
        if (upperPath != null) {
            updateFileList(upperPath);
        }
    }

    public void selectDiskOnAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateFileList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedFilename() {
        if (!clientFilesTable.isFocused()) {
            return null;
        }
        return clientFilesTable.getSelectionModel().getSelectedItem().getFileName();
    }

    public String getCurrentPath() {
        System.out.println("Это путь, который пока не работает корректно: " + pathFiled.getText());
        return pathFiled.getText();
    }
}
