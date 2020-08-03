package com.ss.sql.enums;

/**
 * JavaSqlTypeEnums
 *
 * @author shisong
 * @date 2019/6/19
 */
public enum JavaSqlTypeEnums {

    TINYINT_INTEGER("TINYINT", "Integer"),
    TINYINT_UNSIGNED_INTEGER("TINYINT UNSIGNED", "Integer"),
    SMALLINT_INTEGER("SMALLINT", "Integer"),
    SMALLINT_UNSIGNED_INTEGER("SMALLINT UNSIGNED", "Integer"),
    MEDIUMINT_INTEGER("MEDIUMINT", "Integer"),
    INT_LONG("INT", "Integer"),
    INT_UNSIGNED_LONG("INT UNSIGNED", "Integer"),
    BIGINT_UNSIGNED_BIGINTEGER("BIGINT UNSIGNED", "Long"),
    BIGINT_BIGINTEGER("BIGINT", "Long"),
    CHAR_STRING("CHAR", "String"),
    VARCHAR_STRING("VARCHAR", "String"),
    MEDIUMTEXT_STRING("MEDIUMTEXT", "String"),
    TEXT_STRING("TEXT", "String"),
    LONGTEXT_STRING("LONGTEXT", "String"),
    TIMESTAMP_DATE("TIMESTAMP", "Date"),
    DATETIME_DATE("DATETIME", "Date"),
    DATE_DATE("DATE", "Date"),
    BIT_BOOLEAN("BIT", "Boolean"),
    DOUBLE_DOUBLE("DOUBLE", "Double"),
    DECIMAL_BIGDECIMAL("DECIMAL", "BigDecimal"),
    FLOAT_FLOAT("FLOAT", "Float"),
    BLOB_BYTE("BLOB", "Byte[]"),
    TIME_TIME("TIME", "Time");

    private String sqlType;

    private String javaType;

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    JavaSqlTypeEnums(String sqlType, String javaType) {
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    public static String getValue(String sqlType) {
        for (JavaSqlTypeEnums ele : values()) {
            if (ele.getSqlType().equals(sqlType)) return ele.getJavaType();
        }
        return null;
    }
}
