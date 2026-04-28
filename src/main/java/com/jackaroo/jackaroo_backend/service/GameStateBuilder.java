package com.jackaroo.jackaroo_backend.service;

import com.jackaroo.jackaroo_backend.dto.CellState;
import com.jackaroo.jackaroo_backend.dto.SafeZoneState;
import engine.Game;
import engine.board.Cell;
import engine.board.SafeZone;
import model.Colour;
import model.card.Card;
import model.player.Marble;

import java.util.*;
import java.util.stream.Collectors;

final class GameStateBuilder {

    private GameStateBuilder() {}

    static List<Marble> filterActionableMarbles(List<Marble> all, Card card, Colour owner) {
        if (card == null) return all;
        String name = card.getName().toLowerCase();
        if (name.contains("burner"))
            return all.stream().filter(m -> m.getColour() != owner).collect(Collectors.toList());
        if (name.contains("saver"))
            return all.stream().filter(m -> m.getColour() == owner).collect(Collectors.toList());
        return all;
    }

    static Map<Marble, Integer> buildActionableMap(List<Marble> marbles) {
        Map<Marble, Integer> map = new IdentityHashMap<>();
        for (int i = 0; i < marbles.size(); i++)
            map.put(marbles.get(i), i);
        return map;
    }

    static List<CellState> buildTrack(Game game, Map<Marble, Integer> actionableMap, List<Marble> selected) {
        List<CellState> result = new ArrayList<>();
        for (Cell cell : game.getBoard().getTrack()) {
            Marble m = cell.getMarble();
            result.add(new CellState(
                m != null ? m.getColour().toString() : null,
                cell.getCellType().toString(),
                cell.isTrap(),
                m != null ? actionableMap.getOrDefault(m, -1) : -1,
                m != null && selected.contains(m)
            ));
        }
        return result;
    }

    static List<SafeZoneState> buildSafeZones(Game game, Map<Marble, Integer> actionableMap, List<Marble> selected) {
        List<SafeZoneState> result = new ArrayList<>();
        for (SafeZone sz : game.getBoard().getSafeZones()) {
            List<CellState> cells = new ArrayList<>();
            for (Cell cell : sz.getCells()) {
                Marble m = cell.getMarble();
                cells.add(new CellState(
                    m != null ? m.getColour().toString() : null,
                    "SAFE", false,
                    m != null ? actionableMap.getOrDefault(m, -1) : -1,
                    m != null && selected.contains(m)
                ));
            }
            result.add(new SafeZoneState(sz.getColour().toString(), cells));
        }
        return result;
    }
}
