
package ma.translogix.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/translogix";

    private static final String USER = "root";

    private static final String PASSWORD = "YOUR_PASSWORD";

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(
                URL, USER, PASSWORD
            );

            System.out.println("Connexion MySQL réussie !");
            return connection;

        } catch (SQLException e) {
            System.out.println("Erreur de connexion MySQL : "
                + e.getMessage());
            return null;
        }
    }
}

