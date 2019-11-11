package com.cnstock.tools.utils;

import com.alibaba.fastjson.JSON;
import com.cnstock.tools.ToolsApplication;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author user01
 * @create 2019/11/6
 */
public class EsUtil {
    /**
     * 高阶Rest Client
     */
    private static Logger logger = LoggerFactory.getLogger(EsUtil.class);
    private static final RestHighLevelClient client = Client.buildHighClient();

    public EsUtil() {
        if ( null != client){
            throw new RuntimeException("client as been created");
        }
    }

    private static class Client{
        private static RestHighLevelClient buildHighClient(){
            RestHighLevelClient client = null;
            try {
                client = new RestHighLevelClient(
                        RestClient.builder(
                                new HttpHost("sh.es.resource.cnstock.com", 9200, "http")
                        )
                );
            } catch (Exception e) {
                logger.error("",e);
            }
            return client;
        }
    }

    public static void insert(String index,String type,String id,Map<String,Object> filedMap) {
        IndexRequest request = new IndexRequest(index, type, id).source(filedMap);
        GetRequest getRequest = new GetRequest(index, type, id);
        try {
            //文档不存在则做写入操作
            if (!client.exists(getRequest,RequestOptions.DEFAULT)){
                IndexResponse response = client.index(request, RequestOptions.DEFAULT);
//            XContentBuilder builder = XContentFactory.jsonBuilder();
//            builder.startObject();
//            {
//                builder.field("user", "user01");
//                builder.field("message", "trying out Elasticsearch");
//                builder.field("");
//            }
//            builder.endObject();
                if (response.getResult() == DocWriteResponse.Result.CREATED){
                    logger.info(id + "---added successfully");
                }
            };

        } catch (IOException e) {
            logger.error("",e);
        } catch (ElasticsearchException e){
            if (e.status() == RestStatus.CONFLICT){
                logger.info(id + "---the created document conflicts with an existing one");
            }
        } catch (Exception e){
            logger.error("",e);
        }

    }

    public static void update(String index,String type,String id,Map<String,Object> filedMap){
        UpdateRequest request = new UpdateRequest(index, type, id).doc(filedMap);
        // true，表明如果文档不存在，则新更新的文档内容作为新的内容插入文档，这个和scriptedUpsert的区别是：更新文档的两种不同方式，有的使用doc方法更新有的使用脚本更新
        request.docAsUpsert(true);
        // 为true，表明无论文档是否存在，脚本都会执行（如果不存在时，会创建一个新的文档）
        request.scriptedUpsert(true);
        // 如果文档不存在，使用upsert方法，会根据更新内容创建新的文档
        // 需要更新的内容，以json字符串方式提供
        request.upsert(JSON.toJSONString(filedMap), XContentType.JSON);
        // 等待主分片可用的超时时间
        request.timeout(TimeValue.timeValueMinutes(300));
        //WAIT_UNTIL 一直保持请求连接中，直接当所做的更改对于搜索查询可见时的刷新发生后，再将结果返回
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        // 如果更新的过程中，文档被其它线程进行更新的话，会产生冲突，这个为设置更新失败后重试的次数
        request.retryOnConflict(3);
        // 是否将文档内容作为结果返回，默认是禁止的
        //request.fetchSource(true);
        try {
            UpdateResponse updateResponse = client.update(request, RequestOptions.DEFAULT);
            if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED){
                logger.info(ToolsApplication.count +":"+ id + "---the document was updated successfully!");
            } else if(updateResponse.getResult() == DocWriteResponse.Result.CREATED){
                logger.info(id + "---the document was created successfully!");
            } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {
                // 如果request.detectNoop(true);中设置为false，则这个永远不会进入
                logger.info(id + "---the document has not changed!");
            }
        } catch (IOException e) {
            logger.error("",e);
        } catch (ElasticsearchException  e){
            if (e.status() == RestStatus.CONFLICT){
                logger.info(id + "---the created document conflicts with an existing one");
            }
        } catch (Exception e){
            logger.error("",e);
        }
    }

    public static Map<String, Object> getById(String index,String type,String id){
        Map<String, Object> result = null;
        GetRequest getRequest = new GetRequest(index, type, id);
//        String[] includes = new String[]{"message", "*Date"};
//        String[] excludes = Strings.EMPTY_ARRAY;
//        FetchSourceContext fetchSourceContext =
//                new FetchSourceContext(true, includes, excludes);
//        getRequest.fetchSourceContext(fetchSourceContext);
        try {
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                 result = getResponse.getSourceAsMap();
            } else {
                logger.info(id + "---the document being queried does not exist!");
            }
        } catch (IOException e) {
            logger.error("",e);
        }
        return result;
    }

    public static void delete(String index,String type,String id){
        DeleteRequest request = new DeleteRequest(index, type, id);
        request.timeout(TimeValue.timeValueMinutes(10));
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            if (response.getResult() == DocWriteResponse.Result.NOT_FOUND){
                logger.info(id + "---the deleted document was not found");
                return;
            } else if (response.getResult() == DocWriteResponse.Result.DELETED){
                logger.info(id + "---the document has been deleted!");
            }
        } catch (IOException e) {
            logger.error("",e);
        }
    }

    public static void closeClient() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            logger.error("",e);
        }
    }
}

