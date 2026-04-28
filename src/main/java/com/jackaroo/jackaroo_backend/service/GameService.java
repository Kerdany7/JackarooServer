package com.jackaroo.jackaroo_backend.service;

import com.jackaroo.jackaroo_backend.dto.*;
import engine.Game;
import model.card.Card;
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
        state.setDeckCount(game.getDeckCount());
        state.setRoundCount(game.getRoundCount());
        state.setTurnInRound(game.getTurnInRound());

        Player nextPlayer = game.getNextPlayer();
        state.setNextPlayerName(nextPlayer.getName());
        state.setNextPlayerColour(nextPlayer.getColour().toString());

        state.setEvents(game.pollEvents());

        List<Marble> actionable = GameStateBuilder.filterActionableMarbles(
            game.getBoard().getActionableMarbles(),
            current.getSelectedCard(),
            game.getActivePlayerColour()
        );
        Map<Marble, Integer> actionableMap = GameStateBuilder.buildActionableMap(actionable);
        List<Marble> selected = current.getSelectedMarbles();

        state.setTrack(GameStateBuilder.buildTrack(game, actionableMap, selected));
        state.setSafeZones(GameStateBuilder.buildSafeZones(game, actionableMap, selected));

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

    Game getGame() { return game; }
}
