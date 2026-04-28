package com.jackaroo.jackaroo_backend.dto;

public class CellState {
    private String marbleColour;  // null if empty
    private String cellType;      // NORMAL, BASE, ENTRY, SAFE
    private boolean trap;
    private int actionableIndex;  // -1 if not selectable by current player
    private boolean selected;     // true if this marble is currently selected

    public CellState() {}

    public CellState(String marbleColour, String cellType, boolean trap, int actionableIndex, boolean selected) {
        this.marbleColour = marbleColour;
        this.cellType = cellType;
        this.trap = trap;
        this.actionableIndex = actionableIndex;
        this.selected = selected;
    }

    public String getMarbleColour() { return marbleColour; }
    public void setMarbleColour(String v) { this.marbleColour = v; }
    public String getCellType() { return cellType; }
    public void setCellType(String v) { this.cellType = v; }
    public boolean isTrap() { return trap; }
    public void setTrap(boolean v) { this.trap = v; }
    public int getActionableIndex() { return actionableIndex; }
    public void setActionableIndex(int v) { this.actionableIndex = v; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean v) { this.selected = v; }
}
