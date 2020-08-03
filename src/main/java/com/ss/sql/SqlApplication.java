package com.ss.sql;

import com.alibaba.fastjson.JSONObject;
import com.ss.sql.dto.ImportDto;
import com.ss.sql.enums.JavaSqlTypeEnums;
import com.ss.sql.io.ReadWriteIo;
import com.ss.sql.utils.CamelUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class SqlApplication {
    private static final String MAPPER = "mapper";
    private static final String ENETIY = "entity";
    private static final String VO = "vo";
    private static final String XML = "xml";
    private static final String IMPORT_DATE = "import java.util.Date;";
    private static final String IMPORT_LIST = "import java.util.List;";
    private static final String IMPORT_BIGDECIMAL = "import java.math.BigDecimal;";
    private static final String IMPORT_BIGINTEGER = "import java.math.BigInteger;";
    private static final String IMPORT_TIME = "import java.sql.Time;";
    private static final String IMPORT_LOMBOK = "import lombok.Data;";
    private static final String IMPORT_MAPPER = "import org.apache.ibatis.annotations.Mapper;";
    private static final String IMPORT_PARAM = "import org.apache.ibatis.annotations.Param;";
    private static final String IMPORT_DOC = "import com.autoyol.doc.annotation.AutoDocProperty;";
    private static final String ANNOTATION_DATA = "@Data";
    private static final String ANNOTATION_MAPPER = "@Mapper";
    private static final String ANNOTATION_PARAM = "@Param";
    private static final String SQL_COLUMN = "select_column";
    private static final String RESULT_MAP = "selectColumnMap";


    public static void main(String[] args) {
        SpringApplication.run(SqlApplication.class, args);
        //获取配置文件
        String s = ReadWriteIo.readFileToString();
        ImportDto importDto = JSONObject.parseObject(s, ImportDto.class);
        if(importDto == null || StringUtils.isBlank(importDto.getDataBase()) || StringUtils.isBlank(importDto.getPassword())|| StringUtils.isBlank(importDto.getUrl())|| StringUtils.isBlank(importDto.getUsername())|| StringUtils.isBlank(importDto.getOutPutPath())|| StringUtils.isBlank(importDto.getPackageName()) || CollectionUtils.isEmpty(importDto.getTables())){
            throw new IllegalArgumentException("非法参数");
        }
        start(importDto);
    }

    private static void start(ImportDto importDto) {
        try {
            //1. JDBC连接MYSQL的代码很标准。
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection conn = DriverManager.getConnection("jdbc:mysql://"+importDto.getUrl()+"/"+importDto.getDataBase(), importDto.getUsername(), importDto.getPassword());

            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog(); //catalog 其实也就是数据库名


            for (String tableName : importDto.getTables()) {
                queryTableColumn(metaData, tableName, catalog,importDto);
            }
            System.out.println("end");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void queryTableColumn(DatabaseMetaData metaData, String tableName, String catalog, ImportDto importDto) throws Exception {
        ResultSet primaryKeyResultSet = metaData.getPrimaryKeys(catalog, null, tableName);
        String primaryKeyColumnName = "";
        while (primaryKeyResultSet.next()) {
            primaryKeyColumnName = primaryKeyResultSet.getString("COLUMN_NAME");
        }

        String camelTableName = CamelUtils.underline2Camel(tableName, false);
        String className = CamelUtils.underline2Camel(tableName);
        ResultSet colRet = metaData.getColumns(null, "%", tableName, "%");
        queryAndWriteEnetiy(colRet, camelTableName, tableName,importDto);
        queryAndWriteVo(colRet, camelTableName, tableName,importDto);
        if (StringUtils.isNotBlank(primaryKeyColumnName)) {
            //当主键不为空时，才生成xml和mapper
            queryAndWriteMapper(colRet, camelTableName, className, primaryKeyColumnName,importDto);
            queryAndWriteXml(colRet, camelTableName, className, primaryKeyColumnName, tableName,importDto);
        } else {
            System.out.println(tableName + "不存在主键");
        }


    }

    private static void queryAndWriteEnetiy(ResultSet colRet, String camelTableName, String tableName, ImportDto importDto) throws Exception{
        colRet.beforeFirst();
        Boolean hasDate = false;
        Boolean hasBigDecimal = false;
        Boolean hasBigInteger = false;
        Boolean hasTime = false;
        StringBuilder sb = new StringBuilder();
        sb.append("public ").append("class ").append(camelTableName+"Entity").append("{").append("\n");
        while (colRet.next()) {
            String remarks = colRet.getString("REMARKS");

            //字段名
            String columnName = colRet.getString("COLUMN_NAME");
            String camel = CamelUtils.underline2Camel(columnName);

            //字段类型
            String columnType = colRet.getString("TYPE_NAME");
            String javaType = JavaSqlTypeEnums.getValue(columnType);
            if (StringUtils.isBlank(javaType)) {
                throw new UnsupportedOperationException("不支持的columnType :" + columnType + " 不支持的tableName :" + tableName);
            } else {
                if (StringUtils.isNotBlank(remarks)) {
                    sb.append("\t").append("/**").append("\n");
                    sb.append("\t").append("*").append(remarks).append("\n");
                    sb.append("\t").append("*/").append("\n");
                }
                sb.append("\t").append("private ").append(javaType).append(" ").append(camel).append(";").append("\n");
                if (javaType.equals("Date")) {
                    hasDate = true;
                }
                if (javaType.equals("BigDecimal")) {
                    hasBigDecimal = true;
                }
                if (javaType.equals("BigInteger")) {
                    hasBigInteger = true;
                }
                if (javaType.equals("Time")) {
                    hasTime = true;
                }
            }
        }
        sb.append("}");
        StringBuilder sb1 = new StringBuilder();
        sb1.append("package ").append(importDto.getPackageName()).append(".").append(ENETIY).append(";").append("\n\n");
        sb1.append(IMPORT_LOMBOK).append("\n").append("\n");
        if (hasDate) {
            sb1.append(IMPORT_DATE).append("\n").append("\n");
        }
        if (hasBigDecimal) {
            sb1.append(IMPORT_BIGDECIMAL).append("\n").append("\n");
        }
        if (hasBigInteger) {
            sb1.append(IMPORT_BIGINTEGER).append("\n").append("\n");
        }
        if (hasTime) {
            sb1.append(IMPORT_TIME).append("\n").append("\n");
        }
        sb1.append(getAnnotationInfo(camelTableName+"Entity"));
        sb1.append(ANNOTATION_DATA).append("\n");
        sb1.append(sb);
        String file = importDto.getOutPutPath()+ "/"  + ENETIY;
        String fileName = "/" + camelTableName + "Entity.java";
        ReadWriteIo.WriteStringToFile(file, fileName, sb1.toString());
    }

    private static void queryAndWriteXml(ResultSet colRet, String camelTableName, String className, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >").append("\n");
        sb.append("<mapper namespace=\"").append(importDto.getPackageName()).append(".").append(MAPPER).append(".").append(camelTableName + "Mapper").append("\">").append("\n");
        colRet.beforeFirst();
        appendSql(colRet, sb,importDto);
        appendResultMap(colRet, sb, camelTableName,importDto);
        appendSaveSql(colRet, sb, camelTableName, primaryKeyColumnName, tableName,importDto);
        appendSelectOneSql(colRet, sb, camelTableName, primaryKeyColumnName, tableName,importDto);
        appendSelelctListSql(colRet, sb, camelTableName, primaryKeyColumnName, tableName,importDto);
        appendUpdateSql(colRet, sb, camelTableName, primaryKeyColumnName, tableName,importDto);
        appendDeleteSql(colRet, sb, camelTableName, primaryKeyColumnName, tableName,importDto);
        sb.append("</mapper>");

        String file = importDto.getOutPutPath()+ "/" + XML;
        String fileName = "/" + camelTableName + "Mapper.xml";
        ReadWriteIo.WriteStringToFile(file, fileName, sb.toString());
    }

    private static void appendDeleteSql(ResultSet colRet, StringBuilder sb, String camelTableName, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        //删除 逻辑删除 修改is_delete字段
        colRet.beforeFirst();
        String typeName = "";
        while (colRet.next()) {
            String column_name = colRet.getString("COLUMN_NAME");
            if (column_name.equals(primaryKeyColumnName)) {
                typeName = colRet.getString("TYPE_NAME");
            }
        }
        sb.append("\t").append(" <update id=\"").append("delete" + camelTableName + "ById").append("\" parameterType=\"").append("java.lang.").append(JavaSqlTypeEnums.getValue(typeName)).append("\">").append("\n");
        sb.append("\t\t").append("update ").append(tableName).append(" set").append("\n");
        sb.append("\t\t").append("is_delete = 1,").append("\n");
        sb.append("\t\t").append("update_time = NOW()").append("\n");
        sb.append("\t\t").append("where ").append(primaryKeyColumnName).append(" = #{id}").append(" and is_delete = 0").append("\n");
        sb.append("\t").append("</update>").append("\n\n");
    }

    private static void appendUpdateSql(ResultSet colRet, StringBuilder sb, String camelTableName, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        //修改字段  <update id="updateByPrimaryKeySelective" parameterType="com.autoyol.carInfo.entity.CarInspectDo">
        colRet.beforeFirst();
        sb.append("\t").append(" <update id=\"").append("update" + camelTableName).append("\" parameterType=\"").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity").append("\">").append("\n");
        sb.append("\t\t").append("update ").append(tableName).append(" set").append("\n");
        StringBuilder saveSqlSb = new StringBuilder();
        while (colRet.next()) {
            String column_name = colRet.getString("COLUMN_NAME");
            if (!(column_name.equals(primaryKeyColumnName) || column_name.equals("update_time"))) {
                saveSqlSb.append("\t\t").append("<if test=\"").append(CamelUtils.underline2Camel(column_name)).append(" != null\">").append("\n");
                saveSqlSb.append("\t\t\t").append(column_name).append(" = ").append("#{").append(CamelUtils.underline2Camel(column_name)).append("}").append(",").append("\n");
                saveSqlSb.append("\t\t").append("</if>").append("\n");
            }
        }
        sb.append(saveSqlSb);
        sb.append("\t\t").append("update_time = NOW()").append("\n");
        sb.append("\t\t").append("where ").append(primaryKeyColumnName).append(" = #{id}").append(" and is_delete = 0").append("\n");
        sb.append("\t").append("</update>").append("\n\n");

    }

    private static void appendSelelctListSql(ResultSet colRet, StringBuilder sb, String camelTableName, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        //查询全部
        colRet.beforeFirst();
        sb.append("\t").append("<select id=\"queryList\" resultType=\"").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity").append("\">").append("\n");
        sb.append("\t\t").append("select <include refid=\"").append(SQL_COLUMN).append("\" />").append("\n");
        sb.append("\t\t").append("from ").append(tableName).append("\n");
        sb.append("\t\t").append("where ").append("is_delete = 0").append("\n");
        sb.append("\t").append("</select>").append("\n\n");
    }

    private static void appendSelectOneSql(ResultSet colRet, StringBuilder sb, String camelTableName, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        //根据主键查询 "query"+camelTableName+"ById"
        colRet.beforeFirst();
        sb.append("\t").append("<select id=\"").append("query").append(camelTableName).append("ById").append("\" resultType=\"").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity").append("\">").append("\n");
        sb.append("\t\t").append("select <include refid=\"").append(SQL_COLUMN).append("\" />").append("\n");
        sb.append("\t\t").append("from ").append(tableName).append("\n");
        sb.append("\t\t").append("where ").append(primaryKeyColumnName).append(" = #{id}").append(" and is_delete = 0").append("\n");
        sb.append("\t").append("</select>").append("\n\n");

    }

    private static void appendSaveSql(ResultSet colRet, StringBuilder sb, String camelTableName, String primaryKeyColumnName, String tableName, ImportDto importDto) throws Exception {
        //保存sql
        colRet.beforeFirst();
        StringBuilder saveSqlKeySb = new StringBuilder();
        StringBuilder saveSqlValueSb = new StringBuilder();
        sb.append("\t").append("<insert id=\"").append("save").append(camelTableName).append("\" parameterType=\"").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity")
                .append("\" useGeneratedKeys=\"true\" keyProperty=\"").append(CamelUtils.underline2Camel(primaryKeyColumnName)).append("\" keyColumn=\"").append(primaryKeyColumnName).append("\">").append("\n");
        sb.append("\t\t").append("insert into ").append(tableName).append("(").append("\n");
        while (colRet.next()) {
            String column_name = colRet.getString("COLUMN_NAME");
            if (!(column_name.equals(primaryKeyColumnName) || column_name.equals("create_time"))) {
                saveSqlKeySb.append("\t\t\t").append("<if test=\"").append(CamelUtils.underline2Camel(column_name)).append(" != null\">").append("\n");
                saveSqlKeySb.append("\t\t\t\t").append(column_name).append(",").append("\n");
                saveSqlKeySb.append("\t\t\t").append("</if>").append("\n");
                saveSqlValueSb.append("\t\t\t").append("<if test=\"").append(CamelUtils.underline2Camel(column_name)).append(" != null\">").append("\n");
                saveSqlValueSb.append("\t\t\t\t").append("#{").append(CamelUtils.underline2Camel(column_name)).append("}").append(",").append("\n");
                saveSqlValueSb.append("\t\t\t").append("</if>").append("\n");
            }
        }
        saveSqlKeySb.append("\t\t\t").append("create_time").append("\n");
        saveSqlValueSb.append("\t\t\t").append("NOW())").append("\n");
        sb.append(saveSqlKeySb);
        sb.append("\t\t").append(")");
        sb.append(" values ").append("(").append("\n");
        sb.append(saveSqlValueSb);
        sb.append("\t").append("</insert>").append("\n\n");
    }

    private static void appendResultMap(ResultSet colRet, StringBuilder sb, String camelTableName, ImportDto importDto) throws Exception {
        StringBuilder resultMapSb = new StringBuilder();//resultMap
        resultMapSb.append("\t").append("<resultMap id = \"").append(RESULT_MAP).append("\" type=\"").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity").append("\">").append("\n");
        colRet.beforeFirst();
        while (colRet.next()) {
            String column_name = colRet.getString("COLUMN_NAME");
            // <result property="gpsNo" column="gps_no"/>
            resultMapSb.append("\t\t").append("<result property=\"").append(CamelUtils.underline2Camel(column_name)).append("\" column=\"").append(column_name).append("\"/>").append("\n");
        }
        resultMapSb.append("\t").append("</resultMap>").append("\n\n");
        sb.append(resultMapSb);
    }

    private static void appendSql(ResultSet colRet, StringBuilder sb, ImportDto importDto) throws Exception {
        colRet.beforeFirst();
        sb.append("\t").append("<sql id=\"" + SQL_COLUMN + "\">").append("\n");
        sb.append("\t\t");
        StringBuilder sqlColumnSb = new StringBuilder();//sql
        while (colRet.next()) {
            String column_name = colRet.getString("COLUMN_NAME");
            sqlColumnSb.append(column_name).append(",");
        }
        String sqlColumnStr = "";
        if (sqlColumnSb.lastIndexOf(",") != -1) {
            sqlColumnStr = sqlColumnSb.substring(0, sqlColumnSb.lastIndexOf(","));
        }
        sb.append(sqlColumnStr).append("\n");
        sb.append("\t").append("</sql>").append("\n\n");
    }

    public static void queryAndWriteMapper(ResultSet colRet, String camelTableName, String className, String primaryKeyColumnName, ImportDto importDto) throws Exception {
        colRet.beforeFirst();
        String primaryKeyColumnType = "";
        while (colRet.next()) {
            //字段名
            String columnName = colRet.getString("COLUMN_NAME");
            if (columnName.equals(primaryKeyColumnName)) {
                //字段类型
                String columnType = colRet.getString("TYPE_NAME");
                primaryKeyColumnType = JavaSqlTypeEnums.getValue(columnType);
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(importDto.getPackageName()).append(".").append(MAPPER).append(";").append("\n\n");
        sb.append(IMPORT_MAPPER).append("\n");
        sb.append(IMPORT_PARAM).append("\n\n");
        sb.append("import ").append(importDto.getPackageName()).append(".").append(ENETIY).append(".").append(camelTableName+"Entity").append(";").append("\n\n");
        sb.append(IMPORT_LIST).append("\n\n");
        sb.append(getAnnotationInfo(camelTableName+"Mapper"));
        sb.append(ANNOTATION_MAPPER).append("\n");
        sb.append("public interface ").append(camelTableName + "Mapper").append("{").append("\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 保存").append("\n");
        sb.append("\t * @param ").append(className).append(" 保存信息\n");
        sb.append("\t * @return 成功条数\n");
        sb.append("\t */\n");
        sb.append("\t").append("Integer").append(" ").append("save" + camelTableName).append("(").append(camelTableName + "Entity ").append(className).append(");").append("\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 根据主键查询").append("\n");
        sb.append("\t * @param id 根据主键查询\n");
        sb.append("\t * @return 返回查询到的实体\n");
        sb.append("\t */\n");
        sb.append("\t").append(camelTableName+"Entity").append(" ").append("query" + camelTableName + "ById").append("(").append(ANNOTATION_PARAM).append("(\"").append("id").append("\") ").append(" ").append(primaryKeyColumnType).append(" ").append("id").append(");").append("\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 批量查询").append("\n");
        sb.append("\t * @return 查询列表\n");
        sb.append("\t */\n");
        sb.append("\t").append("List<").append(camelTableName+"Entity").append(">").append(" queryList").append("();").append("\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 修改").append("\n");
        sb.append("\t * @param ").append(className).append(" 修改实体\n");
        sb.append("\t * @return 成功条数\n");
        sb.append("\t */\n");
        sb.append("\t").append("Integer").append(" ").append("update" + camelTableName).append("(").append(camelTableName + "Entity ").append(className).append(");").append("\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 根据主键删除").append("\n");
        sb.append("\t * @param id 主键\n");
        sb.append("\t * @return 删除的数量\n");
        sb.append("\t */\n");
        sb.append("\t").append("Integer").append(" ").append("delete" + camelTableName + "ById").append("(").append(ANNOTATION_PARAM).append("(\"").append("id").append("\") ").append(" ").append(primaryKeyColumnType).append(" ").append("id").append(");").append("\n");
        sb.append("}");
        String file = importDto.getOutPutPath()+ "/"  + MAPPER;
        String fileName = "/" + camelTableName + "Mapper.java";
        ReadWriteIo.WriteStringToFile(file, fileName, sb.toString());
    }

    public static void queryAndWriteVo(ResultSet colRet, String camelTableName, String tableName, ImportDto importDto) throws Exception {
        colRet.beforeFirst();
        Boolean hasDate = false;
        Boolean hasBigDecimal = false;
        Boolean hasBigInteger = false;
        Boolean hasTime = false;
        StringBuilder sb = new StringBuilder();
        sb.append("public ").append("class ").append(camelTableName+"VO").append("{").append("\n");
        while (colRet.next()) {
            String remarks = colRet.getString("REMARKS");

            //字段名
            String columnName = colRet.getString("COLUMN_NAME");
            String camel = CamelUtils.underline2Camel(columnName);

            //字段类型
            String columnType = colRet.getString("TYPE_NAME");
            String javaType = JavaSqlTypeEnums.getValue(columnType);
            if (StringUtils.isBlank(javaType)) {
                throw new UnsupportedOperationException("不支持的columnType :" + columnType + " 不支持的tableName :" + tableName);
            } else {
                if (StringUtils.isNotBlank(remarks)) {
                    sb.append("\t").append("@AutoDocProperty(value=\"").append(remarks).append("\")").append("\n");
                }
                sb.append("\t").append("private ").append(javaType).append(" ").append(camel).append(";").append("\n");
                if (javaType.equals("Date")) {
                    hasDate = true;
                }
                if (javaType.equals("BigDecimal")) {
                    hasBigDecimal = true;
                }
                if (javaType.equals("BigInteger")) {
                    hasBigInteger = true;
                }
                if (javaType.equals("Time")) {
                    hasTime = true;
                }
            }
        }
        sb.append("}");
        StringBuilder sb1 = new StringBuilder();
        sb1.append("package ").append(importDto.getPackageName()).append(".").append(VO).append(";").append("\n\n");
        sb1.append(IMPORT_LOMBOK).append("\n").append("\n");
        sb1.append(IMPORT_DOC).append("\n").append("\n");
        if (hasDate) {
            sb1.append(IMPORT_DATE).append("\n").append("\n");
        }
        if (hasBigDecimal) {
            sb1.append(IMPORT_BIGDECIMAL).append("\n").append("\n");
        }
        if (hasBigInteger) {
            sb1.append(IMPORT_BIGINTEGER).append("\n").append("\n");
        }
        if (hasTime) {
            sb1.append(IMPORT_TIME).append("\n").append("\n");
        }
        sb1.append(getAnnotationInfo(camelTableName+"VO"));
        sb1.append(ANNOTATION_DATA).append("\n");
        sb1.append(sb);
        String file = importDto.getOutPutPath()+ "/"  + VO;
        String fileName = "/" + camelTableName + "VO.java";
        ReadWriteIo.WriteStringToFile(file, fileName, sb1.toString());
    }

    private static String getAnnotationInfo(String className){

        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * " + className + "\n");
        sb.append(" *\n");
        sb.append(" * @author shisong\n");
        sb.append(" * @date " + getDateStr() + "\n");
        sb.append(" */\n");
        return sb.toString();
    }

    private static String getDateStr(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.format(formatter);
    }

}
