package io.demo.kafka.opensearch;

import com.google.gson.JsonParser;
import io.demo.kafka.readPropertiesFile;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpensearchConsumer {


    public static RestHighLevelClient createOpenSourceClient(){
        String connString = "http://localhost:9200";
//        String connString = "https://c9p5mwld41:45zeygn9hy@kafka-course-2322630105.eu-west-1.bonsaisearch.net:443";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }
        return restHighLevelClient;
    }

    private static String extractId(String json){
        // gson library
        return JsonParser.parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }

    public static KafkaConsumer<String, String> createKafkaConsumer(){
        String groupID = "consumer-opensearch";
        readPropertiesFile propertiesFile = new readPropertiesFile();

        //Create Consumer Config
        Properties properties = new Properties();
        try {
            properties.setProperty("bootstrap.servers", propertiesFile.readFile("bootstrap.servers"));
            properties.setProperty("sasl.mechanism", propertiesFile.readFile("sasl.mechanism"));
            properties.setProperty("security.protocol", propertiesFile.readFile("security.protocol"));
            properties.setProperty("sasl.jaas.config", propertiesFile.readFile("sasl.jaas.config"));
            properties.setProperty("key.deserializer", StringDeserializer.class.getName());
            properties.setProperty("value.deserializer", StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupID);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new KafkaConsumer<>(properties);
    }

    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(OpensearchConsumer.class.getName());
        //Create OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSourceClient();

        //Create Kafka Client
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        //create a reference to the main thread
        final Thread mainThread = Thread.currentThread();

        //add the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                log.info("Detected the shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //create index if it does not exist
        try(openSearchClient; consumer) {
            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);
            if(!indexExists){
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("The Wikimedia Index has been Created!!!!");
            }else{
                log.info("Wikimedia Index already exists!!!!");
            }

            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            while(true){
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

                int recordCount = records.count();
                log.info("Received " + recordCount + " records");

                BulkRequest bulkRequest = new BulkRequest();

                for(ConsumerRecord<String, String> record : records){
                    //strategy 1
                    //define id using kafka record coordinates
                    //String id = record.topic() +"_"+ record.partition()+"_"+record.offset();

                    try{
                        //strategy 2
                        //extract id from json
                        String id = extractId(record.value());
                        IndexRequest indexRequest = new IndexRequest("wikimedia")
                                .source(record.value(), XContentType.JSON)
                                .id(id);
                        bulkRequest.add(indexRequest);
                        //to insert 1 by 1
                        /*try {
                            IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                            log.info(response.getId());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }*/
                    }catch (Exception e){

                    }
                }
                if(bulkRequest.numberOfActions()>0){
                    BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    log.info("Inserted "+ bulkResponse.getItems().length + " records");
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    //commit offsets after the batch is consumed
                    consumer.commitAsync();
                    log.info("Offsets have been committed");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }catch (WakeupException e){
            log.info("Consumer is Shutting down");
        }finally {
            consumer.close();
            try {
                openSearchClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //main code logic

        //close things
    }

}
