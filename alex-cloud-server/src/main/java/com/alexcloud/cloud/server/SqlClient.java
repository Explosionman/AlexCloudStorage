package com.alexcloud.cloud.server;

import java.sql.*;

public class SqlClient {
    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:alex-cloud-server/storage-server.db");
            statement = connection.createStatement();
            System.out.println("Подключились к БД");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static String getNickname(String login, String password) {
        String query = String.format("select login from clients_tbl where login='%s' and password='%s'", login, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next()) {
                return set.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    synchronized static void createUser(String login, String password) {
        try {
            statement.executeUpdate(String.format("insert into clients_tbl (`login`, `password`) values ('%s', '%s')", login, password));
            System.out.println("В БД добален пользователь: " + login);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static boolean checkUserExists(String clientName) {
        String nameInDb;
        String query = String.format("select login from clients_tbl");
        try (ResultSet set = statement.executeQuery(query)) {
            while (set.next()) {
                nameInDb = set.getString(1);
                System.out.println("Имя из БД при проверке: " + nameInDb);
                if (nameInDb.toLowerCase().equals(clientName.toLowerCase())) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
