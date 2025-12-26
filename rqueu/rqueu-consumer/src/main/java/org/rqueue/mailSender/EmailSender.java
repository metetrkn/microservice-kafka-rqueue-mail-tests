package org.rqueue.mailSender;

import org.sharedLib.EmailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private final String mailgunUrl;
    private final String apiKey;
    private final String defaultFrom; // Renamed for clarity
    private final HttpClient client;

    public EmailSender(@Value("${mail.provider.url}") String mailgunUrl,
                       @Value("${mail.provider.key}") String apiKey) {
        this.mailgunUrl = mailgunUrl;
        this.apiKey = apiKey;
        this.defaultFrom = "sender@example.com";
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendEmail(EmailDTO emailDto) {
        // 1. Determine "From" (Use DTO if available, else default)
        String finalFrom = (emailDto.getFrom() != null && !emailDto.getFrom().isEmpty())
                ? emailDto.getFrom()
                : defaultFrom;

        // 2. Build form data for WireMock/Mailgun
        String formData = buildFormData(finalFrom, emailDto.getTo(), emailDto.getSubject(), "Body content here...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mailgunUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(apiKey.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        // 3. Send and Log exactly what you requested
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        long lag = System.currentTimeMillis() - emailDto.getCreatedAt().getTime();

                        // --- EXACT LOG FORMAT ---
                        logger.info("| From: {} | To: {} | Subject: {} | HTTP: 200 OK | Lag: {}ms",
                                finalFrom,
                                emailDto.getTo(),
                                emailDto.getSubject(),
                                lag
                        );
                        // ------------------------

                    } else {
                        String errorMsg = String.format("Error, Mail Provider failed. Status: %d | Body: %s", response.statusCode(), response.body());
                        logger.error(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                })
                .exceptionally(ex -> {
                    throw new RuntimeException("Error, Network error sending email: " + ex.getMessage(), ex);
                })
                .join();
    }

    private String buildFormData(String from, String to, String subject, String body) {
        return "from=" + encode(from) +
                "&to=" + encode(to) +
                "&subject=" + encode(subject) +
                "&text=" + encode(body);
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}