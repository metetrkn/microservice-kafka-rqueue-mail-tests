package org.rqueue.config;

import com.github.sonus21.rqueue.listener.RqueueMessageListenerContainer; // <--- NEW IMPORT
import lombok.RequiredArgsConstructor;
import org.rqueue.consumer.EmailConsumer;
import org.rqueue.mailSender.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Configuration
@RequiredArgsConstructor // <--- Generates constructor for 'rqueueContainer'
public class WireMockInitializer {

    @Autowired
    public final EmailConsumer emailConsumer;

    private static final Logger logger = LoggerFactory.getLogger(WireMockInitializer.class);
    private static final String WIREMOCK_ADMIN_URL = "http://localhost:8080/__admin/mappings";

    // 1. Inject the Rqueue Container so we can control it
    private final RqueueMessageListenerContainer rqueueContainer;

    @Bean
    public CommandLineRunner registerWireMockStubs() {
        return args -> {
            try {
                // (Stub JSON remains the same...)
                String stubJson = "{"
                        + "\"request\": {"
                        + "    \"method\": \"POST\","
                        + "    \"url\": \"/v3/sandbox.mailgun.org/messages\""
                        + "},"
                        + "\"response\": {"
                        + "    \"status\": 200,"
                        + "    \"body\": \"{\\\"id\\\": \\\"<2023.123@mailgun.org>\\\", \\\"message\\\": \\\"Queued\\\"}\","
                        + "    \"headers\": {"
                        + "        \"Content-Type\": \"application/json\""
                        + "    }"
                        + "}"
                        + "}";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(WIREMOCK_ADMIN_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(stubJson))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    logger.info("✅ WireMock Stub Registered!");

                    // 2. THE FIX: Start Rqueue NOW (only after stub is ready)
                    logger.info("🚀 Starting HighConsumers. Concurrency range: {}", emailConsumer.getConcurrencyHigh());
                    logger.info("🚀 Starting LowConsumers. Concurrency range: {}", emailConsumer.getConcurrencyLow());
                    rqueueContainer.start();

                } else {
                    logger.warn("⚠️ Failed to register WireMock stub. Status: {}", response.statusCode());
                }

            } catch (Exception e) {
                logger.error("❌ Connection Refused: WireMock is down. Rqueue will NOT start.", e);
            }
        };
    }
}