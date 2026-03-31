package com.jackaroo.jackaroo_backend.service;
import com.jackaroo.jackaroo_backend.dto.GameState;
import engine.Game;
import model.card.Card;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GameService {
    private Game game;
    public GameState startGame(String playerName) throws Exception{
        game= new Game(playerName);
        return buildGameState();
    }
    public GameState getGameState() {
        return buildGameState();
    }
    private GameState buildGameState(){
        GameState state = new GameState();
        var currentPlayer= game.getPlayers().get(0);
        state.setCurrentPlayerName(currentPlayer.getName());
        state.setCurrentPlayerColour(currentPlayer.getColour().toString());
        //convert Card object to name of Cards for https usage
        List<String> cardNames = currentPlayer.getHand().stream().map(Card::getName).toList();
        state.setCardNames(cardNames);
        state.setCanPlay(game.canPlayTurn());
        var winner = game.checkWin();
        state.setWinner(winner != null ? winner.toString() : null);

        return state;
        }
    }

