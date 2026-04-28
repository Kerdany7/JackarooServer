package com.jackaroo.jackaroo_backend.service;

import com.jackaroo.jackaroo_backend.dto.*;
import com.jackaroo.jackaroo_backend.model.GameRoom;
import com.jackaroo.jackaroo_backend.model.PlayerSlot;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final long DISCONNECT_TIMEOUT_MS = 15_000;
    private static final long GRACE_PERIOD_MS = 60_000;

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom rng = new SecureRandom();

    @Autowired(required = false)
    private SimpMessagingTemplate ws;

    // ── Room lifecycle ────────────────────────────────────────────────────────

    public LobbyState createRoom(String hostName) {
        String code = generateCode();
        String token = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(code);
        room.getSlots().add(new PlayerSlot(token, hostName, true));
        rooms.put(code, room);
        return buildLobbyState(room, token);
    }

    public LobbyState joinRoom(String roomCode, String playerName) {
        GameRoom room = getRoom(roomCode);
        if (room.getState() != GameRoom.State.WAITING)
            throw new IllegalStateException("Game already started.");
        if (room.isFull())
            throw new IllegalStateException("Room is full.");
        for (PlayerSlot slot : room.getSlots())
            if (slot.getPlayerName().equalsIgnoreCase(playerName))
                throw new IllegalStateException("Name '" + playerName + "' is already taken in this room.");
        String token = UUID.randomUUID().toString();
        room.getSlots().add(new PlayerSlot(token, playerName, false));
        broadcastLobby(room);
        return buildLobbyState(room, token);
    }

    public LobbyState getLobby(String roomCode, String sessionToken) {
        GameRoom room = getRoom(roomCode);
        validateSlot(room, sessionToken);
        return buildLobbyState(room, sessionToken);
    }

    public GameState startGame(String roomCode, String sessionToken) throws Exception {
        GameRoom room = getRoom(roomCode);
        PlayerSlot caller = validateSlot(room, sessionToken);
        if (!caller.isHost()) throw new IllegalStateException("Only the host can start the game.");
        if (room.getState() != GameRoom.State.WAITING) throw new IllegalStateException("Game already started.");
        if (room.getSlots().isEmpty()) throw new IllegalStateException("Need at least 1 player.");

        List<String> humanNames = room.getSlots().stream().map(PlayerSlot::getPlayerName).toList();
        Game game = new Game(humanNames);
        room.setGame(game);
        room.setState(GameRoom.State.IN_PROGRESS);

        for (int i = 0; i < room.getSlots().size(); i++) {
            PlayerSlot slot = room.getSlots().get(i);
            slot.setPlayerIndex(i);
            slot.setColour(game.getPlayers().get(i).getColour().toString());
        }

        GameState state = buildRoomGameState(room, sessionToken);
        if (ws != null) ws.convertAndSend("/topic/room/" + roomCode, buildRoomGameState(room));
        broadcastLobby(room);
        return state;
    }

    public void kickPlayerByName(String roomCode, String hostToken, String playerName) {
        GameRoom room = getRoom(roomCode);
        PlayerSlot host = validateSlot(room, hostToken);
        if (!host.isHost()) throw new IllegalStateException("Only host can kick.");
        if (room.getState() != GameRoom.State.WAITING) throw new IllegalStateException("Can only kick in lobby.");
        room.getSlots().removeIf(s -> s.getPlayerName().equalsIgnoreCase(playerName) && !s.isHost());
        broadcastLobby(room);
    }

    public void leaveRoom(String roomCode, String sessionToken) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) return;
        PlayerSlot slot = room.findSlot(sessionToken);
        if (slot == null) return;
        if (room.getState() == GameRoom.State.WAITING) {
            room.getSlots().remove(slot);
            if (room.getSlots().isEmpty()) { rooms.remove(roomCode); return; }
        } else {
            slot.setConnected(false);
            replacedWithCPU(room, slot);
        }
        broadcastLobby(room);
    }

    // ── Game actions ──────────────────────────────────────────────────────────

    public GameState selectCard(String roomCode, String sessionToken, int cardIndex) throws Exception {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        Player current = currentPlayer(room.getGame());
        Card card = current.getHand().get(cardIndex);
        room.getGame().selectCard(card);
        return broadcastAndReturn(room, sessionToken);
    }

    public GameState selectMarble(String roomCode, String sessionToken, int marbleIndex) throws Exception {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        Player current = currentPlayer(room.getGame());
        List<Marble> actionable = filterActionableMarbles(
            room.getGame().getBoard().getActionableMarbles(),
            current.getSelectedCard(),
            room.getGame().getActivePlayerColour()
        );
        if (marbleIndex < 0 || marbleIndex >= actionable.size())
            throw new IndexOutOfBoundsException("Marble index out of range.");
        room.getGame().selectMarble(actionable.get(marbleIndex));
        return broadcastAndReturn(room, sessionToken);
    }

    public GameState play(String roomCode, String sessionToken) throws Exception {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        room.getGame().playPlayerTurn();
        return broadcastAndReturn(room, sessionToken);
    }

    public GameState endTurn(String roomCode, String sessionToken) {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        room.getGame().endPlayerTurn();
        // Auto-play any CPU turns
        while (room.getGame().checkWin() == null && room.getGame().isCurrentPlayerCPU()) {
            try { room.getGame().playPlayerTurn(); } catch (Exception ignored) {}
            room.getGame().endPlayerTurn();
        }
        if (room.getGame().checkWin() != null)
            room.setState(GameRoom.State.FINISHED);
        return broadcastAndReturn(room, sessionToken);
    }

    public GameState deselect(String roomCode, String sessionToken) {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        room.getGame().deselectAll();
        return broadcastAndReturn(room, sessionToken);
    }

    public GameState getGameState(String roomCode, String sessionToken) {
        GameRoom room = getRoom(roomCode);
        validateSlot(room, sessionToken);
        if (room.getGame() == null) throw new IllegalStateException("Game not started yet.");
        return buildRoomGameState(room, sessionToken);
    }

    public GameState setSplitDistance(String roomCode, String sessionToken, int distance) throws Exception {
        GameRoom room = getRoom(roomCode);
        validateTurn(room, sessionToken);
        room.getGame().editSplitDistance(distance);
        return buildRoomGameState(room, sessionToken);
    }

    public void heartbeat(String roomCode, String sessionToken) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) return;
        PlayerSlot slot = room.findSlot(sessionToken);
        if (slot == null) return;
        slot.setLastHeartbeat(System.currentTimeMillis());
        if (!slot.isConnected()) {
            slot.setConnected(true);
            slot.setDisconnectedAt(0);
            broadcastLobby(room);
            if (room.getGame() != null && ws != null)
                ws.convertAndSend("/topic/room/" + roomCode, buildRoomGameState(room));
        }
    }

    // ── Scheduled disconnect detection ────────────────────────────────────────

    @Scheduled(fixedRate = 5000)
    public void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (GameRoom room : rooms.values()) {
            if (room.getState() == GameRoom.State.WAITING &&
                now - room.getCreatedAt() > 30 * 60_000) {
                rooms.remove(room.getRoomCode());
                continue;
            }
            if (room.getState() != GameRoom.State.IN_PROGRESS) continue;
            for (PlayerSlot slot : room.getSlots()) {
                if (!slot.isConnected()) {
                    if (slot.getDisconnectedAt() > 0 && now - slot.getDisconnectedAt() > GRACE_PERIOD_MS) {
                        replacedWithCPU(room, slot);
                        broadcastLobby(room);
                    }
                } else if (now - slot.getLastHeartbeat() > DISCONNECT_TIMEOUT_MS) {
                    slot.setConnected(false);
                    slot.setDisconnectedAt(now);
                    broadcastLobby(room);
                    if (ws != null)
                        ws.convertAndSend("/topic/room/" + room.getRoomCode(),
                            Map.of("type", "DISCONNECT", "player", slot.getPlayerName(), "gracePeriodSec", 60));
                }
            }
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void replacedWithCPU(GameRoom room, PlayerSlot slot) {
        slot.setConnected(false);
        if (ws != null)
            ws.convertAndSend("/topic/room/" + room.getRoomCode(),
                Map.of("type", "CPU_TAKEOVER", "player", slot.getPlayerName()));
    }

    private GameRoom getRoom(String code) {
        GameRoom room = rooms.get(code.toUpperCase());
        if (room == null) throw new NoSuchElementException("Room '" + code + "' not found.");
        return room;
    }

    private PlayerSlot validateSlot(GameRoom room, String token) {
        PlayerSlot slot = room.findSlot(token);
        if (slot == null) throw new IllegalStateException("Invalid session token for room " + room.getRoomCode());
        return slot;
    }

    private void validateTurn(GameRoom room, String token) {
        PlayerSlot slot = validateSlot(room, token);
        if (room.getGame() == null) throw new IllegalStateException("Game not started.");
        String currentColour = room.getGame().getActivePlayerColour().toString();
        if (!currentColour.equals(slot.getColour()))
            throw new IllegalStateException("It is not your turn.");
    }

    private Player currentPlayer(Game game) {
        return game.getPlayers().stream()
            .filter(p -> p.getColour() == game.getActivePlayerColour())
            .findFirst().orElseThrow();
    }

    private GameState broadcastAndReturn(GameRoom room, String token) {
        if (ws != null) ws.convertAndSend("/topic/room/" + room.getRoomCode(), buildRoomGameState(room));
        return buildRoomGameState(room, token);
    }

    private void broadcastLobby(GameRoom room) {
        if (ws != null)
            ws.convertAndSend("/topic/lobby/" + room.getRoomCode(), buildLobbyState(room, null));
    }

    /** Generic state (used for WebSocket broadcasts — no personal card data). */
    GameState buildRoomGameState(GameRoom room) {
        return buildRoomGameState(room, null);
    }

    /** Personalized state: includes myTurn, myColour, myCardNames for the requesting player. */
    GameState buildRoomGameState(GameRoom room, String token) {
        Game game = room.getGame();
        GameState state = new GameState();
        state.setRoomCode(room.getRoomCode());

        Player current = currentPlayer(game);
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
        var firePit = game.getFirePit();
        state.setFirePitCount(firePit.size());
        state.setFirePitTopCard(firePit.isEmpty() ? null : firePit.get(firePit.size() - 1).getDisplayName());
        state.setDeckCount(Deck.getPoolSize());
        state.setRoundCount(game.getRoundCount());
        state.setTurnInRound(game.getTurnInRound());

        Player nextPlayer = game.getNextPlayer();
        state.setNextPlayerName(nextPlayer.getName());
        state.setNextPlayerColour(nextPlayer.getColour().toString());

        state.setEvents(game.pollEvents());

        // Actionable marbles filtered by selected card
        List<Marble> actionableMarbles = filterActionableMarbles(
            game.getBoard().getActionableMarbles(),
            current.getSelectedCard(),
            game.getActivePlayerColour()
        );
        Map<Marble, Integer> actionableMap = new IdentityHashMap<>();
        for (int i = 0; i < actionableMarbles.size(); i++)
            actionableMap.put(actionableMarbles.get(i), i);

        List<Marble> selectedMarbles = current.getSelectedMarbles();

        // Track
        List<CellState> track = new ArrayList<>();
        for (Cell cell : game.getBoard().getTrack()) {
            Marble m = cell.getMarble();
            track.add(new CellState(
                m != null ? m.getColour().toString() : null,
                cell.getCellType().toString(), cell.isTrap(),
                m != null ? actionableMap.getOrDefault(m, -1) : -1,
                m != null && selectedMarbles.contains(m)
            ));
        }
        state.setTrack(track);

        // Safe zones
        List<SafeZoneState> szList = new ArrayList<>();
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
            szList.add(new SafeZoneState(sz.getColour().toString(), cells));
        }
        state.setSafeZones(szList);

        // Players
        List<com.jackaroo.jackaroo_backend.dto.PlayerInfo> infos = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            boolean isCPU = p instanceof CPU;
            String colour = p.getColour().toString();
            boolean disconnected = room.getSlots().stream()
                .anyMatch(s -> colour.equals(s.getColour()) && !s.isConnected());
            infos.add(new com.jackaroo.jackaroo_backend.dto.PlayerInfo(
                p.getName(), colour,
                p.getMarbles().size(), p.getHand().size(),
                !isCPU && !disconnected,
                p.getColour() == game.getActivePlayerColour()
            ));
        }
        state.setPlayers(infos);

        // Personalized fields (when a specific player is requesting)
        if (token != null) {
            PlayerSlot slot = room.findSlot(token);
            if (slot != null && slot.getColour() != null) {
                String myColour = slot.getColour();
                state.setMyColour(myColour);
                state.setMyTurn(myColour.equals(current.getColour().toString()));

                // Find the player in the game that corresponds to this slot
                Player myPlayer = game.getPlayers().stream()
                    .filter(p -> p.getColour().toString().equals(myColour))
                    .findFirst().orElse(null);
                if (myPlayer != null) {
                    state.setMyCardNames(myPlayer.getHand().stream().map(Card::getDisplayName).toList());
                    state.setMyCardDescriptions(myPlayer.getHand().stream().map(Card::getDescription).toList());
                    if (myPlayer.getSelectedCard() != null)
                        state.setMySelectedCardName(myPlayer.getSelectedCard().getDisplayName());
                }
            }
        }

        return state;
    }

    private LobbyState buildLobbyState(GameRoom room, String forToken) {
        LobbyState ls = new LobbyState();
        ls.setRoomCode(room.getRoomCode());
        ls.setSessionToken(forToken);
        ls.setRoomState(room.getState().name());
        List<SlotInfo> slotInfos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i < room.getSlots().size()) {
                PlayerSlot s = room.getSlots().get(i);
                slotInfos.add(new SlotInfo(i, s.getPlayerName(), s.isHost(), s.isConnected()));
            } else {
                slotInfos.add(new SlotInfo(i, null, false, false));
            }
        }
        ls.setSlots(slotInfos);
        if (forToken != null) {
            PlayerSlot caller = room.findSlot(forToken);
            ls.setHost(caller != null && caller.isHost());
            if (caller != null) ls.setSessionToken(forToken);
        }
        return ls;
    }

    private List<Marble> filterActionableMarbles(List<Marble> marbles, Card card, model.Colour ownerColour) {
        if (card == null) return marbles;
        String name = card.getName().toLowerCase();
        if (name.contains("burner"))
            return marbles.stream().filter(m -> m.getColour() != ownerColour).collect(Collectors.toList());
        if (name.contains("saver"))
            return marbles.stream().filter(m -> m.getColour() == ownerColour).collect(Collectors.toList());
        return marbles;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(rng.nextInt(CODE_CHARS.length())));
        String code = sb.toString();
        return rooms.containsKey(code) ? generateCode() : code;
    }

    public Map<String, GameRoom> getRooms() { return rooms; }
}
