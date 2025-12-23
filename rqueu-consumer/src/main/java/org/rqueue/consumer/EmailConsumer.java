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
            // Updated log statement
            log.info("[VIP SUCCESS] To: {} | From: {} | Subject: '{}' | Created: {}",
                    email.getTo(), email.getFrom(), email.getSubject(), email.getCreatedAt().getTime());
        } catch (Exception e) {
            log.error("[VIP FAILED] To: {} | Subject: '{}'", email.getTo(), email.getSubject(), e);
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
            // Updated log statement
            log.info("[STD SUCCESS] To: {} | From: {} | Subject: '{}' | Created: {}",
                    email.getTo(), email.getFrom(), email.getSubject(), email.getCreatedAt().getTime());
        } catch (Exception e) {
            log.error("[STD FAILED] To: {} | Subject: '{}'", email.getTo(), email.getSubject(), e);
            throw new RuntimeException("Email service failed");
        }
    }

    private void processEmail(EmailDTO email) throws InterruptedException {
        Thread.sleep(200);
    }
}