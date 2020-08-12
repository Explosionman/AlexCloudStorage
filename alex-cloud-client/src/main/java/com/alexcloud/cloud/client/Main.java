package com.alexcloud.cloud.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

public class Main extends Application {

    private final byte SIGNAL_BYTE_CREATE_USER = 5;
    private static Stage stage;
    public static String clientName;
    public static boolean authOK = false;
    public static boolean authFailed = false;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Alex`s Cloud Storage");

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
        Button btnRegister = new Button("Register");

        //Добавляем элементы на GridPane
        gridPane.add(lblUserName, 0, 0);
        gridPane.add(txtUserName, 1, 0);
        gridPane.add(lblPassword, 0, 1);
        gridPane.add(passwordField, 1, 1);
        gridPane.add(btnLogin, 2, 1);
        gridPane.add(lblMessage, 1, 2);
        gridPane.add(btnRegister, 2, 0);

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

        //Присваиваем ID
        bp.setId("bp");
        gridPane.setId("root");
        btnLogin.setId("btnLogin");
        btnRegister.setId("btnRegister");
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
                    lblMessage.setText("Аутентификация...");

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

        //Действие при нажатии btnRegister
        btnRegister.setOnAction(new EventHandler() {
            public void handle(Event event) {
                stage.hide();

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
                Label lblUserName = new Label("Введите логин");
                final TextField txtUserName = new TextField();
                Label lblPassword1 = new Label("Введите пароль");
                final PasswordField passwordField1 = new PasswordField();
                Label lblPassword2 = new Label("Повторите пароль");
                final PasswordField passwordField2 = new PasswordField();

                final Label regLblMessage = new Label();
                Button btnRegisterNow = new Button("Register now!");
                Button btnBack = new Button("Back to main menu");

                //Добавляем элементы на GridPane
                gridPane.add(lblUserName, 0, 0);
                gridPane.add(txtUserName, 1, 0);
                gridPane.add(lblPassword1, 0, 1);
                gridPane.add(passwordField1, 1, 1);
                gridPane.add(lblPassword2, 0, 2);
                gridPane.add(passwordField2, 1, 2);
                gridPane.add(regLblMessage, 1, 3);
                gridPane.add(btnRegisterNow, 1, 4);
                gridPane.add(btnBack, 0, 4);

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

                //Присваиваем ID
                bp.setId("regBp");
                gridPane.setId("regRoot");
                btnRegisterNow.setId("btnRegisterNow");
                text.setId("regText");
                btnBack.setId("btnBack");

                //Расставляем HBox и GridPane по BorderPane
                bp.setTop(hb);
                bp.setCenter(gridPane);

                //Действие при нажатии btnRegisterNow
                btnRegisterNow.setOnAction(new EventHandler() {
                    public void handle(Event event) {
                        try {
                            ClientWithoutFX.start();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        String newUserName = txtUserName.getText();
                        String password1 = passwordField1.getText();
                        String password2 = passwordField2.getText();
                        if (password1.equals(password2)) {
                            regLblMessage.setTextFill(Color.GREEN);
                            regLblMessage.setText("Ожидание ответа от сервера...");

                            String userData = newUserName + "SEPARATOR" + password2;
                            System.out.println("Эти данные будут отправлены на сервер: " + userData);

                            byte[] loginAndPassword = userData.getBytes(StandardCharsets.UTF_8);

                            ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + loginAndPassword.length);
                            buf.writeByte(SIGNAL_BYTE_CREATE_USER);
                            System.out.println("Записали сигнальный байт " + SIGNAL_BYTE_CREATE_USER);

                            buf.writeInt(loginAndPassword.length);
                            System.out.println("Записали длину данных для регистрации");

                            buf.writeBytes(loginAndPassword);
                            System.out.println("Записали данные для регистации");

                            ClientNetwork.getInstance().getCurrentChannel().writeAndFlush(buf);
                            System.out.println("Отправили данные для регистрации");

                            ClientNetwork.getInstance().setOnUserSuccessfullyCreatedCallback(() -> {
                                regLblMessage.setText("Аккаунт успешно создан!");
                                txtUserName.setText("");
                                passwordField1.setText("");
                                passwordField2.setText("");
                            });
                        } else {
                            regLblMessage.setTextFill(Color.RED);
                            regLblMessage.setText("Пароли не совпадают");
                        }
                        ClientNetwork.getInstance().setOnUserAlreadyExistsCallback(() -> {
                            regLblMessage.setText("Есть такое имя");
                        });
                    }
                });

                //Действие при нажатии btnBack
                btnBack.setOnAction(new EventHandler() {
                    public void handle(Event event) {
                        stage.hide();
                        stage.setScene(scene);
                        stage.setResizable(false);
                        stage.show();
                    }
                });

                regLblMessage.setTextFill(Color.GREEN);
                regLblMessage.setText("Введите данные для регистрации");

                //Добавляем BorderPane на сцену с подгрузкой css
                Scene RegisterScene = new Scene(bp);
                RegisterScene.getStylesheets().add(getClass().getClassLoader().getResource("login.css").toExternalForm());
                stage.setScene(RegisterScene);
                stage.setResizable(false);
                stage.show();
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

    @FXML
    public static void setRegisterScene() throws IOException {
        System.out.println("Меняем сцену на регистрацию");
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
