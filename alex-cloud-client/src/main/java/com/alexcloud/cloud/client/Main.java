package com.alexcloud.cloud.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class Main extends Application {

    private static Stage stage;
    public static String clientName;
    public static boolean authOK = false;
    public static boolean authFailed = false;

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        stage = primaryStage;
        stage.setTitle("Alex`s Cloud Storage");

        //Честно скопировал расстановку кнопок и полей из интернета для входного меню вместе с css (подредактировал под себя)
        //не хотел тратить время на зарисовку, планирую перенести в FXML
        //остальное всё своё родное
        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(10, 50, 50, 50));

        //добавляем HBox
        HBox hb = new HBox();
        hb.setPadding(new Insets(20, 20, 20, 30));

        //добавляем GridPane
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20, 20, 20, 20));
        gridPane.setHgap(5);
        gridPane.setVgap(5);

        //Создаём элементы для GridPane
        Label lblUserName = new Label("Username");
        final TextField txtUserName = new TextField();
        Label lblPassword = new Label("Password");
        final PasswordField passwordField = new PasswordField();
        Button btnLogin = new Button("Login");
        final Label lblMessage = new Label();

        //Добавляем элементы на GridPane
        gridPane.add(lblUserName, 0, 0);
        gridPane.add(txtUserName, 1, 0);
        gridPane.add(lblPassword, 0, 1);
        gridPane.add(passwordField, 1, 1);
        gridPane.add(btnLogin, 2, 1);
        gridPane.add(lblMessage, 1, 2);

        //Отражение
        Reflection r = new Reflection();
        r.setFraction(0.7f);
        gridPane.setEffect(r);

        //Тень
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(5);
        dropShadow.setOffsetY(5);


        Text text = new Text("Alex`s Cloud Storage");
        text.setFont(Font.font("Courier New", FontWeight.BOLD, 28));
        text.setEffect(dropShadow);

        //Добавляем текст на HBox
        hb.getChildren().add(text);

        //Add ID's to Nodes
        bp.setId("bp");
        gridPane.setId("root");
        btnLogin.setId("btnLogin");
        text.setId("text");

        //Расставляем HBox и GridPane по BorderPane
        bp.setTop(hb);
        bp.setCenter(gridPane);

        lblMessage.setTextFill(Color.GREEN);
        lblMessage.setText("Введите логин и пароль для входа");

        //Добавляем BorderPane на сцену с подгрузкой css
        Scene scene = new Scene(bp);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("login.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        //Действие при нажатии btnLogin
        btnLogin.setOnAction(new EventHandler() {
            public void handle(Event event) {
                try {
                    ClientWithoutFX.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    String lp = txtUserName.getText() + "Auth" + passwordField.getText();

                    byte[] loginAndPassword = lp.getBytes(StandardCharsets.UTF_8);

                    ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + loginAndPassword.length);
                    buf.writeByte((byte) 15);
                    System.out.println("Записали сигнальный байт 15");

                    buf.writeInt(loginAndPassword.length);
                    System.out.println("Записали длину данных для входа");

                    buf.writeBytes(loginAndPassword);
                    System.out.println("Записали данные для входа");

                    ClientNetwork.getInstance().getCurrentChannel().writeAndFlush(buf);
                    System.out.println("Отправили данные для входа");
                    passwordField.setText("Аутентификация...");

                    while (authOK != true || authFailed != true) {
                        Thread.sleep(200);

                        if (authFailed == true) {
                            passwordField.setText("");
                            System.out.println("НЕВЕРНО УКАЗАН ПАРОЛЬ");
                            lblMessage.setText("Неверно указан логин или пароль.");
                            lblMessage.setTextFill(Color.RED);
                            authFailed = false;
                            break;
                        }
                        if (authOK == true) {
                            lblMessage.setText("Добро пожаловать, " + clientName);
                            lblMessage.setTextFill(Color.GREEN);
                            clientName = txtUserName.getText();

                            //После авторизации меняем сцену
                            setMainScene();
                            break;
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @FXML
    public static void setMainScene() throws IOException {
        System.out.println("Меняем сцену на главную");
        stage.hide();
        Parent root = FXMLLoader.load(Main.class.getResource("/main.fxml"));
        Scene scene = new Scene(root, 1200, 600);
        scene.getStylesheets().add(Main.class.getClassLoader().getResource("login.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
