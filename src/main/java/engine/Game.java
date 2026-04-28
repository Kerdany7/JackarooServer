package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import engine.board.Board;
import engine.board.SafeZone;
import exception.CannotDiscardException;
import exception.CannotFieldException;
import exception.GameException;
import exception.IllegalDestroyException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import model.Colour;
import model.card.Card;
import model.card.Deck;
import model.player.*;

@SuppressWarnings("unused")
public class Game implements GameManager {
    private final Board board;
    private final Deck deck;
    private final ArrayList<Player> players;
	private int currentPlayerIndex;
    private final ArrayList<Card> firePit;
    private int turn;
    private int roundCount;
    private final ArrayList<String> events;

    /** Single-player shorthand: one human + 3 CPUs. */
    public Game(String playerName) throws IOException {
        this(List.of(playerName));
    }

    /** Primary constructor. First N names are humans, remaining slots are filled by CPUs. */
    public Game(List<String> playerNames) throws IOException {
        turn = 0;
        currentPlayerIndex = 0;
        roundCount = 1;
        firePit = new ArrayList<>();
        events = new ArrayList<>();

        ArrayList<Colour> colourOrder = new ArrayList<>(Arrays.asList(Colour.values()));
        Collections.shuffle(colourOrder);

        this.board = new Board(colourOrder, this);
        this.deck = new Deck();
        this.deck.loadCardPool(this.board, this);

        this.players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i < playerNames.size())
                this.players.add(new Player(playerNames.get(i), colourOrder.get(i)));
            else
                this.players.add(new CPU("CPU " + (i - playerNames.size() + 1), colourOrder.get(i), this.board));
        }
        for (Player p : this.players)
            p.setHand(this.deck.drawCards());
    }
    
    public Board getBoard() {
        return board;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<Card> getFirePit() {
        return firePit;
    }
    
    public void selectCard(Card card) throws InvalidCardException {
        players.get(currentPlayerIndex).selectCard(card);
    }

    public void selectMarble(Marble marble) throws InvalidMarbleException {
        players.get(currentPlayerIndex).selectMarble(marble);
    }

    public void deselectAll() {
        players.get(currentPlayerIndex).deselectAll();
    }

    public void editSplitDistance(int splitDistance) throws SplitOutOfRangeException {
        if(splitDistance < 1 || splitDistance > 6)
            throw new SplitOutOfRangeException();

        board.setSplitDistance(splitDistance);
    }

    public boolean canPlayTurn() {
        return players.get(currentPlayerIndex).getHand().size() == (4 - turn);
    }

    public void playPlayerTurn() throws GameException {
        players.get(currentPlayerIndex).play();
    }

    public void endPlayerTurn() {
        Card selected = players.get(currentPlayerIndex).getSelectedCard();
        if (selected != null) {
            players.get(currentPlayerIndex).getHand().remove(selected);
            firePit.add(selected);
        }
        players.get(currentPlayerIndex).deselectAll();

        currentPlayerIndex = (currentPlayerIndex + 1) % 4;

        if(currentPlayerIndex == 0 && turn < 3)
            turn++;

        else if (currentPlayerIndex == 0 && turn == 3) {
        	turn = 0;
        	roundCount++;
        	for (Player p : players) {
        	    firePit.addAll(p.getHand());
        	    p.getHand().clear();
        	}
        	if (deck.getPoolSize() < 16) {
        	    deck.refillPool(firePit);
        	    firePit.clear();
        	}
        	for (Player p : players) {
                if (deck.getPoolSize() < 4) {
                    deck.refillPool(firePit);
                    firePit.clear();
                }
                p.setHand(deck.drawCards());
        	}
        }

    }

    public int getRoundCount() { return roundCount; }
    public int getTurnInRound() { return turn; }
    public int getDeckCount() { return deck.getPoolSize(); }

    public Player getNextPlayer() {
        return players.get((currentPlayerIndex + 1) % 4);
    }

    @Override
    public void addEvent(String event) {
        events.add(event);
    }

    public List<String> pollEvents() {
        List<String> copy = new ArrayList<>(events);
        events.clear();
        return copy;
    }

    public Colour checkWin() {
        for(SafeZone safeZone : board.getSafeZones()) 
            if(safeZone.isFull())
                return safeZone.getColour();
    
        return null;
    }

    @Override
    public void sendHome(Marble marble) {
        for (Player player : players) {
            if (player.getColour() == marble.getColour()) {
                player.regainMarble(marble);
                addEvent("HOME:" + marble.getColour().name());
                break;
            }
        }
    }

    @Override
    public void fieldMarble() throws CannotFieldException, IllegalDestroyException {
        Marble marble = players.get(currentPlayerIndex).getOneMarble();

        if (marble == null)
        	throw new CannotFieldException("No marbles left in the Home Zone to field.");

        board.sendToBase(marble);
        players.get(currentPlayerIndex).getMarbles().remove(marble);
        addEvent("FIELD:" + marble.getColour().name());
    }
    
    @Override
    public void discardCard(Colour colour) throws CannotDiscardException {
        for (Player player : players) {
            if (player.getColour() == colour) {
                int handSize = player.getHand().size();
                if(handSize == 0)
                    throw new CannotDiscardException("Player has no cards to discard.");
                int randIndex = (int) (Math.random() * handSize);
                this.firePit.add(player.getHand().remove(randIndex));
            }
        }
    }

    @Override
    public void discardCard() throws CannotDiscardException {
        int randIndex = (int) (Math.random() * 4);
        while(randIndex == currentPlayerIndex)
            randIndex = (int) (Math.random() * 4);

        discardCard(players.get(randIndex).getColour());
    }

    @Override
    public Colour getActivePlayerColour() {
        return players.get(currentPlayerIndex).getColour();
    }

    @Override
    public Colour getNextPlayerColour() {
        return players.get((currentPlayerIndex + 1) % 4).getColour();
    }

    public boolean isCurrentPlayerCPU() {
        return players.get(currentPlayerIndex) instanceof CPU;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
}
