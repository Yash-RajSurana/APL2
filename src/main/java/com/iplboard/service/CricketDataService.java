package com.iplboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplboard.model.MatchContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CricketDataService {

    @Value("${cricket.api.key}")
    private String apiKey;

    @Value("${cricket.api.url}")
    private String apiUrl;

    @Value("${cricket.use.mock:false}")
    private boolean useMock;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public MatchContext getLiveMatchContext() {
        if (useMock) return getMockContext();

        try {
            String response = webClient.get()
                .uri(apiUrl + "/currentMatches?apikey=" + apiKey + "&offset=0")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = mapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isArray() && data.size() > 0) {
                JsonNode match = data.get(0);

                for (JsonNode m : data) {
                    String name = m.path("name").asText("");
                    if (name.contains("IPL") || name.contains("Indian Premier")) {
                        match = m;
                        break;
                    }
                }

                String matchName = match.path("name").asText("IPL Match");
                String status = match.path("status").asText("In Progress");
                boolean isLive = "Live".equalsIgnoreCase(match.path("matchType").asText())
                    || status.toLowerCase().contains("live");

                JsonNode teams = match.path("teams");
                String team1 = teams.isArray() && teams.size() > 0 ? teams.get(0).asText("Team A") : "Team A";
                String team2 = teams.isArray() && teams.size() > 1 ? teams.get(1).asText("Team B") : "Team B";

                JsonNode scores = match.path("score");
                String scoreStr = "Score unavailable";
                if (scores.isArray() && scores.size() > 0) {
                    JsonNode s = scores.get(0);
                    scoreStr = s.path("inning").asText("") + ": "
                        + s.path("r").asText("0") + "/"
                        + s.path("w").asText("0")
                        + " (" + s.path("o").asText("0") + " ov)";
                }

                String venue = match.path("venue").asText("IPL Stadium");
                String recentEvent = generateRecentEvent(match);

                return new MatchContext(matchName, scoreStr, recentEvent, venue, team1, team2, isLive);
            }
        } catch (Exception e) {
            System.err.println("Cricket API error, falling back to mock: " + e.getMessage());
        }

        return getMockContext();
    }

    private String generateRecentEvent(JsonNode match) {
        String status = match.path("status").asText("");
        if (!status.isEmpty() && !status.equals("Match not started")) {
            return status;
        }
        return "Live match in progress";
    }

    private MatchContext getMockContext() {
        String[] events = {
            "WICKET! Virat Kohli caught at slip for 67",
            "SIX! MS Dhoni smashes one over long-on!",
            "FOUR! Rohit Sharma cuts it through the covers",
            "50 UP for Shubman Gill! Half-century in 34 balls",
            "MAIDEN OVER by Bumrah! Pressure building!",
            "Wide ball! Free hit coming up!",
            "DRS Review! Original decision stands - OUT!",
            "100 Partnership between the two batters!"
        };
        int idx = (int)(System.currentTimeMillis() / 30000) % events.length;

        return new MatchContext(
            "MI vs RCB - IPL 2026 Match 42",
            "MI: 156/4 (16.3 ov)",
            events[idx],
            "Wankhede Stadium, Mumbai",
            "Mumbai Indians",
            "Royal Challengers Bengaluru",
            true
        );
    }
}
