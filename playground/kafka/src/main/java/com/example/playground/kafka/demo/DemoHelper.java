package com.example.playground.kafka.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class DemoHelper {

    private static final Logger log = LoggerFactory.getLogger(DemoHelper.class);

    private DemoHelper() {

    }

    public static void sendPostRequest(String apiUrl, String jsonPayload) {
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080").build();
        String response = webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking call (use reactive handling for async)
        log.info("Response: {}", response);
    }

    public static String getJsonPayload(String jsonClassPath) throws IOException, URISyntaxException {
        Path jsonPath = Paths.get(Objects.requireNonNull(TransactionProducerDemo.class.getClassLoader().getResource(jsonClassPath)).toURI());
        return new String(Files.readAllBytes(jsonPath));
    }
}
