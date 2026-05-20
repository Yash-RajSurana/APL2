package com.iplboard.model;

public enum AgentType {
    MEME("🎭 Meme Agent"),
    PREDICTION("📊 Prediction Agent"),
    STATS("📈 Stats Agent"),
    HYPE("🔥 Hype Agent");

    private final String displayName;
    AgentType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
