package com.iplboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplboard.model.MatchContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class MemeImageService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.image.url}")
    private String imageUrl;

    private final WebClient webClient = WebClient.builder()
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build())
        .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generates a cricket meme image using Imagen 4 Fast via :predict endpoint.
     * Returns base64-encoded PNG, or null if generation fails.
     */
    public String generateMemeImage(MatchContext context) {
        String prompt = buildMemePrompt(context);

        try {
            // Imagen 4 :predict format
            Map<String, Object> requestBody = Map.of(
                "instances", List.of(Map.of("prompt", prompt)),
                "parameters", Map.of(
                    "sampleCount", 1,
                    "aspectRatio", "16:9"
                )
            );

            String response = webClient.post()
                .uri(imageUrl + "?key=" + apiKey)
                .header("content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = mapper.readTree(response);

            // Imagen predict returns: { "predictions": [{ "bytesBase64Encoded": "...", "mimeType": "image/png" }] }
            JsonNode predictions = root.path("predictions");
            if (predictions.isArray() && predictions.size() > 0) {
                String base64 = predictions.get(0).path("bytesBase64Encoded").asText("");
                if (!base64.isEmpty()) {
                    System.out.println("🎨 Meme image generated successfully!");
                    return base64;
                }
            }

            System.err.println("Imagen: no image in response. Full response: " + response.substring(0, Math.min(500, response.length())));
        } catch (Exception e) {
            System.err.println("Imagen API error: " + e.getMessage());
        }

        return null;
    }

    private String buildMemePrompt(MatchContext context) {
        String event = context.getRecentEvent();
        String match = context.getMatchTitle();
        String tone = getMemeStyle(event);

        return String.format(
            "Create a vibrant, funny IPL cricket meme image. Event: \"%s\" in match %s. " +
            "Style: %s. Colorful, cartoon/comic style, funny for Indian cricket fans. " +
            "Bold white Impact font meme text at top and bottom with witty caption about the event.",
            event, match, tone
        );
    }

    private String getMemeStyle(String event) {
        if (event.contains("WICKET") || event.contains("OUT"))
            return "shocked batsman walking back, bowler celebrating wildly";
        if (event.contains("SIX"))
            return "massive six, ball flying out of stadium, crowd going crazy";
        if (event.contains("FOUR"))
            return "ball racing to boundary, fielder diving and missing";
        if (event.contains("MAIDEN"))
            return "dominant bowler, frustrated batsman, intense pressure";
        if (event.contains("50") || event.contains("100") || event.contains("Partnership"))
            return "batsman raising bat, crowd celebration, confetti";
        if (event.contains("DRS") || event.contains("Review"))
            return "big screen review, players watching anxiously";
        if (event.contains("Wide") || event.contains("Free hit"))
            return "excited batsman, embarrassed bowler, free hit opportunity";
        return "exciting IPL cricket action, passionate Indian crowd";
    }
}
