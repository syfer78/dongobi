package me.genomatch.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabasePool {
    private String jdbcUrl;
    public DatabasePool(String dbType, String dbName, String userName, String password, String hostName, String port) throws ClassNotFoundException {

        if(dbType.equalsIgnoreCase("mysql")) {
            Class.forName("com.mysql.jdbc.Driver");
        } else {
            throw new ClassNotFoundException("Not Supported DB");
        }
        jdbcUrl = "jdbc:mysql://" + hostName + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password + "&autoReconnection=true";
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
