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
public class ClaudeAgentService {

    @Value("${anthropic.api.key:disabled}")
    private String apiKey;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${anthropic.model:claude-sonnet-4-5}")
    private String model;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateContent(AgentType agentType, MatchContext context) {
        String prompt = buildPrompt(agentType, context);

        try {
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 300,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            String response = webClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            var jsonNode = mapper.readTree(response);
            return jsonNode.path("content").get(0).path("text").asText();

        } catch (Exception e) {
            System.err.println("Claude API error: " + e.getMessage());
            return getFallbackContent(agentType, context);
        }
    }

    private String buildPrompt(AgentType agentType, MatchContext context) {
        String matchInfo = String.format(
            "Match: %s | Score: %s | Recent: %s | Venue: %s",
            context.getMatchTitle(), context.getCurrentScore(),
            context.getRecentEvent(), context.getVenue()
        );

        return switch (agentType) {
            case MEME -> String.format("""
                You are a witty IPL cricket meme creator. Based on this match situation:
                %s
                
                Generate ONE hilarious, punchy meme caption or cricket banter post (max 2 lines).
                Make it funny, relatable for Indian cricket fans. Use emojis. Be creative!
                Only output the meme text, nothing else.""", matchInfo);

            case PREDICTION -> String.format("""
                You are a bold cricket prediction analyst. Based on this match situation:
                %s
                
                Generate ONE bold, data-backed prediction for the next 2-3 overs or match outcome.
                Include a win probability estimate. Be confident and specific.
                Format: [Team] Win Probability: XX%% | Prediction: [your prediction]
                Only output the prediction, nothing else.""", matchInfo);

            case STATS -> String.format("""
                You are a cricket stats expert. Based on this match situation:
                %s
                
                Generate a brief but insightful stats breakdown about the current match situation.
                Mention historical context, milestones, or interesting analytics angle (2-3 sentences).
                Only output the stats insight, nothing else.""", matchInfo);

            case HYPE -> String.format("""
                You are an ultra-enthusiastic IPL fan hype machine. Based on this match situation:
                %s
                
                Generate ONE high-energy, motivational fan post. Pure hype, lots of energy.
                Use caps for emphasis, emojis, cricket slang. Max 3 lines.
                Only output the hype post, nothing else.""", matchInfo);
        };
    }

    private String getFallbackContent(AgentType agentType, MatchContext context) {
        String event = context.getRecentEvent();
        String batting = context.getBattingTeam();
        String bowling = context.getBowlingTeam();
        String score = context.getCurrentScore();

        return switch (agentType) {
            case MEME -> buildMemeFallback(event);
            case PREDICTION -> String.format(
                "📊 %s Win Probability: 64%% | Prediction: After \"%s\" the momentum firmly shifts. Expect a 10-15 run over to follow. %s under pressure now.",
                batting, event, bowling);
            case STATS -> String.format(
                "📈 \"%s\" — %s currently on %s. Historical data shows teams at this score in T20s win 61%% of the time when the powerplay ends strong. Key partnership needed.",
                event, batting, score);
            case HYPE -> buildHypeFallback(event, batting);
        };
    }

    private String buildMemeFallback(String event) {
        if (event.contains("WICKET") || event.contains("OUT"))
            return "😂 The batter walked back like he meant to do that all along 💀🏏 \"Tactical retirement\" they call it 😭";
        if (event.contains("SIX"))
            return "🚀 That ball is still travelling!! The bowler's ego has left the stadium too 😭🔥 #MassiveMax";
        if (event.contains("FOUR"))
            return "😤 The fielder dove, the ball laughed, the crowd roared 🏏💨 \"BOUNDARY!\" hits different at this score";
        if (event.contains("MAIDEN"))
            return "🥶 Bowler just served a maiden over like he's got a flight to catch 😤 Dot. Dot. Dot. Dot. Dot. Dot. COLD.";
        if (event.contains("50") || event.contains("century") || event.contains("100"))
            return "🎉 FIFTY UP! The bat raise was so casual, like he parks cars for a living 😂🏏 Absolute class!";
        if (event.contains("DRS") || event.contains("Review"))
            return "📺 DRS Review: 3 mins of tension for the umpire to say \"yeah I was right\" 😭🫡 Technology, they said. It'll help, they said.";
        if (event.contains("Wide") || event.contains("Free hit"))
            return "🎁 FREE HIT! The bowler just handed out gifts like it's Diwali 🪔😂 The batter's eyes lit up like a kid on Christmas!";
        return "😂 Cricket: the only sport where 50,000 people hold their breath for a dot ball 🏏🤣 #IPL2026";
    }

    private String buildHypeFallback(String event, String battingTeam) {
        if (event.contains("SIX"))
            return String.format("🔥🔥 MAXIMUMMMM!! %s IS ON FIRE!! THE CROWD IS LOSING THEIR MINDS!! 💪🏏 WE GO AGAIN!! #IPL2026", battingTeam.split(" ")[0].toUpperCase());
        if (event.contains("WICKET") || event.contains("OUT"))
            return String.format("💥 YESSSS WICKET!! %s FIGHT BACK!! THIS IS WHAT WE CAME FOR!! THE MOMENTUM SHIFTS NOW!! 🔥🏏", battingTeam.contains("Royal") ? "RCB" : batting(battingTeam));
        if (event.contains("FOUR"))
            return String.format("⚡ FOUR!! %s DOMINATING!! EVERY SHOT BETTER THAN THE LAST!! THIS IS PURE CRICKET GOLD!! 🏏🔥🔥", battingTeam.split(" ")[0].toUpperCase());
        return String.format("🏏 LET'S GOOO %s!! THIS IS OUR MATCH!! THE ENERGY IS UNREAL!! BELIEVE!! 💪🔥🔥 #IPL2026", battingTeam.split(" ")[0].toUpperCase());
    }

    private String batting(String team) {
        String[] words = team.split(" ");
        return words[words.length - 1].toUpperCase();
    }
}
