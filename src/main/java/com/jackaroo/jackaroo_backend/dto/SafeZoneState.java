package com.jackaroo.jackaroo_backend.dto;

import java.util.List;

public class SafeZoneState {
    private String ownerColour;
    private List<CellState> cells; // always 4 cells

    public SafeZoneState() {}

    public SafeZoneState(String ownerColour, List<CellState> cells) {
        this.ownerColour = ownerColour;
        this.cells = cells;
    }

    public String getOwnerColour() { return ownerColour; }
    public void setOwnerColour(String v) { this.ownerColour = v; }
    public List<CellState> getCells() { return cells; }
    public void setCells(List<CellState> v) { this.cells = v; }
}
