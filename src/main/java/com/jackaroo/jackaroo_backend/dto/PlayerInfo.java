package com.jackaroo.jackaroo_backend.dto;

public class PlayerInfo {
    private String name;
    private String colour;
    private int homeCount;
    private int handSize;
    private boolean human;
    private boolean current;

    public PlayerInfo() {}

    public PlayerInfo(String name, String colour, int homeCount, int handSize, boolean human, boolean current) {
        this.name = name;
        this.colour = colour;
        this.homeCount = homeCount;
        this.handSize = handSize;
        this.human = human;
        this.current = current;
    }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getColour() { return colour; }
    public void setColour(String v) { this.colour = v; }
    public int getHomeCount() { return homeCount; }
    public void setHomeCount(int v) { this.homeCount = v; }
    public int getHandSize() { return handSize; }
    public void setHandSize(int v) { this.handSize = v; }
    public boolean isHuman() { return human; }
    public void setHuman(boolean v) { this.human = v; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean v) { this.current = v; }
}
