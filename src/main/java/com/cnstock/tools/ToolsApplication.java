package com.cnstock.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cnstock.tools.utils.EsUtil;
import com.cnstock.tools.utils.HttpUtil;
import com.cnstock.tools.utils.PdfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@SpringBootApplication
public class ToolsApplication {

    private static Logger logger = LoggerFactory.getLogger(ToolsApplication.class);
    private static Queue<JSONObject> queue = new LinkedBlockingQueue();
    public static Integer count = 0;
    public static ExecutorService threadPool = Executors.newFixedThreadPool(20);
    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
        long startTime = 0;
        while (true){
            String str = HttpUtil.get("http://esquery.api.cnstock.com/index.php/tools/report?rows=10000", null);
            JSONObject jsonObject = JSON.parseObject(str);
            List<JSONObject> list = (List)jsonObject.get("item");
            if (null != list && list.size() >0 ){
                list.forEach(o -> {
                    queue.offer(o);
                });
                startTime = System.currentTimeMillis();
                for (int i = 0; i < list.size(); i++) {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                if (queue.size() == 0) {
                                    break;
                                }
                                try {
                                    JSONObject object = queue.poll();
                                    String id = object.get("id").toString();
                                    String url = object.get("url").toString();
                                    Map<String,Object> filedMap = new LinkedHashMap<>();
                                    filedMap.put("wordcount",1);
                                    if (url.endsWith(".pdf")){
                                        String text = PdfUtil.readPdfByUrl(url);
                                        if (null != text && !("".equals(text))){
                                            filedMap.put("content",text);
                                            EsUtil.update("gather_report", "document", id,filedMap);
                                            synchronized (this){
                                                count ++;
                                            }
                                        }
                                    }
                                } catch (Exception e){
                                    logger.error("",e);
                                    continue;
                                }
                            }
                        }
                    });
                }
            } else {
                logger.info(Thread.currentThread().getName() + "运行时间：" + (System.currentTimeMillis() - startTime)/1000.00 + "秒");
                threadPool.shutdown();
                EsUtil.closeClient();
                break;
            }
        }
    }
}
