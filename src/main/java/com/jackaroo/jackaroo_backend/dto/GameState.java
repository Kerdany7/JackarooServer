package com.jackaroo.jackaroo_backend.dto;

import java.util.List;

public class GameState {
    private String currentPlayerName;
    private String currentPlayerColour;
    private List<String> cardNames;
    private boolean canPlay;
    private String winner;

    // Board state
    private List<CellState> track;
    private List<SafeZoneState> safeZones;
    private List<PlayerInfo> players;

    // Selection state
    private String selectedCardName;
    private List<String> cardDescriptions;

    // Room info (for multiplayer)
    private String roomCode;

    // Multiplayer: personalized fields (filled only for the requesting player)
    private boolean myTurn;
    private String myColour;
    private List<String> myCardNames;
    private List<String> myCardDescriptions;
    private String mySelectedCardName;

    // Extended state
    private String firePitTopCard;
    private int firePitCount;
    private int deckCount;
    private int roundCount;
    private int turnInRound;
    private String nextPlayerName;
    private String nextPlayerColour;
    private List<String> events;

    public GameState() {}

    public String getCurrentPlayerName() { return currentPlayerName; }
    public void setCurrentPlayerName(String v) { this.currentPlayerName = v; }
    public String getCurrentPlayerColour() { return currentPlayerColour; }
    public void setCurrentPlayerColour(String v) { this.currentPlayerColour = v; }
    public List<String> getCardNames() { return cardNames; }
    public void setCardNames(List<String> v) { this.cardNames = v; }
    public boolean isCanPlay() { return canPlay; }
    public void setCanPlay(boolean v) { this.canPlay = v; }
    public String getWinner() { return winner; }
    public void setWinner(String v) { this.winner = v; }
    public List<CellState> getTrack() { return track; }
    public void setTrack(List<CellState> v) { this.track = v; }
    public List<SafeZoneState> getSafeZones() { return safeZones; }
    public void setSafeZones(List<SafeZoneState> v) { this.safeZones = v; }
    public List<PlayerInfo> getPlayers() { return players; }
    public void setPlayers(List<PlayerInfo> v) { this.players = v; }
    public String getSelectedCardName() { return selectedCardName; }
    public void setSelectedCardName(String v) { this.selectedCardName = v; }
    public List<String> getCardDescriptions() { return cardDescriptions; }
    public void setCardDescriptions(List<String> v) { this.cardDescriptions = v; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String v) { this.roomCode = v; }

    public String getFirePitTopCard() { return firePitTopCard; }
    public void setFirePitTopCard(String v) { this.firePitTopCard = v; }
    public int getFirePitCount() { return firePitCount; }
    public void setFirePitCount(int v) { this.firePitCount = v; }
    public int getDeckCount() { return deckCount; }
    public void setDeckCount(int v) { this.deckCount = v; }
    public int getRoundCount() { return roundCount; }
    public void setRoundCount(int v) { this.roundCount = v; }
    public int getTurnInRound() { return turnInRound; }
    public void setTurnInRound(int v) { this.turnInRound = v; }
    public String getNextPlayerName() { return nextPlayerName; }
    public void setNextPlayerName(String v) { this.nextPlayerName = v; }
    public String getNextPlayerColour() { return nextPlayerColour; }
    public void setNextPlayerColour(String v) { this.nextPlayerColour = v; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> v) { this.events = v; }

    public boolean isMyTurn() { return myTurn; }
    public void setMyTurn(boolean v) { this.myTurn = v; }
    public String getMyColour() { return myColour; }
    public void setMyColour(String v) { this.myColour = v; }
    public List<String> getMyCardNames() { return myCardNames; }
    public void setMyCardNames(List<String> v) { this.myCardNames = v; }
    public List<String> getMyCardDescriptions() { return myCardDescriptions; }
    public void setMyCardDescriptions(List<String> v) { this.myCardDescriptions = v; }
    public String getMySelectedCardName() { return mySelectedCardName; }
    public void setMySelectedCardName(String v) { this.mySelectedCardName = v; }
}
