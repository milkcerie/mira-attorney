import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        GamePanel gamePanel = new GamePanel();

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setTitle("Mira Attorney!!!!!!!!!!!!!!!!!!!");

        window.add(gamePanel);
        window.pack(); //size will be set in gamepanel class
        window.setLocationRelativeTo(null); //centering it
        window.setVisible(true);
        gamePanel.startGameThread(); //START THE GAME YAYYYAYYAYYY

    }
}
