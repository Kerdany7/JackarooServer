package com.jackaroo.jackaroo_backend.controller;

import com.jackaroo.jackaroo_backend.dto.GameState;
import com.jackaroo.jackaroo_backend.dto.LobbyState;
import com.jackaroo.jackaroo_backend.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/create")
    public LobbyState create(@RequestParam String hostName) {
        return roomService.createRoom(hostName);
    }

    @PostMapping("/{roomCode}/join")
    public LobbyState join(@PathVariable String roomCode, @RequestParam String playerName) {
        return roomService.joinRoom(roomCode, playerName);
    }

    @GetMapping("/{roomCode}")
    public LobbyState getLobby(@PathVariable String roomCode,
                               @RequestHeader("X-Session-Token") String token) {
        return roomService.getLobby(roomCode, token);
    }

    @PostMapping("/{roomCode}/start")
    public GameState startGame(@PathVariable String roomCode,
                               @RequestHeader("X-Session-Token") String token) throws Exception {
        return roomService.startGame(roomCode, token);
    }

    @PostMapping("/{roomCode}/kick")
    public void kick(@PathVariable String roomCode,
                     @RequestHeader("X-Session-Token") String hostToken,
                     @RequestParam String playerName) {
        roomService.kickPlayerByName(roomCode, hostToken, playerName);
    }

    @PostMapping("/{roomCode}/leave")
    public void leave(@PathVariable String roomCode,
                      @RequestHeader("X-Session-Token") String token) {
        roomService.leaveRoom(roomCode, token);
    }

    @PostMapping("/{roomCode}/heartbeat")
    public void heartbeat(@PathVariable String roomCode,
                          @RequestHeader("X-Session-Token") String token) {
        roomService.heartbeat(roomCode, token);
    }

    // Game actions within a room
    @PostMapping("/{roomCode}/select-card")
    public GameState selectCard(@PathVariable String roomCode,
                                @RequestHeader("X-Session-Token") String token,
                                @RequestParam int cardIndex) throws Exception {
        return roomService.selectCard(roomCode, token, cardIndex);
    }

    @PostMapping("/{roomCode}/select-marble")
    public GameState selectMarble(@PathVariable String roomCode,
                                  @RequestHeader("X-Session-Token") String token,
                                  @RequestParam int marbleIndex) throws Exception {
        return roomService.selectMarble(roomCode, token, marbleIndex);
    }

    @PostMapping("/{roomCode}/play")
    public GameState play(@PathVariable String roomCode,
                          @RequestHeader("X-Session-Token") String token) throws Exception {
        return roomService.play(roomCode, token);
    }

    @PostMapping("/{roomCode}/end-turn")
    public GameState endTurn(@PathVariable String roomCode,
                             @RequestHeader("X-Session-Token") String token) {
        return roomService.endTurn(roomCode, token);
    }

    @PostMapping("/{roomCode}/deselect")
    public GameState deselect(@PathVariable String roomCode,
                              @RequestHeader("X-Session-Token") String token) {
        return roomService.deselect(roomCode, token);
    }

    @GetMapping("/{roomCode}/game-state")
    public GameState getGameState(@PathVariable String roomCode,
                                  @RequestHeader("X-Session-Token") String token) {
        return roomService.getGameState(roomCode, token);
    }

    @PostMapping("/{roomCode}/set-split")
    public GameState setSplitDistance(@PathVariable String roomCode,
                                      @RequestHeader("X-Session-Token") String token,
                                      @RequestParam int distance) throws Exception {
        return roomService.setSplitDistance(roomCode, token, distance);
    }
}
