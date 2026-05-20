package com.iplboard.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentTask {
    private String id;
    private AgentType agentType;
    private TaskStatus status;
    private String triggerEvent;
    private String generatedContent;
    private String imageBase64;       // for Meme agent — Imagen generated image
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AgentTask(AgentType agentType, String triggerEvent) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.agentType = agentType;
        this.status = TaskStatus.TODO;
        this.triggerEvent = triggerEvent;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public AgentType getAgentType() { return agentType; }
    public TaskStatus getStatus() { return status; }
    public String getTriggerEvent() { return triggerEvent; }
    public String getGeneratedContent() { return generatedContent; }
    public String getImageBase64() { return imageBase64; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public void setGeneratedContent(String content) { this.generatedContent = content; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
