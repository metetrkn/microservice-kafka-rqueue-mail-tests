package org.rqueue.consumer;

import org.rqueue.dto.EmailDTO;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailConsumer {

    /**
     * HIGH PRIORITY QUEUE
     * Tuning: High concurrency (5-10 threads) to ensure VIP emails go out instantly.
     */
    @RqueueListener(value = "high-priority-mails", concurrency = "5-10")
    public void onHighPriorityMessage(EmailDTO email) {
         try {
            processEmail(email);
            log.info("[VIP SUCCESS] Sent to {}", email.getTo());
        } catch (Exception e) {
            log.error("[VIP FAILED] Could not send to {}", email.getTo(), e);
            // Throwing an exception triggers Rqueue to RETRY automatically
            throw new RuntimeException("Email service failed");
        }
    }

    /**
     * LOW PRIORITY QUEUE
     * Tuning: Low concurrency (1-2 threads) to save resources and avoid rate limits.
     */
    @RqueueListener(value = "low-priority-mails", concurrency = "1-2")
    public void onLowPriorityMessage(EmailDTO email) {
        try {
            processEmail(email);
            log.info("[STD SUCCESS] Sent to {}", email.getTo());
        } catch (Exception e) {
            log.error("[STD FAILED] Could not send to {}", email.getTo(), e);
            throw new RuntimeException("Email service failed");
        }
    }

    private void processEmail(EmailDTO email) throws InterruptedException {
        Thread.sleep(200);
    }
}