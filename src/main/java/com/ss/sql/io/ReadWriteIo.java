package com.ss.sql.io;

import java.io.*;

/**
 * ReadWriteIo
 *
 * @author shisong
 * @date 2019/6/20
 */
public class ReadWriteIo {

    public static void WriteStringToFile(String filePath, String fileName, String str) {
        PrintStream ps = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            File file1 = new File(filePath + "/" + fileName);
            if (!file1.exists()) {
                file1.createNewFile();

            }
            System.out.println("开始写入:" + fileName);
            ps = new PrintStream(new FileOutputStream(file1),true,"UTF-8");
            ps.println(str);// 往文件里写入字符串
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ps.close();
        }
    }

    public static String readFileToString() {
        StringBuilder sb = new StringBuilder();
        FileInputStream fis = null;
        String property = System.getProperty("user.dir");
        try {
            fis = new FileInputStream(property+"\\conf.json");//读取数据
            //一次性取多少字节
            byte[] bytes = new byte[2048];
            //接受读取的内容(n就代表的相关数据，只不过是数字的形式)
            int n = -1;
            //循环取出数据
            while ((n = fis.read(bytes,0,bytes.length)) != -1) {
                //转换成字符串
                String str = new String(bytes,0,n);
                sb.append(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
