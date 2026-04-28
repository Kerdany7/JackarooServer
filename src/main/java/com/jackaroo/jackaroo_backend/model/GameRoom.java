package com.jackaroo.jackaroo_backend.model;

import engine.Game;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    public enum State { WAITING, IN_PROGRESS, FINISHED }

    private final String roomCode;
    private final long createdAt;
    private State state;
    private final List<PlayerSlot> slots;
    private Game game;

    public GameRoom(String roomCode) {
        this.roomCode = roomCode;
        this.createdAt = System.currentTimeMillis();
        this.state = State.WAITING;
        this.slots = new ArrayList<>();
    }

    public String getRoomCode() { return roomCode; }
    public long getCreatedAt() { return createdAt; }
    public State getState() { return state; }
    public void setState(State v) { this.state = v; }
    public List<PlayerSlot> getSlots() { return slots; }
    public Game getGame() { return game; }
    public void setGame(Game v) { this.game = v; }

    public boolean isFull() { return slots.size() >= 4; }

    public PlayerSlot findSlot(String sessionToken) {
        return slots.stream()
            .filter(s -> s.getSessionToken().equals(sessionToken))
            .findFirst()
            .orElse(null);
    }

    public PlayerSlot getHostSlot() {
        return slots.stream().filter(PlayerSlot::isHost).findFirst().orElse(null);
    }
}
