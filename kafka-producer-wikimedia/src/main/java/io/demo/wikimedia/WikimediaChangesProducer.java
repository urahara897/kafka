package io.demo.wikimedia;

import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import com.launchdarkly.eventsource.background.BackgroundEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {
    public static void main(String[] args) {

        final Logger log = LoggerFactory.getLogger(WikimediaChangeHandler.class.getName());
       readFromConfig configReader = new readFromConfig();

        Properties properties = new Properties();
        try {
            //connects to a kafka cluster at upstash
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configReader.readConfig("bootstrap.servers"));
            properties.setProperty("sasl.mechanism", configReader.readConfig("sasl.mechanism"));
            properties.setProperty("security.protocol", configReader.readConfig("security.protocol"));
            properties.setProperty("sasl.jaas.config", configReader.readConfig("sasl.jaas.config"));
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //high throughput producer configs
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        String topic = "wikimedia.recentchange";

        BackgroundEventHandler eventHandler = new WikimediaChangeHandler(producer, topic);
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
        EventSource.Builder eventSourceBuilder = new EventSource.Builder(URI.create(url));
        BackgroundEventSource eventSource = new BackgroundEventSource.Builder(eventHandler, eventSourceBuilder).build();

        eventSource.start();

        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            log.error("Error Occured: ", e);
            throw new RuntimeException(e);
        }
    }
}
