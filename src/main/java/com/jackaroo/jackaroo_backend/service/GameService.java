package com.jackaroo.jackaroo_backend.service;
import com.jackaroo.jackaroo_backend.dto.GameState;
import engine.Game;
import model.card.Card;
import model.player.Marble;
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
        var currentPlayer = game.getPlayers().stream().filter(p -> p.getColour() == game.getActivePlayerColour())
                .findFirst()
                .orElse(game.getPlayers().get(0));
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
        //Changed from using card name to using index due to potential conflicting cards
    private Card findCardByIndex(int CardIndex){
        return game.getPlayers().get(0).getHand().get(CardIndex);
    }
    public GameState selectCard(int cardIndex) throws Exception{
    Card card= findCardByIndex(cardIndex);
    game.selectCard(card);
    return buildGameState();
    }
    public GameState selectMarble(int marbleIndex) throws Exception {
        Marble marble = game.getBoard().getTrack().get(marbleIndex).getMarble();
        game.selectMarble(marble);
        return buildGameState();
    }
    public GameState playTurn() throws Exception {
        game.playPlayerTurn();
        return buildGameState();
    }

    public GameState endTurn() {
        game.endPlayerTurn();
        return buildGameState();
    }

    public GameState deselect() {
        game.deselectAll();
        return buildGameState();
    }
    }

