# Jackaroo — Multiplayer Backend

> Spring Boot backend for Jackaroo, a real-time online multiplayer card board game. Handles room management, game state, turn logic, and live updates via WebSocket (STOMP).

**Frontend repo:** [JackarooFrontend](https://github.com/Kerdany7) <!-- replace with actual link -->

---

## What is Jackaroo?

Jackaroo is a popular Egyptian card board game where players race their marbles around a 100-cell track using a deck of special cards. Each card has a unique ability — some move marbles forward, some swap, some send marbles back to base. This project brings it online, allowing real players to join a shared room and play together in real time.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Real-time | WebSocket (STOMP over SockJS) |
| API | REST (JSON) |
| Build | Maven |
| Game Engine | Custom OOP engine (ported from university project) |

---

## Architecture

The backend is split into two layers:

**API / Application layer** (`com.jackaroo.jackaroo_backend`)
- `LobbyController` — room creation, joining, leaving, kicking, heartbeat
- `GameController` — single-player game actions (start, select card, select marble, play, end turn, CPU step)
- `RoomService` — manages active game rooms, session tokens, and routes player actions to the correct room
- `GameService` — wraps the game engine and exposes game actions as a stateful service
- `WebSocketConfig` — STOMP broker on `/ws`, publishes live game state updates to `/topic/...`
- DTOs — `GameState`, `LobbyState`, `CellState`, `SafeZoneState`, `PlayerInfo`, `SlotInfo`

**Game Engine** (`engine` / `model`)
- `Game` — main game controller, manages players, turns, fire pit (discard pile), and win detection
- `Board` — 100-cell circular track with NORMAL, ENTRY, BASE, and TRAP cell types
- `SafeZone` — per-player safe zone cells
- `Card` hierarchy — abstract `Card` with 12 concrete types: Ace, Four, Five, Seven, Ten, Jack, King, Queen, Burner, Saver, and wild variants
- `Deck` — loaded from `Cards.csv`, handles drawing and reshuffling
- `Player` / `CPU` — players hold a hand of cards and 4 marbles; CPU plays automatically

---

## Room Flow

```
POST /api/lobby/create?hostName=     → creates room, returns roomCode + session token
POST /api/lobby/{roomCode}/join      → join room, returns session token
POST /api/lobby/{roomCode}/start     → host starts game (requires X-Session-Token header)
POST /api/lobby/{roomCode}/select-card
POST /api/lobby/{roomCode}/select-marble
POST /api/lobby/{roomCode}/play
POST /api/lobby/{roomCode}/end-turn
POST /api/lobby/{roomCode}/deselect
POST /api/lobby/{roomCode}/set-split
POST /api/lobby/{roomCode}/heartbeat
POST /api/lobby/{roomCode}/leave
POST /api/lobby/{roomCode}/kick
GET  /api/lobby/{roomCode}/game-state
```

All room actions require an `X-Session-Token` header returned at join/create time.

---

## Running Locally

**Prerequisites:** Java 21, Maven

```bash
git clone https://github.com/Kerdany7/JackarooServer.git
cd JackarooServer
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

Make sure the frontend is configured to point to `http://localhost:8080` (or update CORS in `CorsConfig.java`).

---

## Project Structure

```
src/main/java/
├── com/jackaroo/jackaroo_backend/
│   ├── config/          # CORS + WebSocket config
│   ├── controller/      # REST endpoints (Lobby + Game)
│   ├── dto/             # JSON response shapes
│   ├── model/           # GameRoom, PlayerSlot
│   └── service/         # GameService, RoomService
├── engine/              # Game loop, Board, BoardManager
├── model/               # Card hierarchy, Player, Marble, Deck
└── exception/           # Custom game exceptions
src/main/resources/
└── Cards.csv            # Card definitions loaded at runtime
```

---

## Status

> Work in progress — core gameplay is functional, some edge cases are still being ironed out.
