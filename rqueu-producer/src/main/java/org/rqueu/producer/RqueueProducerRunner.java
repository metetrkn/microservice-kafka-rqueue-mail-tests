package org.rqueu.producer;

import org.rqueu.dto.EmailDTO;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.github.sonus21.rqueue.core.RqueueEndpointManager; // <--- NEW IMPORT
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
    private final RqueueEndpointManager rqueueEndpointManager; // <--- INJECT THIS

    private static final String HIGH_PRIORITY_QUEUE = "high-priority-mails";
    private static final String LOW_PRIORITY_QUEUE = "low-priority-mails";

    @Override
    public void run(String... args) throws Exception {
        log.info("=== STARTING PRODUCER RUNNER ===");

        // --- CRITICAL FIX: Tell the Producer these queues represent valid destinations ---
        rqueueEndpointManager.registerQueue(HIGH_PRIORITY_QUEUE, HIGH_PRIORITY_QUEUE);
        rqueueEndpointManager.registerQueue(LOW_PRIORITY_QUEUE, LOW_PRIORITY_QUEUE);
        log.info("Queues manually registered!");
        // ---------------------------------------------------------------------------------

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                sendOneByOne(HIGH_PRIORITY_QUEUE, 10);
            } catch (Exception e) {
                log.error("CRITICAL ERROR in High Priority Thread", e);
            }
        });

        executor.submit(() -> {
            try {
                sendOneByOne(LOW_PRIORITY_QUEUE, 100);
            } catch (Exception e) {
                log.error("CRITICAL ERROR in Low Priority Thread", e);
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        log.info("=== PRODUCER RUNNER FINISHED ===");
    }

    private void sendOneByOne(String queueName, int count) {
        String type = queueName.equals(HIGH_PRIORITY_QUEUE) ? "VIP" : "Standard";

        for (int i = 0; i < count; i++) {
            EmailDTO payload = new EmailDTO(
                    "noreply@hitract.se",
                    "user-" + i + "@" + type.toLowerCase() + ".com",
                    type + " Alert #" + i,
                    "Please process immediately.",
                    System.currentTimeMillis()
            );

            rqueueEnqueuer.enqueue(queueName, payload);

            if (i % 10 == 0) log.info("Sent {}/{} messages to {}", i, count, queueName);
        }
    }
}