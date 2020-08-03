package com.ss.sql.dto;

import java.util.List;

/**
 * ImportDto
 *
 * @author shisong
 * @date 2019/6/20
 */
public class ImportDto {

    private String username;

    private String password;

    private String dataBase;

    private String url;

    private List<String> tables;

    private String outPutPath;

    private String packageName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDataBase() {
        return dataBase;
    }

    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public String getOutPutPath() {
        return outPutPath;
    }

    public void setOutPutPath(String outPutPath) {
        this.outPutPath = outPutPath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "ImportDto{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", dataBase='" + dataBase + '\'' +
                ", url='" + url + '\'' +
                ", tables=" + tables +
                ", outPutPath='" + outPutPath + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}


