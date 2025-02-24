package com.example.playground.kafka.demo;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/***
 * Helper class for producer such as reading json and making REST calls
 */
public class ProducerHelper {

    private ProducerHelper() {

    }

    public static void sendPostRequest(String apiUrl, String jsonPayload) {
        sendRequest(apiUrl, jsonPayload, HttpMethod.POST);
    }

    public static void sendDeleteRequest(String apiUrl, String jsonPayload) {
       sendRequest(apiUrl, jsonPayload, HttpMethod.DELETE);
    }

    private static void sendRequest(String apiUrl, String jsonPayload, HttpMethod method) {
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080").build();
        webClient.method(method)
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking call (use reactive handling for async)
    }

    public static String getJsonPayload(String jsonClassPath) throws IOException, URISyntaxException {
        Path jsonPath = Paths.get(Objects.requireNonNull(ProducerHelper.class.getClassLoader().getResource(jsonClassPath)).toURI());
        return new String(Files.readAllBytes(jsonPath));
    }
}
