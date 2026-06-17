package core;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class DialogueEngine {
    private String[] speakers;
    private String[] texts;
    private String[] portraitKeys; //mapping to assets
    private String[] sfxKeys; //mapping to sound assets etc
    private String[] backgrounds;

    //num lines in current script
    private int count = 0;
    //which line of the script we are on
    private int currentIndex = 0;
    //for scroll - how many chars to reveal
    private float charIndex = 0f;
    //true when last line has been advanced past
    private boolean finished = true;
    private final Map<String, BufferedImage> portraits = new HashMap<>();
    private SoundManager sound; //per line sfx
    private GameState state; //read scroll speed

    // history system
    private final ArrayList<String[]> history = new ArrayList<>(); // each entry is {speaker, text}
    private boolean reviewing = false;
    private int reviewIndex = 0;

    //constructors
    public DialogueEngine() {
    }
    public DialogueEngine(GameState state, SoundManager sound) {
        this.state = state;
        this.sound = sound;
    }

    public void loadFromFile(String filename) {
        try {
            FileReader reader = new FileReader("assets/scripts/" + filename);
            StringBuilder sb = new StringBuilder();
            int c;
            while((c = reader.read()) != -1) sb.append((char) c);
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            count = arr.length();
            speakers = new String[count];
            texts = new String[count];
            portraitKeys = new String[count];
            sfxKeys = new String[count];
            backgrounds = new String[count];

            for(int i = 0; i < count; i++) {
                JSONObject obj = arr.getJSONObject(i);
                speakers[i] = obj.optString("speaker", "");
                texts[i] = obj.optString("text", "");
                portraitKeys[i] = obj.optString("portrait", "");
                sfxKeys[i] = obj.optString("sfx", "");
                preloadPortrait(portraitKeys[i]);
                backgrounds[i] = obj.optString("background", "");
            }
            //reset playback position
            currentIndex = 0;
            charIndex = 0f;
            finished = false;
        }
        catch(IOException e) {
            System.err.println("Could not load script: " + filename);
            finished = true;
        }
    }

    private void preloadPortrait(String key) {
        if(key == null || key.isEmpty() || portraits.containsKey(key)) return;
        try {
            BufferedImage img = ImageIO.read(new File("assets/sprites/" + key + ".png"));
            portraits.put(key, img);
        } catch(IOException e) {
            System.err.println("Could not load portrait: " + key);
            portraits.put(key, null);
        }
    }

    public void update(InputManager input) {
    if (finished || count == 0) return;
    String currentText = texts[currentIndex];
    float speed = (state != null) ? state.getTextScrollSpeed() : 1f;

    boolean advance = input.justPressed(java.awt.event.KeyEvent.VK_RIGHT)
                   || input.justPressed(java.awt.event.KeyEvent.VK_LEFT)
                   || input.justPressed(java.awt.event.KeyEvent.VK_DOWN)
                   || input.justPressed(java.awt.event.KeyEvent.VK_UP);

    if (charIndex < currentText.length()) {
        if (advance) {
            charIndex = currentText.length();
        } else {
            charIndex = Math.min(charIndex + speed, currentText.length());
        }
    } else {
        if (advance) {
            advanceLine();
        }
    }
}

    private void advanceLine() {
        // save current line to history before advancing
        history.add(new String[]{ speakers[currentIndex], texts[currentIndex] });
        currentIndex++;
        if(currentIndex >= count) {
            finished = true;
        } else {
            charIndex = 0f;
            String sfx = sfxKeys[currentIndex];
            // null check on sound — only play if sound was set
            if(sound != null && sfx != null && !sfx.isEmpty()) {
                sound.playSFX(sfx);
            }
        }
    }

    public void render(Graphics2D g) {
        if(finished || count == 0) return;
        String portraitKey = portraitKeys[currentIndex];
        BufferedImage portrait = portraits.get(portraitKey);

        if(portrait != null) {
            //position is left side above the text box
            g.drawImage(portrait, 20, 100, 300, 400, null);
        }
    }

    // ── History system ─────────────────────────────────────────────────────

    public boolean isReviewing() { return reviewing; }

    public void toggleReview() {
        reviewing = !reviewing;
        if (reviewing && !history.isEmpty()) {
            reviewIndex = history.size() - 1; // start at most recent
        }
    }

    public void updateReview(InputManager input) {
        if (input.justPressed(java.awt.event.KeyEvent.VK_UP))
            reviewIndex = Math.max(0, reviewIndex - 1);
        if (input.justPressed(java.awt.event.KeyEvent.VK_DOWN))
            reviewIndex = Math.min(history.size() - 1, reviewIndex + 1);
        if (input.justPressed(java.awt.event.KeyEvent.VK_X)
        ||  input.justPressed(java.awt.event.KeyEvent.VK_ESCAPE))
            reviewing = false;
    }

    public void renderHistory(Graphics2D g) {
        if (history.isEmpty()) return;

        // dark overlay
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 816, 624);

        // panel
        g.setColor(new Color(20, 20, 40));
        g.fillRoundRect(40, 40, 736, 544, 14, 14);
        g.setColor(new Color(180, 180, 255));
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.drawRoundRect(40, 40, 736, 544, 14, 14);

        // title and controls hint
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Dialogue History", 60, 72);
        g.setColor(new Color(150, 150, 150));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("↑ ↓ scroll    X / Esc = close", 520, 72);

        // show lines centred around reviewIndex
        int startIdx = Math.max(0, reviewIndex - 3);
        int endIdx   = Math.min(history.size(), startIdx + 7);
        int y = 110;

        for (int i = startIdx; i < endIdx; i++) {
            String[] line = history.get(i);
            boolean selected = (i == reviewIndex);

            if (selected) {
                g.setColor(new Color(60, 60, 100));
                g.fillRoundRect(50, y - 18, 716, 48, 8, 8);
            }

            g.setColor(new Color(180, 180, 255));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(line[0].isEmpty() ? "—" : line[0], 65, y);

            g.setColor(selected ? Color.WHITE : new Color(200, 200, 200));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            // truncate long lines so they don't overflow the panel
            String text = line[1].length() > 70 ? line[1].substring(0, 70) + "..." : line[1];
            g.drawString(text, 65, y + 20);

            y += 60;
        }
    }

    //accessor methods
    public boolean isFinished() {
        return finished;
    }
    public boolean isActive() {
        return !finished && count > 0;
    }
    public String getCurrentSpeaker() {
        if(finished || count == 0) return "";
        return speakers[currentIndex];
    }
    public String getCurrentText() {
        if(finished || count == 0) return "";
        return texts[currentIndex];
    }
    public int getCharIndex() {
        return (int) charIndex;
    }
    public String getCurrentBackground() {
    if (finished || count == 0) return "";
    return backgrounds[currentIndex];
}
}