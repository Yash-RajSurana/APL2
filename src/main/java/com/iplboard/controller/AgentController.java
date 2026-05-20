package com.iplboard.controller;

import com.iplboard.model.AgentTask;
import com.iplboard.model.MatchContext;
import com.iplboard.service.AgentOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgentController {

    @Autowired
    private AgentOrchestrator orchestrator;

    @GetMapping("/tasks")
    public Map<String, List<AgentTask>> getTasks() {
        return orchestrator.getTasksByStatus();
    }

    @GetMapping("/match")
    public MatchContext getMatch() {
        return orchestrator.getCurrentMatchContext();
    }

    @PostMapping("/trigger")
    public Map<String, String> triggerAgents() {
        orchestrator.manualTrigger();
        return Map.of("status", "triggered", "message", "All 4 agents triggered!");
    }
}
