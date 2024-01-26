package io.demo.wikimedia;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateTopic {

    public static void main(String[] args) {
        final Logger log = LoggerFactory.getLogger(CreateTopic.class.getName());
        Properties props = new Properties();
        readFromConfig configReader = new readFromConfig();

        try {
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configReader.readConfig("bootstrap.servers"));
            props.put("sasl.mechanism", configReader.readConfig("sasl.mechanism"));
            props.put("security.protocol", configReader.readConfig("security.protocol"));
            props.put("sasl.jaas.config", configReader.readConfig("sasl.jaas.config"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(AdminClient adminClient = KafkaAdminClient.create(props)){
            NewTopic newTopic = new NewTopic("check", 3, (short) 1);
            final CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singleton(newTopic));
            createTopicsResult.values().get("check").get();
        } catch (ExecutionException e) {
            log.error("Error: ", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }
}
