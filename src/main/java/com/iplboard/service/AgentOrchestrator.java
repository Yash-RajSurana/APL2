package com.iplboard.service;

import com.iplboard.model.AgentTask;
import com.iplboard.model.AgentType;
import com.iplboard.model.MatchContext;
import com.iplboard.model.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AgentOrchestrator {

    @Autowired
    private CricketDataService cricketDataService;

    @Autowired
    private GeminiAgentService geminiAgentService;

    @Autowired
    private MemeImageService memeImageService;

    private final List<AgentTask> allTasks = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private String lastKnownEvent = "";

    /**
     * Auto-trigger every 60 seconds — reduced from 15s to avoid API flooding.
     */
    @Scheduled(fixedDelay = 60000)
    public void triggerAgentsOnMatchEvent() {
        MatchContext context = cricketDataService.getLiveMatchContext();

        if (context.getRecentEvent().equals(lastKnownEvent)) return;

        lastKnownEvent = context.getRecentEvent();
        System.out.println("🏏 New event detected: " + context.getRecentEvent());

        for (AgentType agentType : AgentType.values()) {
            AgentTask task = new AgentTask(agentType, context.getRecentEvent());
            allTasks.add(task);

            if (agentType == AgentType.MEME) {
                // Meme: skip TODO stagger — start image generation IMMEDIATELY
                executor.submit(() -> processMemeTask(task, context));
            } else {
                // Text agents: visible TODO → PROCESSING → POSTED flow
                int idx = agentType.ordinal() - 1; // PREDICTION=0, STATS=1, HYPE=2
                executor.submit(() -> processTextTask(task, idx, context));
            }
        }
    }

    /**
     * Meme task — uses dynamic text memes (no image gen for now).
     */
    private void processMemeTask(AgentTask task, MatchContext context) {
        try {
            task.setStatus(TaskStatus.PROCESSING);
            System.out.println("🎭 Processing meme for: " + context.getRecentEvent());
            Thread.sleep(3000L); // visible processing time

            task.setGeneratedContent(buildTextMemeFallback(context.getRecentEvent()));
            task.setStatus(TaskStatus.POSTED);
            System.out.println("✅ Meme posted!");
        } catch (Exception e) {
            System.err.println("Meme task error: " + e.getMessage());
            task.setGeneratedContent(buildTextMemeFallback(context.getRecentEvent()));
            task.setStatus(TaskStatus.POSTED);
        }
    }

    /**
     * Text agent task — staggered TODO → PROCESSING (5s) → POSTED with Gemini text.
     */
    private void processTextTask(AgentTask task, int idx, MatchContext context) {
        try {
            // Stagger so cards appear one-by-one in TODO
            Thread.sleep(idx * 600L);          // 0s, 0.6s, 1.2s
            Thread.sleep(2000L);               // visible TODO time

            task.setStatus(TaskStatus.PROCESSING);
            System.out.println("⚙️ Processing: " + task.getAgentType().getDisplayName());
            Thread.sleep(5000L);               // visible PROCESSING time with spinner

            String content = geminiAgentService.generateContent(task.getAgentType(), context);
            task.setGeneratedContent(content);

            task.setStatus(TaskStatus.POSTED);
            System.out.println("✅ Posted: " + task.getAgentType().getDisplayName());

        } catch (Exception e) {
            System.err.println("Text task error: " + e.getMessage());
            task.setStatus(TaskStatus.TODO);
        }
    }

    private String buildTextMemeFallback(String event) {
        if (event.contains("WICKET") || event.contains("OUT"))
            return "😂 Batter walked back like he meant to do that all along 💀🏏 \"Tactical retirement\" they call it 😭";
        if (event.contains("SIX"))
            return "🚀 That ball is STILL travelling!! The bowler's ego left the stadium too 😭🔥 #MassiveMax";
        if (event.contains("FOUR"))
            return "😤 Fielder dove, ball laughed, crowd roared 🏏💨 BOUNDARY hits different at this score!";
        if (event.contains("MAIDEN"))
            return "🥶 Bowler sent a maiden over like he had a flight to catch 😤 Dot. Dot. Dot. Dot. Dot. Dot. ICE COLD.";
        if (event.contains("50") || event.contains("100") || event.contains("Partnership"))
            return "🎉 The bat raise was so casual, like he parks cars for a living 😂🏏 Pure class! Absolute legend!";
        if (event.contains("DRS") || event.contains("Review"))
            return "📺 3 mins of tension for the umpire to say \"yeah I was right\" 😭🫡 Technology they said. It'll help they said.";
        if (event.contains("Wide") || event.contains("Free hit"))
            return "🎁 FREE HIT!! Bowler handed out gifts like it's Diwali 🪔😂 Batter's eyes lit up like Christmas morning!";
        return "😂 Cricket: the only sport where 50,000 people hold their breath for a dot ball 🏏🤣 #IPL2026";
    }

    public List<AgentTask> getAllTasks() {
        // Keep only last 20 tasks
        int size = allTasks.size();
        int start = Math.max(0, size - 20);
        return new ArrayList<>(allTasks.subList(start, size));
    }

    public Map<String, List<AgentTask>> getTasksByStatus() {
        List<AgentTask> tasks = getAllTasks();
        return Map.of(
            "TODO",       tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).collect(Collectors.toList()),
            "PROCESSING", tasks.stream().filter(t -> t.getStatus() == TaskStatus.PROCESSING).collect(Collectors.toList()),
            "POSTED",     tasks.stream().filter(t -> t.getStatus() == TaskStatus.POSTED).collect(Collectors.toList())
        );
    }

    public MatchContext getCurrentMatchContext() {
        return cricketDataService.getLiveMatchContext();
    }

    public void manualTrigger() {
        lastKnownEvent = "";
        triggerAgentsOnMatchEvent();
    }
}
