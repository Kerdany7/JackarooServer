# Jackaroo — Multiplayer Backend

> Spring Boot backend for Jackaroo, a real-time online multiplayer card board game. Handles room management, game state, turn logic, and live updates via WebSocket (STOMP).

**Frontend repo:** [JackarooFrontend](https://github.com/Kerdany7) <!-- replace with actual link -->

---

## What is Jackaroo?

Jackaroo is a 4-player marble racing card game. Each player controls 4 marbles and tries to get all of them into their safe zone before anyone else. On each turn, a player plays a card from their hand to either move a marble, field a new marble from home, or use a special ability. Some cards let you attack opponents, swap marbles across the board, or split movement between two marbles.

The game plays over multiple rounds. Each round players receive a shrinking hand (4 cards, then 3, then 2, then 1) before the deck is reshuffled and hands are dealt again.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Real-time | WebSocket (STOMP over SockJS) |
| API | REST (JSON) |
| Build | Maven |
| Game Engine | Custom OOP engine |

---

## Architecture

The backend is split into two layers:

**API / Application layer** (`com.jackaroo.jackaroo_backend`)
- `LobbyController` — room creation, joining, leaving, kicking, heartbeat
- `GameController` — single-player game actions
- `RoomService` — manages active game rooms, session tokens, and routes player actions to the correct room
- `GameService` — wraps the game engine and exposes game actions as a stateful service
- `WebSocketConfig` — STOMP broker on `/ws`, publishes live game state updates to `/topic/...`
- DTOs — `GameState`, `LobbyState`, `CellState`, `SafeZoneState`, `PlayerInfo`, `SlotInfo`

**Game Engine** (`engine` / `model`)
- `Game` — main controller managing players, turns, the fire pit (discard pile), round progression, and win detection
- `Board` — 100-cell circular track with NORMAL, ENTRY, BASE, and TRAP cell types, plus one SafeZone per player
- `Card` hierarchy — abstract `Card` with concrete implementations:

| Card | Effect |
|---|---|
| Ace | Field a marble from home, or move 1 step |
| King | Field a marble from home, or move 13 steps |
| Jack | Swap one of your marbles with an opponent's |
| Seven | Move 7 steps, or split the movement between two marbles |
| Four | Move backward 4 steps |
| Five | Move forward 5 steps |
| Ten | Move forward 10 steps |
| Queen | Move forward 12 steps |
| Burner (wild) | Destroy any opponent marble, sending it back home |
| Saver (wild) | Send one of your marbles directly to your safe zone |

- `Deck` — loaded from `Cards.csv`, handles drawing and reshuffling into the fire pit when the pool runs low
- `Player` / `CPU` — players hold a hand of cards and up to 4 marbles; CPU plays automatically

---

## Room & Game Flow

```
POST /api/lobby/create?hostName=          → create room, returns roomCode + session token
POST /api/lobby/{roomCode}/join           → join room, returns session token
POST /api/lobby/{roomCode}/start          → host starts the game
POST /api/lobby/{roomCode}/select-card    → select a card from hand
POST /api/lobby/{roomCode}/select-marble  → select a marble to act on
POST /api/lobby/{roomCode}/play           → execute the selected card + marble
POST /api/lobby/{roomCode}/end-turn       → discard selected card and end turn
POST /api/lobby/{roomCode}/deselect       → deselect current card/marble
POST /api/lobby/{roomCode}/set-split      → set split distance for Seven card
POST /api/lobby/{roomCode}/heartbeat      → keep connection alive
POST /api/lobby/{roomCode}/leave          → leave the room
POST /api/lobby/{roomCode}/kick           → host kicks a player
GET  /api/lobby/{roomCode}/game-state     → fetch current game state
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

Server starts on `http://localhost:8080`. Make sure the frontend is configured to point to this URL (or update the allowed origins in `CorsConfig.java`).

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

> Work in progress — core gameplay is functional, some edge cases are still being worked on.
