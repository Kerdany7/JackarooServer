package com.jackaroo.jackaroo_backend.controller;
import com.jackaroo.jackaroo_backend.dto.GameState;
import com.jackaroo.jackaroo_backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/start")
    public GameState startGame(@RequestParam String playerName) throws Exception {
        return gameService.startGame(playerName);
    }

    @GetMapping("/state")
    public GameState getGameState() {
        return gameService.getGameState();
    }

    @PostMapping("/select-card")
    public GameState selectCard(@RequestParam int cardIndex) throws Exception {
        return gameService.selectCard(cardIndex);
    }

    @PostMapping("/select-marble")
    public GameState selectMarble(@RequestParam int marbleIndex) throws Exception {
        return gameService.selectMarble(marbleIndex);
    }

    @PostMapping("/play")
    public GameState playTurn() throws Exception {
        return gameService.playTurn();
    }

    @PostMapping("/end-turn")
    public GameState endTurn() {
        return gameService.endTurn();
    }

    @PostMapping("/deselect")
    public GameState deselect() {
        return gameService.deselect();
    }

}




