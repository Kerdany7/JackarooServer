package com.jackaroo.jackaroo_backend.dto;
import java.util.List;
public class GameState {
    private String currentPlayerName;
    private String currentPlayerColour;
    private List<String> cardNames;
    private boolean canPlay;
    private String winner;
    public GameState(){}

        public void setCurrentPlayerName(String currentPlayerName){
            this.currentPlayerName=currentPlayerName;
        }
        public String getCurrentPlayerName(){
            return currentPlayerName;
        }
        public void setCurrentPlayerColour(String currentPlayerColour){
            this.currentPlayerColour=currentPlayerColour;
        }

        public String getCurrentPlayerColour(){
            return currentPlayerColour;
        }
        public void setCardNames(List<String> cardNames){
            this.cardNames = cardNames;
        }
        public List<String> getCardNames(){
            return cardNames;
        }
        public void setCanPlay(boolean canPlay){
            this.canPlay = canPlay;
        }
        public boolean isCanPlay(){
            return canPlay;
        }
        public void setWinner(String winner){
            this.winner = winner;
        }


        public String getWinner(){
            return winner;
        }




    }

