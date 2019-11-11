package com.cnstock.tools.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * @author user01
 * @create 2019/11/7
 */
public class HttpUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    public static String getHtml(String url,String method,String charset,String jsonParam){
        StringBuilder sb = new StringBuilder();
        URL url1 = null;
        HttpURLConnection conn = null;
        BufferedReader bf = null;
        try {
            url1 = new URL(url);
            conn = (HttpURLConnection)url1.openConnection();
            conn.setConnectTimeout(3*1000);
            conn.setRequestMethod(method.toUpperCase());
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            if (null != jsonParam){
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length",String.valueOf(jsonParam.getBytes().length));
                OutputStream outputStream = conn.getOutputStream();
                outputStream.write(jsonParam.getBytes());
                outputStream.flush();
                outputStream.close();
            }
//            System.out.println(connection.getResponseCode());
            bf = new BufferedReader(new InputStreamReader(conn.getInputStream(),Charset.forName(charset)));
            String temp = "";
            while ((temp = bf.readLine())!=null){
                sb.append(temp + "\n");
            }
        } catch (MalformedURLException e) {
            logger.error("",e);
        } catch (IOException e) {
            logger.error("",e);
        } catch (Exception e){
            logger.error("",e);
        } finally {
            try {
                if (null != bf) {
                    bf.close();
                }
            } catch (IOException e) {
                logger.error("",e);
            }
        }
        return sb.toString();
    }

    public static String get(String url,String jsonParam){
        return getHtml(url,"get","utf8",jsonParam);
    }

    public static String post(String url,String jsonParam){
        return getHtml(url,"post","utf8",jsonParam);
    }
}

