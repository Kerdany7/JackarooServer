package com.jackaroo.jackaroo_backend.dto;

import java.util.List;

public class LobbyState {
    private String roomCode;
    private String sessionToken;
    private String roomState;   // WAITING, IN_PROGRESS, FINISHED
    private List<SlotInfo> slots;
    private boolean host;

    public LobbyState() {}

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String v) { this.roomCode = v; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String v) { this.sessionToken = v; }
    public String getRoomState() { return roomState; }
    public void setRoomState(String v) { this.roomState = v; }
    public List<SlotInfo> getSlots() { return slots; }
    public void setSlots(List<SlotInfo> v) { this.slots = v; }
    public boolean isHost() { return host; }
    public void setHost(boolean v) { this.host = v; }
}
