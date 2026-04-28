package com.jackaroo.jackaroo_backend.model;

public class PlayerSlot {
    private final String sessionToken;
    private final String playerName;
    private final boolean host;
    private boolean connected;
    private long lastHeartbeat;
    private long disconnectedAt;   // 0 if connected
    private String colour;         // assigned when game starts
    private int playerIndex;       // index in Game.players, -1 until game starts

    public PlayerSlot(String sessionToken, String playerName, boolean host) {
        this.sessionToken = sessionToken;
        this.playerName = playerName;
        this.host = host;
        this.connected = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.disconnectedAt = 0;
        this.playerIndex = -1;
    }

    public String getSessionToken() { return sessionToken; }
    public String getPlayerName() { return playerName; }
    public boolean isHost() { return host; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean v) { this.connected = v; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(long v) { this.lastHeartbeat = v; }
    public long getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(long v) { this.disconnectedAt = v; }
    public String getColour() { return colour; }
    public void setColour(String v) { this.colour = v; }
    public int getPlayerIndex() { return playerIndex; }
    public void setPlayerIndex(int v) { this.playerIndex = v; }
}
