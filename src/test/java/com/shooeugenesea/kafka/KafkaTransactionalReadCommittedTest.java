package com.shooeugenesea.kafka;

import com.shooeugenesea.IntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import static com.shooeugenesea.config.KafkaTopicConfig.TX_REQ_TOPIC_1;

@SpringBootTest
public class KafkaTransactionalReadCommittedTest extends IntegrationTest {

    private String GROUP_ID = UUID.randomUUID().toString();

    @Test
    public void test() {
        try (
                KafkaProducer<String, String> producer = producer(UUID.randomUUID().toString(), "tx-" + UUID.randomUUID().toString());
                KafkaConsumer<String, String> consumer = consumer_read_committed(UUID.randomUUID().toString(), TX_REQ_TOPIC_1)) {
            producer.beginTransaction();
            String msg = UUID.randomUUID().toString();
            producer.send(new ProducerRecord<>(TX_REQ_TOPIC_1, msg));
            
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            Assert.assertEquals(0, records.count());
            records.forEach(r -> System.out.println("Before commit: " + r.value() + " sent:" + msg));
            
            producer.commitTransaction();
            
            records = consumer.poll(Duration.ofSeconds(10));
            Assert.assertEquals(1, records.count());
            records.forEach(r -> System.out.println("After commit: " + r.value() + ", sent:" + msg));
        }
    }
    
    private KafkaProducer<String, String> producer(String clientId, String txId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, txId);
        
        KafkaProducer producer = new KafkaProducer<>(props);
        producer.initTransactions();
        return producer;
    }
    
    private KafkaConsumer<String, String> consumer_read_committed(String...topics) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // let consumer only consume message which is committed
        
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topics));
        return consumer;
    }

}