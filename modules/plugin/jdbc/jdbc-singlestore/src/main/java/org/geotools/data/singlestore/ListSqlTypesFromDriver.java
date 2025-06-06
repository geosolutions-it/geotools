package org.geotools.data.singlestore;

import java.sql.*;

public class ListSqlTypesFromDriver {
    public static void main(String[] args) throws Exception {
        // Replace with your JDBC URL, username, and password
        String url = "jdbc:singlestore://localhost:3306/geotools";
        String user = "geotools";
        String password = "geotools";
        Class.forName("com.singlestore.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, "schema", "road", null);

            System.out.printf("%-20s %-10s %-10s%n", "Type Name", "SQL Type", "Nullable");
            while (columns.next()) {
                String typeName = columns.getString("TYPE_NAME");
                int dataType = columns.getInt("DATA_TYPE");
                boolean nullable = columns.getInt("NULLABLE") == DatabaseMetaData.typeNullable;

                System.out.printf("%-20s %-10d %-10s%n", typeName, dataType, nullable ? "YES" : "NO");
            }
        }
    }
}