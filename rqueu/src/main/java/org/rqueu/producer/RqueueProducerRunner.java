package org.rqueu.producer;

import org.rqueu.dto.EmailDTO;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RqueueProducerRunner implements CommandLineRunner {

    private final RqueueMessageEnqueuer rqueueEnqueuer;

    private static final String HIGH_PRIORITY_QUEUE = "high-priority-mails";
    private static final String LOW_PRIORITY_QUEUE = "low-priority-mails";

    @Override
    public void run(String... args) throws Exception {
        // Mimic your Kafka setup: 2 threads running in parallel
        ExecutorService executor = Executors.newFixedThreadPool(2);

        log.info("Starting default Rqueue producer...");

        // Task 1: VIP Mails (10 messages)
        executor.submit(() -> sendOneByOne(HIGH_PRIORITY_QUEUE, 10));

        // Task 2: Standard Mails (200,000 messages)
        // Since we are not batching, this will make 200,000 separate network calls to Redis.
        executor.submit(() -> sendOneByOne(LOW_PRIORITY_QUEUE, 10));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        log.info("All producer threads finished.");
    }

    private void sendOneByOne(String queueName, int count) {
        String type = queueName.equals(HIGH_PRIORITY_QUEUE) ? "VIP" : "Standard";

        for (int i = 0; i < count; i++) {
            EmailDTO payload = new EmailDTO(
                    "user-" + i + "@" + type.toLowerCase() + ".com",
                    type + " Alert #" + i,
                    "Please process immediately.",
                    System.currentTimeMillis() // Timestamp
            );

            // Default Rqueue behavior:
            // 1. Serialize object to JSON
            // 2. Send to Redis
            // 3. Wait for Redis ACK
            rqueueEnqueuer.enqueue(queueName, payload);

            // Optional: Print status every 10k messages so you know it's working
            if (i > 0 && i % 10000 == 0) {
                log.info("{} messages sent to {}", i, queueName);
            }
        }
        log.info("Finished generating {} messages for {}", count, queueName);
    }
}