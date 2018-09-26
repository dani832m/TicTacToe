import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * En klientklasse for Kryds og Bolle, modificeret og udvidet fra klassen,
 * som er præsenteret i bogen: Deitel and Deitel "Java How to Program".
 */
public class TicTacToeClient {

    private JFrame frame = new JFrame("Kryds og Bolle");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;
    private ImageIcon opponentIcon;

    private Square[] board = new Square[9];
    private Square currentSquare;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Konstruerer klienten ved at connecte til en server, som sender
     * en GUI ud og registerer klienten af GUI'en
     */
    public TicTacToeClient(String serverAddress) throws Exception {

        // Netværkets setup
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Layout GUI
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    currentSquare = board[j];
                    out.println("MOVE " + j);}});
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, "Center");
    }

    /**
     * Klientens main thread vil lytte efter beskeder fra serveren.
     * Den første besked vil være "WELCOME", hvor vi modtager vores
     * brik. Herefter kommer vi ind i et loop som lytter efter
     * "VALID_MOVE", "OPPONENT MOVED", "VICTORY", "DEFEAT", "TIE",
     * "OPPENENT_QUIT", eller "MESSAGE". Beskeden "VICTORY", "DEFEAT"
     * og "TIE" spørg om brugeren vil spille et nyt spil eller ej.
     * Hvis svaret er nej, så stopper vores loop og der bliver sendt
     * en "QUIT" besked. Det samme sker, hvis modspilleren siger nej.
     */
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                icon = new ImageIcon(mark == 'X' ? "res/blueX.png" : "res/blueCircle.png");
                opponentIcon  = new ImageIcon(mark == 'X' ? "res/redCircle.png" : "res/redX.png");
                frame.setTitle("Kryds og Bolle - Spiller " + mark);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Modstanders tur.");
                    currentSquare.setIcon(icon);
                    currentSquare.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(opponentIcon);
                    board[loc].repaint();
                    messageLabel.setText("Din tur.");
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("Du har vundet!");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("Du har tabt!");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("Uafgjort!");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
        }
        finally {
            socket.close();
        }
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
                "Spil igen?",
                "Kryds og Bolle er sjooooooovt! :D",
                JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Grafisk firkant i klientens vindue. Hver firkant er en "white
     * panel containing". En klient caller setIcon() for at fylde ud
     * med et ikon, altså X eller O.
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }

    /**
     * Starter klienten som en applikation.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            TicTacToeClient client = new TicTacToeClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(500, 570);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}