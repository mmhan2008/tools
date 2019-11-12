package com.cnstock.tools.utils;

import org.pdfbox.cos.COSDocument;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author user01
 * @create 2019/11/6
 */
public class PdfUtil {

    private static Logger logger = LoggerFactory.getLogger(PdfUtil.class);

    public  static String readPdfByUrl(String url) {
        URL url1 = null;
        URLConnection connection = null;
        InputStream inputStream = null;
        PDDocument pdDocument = null;
        StringBuffer buffer = new StringBuffer();
        try {
            url1 = new URL(url);
            connection = url1.openConnection();
            connection.setConnectTimeout(3*1000);
            inputStream = connection.getInputStream();
            PDFTextStripper ts = new PDFTextStripper();
            pdDocument = PDDocument.load(inputStream);
            StringWriter writer = new StringWriter();
            ts.writeText(pdDocument,writer);
            buffer.append(writer.getBuffer());
        } catch (IOException e) {
            return null;
        } catch (Exception e){
            return null;
        } finally {
            try {
                if (null != inputStream){
                    inputStream.close();
                }
                if (null != pdDocument){
                    COSDocument cos = pdDocument.getDocument();
                    cos.close();
                    pdDocument.close();
                }
            } catch (IOException e) {
                logger.info("",e);
            }
        }
        return buffer.toString();
    }


    public static void main(String[] args) {
        System.out.println(readPdfByUrl("http://img.oss.cnstock.com/pdf/H301_AP201911111370634398_1.pdf"));
    }
}

