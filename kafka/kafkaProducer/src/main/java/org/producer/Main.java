package org.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // Both threads work simultaneously
            executor.submit(() -> sendBatch(producer, "high-priority-mails", 500));
            executor.submit(() -> sendBatch(producer, "low-priority-mails", 5000));

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            producer.flush();
            System.out.println("All producer threads finished.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sendBatch(KafkaProducer<String, String> producer, String topic, int count) {
        for (int i = 0; i < count; i++) {

            String type = topic.contains("high") ? "VIP" : "Standard";
            String to = "user-" + i + "@" + type.toLowerCase() + ".com";
            String subject = type + " Alert #" + i;
            String body = "Please process immediately.";

            // --- CHANGE HERE: We now add the 'createdAt' timestamp ---
            long creationTime = System.currentTimeMillis();

            // JSON now contains creation time
            String json = String.format(
                    "{ \"to\": \"%s\", \"subject\": \"%s\", \"body\": \"%s\", \"createdAt\": %d }",
                    to, subject, body, creationTime
            );

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key-" + i, json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) exception.printStackTrace();
            });
        }
        System.out.println("Finished generating " + count + " messages for " + topic);
    }
}