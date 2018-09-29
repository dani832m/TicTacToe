// Imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serverklassen til vores netværks-multi-player TicTacToe-spil.
 *
 * Client -> Server           Server -> Client
 * ----------------           ----------------
 * MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 * QUIT                       VALID_MOVE
 *                            OTHER_PLAYER_MOVED <n>
 *                            VICTORY
 *                            DEFEAT
 *                            TIE
 *                            MESSAGE <text>
 */
public class TicTacToeServer {

    /**
     * Kører applikationen, og forbinder klienter der tilslutter.
     */
    public static void main(String[] args) throws Exception {

        // Laver et nyt ServerSocket-objekt som lytter på port 8901.
        ServerSocket listener = new ServerSocket(8901);

        // Skriver til konsollen, hvis serveren kører.
        System.out.println("TicTacToe-server kører!");

        // Kører et while-loop, der deklarerer og initialiserer et nyt Game-objekt.
        try {
            while (true) {
                Game game = new Game();
                // Spiller X og O kobles på spil-objektet.
                Game.Player playerX = game.new Player(listener.accept(), 'X');
                Game.Player playerO = game.new Player(listener.accept(), 'O');
                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX;
                playerX.start();
                playerO.start();
            }
        } finally {
            listener.close();
        }
    }
}

/**
 * Inner class. Game-klassen bruges i ovenstående kode i TicTacToeServer.java
 */
class Game {

    /**
     * Et bræt har 9 felter.
     * Hvert felt er enten tomt eller besat af en spiller.
     * Derfor bruger vi få simpele henvisninger.
     * Hvis en arraycelle viser null, rr det tilsvarende felt tomt,
     * og ellers lagres en henvisning til spilleren der har besat det.
     */
    private Player[] board = {
            null, null, null,
            null, null, null,
            null, null, null};

    // Den nuværende spiller.
    Player currentPlayer;

    /**
     * Returnerer, om der er en vinder, med den nuværende besættelse af spillepladen.
     */
    public boolean hasWinner() {
        return
                (board[0] != null && board[0] == board[1] && board[0] == board[2])
                        ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
                        ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
                        ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
                        ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
                        ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
                        ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
                        ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    /**
     * Returnerer, om alle felter er besatte.
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kaldet af player trådene, når en spiller prøver at besætte en plads.
     * Metoden tjekker om en besættelse er godkendt:
     * spilleren som prøver at besætte har turen,
     * og at feltet som førsøgt besat er frit.
     * Hvis besættelsen er godkendt, opdateres spillet,
     * så modspilleren bliver informeret om tur skifte og
     * besættelse af felt.
     */
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }

    /**
     * Klasse indeholdende en socket med dens input og output,
     * da input kun er tekst, bruger vi en reader og en writer.
     */
    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Venter på modspiller...");
            } catch (IOException e) {
                System.out.println("Spiller døde: " + e);
            }
        }

        /**
         * Besked om, hvem modspilleren er.
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Giver besked om, at modspilleren har taget sin tur.
         */
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                    hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        /**
         * Run metoden.
         */
        public void run() {
            try {
                // Tråden starter, når to spillere er forbundet.
                output.println("Begge spillere er forbundet.");

                // Besked til første spiller om at starte.
                if (mark == 'X') {
                    output.println("MESSAGE Din tur.");
                }

                // Får kommandoer fra klienten og behandler dem.
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        if (legalMove(location, this)) {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY"
                                    : boardFilledUp() ? "TIE"
                                    : "");
                        } else {
                            output.println("MESSAGE ?");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Spiller døde: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}