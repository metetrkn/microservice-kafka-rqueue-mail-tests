package org.rqueue.consumer;

import org.rqueue.mailSender.EmailSender;
import org.sharedLib.EmailDTO;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor // Lombok generates constructor for 'emailSender' injection
public class EmailConsumer {

    private final EmailSender emailSender;
    private final String concurrencyHigh  = "5-10";
    private final String concurrencyLow  = "1-2";

    /**
     * HIGH PRIORITY QUEUE
     */
    @RqueueListener(value = "high-priority-mails", concurrency = concurrencyHigh)
    public void onHighPriorityMessage(EmailDTO email) {
        log.info("[VIP START] Processing email for: {}", email.getTo());
        try {
            // Hand over to the sender service
            emailSender.sendEmail(email);
        } catch (Exception e) {
            log.error("[VIP FAILED] Could not send email to {}. Rqueue will retry.", email.getTo(), e);
            // Re-throw exception so Rqueue knows to retry this message later
            throw e;
        }
    }

    /**
     * LOW PRIORITY QUEUE
     */
    @RqueueListener(value = "low-priority-mails", concurrency = concurrencyLow)
    public void onLowPriorityMessage(EmailDTO email) {
        log.info("[STD START] Processing email for: {}", email.getTo());
        try {
            emailSender.sendEmail(email);
        } catch (Exception e) {
            log.error("[STD FAILED] Could not send email to {}. Rqueue will retry.", email.getTo(), e);
            throw e;
        }
    }

    // Getters for concurrency
    public String getConcurrencyHigh() {
        return concurrencyHigh;
    }

    public String getConcurrencyLow() {
        return concurrencyLow;
    }
}