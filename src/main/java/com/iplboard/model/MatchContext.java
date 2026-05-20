package com.iplboard.model;

public class MatchContext {
    private String matchTitle;
    private String currentScore;
    private String recentEvent;
    private String venue;
    private String battingTeam;
    private String bowlingTeam;
    private boolean isLive;

    public MatchContext() {}

    public MatchContext(String matchTitle, String currentScore, String recentEvent,
                        String venue, String battingTeam, String bowlingTeam, boolean isLive) {
        this.matchTitle = matchTitle;
        this.currentScore = currentScore;
        this.recentEvent = recentEvent;
        this.venue = venue;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.isLive = isLive;
    }

    public String getMatchTitle() { return matchTitle; }
    public String getCurrentScore() { return currentScore; }
    public String getRecentEvent() { return recentEvent; }
    public String getVenue() { return venue; }
    public String getBattingTeam() { return battingTeam; }
    public String getBowlingTeam() { return bowlingTeam; }
    public boolean isLive() { return isLive; }
    public void setMatchTitle(String t) { this.matchTitle = t; }
    public void setCurrentScore(String s) { this.currentScore = s; }
    public void setRecentEvent(String e) { this.recentEvent = e; }
    public void setVenue(String v) { this.venue = v; }
    public void setBattingTeam(String t) { this.battingTeam = t; }
    public void setBowlingTeam(String t) { this.bowlingTeam = t; }
    public void setLive(boolean l) { this.isLive = l; }
}
