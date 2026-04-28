package com.jackaroo.jackaroo_backend.service;

import com.jackaroo.jackaroo_backend.dto.*;
import engine.Game;
import engine.board.Cell;
import engine.board.SafeZone;
import model.card.Card;
import model.card.Deck;
import model.player.CPU;
import model.player.Marble;
import model.player.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {
    private Game game;

    @Autowired(required = false)
    private SimpMessagingTemplate ws;

    public GameState startGame(String playerName) throws Exception {
        game = new Game(playerName);
        return buildGameState();
    }

    public GameState restartGame() throws Exception {
        if (game == null) throw new IllegalStateException("No game in progress to restart.");
        String humanName = game.getPlayers().stream()
            .filter(p -> !(p instanceof CPU))
            .findFirst()
            .map(Player::getName)
            .orElseThrow(() -> new IllegalStateException("No human player found."));
        game = new Game(humanName);
        return buildGameState();
    }

    public GameState getGameState() {
        if (game == null) throw new IllegalStateException("Game not started. POST /api/game/start first.");
        return buildGameState();
    }

    public GameState selectCard(int cardIndex) throws Exception {
        if (game == null) throw new IllegalStateException("Game not started.");
        Card card = currentPlayer().getHand().get(cardIndex);
        game.selectCard(card);
        return buildGameState();
    }

    public GameState selectMarble(int marbleIndex) throws Exception {
        if (game == null) throw new IllegalStateException("Game not started.");
        List<Marble> actionable = game.getBoard().getActionableMarbles();
        if (marbleIndex < 0 || marbleIndex >= actionable.size())
            throw new IndexOutOfBoundsException("Marble index " + marbleIndex + " out of range (0-" + (actionable.size()-1) + ").");
        game.selectMarble(actionable.get(marbleIndex));
        return buildGameState();
    }

    public GameState playTurn() throws Exception {
        if (game == null) throw new IllegalStateException("Game not started.");
        game.playPlayerTurn();
        return buildGameState();
    }

    /** Ends the current human turn only — does NOT auto-play CPUs. */
    public GameState endTurn() {
        if (game == null) throw new IllegalStateException("Game not started.");
        game.endPlayerTurn();
        return buildGameState();
    }

    /** Plays exactly one CPU turn if the current player is a CPU, then ends their turn. */
    public GameState cpuStep() {
        if (game == null) throw new IllegalStateException("Game not started.");
        if (!game.isCurrentPlayerCPU()) return buildGameState();
        try {
            game.playPlayerTurn();
        } catch (Exception ignored) {
            // CPU AI failure — pass the turn silently
        }
        game.endPlayerTurn();
        return buildGameState();
    }

    public GameState setSplitDistance(int distance) throws Exception {
        if (game == null) throw new IllegalStateException("Game not started.");
        game.editSplitDistance(distance);
        return buildGameState();
    }

    public GameState deselect() {
        if (game == null) throw new IllegalStateException("Game not started.");
        game.deselectAll();
        return buildGameState();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Player currentPlayer() {
        return game.getPlayers().stream()
            .filter(p -> p.getColour() == game.getActivePlayerColour())
            .findFirst()
            .orElseThrow();
    }

    GameState buildGameState() {
        GameState state = new GameState();
        Player current = currentPlayer();

        state.setCurrentPlayerName(current.getName());
        state.setCurrentPlayerColour(current.getColour().toString());
        state.setCardNames(current.getHand().stream().map(Card::getDisplayName).toList());
        state.setCardDescriptions(current.getHand().stream().map(Card::getDescription).toList());
        state.setCanPlay(game.canPlayTurn());
        var winner = game.checkWin();
        state.setWinner(winner != null ? winner.toString() : null);

        if (current.getSelectedCard() != null)
            state.setSelectedCardName(current.getSelectedCard().getDisplayName());

        // Extended fields
        ArrayList<Card> firePit = game.getFirePit();
        state.setFirePitCount(firePit.size());
        state.setFirePitTopCard(firePit.isEmpty() ? null : firePit.get(firePit.size() - 1).getDisplayName());
        state.setDeckCount(Deck.getPoolSize());
        state.setRoundCount(game.getRoundCount());
        state.setTurnInRound(game.getTurnInRound());

        Player nextPlayer = game.getNextPlayer();
        state.setNextPlayerName(nextPlayer.getName());
        state.setNextPlayerColour(nextPlayer.getColour().toString());

        state.setEvents(game.pollEvents());

        // Actionable marbles — filter based on selected card to avoid misleading UI rings
        List<Marble> actionableMarbles = filterActionableMarbles(
            game.getBoard().getActionableMarbles(),
            current.getSelectedCard(),
            game.getActivePlayerColour()
        );
        Map<Marble, Integer> actionableMap = new IdentityHashMap<>();
        for (int i = 0; i < actionableMarbles.size(); i++)
            actionableMap.put(actionableMarbles.get(i), i);

        List<Marble> selectedMarbles = current.getSelectedMarbles();

        // Track cells
        List<CellState> track = new ArrayList<>();
        for (Cell cell : game.getBoard().getTrack()) {
            Marble m = cell.getMarble();
            track.add(new CellState(
                m != null ? m.getColour().toString() : null,
                cell.getCellType().toString(),
                cell.isTrap(),
                m != null ? actionableMap.getOrDefault(m, -1) : -1,
                m != null && selectedMarbles.contains(m)
            ));
        }
        state.setTrack(track);

        // Safe zones
        List<SafeZoneState> szStates = new ArrayList<>();
        for (SafeZone sz : game.getBoard().getSafeZones()) {
            List<CellState> cells = new ArrayList<>();
            for (Cell cell : sz.getCells()) {
                Marble m = cell.getMarble();
                cells.add(new CellState(
                    m != null ? m.getColour().toString() : null,
                    "SAFE", false,
                    m != null ? actionableMap.getOrDefault(m, -1) : -1,
                    m != null && selectedMarbles.contains(m)
                ));
            }
            szStates.add(new SafeZoneState(sz.getColour().toString(), cells));
        }
        state.setSafeZones(szStates);

        // Players
        List<PlayerInfo> playerInfos = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            playerInfos.add(new PlayerInfo(
                p.getName(), p.getColour().toString(),
                p.getMarbles().size(), p.getHand().size(),
                !(p instanceof CPU),
                p.getColour() == game.getActivePlayerColour()
            ));
        }
        state.setPlayers(playerInfos);

        return state;
    }

    /**
     * Filters the raw actionable marble list based on which card is selected,
     * so the frontend doesn't show misleading selection rings on invalid targets.
     */
    private List<Marble> filterActionableMarbles(List<Marble> marbles, Card card, model.Colour ownerColour) {
        if (card == null) return marbles;
        String name = card.getName().toLowerCase();

        if (name.contains("burner")) {
            // Burner: only opponent marbles (not owner's own marbles)
            return marbles.stream()
                .filter(m -> m.getColour() != ownerColour)
                .collect(java.util.stream.Collectors.toList());
        }
        if (name.contains("saver")) {
            // Saver: only own marbles on track (safe-zone marbles can't be re-saved)
            return marbles.stream()
                .filter(m -> m.getColour() == ownerColour)
                .collect(java.util.stream.Collectors.toList());
        }
        return marbles;
    }

    Game getGame() { return game; }
}
