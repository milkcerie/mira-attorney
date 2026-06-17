package core;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import scene.SceneManager;

public class GamePanel extends JPanel implements Runnable {
    // constants/dimensions idk mannnn
    private static final int SCREEN_WIDTH = 816;
    private static final int SCREEN_HEIGHT = 624;
    private static final int TARGET_FPS = 60;
    // how many nanosecs should pass btwn updates
    private static final long NS_PER_FRAME = 1_000_000_000L / TARGET_FPS;
    // thread running game loop
    private Thread gameThread;
    private boolean running = false;
    // shared objects
    private final InputManager inputManager;
    private final SceneManager sceneManager;
    private final SoundManager soundManager;
    private final GameState gameState;

    // constructor
    public GamePanel() {
        // pack method in main reads to set the jframe size
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        // make the shared objects in order of dependencies
        gameState    = new GameState();
        soundManager = new SoundManager();
        inputManager = new InputManager();
        sceneManager = new SceneManager(gameState, inputManager, soundManager);

        soundManager.loadMusic("court", "assets/audio/music/court.mp3");
        System.out.println("Audio loaded. Working dir: " + System.getProperty("user.dir"));
        soundManager.loadSFX("blink", "assets/audio/music/blink.wav");
        // stuff for keyboard input
        addKeyListener(inputManager);
        addMouseListener(inputManager);
        sceneManager.switchTo(SceneManager.S.HOME);
    }

    public void start() {
    if (running) return;
    running = true;          // ADD THIS LINE
    gameThread = new Thread(this);
    gameThread.start();
}

    public void stop() {
        running = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // run() is required by runnable in main so this is the game loop
@Override
public void run() {
    long lastTime = System.nanoTime();
    long lag = 0;
    while (running) {
        long now = System.nanoTime();
        lag += now - lastTime;
        lastTime = now;
        while (lag >= NS_PER_FRAME) {
            update();
            inputManager.endFrame(); // moved here
            lag -= NS_PER_FRAME;
        }
        repaint();
        try { Thread.sleep(1); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

    private void update() {
    if (inputManager.justPressed(KeyEvent.VK_ESCAPE)) {
        if (sceneManager.isOptionsOpen()) sceneManager.updateOptions();
        else sceneManager.openOptions();
        return;
    }
    if (sceneManager.isOptionsOpen()) sceneManager.updateOptions();
    else sceneManager.update();
}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g; // fixed: was g2D
        // anti aliasing so the quality is good
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (sceneManager.isOptionsOpen()) {
            // options is an overlay — render the underlying scene first
            // then draw the semi-transparent options panel on top of it
            // renderOptions() handles both layers
            sceneManager.renderOptions(g2d);
        } else {
            sceneManager.render(g2d);
        }
        // dispose() releases the graphics2D resources we used this frame
        // not calling it is a memory leak
        // dispose the cast g2d not the original g since swing still needs the original after this method
        g2d.dispose();
    }
}