package org.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String HIGH_TOPIC = "high-priority-mails";
    private static final String LOW_TOPIC  = "low-priority-mails";
    private static final String HIGH_GROUP = "high-group-tuned-v2";
    private static final String LOW_GROUP  = "low-group-tuned-v2";

    // --- TUNING CONFIGURATION ---

    // HIGH: Fast, snappy, frequent checks
    // Consumer asks to que each 75 ms if there is new mail, repeats each 75 ms until message arrives
    // If one arrives, it sends directly, no wait time
    // If multiple arrive at the same time in high traffic, it batches up to 5 emails before sending
    private static final int HIGH_BATCH_LIMIT = 50;
    private static final long HIGH_WAIT_MS    = 5;

    // LOW: Efficient, bulk, patient checks
    private static final int LOW_BATCH_LIMIT  = 200;
    private static final long LOW_WAIT_MS     = 10000;

    // --- CONSUMER COUNTS ---
    private static final int HIGH_WORKERS = 6;
    private static final int LOW_WORKERS  = 1;
    private static final int CONSUMER_POOL  = HIGH_WORKERS + LOW_WORKERS;

    // --- THREAD POOLS (Based on previous calculation) ---
    private static final ExecutorService highWorkers = Executors.newFixedThreadPool(HIGH_WORKERS);
    private static final ExecutorService lowWorkers  = Executors.newFixedThreadPool(LOW_WORKERS);

    private static final ExecutorService consumerRunnerPool = Executors.newFixedThreadPool(CONSUMER_POOL);
    private static final List<KafkaEmailConsumer> activeConsumers = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        configureWireMock();

        // 1. Start HIGH Priority Consumers (6 Consumers)
        for (int i = 0; i < HIGH_WORKERS; i++) {
            KafkaEmailConsumer consumer = new KafkaEmailConsumer(
                    HIGH_TOPIC, HIGH_GROUP, i, highWorkers,
                    HIGH_BATCH_LIMIT, HIGH_WAIT_MS // 5 emails, 75ms wait
            );
            activeConsumers.add(consumer);
            consumerRunnerPool.submit(consumer);
        }

        // 2. Start LOW Priority Consumers (1 Consumers)
        for (int i = 0; i < LOW_WORKERS; i++) {
            KafkaEmailConsumer consumer = new KafkaEmailConsumer(
                    LOW_TOPIC, LOW_GROUP, i, lowWorkers,
                    LOW_BATCH_LIMIT, LOW_WAIT_MS // 30 emails, 200ms wait
            );
            activeConsumers.add(consumer);
            consumerRunnerPool.submit(consumer);
        }


        // --- Graceful Shutdown ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");

            // 1. Signal all consumers to stop looping
            for (KafkaEmailConsumer c : activeConsumers) c.shutdown();

            // 2. Shut down the pools (stop accepting NEW tasks)
            consumerRunnerPool.shutdown();
            highWorkers.shutdown();
            lowWorkers.shutdown();

            try {
                // 3. Wait long enough for CURRENT tasks (HTTP requests) to finish.
                // CHANGE: Increased from 5 to 15 seconds to exceed HTTP timeout (10s).
                if (!highWorkers.awaitTermination(15, TimeUnit.SECONDS)) highWorkers.shutdownNow();
                if (!lowWorkers.awaitTermination(15, TimeUnit.SECONDS)) lowWorkers.shutdownNow();

                // Don't forget to wait for the Consumer Runners too!
                if (!consumerRunnerPool.awaitTermination(5, TimeUnit.SECONDS)) consumerRunnerPool.shutdownNow();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.info("Shutdown complete.");
        }));

        Thread.currentThread().join();
    }

    // WireMock stubbing
    private static void configureWireMock() {
        try {
            String jsonPayload = "{"
                    + "\"request\": {\"method\": \"POST\", \"url\": \"/v1/send-email\"},"
                    + "\"response\": {"
                    + "  \"status\": 200,"
                    + "  \"headers\": {\"Content-Type\": \"application/json\"},"
                    + "  \"body\": \"{\\\"message\\\": \\\"Queued\\\", \\\"id\\\": \\\"fake-123\\\"}\""
                    + "}}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/__admin/mappings"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            logger.info("WireMock configured successfully.");
        } catch (Exception e) {
            logger.warn("Could not configure WireMock (is it running?): " + e.getMessage());
        }
    }
}