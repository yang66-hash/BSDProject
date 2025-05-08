package com.yang.apm.springplugin.constant;

public enum SQLType {
    MYSQL("mysql"),
    ORACLE("oracle"),
    SQLSERVER("sqlserver"),
    POSTGRESQL("postgresql"),
    DB2("db2"),
    H2("h2"),
    MariaDB("mariadb"),
    SQLITE("sqlite"),
    SYBASE("sybase");

    private String sqlType;

    SQLType(String type) {
        this.sqlType = type;
    }
    public static boolean contains(String sqlType) {
        for (SQLType type : SQLType.values()) {
            System.out.println(type.sqlType);
            if (sqlType.equals(type.sqlType)) {
                return true;
            }
        }
        return false;
    }
}
