package org.rqueue.producer;

import org.sharedLib.EmailDTO;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.github.sonus21.rqueue.core.RqueueEndpointManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext; // <--- NEW IMPORT
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.rqueue.enums.Subject;

@Component
@RequiredArgsConstructor
@Slf4j
public class RqueueProducerRunner implements CommandLineRunner {

    private final RqueueMessageEnqueuer rqueueEnqueuer;
    private final RqueueEndpointManager rqueueEndpointManager;
    private final ConfigurableApplicationContext context;

    private static final String HIGH_PRIORITY_QUEUE = "high-priority-mails";
    private static final String LOW_PRIORITY_QUEUE = "low-priority-mails";

    @Override
    public void run(String... args) throws Exception {
        log.info("=== STARTING PRODUCER WORKER ===");

        rqueueEndpointManager.registerQueue(HIGH_PRIORITY_QUEUE, HIGH_PRIORITY_QUEUE);
        rqueueEndpointManager.registerQueue(LOW_PRIORITY_QUEUE, LOW_PRIORITY_QUEUE);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                sendOneByOne(HIGH_PRIORITY_QUEUE, 1000, Subject.VIP.name());
            } catch (Exception e) {
                log.error("Error in High Priority", e);
            }
        });

        executor.submit(() -> {
            try {
                sendOneByOne(LOW_PRIORITY_QUEUE, 10000, Subject.STANDARD.name());
            } catch (Exception e) {
                log.error("Error in Low Priority", e);
            }
        });

        executor.shutdown();

        // Wait for tasks to finish
        if (executor.awaitTermination(7, TimeUnit.MINUTES)) {
            log.info("All messages sent successfully.");
        } else {
            log.error("Timed out waiting for messages to send.");
        }

        log.info("=== WORKER FINISHED - SHUTTING DOWN ===");

        // --- THE KILL SWITCH ---
        // This closes Redis connections and kills the app gracefully
        SpringApplication.exit(context, () -> 0);
        System.exit(0);
    }

    private void sendOneByOne(String queueName, int count, String subject) {
        Date createdAt;

        for (int i = 0; i < count; i++) {
            createdAt = new Date();
            EmailDTO payload = new EmailDTO(
                    "noreply@hitract.se",
                    "user-" + i + "@student.com",
                    subject,
                    "Please process immediately.",
                    createdAt
            );

            rqueueEnqueuer.enqueue(queueName, payload);

            if (i % 10 == 0) log.info("Sent {}/{} messages to {}", i, count, queueName);
        }
    }
}