import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import core.GamePanel;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel gamePanel = new GamePanel();
            // make the jframe
            JFrame frame = new JFrame("Mira Attorney!!!!!!!!");
            // close the application when the window closes
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // set the size of the frame (default browser/wallpaper size)
            frame.setSize(1920, 1080);
            frame.setResizable(false);
            frame.add(gamePanel);
            frame.pack();
            // center the frame
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            gamePanel.requestFocusInWindow();
            gamePanel.start();
        });
    }
}