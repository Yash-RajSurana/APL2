package com.iplboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplboard.model.AgentType;
import com.iplboard.model.MatchContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiAgentService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.text.url}")
    private String textUrl;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateContent(AgentType agentType, MatchContext context) {
        String prompt = buildPrompt(agentType, context);

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            );

            String response = webClient.post()
                .uri(textUrl + "?key=" + apiKey)
                .header("content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            var root = mapper.readTree(response);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText();

        } catch (Exception e) {
            System.err.println("Gemini text API error: " + e.getMessage());
            return getFallbackContent(agentType, context);
        }
    }

    private String buildPrompt(AgentType agentType, MatchContext context) {
        String matchInfo = String.format(
            "Match: %s | Score: %s | Recent Event: %s | Venue: %s | Batting: %s | Bowling: %s",
            context.getMatchTitle(), context.getCurrentScore(),
            context.getRecentEvent(), context.getVenue(),
            context.getBattingTeam(), context.getBowlingTeam()
        );

        return switch (agentType) {
            case PREDICTION -> String.format("""
                You are a bold IPL cricket prediction analyst. Match situation:
                %s

                Give ONE confident, data-backed prediction for the next 2-3 overs or final outcome.
                Include a win probability. Be specific and bold.
                Format: [Team] Win Probability: XX%% | Prediction: [bold call]
                Output the prediction only, no explanation.""", matchInfo);

            case STATS -> String.format("""
                You are an IPL cricket stats expert. Match situation:
                %s

                Give a brief (2-3 sentence) stats insight: milestone context, historical angle, or analytics.
                Make it feel like real expert commentary. Output the insight only.""", matchInfo);

            case HYPE -> String.format("""
                You are an ultra-hyped IPL fan. Match situation:
                %s

                Write ONE energetic fan post — pure hype, caps for emphasis, emojis, cricket slang.
                Max 3 lines. Output the post only.""", matchInfo);

            default -> "Describe this cricket event: " + matchInfo;
        };
    }

    private String getFallbackContent(AgentType agentType, MatchContext context) {
        String event = context.getRecentEvent();
        String batting = context.getBattingTeam();
        String bowling = context.getBowlingTeam();
        String score = context.getCurrentScore();

        return switch (agentType) {
            case PREDICTION -> String.format(
                "📊 %s Win Probability: 64%% | After \"%s\" the momentum shifts firmly. Expect %s to push 10-15 runs next over. %s under pressure.",
                batting, event, batting, bowling);
            case STATS -> String.format(
                "📈 \"%s\" — %s at %s. T20 data shows teams in this position win 61%% when wickets in hand. Critical phase ahead.",
                event, batting, score);
            case HYPE -> buildHypeFallback(event, batting);
            default -> "🏏 Great moment in this IPL match!";
        };
    }

    private String buildHypeFallback(String event, String battingTeam) {
        String name = battingTeam.split(" ")[0].toUpperCase();
        if (event.contains("SIX"))
            return "🔥🔥 MAXIMUMMMM!! " + name + " IS ON FIRE!! THE CROWD IS GOING INSANE!! 💪🏏 WE GO AGAIN!! #IPL2026";
        if (event.contains("WICKET") || event.contains("OUT"))
            return "💥 YESSSS WICKET!! FIGHT BACK TIME!! THIS IS WHAT WE CAME FOR!! THE MOMENTUM IS OURS!! 🔥🏏";
        if (event.contains("FOUR"))
            return "⚡ FOUR!! " + name + " DOMINATING!! EVERY SHOT IS PURE GOLD!! THIS IS IPL MAGIC!! 🏏🔥🔥";
        if (event.contains("50") || event.contains("HALF"))
            return "🎉 HALF CENTURY!! WHAT A KNOCK!! " + name + " FANS ARE GOING WILD!! KEEP IT UP!! 💪🏏🔥";
        return "🏏 LET'S GOOO " + name + "!! THIS IS OUR MATCH!! THE ENERGY IS UNREAL!! BELIEVE!! 💪🔥🔥 #IPL2026";
    }
}
