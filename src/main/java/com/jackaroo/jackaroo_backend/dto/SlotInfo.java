package com.jackaroo.jackaroo_backend.dto;

public class SlotInfo {
    private int slotIndex;
    private String playerName;
    private boolean host;
    private boolean connected;

    public SlotInfo() {}

    public SlotInfo(int slotIndex, String playerName, boolean host, boolean connected) {
        this.slotIndex = slotIndex;
        this.playerName = playerName;
        this.host = host;
        this.connected = connected;
    }

    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int v) { this.slotIndex = v; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { this.playerName = v; }
    public boolean isHost() { return host; }
    public void setHost(boolean v) { this.host = v; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean v) { this.connected = v; }
}
