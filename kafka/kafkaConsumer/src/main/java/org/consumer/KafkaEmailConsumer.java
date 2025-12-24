package org.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaEmailConsumer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEmailConsumer.class);

    private final String topic;
    private final String groupId;
    private final int consumerIndex;
    private final ExecutorService emailExecutor;

    // Configurable Settings
    private final int batchSize;
    private final long pollDurationMs;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, String> consumer;

    public KafkaEmailConsumer(String topic, String groupId, int consumerIndex,
                              ExecutorService emailExecutor, int batchSize, long pollDurationMs) {
        this.topic = topic;
        this.groupId = groupId;
        this.consumerIndex = consumerIndex;
        this.emailExecutor = emailExecutor;
        this.batchSize = batchSize;
        this.pollDurationMs = pollDurationMs; // Store the custom wait time
    }

    public void shutdown() {
        running.set(false);
        if (consumer != null) consumer.wakeup();
    }

    @Override
    public void run() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(batchSize));

        try {
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList(topic));
            logger.info("Started Consumer {} | Topic: {} | Batch: {} | Wait: {}ms",
                    consumerIndex, topic, batchSize, pollDurationMs);

            while (running.get()) {
                ConsumerRecords<String, String> records;
                try {
                    // USE DYNAMIC WAIT TIME HERE
                    records = consumer.poll(Duration.ofMillis(pollDurationMs));
                } catch (WakeupException e) { continue; }

                if (records.isEmpty()) continue;

                List<EmailSender.EmailRequest> emailBatch = new ArrayList<>();
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        JSONObject json = new JSONObject(record.value());

                        // Extract creation time from Producer
                        long createdAt = json.has("createdAt") ? json.getLong("createdAt") : System.currentTimeMillis();

                        emailBatch.add(new EmailSender.EmailRequest(
                                json.getString("to"),
                                json.getString("subject"),
                                json.getString("body"),
                                createdAt // Pass it here
                        ));
                    } catch (JSONException e) {
                        logger.error("Skipping bad JSON");
                    }
                }

                if (emailBatch.isEmpty()) {
                    consumer.commitSync();
                    continue;
                }

                // Async Send (Waits for completion before next poll)
                CompletableFuture.runAsync(() -> {
                    long start = System.currentTimeMillis();

                    EmailSender.sendBatch(emailBatch);

                    long duration = System.currentTimeMillis() - start;
                    logger.info("Consumer {} sent {} emails in {}ms",
                            consumerIndex, emailBatch.size(), duration);

                }, emailExecutor).join();

                consumer.commitSync();
            }
        } catch (Exception e) {
            logger.error("Consumer Error", e);
        } finally {
            if (consumer != null) consumer.close();
        }
    }
}