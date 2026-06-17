package scene;

import core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;

public class SceneManager {

    public enum S { HOME, INTRODUCTION, INVESTIGATION, COURT, OPTIONS, VERDICT }

    //weeeeeeeeeeeee
    private S scene = S.HOME;
    private final GameState      state;
    private final InputManager   input;
    private final SoundManager   sound;
    private final DialogueEngine dlg;
    private JButton playButton, quitButton;
    private int titleTimer = 180;

    // image cache so we never load the same file twice
    private final Map<String, BufferedImage> imgCache = new HashMap<>();
    private BufferedImage bg;

    // investigation scene
    private final ArrayList<int[]>    hBounds    = new ArrayList<>();
    private final ArrayList<Evidence> hEvidence  = new ArrayList<>();
    private final ArrayList<Boolean>  hCollected = new ArrayList<>();
    private boolean justCollected = false;
    private boolean concludingDlgPlayed = false;

    // court scene
    private final ArrayList<Statement> stmts = new ArrayList<>();
    private int stmtIdx, sentIdx, objTimer, selEv;
    private boolean recordOpen, correcting;
    private boolean courtStarted = false;
    private String leftSprite, rightSprite;
    private static final int OBJ_DUR = 90;

    // options scene
    private S prevScene;
    private boolean optOpen;
    private float mSlider, sSlider, tSlider;
    private static final int SL_X=180, SL_W=300, SL_MY=260, SL_SY=320, SL_TY=380;

    // constructor
    public SceneManager(GameState state, InputManager input, SoundManager sound) {
        this.state = state;
        this.input = input;
        this.sound = sound;
        this.dlg   = new DialogueEngine(); // no-arg constructor
    }

    // ── Public API ─────────────────────────────────────────────────────────
    public void switchTo(S s)      { scene = s; enter(s); }
    public boolean isOptionsOpen() { return optOpen; }

    public void openOptions() {
        prevScene = scene;
        scene     = S.OPTIONS;
        optOpen   = true;
        mSlider   = state.getMusicVolume();
        sSlider   = state.getSFXVolume();
        tSlider   = (state.getTextScrollSpeed() - 0.5f) / 4.5f;
    }

    public void update() {
    if (input.justPressed(java.awt.event.KeyEvent.VK_H)) { dlg.toggleReview(); return; }
    if (dlg.isReviewing()) { dlg.updateReview(input); return; }
        switch (scene) {
            case HOME:          updateHome();         break;
            case INTRODUCTION:  updateIntroduction();  break;
            case INVESTIGATION: updateInvestigation(); break;
            case COURT:         updateCourt();         break;
            case VERDICT:       updateVerdict();       break;
            default: break;
        }
    }

    public void updateOptions() {
        if (!input.clicked()) return;
        float v;
        v = sliderHit(SL_MY); if (v >= 0) { mSlider=v; state.setMusicVolume(v); sound.setMusicVolume(v); }
        v = sliderHit(SL_SY); if (v >= 0) { sSlider=v; state.setSFXVolume(v);   sound.setSFXVolume(v); }
        v = sliderHit(SL_TY); if (v >= 0) { tSlider=v; state.setTextScrollSpeed(0.5f + v * 4.5f); }
        if (input.clickedIn(20, 20, 40, 40)) { optOpen = false; scene = prevScene; }
    }
    private void drawOptionsButton(Graphics2D g) {
    g.setColor(new Color(60, 60, 70));
    g.fillRoundRect(756, 574, 50, 40, 8, 8);
    g.setColor(Color.WHITE);
    g.setFont(new Font("SansSerif", Font.PLAIN, 18));
    g.drawString("⚙", 766, 601);
}

    public void render(Graphics2D g) {
    switch (scene) {
        case HOME:          renderHome(g);          break;
        case INTRODUCTION:  renderIntroduction(g);  break;
        case INVESTIGATION: renderInvestigation(g); break;
        case COURT:         renderCourt(g);          break;
        case VERDICT:       renderVerdict(g);        break;
        default: break;
    }
    if (dlg.isReviewing()) dlg.renderHistory(g);
    drawOptionsButton(g); // ADD THIS — draws on top of every scene
}

    public void renderOptions(Graphics2D g) {
        render(g); // draw the game scene underneath first
        Composite c = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.setColor(Color.BLACK); g.fillRect(0, 0, 816, 624);
        g.setComposite(c);
        g.setColor(new Color(30,30,40));  g.fillRoundRect(120,100,576,400,20,20);
        g.setColor(Color.GRAY);           g.drawRoundRect(120,100,576,400,20,20);
        g.setColor(Color.WHITE);          g.setFont(new Font("SansSerif",Font.BOLD,20));
        g.drawString("Options", 350, 150);
        g.setColor(new Color(60,60,70));  g.fillRoundRect(20,20,40,40,8,8);
        g.setColor(Color.WHITE);          g.setFont(new Font("SansSerif",Font.PLAIN,18));
        g.drawString("←", 30, 46);
        drawSlider(g, "Music",      SL_X, SL_MY, mSlider);
        drawSlider(g, "SFX",        SL_X, SL_SY, sSlider);
        drawSlider(g, "Text speed", SL_X, SL_TY, tSlider);
    }

    // enter scene setup
    private void enter(S s) {
        switch (s) {
            case HOME: {
                
                bg = loadImg("assets/backgrounds/home.png");
                sound.playMusic("theme", true);
                if (input.clicked()) {
                System.out.println("Click at: " + input.mx() + ", " + input.my());
            }
            if (input.clickedIn(308,300,184,50)) switchTo(S.INVESTIGATION);
            if (input.clickedIn(308,370,184,50)) System.exit(0);
                break;
            }
            case INTRODUCTION: {
                bg = loadImg("assets/backgrounds/black.png");
                hBounds.clear(); hEvidence.clear(); hCollected.clear();
                state.saveCheckpoint(GameState.Scene.INVESTIGATION);
                dlg.loadFromFile("intro.json");
                break;
            }
            case INVESTIGATION: {
                bg = loadImg("assets/backgrounds/investigation.png");
                hBounds.clear(); hEvidence.clear(); hCollected.clear();
                justCollected = false;
                concludingDlgPlayed = false;
                state.saveCheckpoint(GameState.Scene.INVESTIGATION);
                dlg.loadFromFile("investigation_intro.json");
                // evidence here
                // addHotspot(x, y, w, h, new Evidence("Name","Desc","img.png"));
                addHotspot(500, 300, 100, 100, new Evidence("Pendant", "A gold pendant, though slightly tarnished so perhaps not as high quality as a more expensive one.", "pendant.png"));
                addHotspot(50, 100, 100, 100, new Evidence("Letter", "The top scrap of a small letter; the only visible words are the name Cassandra, a few expenses listed out, and the word 'soon'.", "letter.png"));
                addHotspot(600, 200, 100, 100, new Evidence("Article", "A newspaper article from exactly two weeks ago reporting that the Richmans have surpassed the Gold family in wealth.", "article.png"));
                addHotspot(700, 400, 100, 100, new Evidence("Candle", "A tipped-over candle.", "candle.png"));

                break;
            }
            case COURT: {
                bg = loadImg("assets/backgrounds/court.png");
                stmts.clear(); stmtIdx=0; sentIdx=0; objTimer=0;
                recordOpen=false; correcting=false;
                courtStarted = false;
                leftSprite="phoenix_normal"; rightSprite="judge_normal";
                state.saveCheckpoint(GameState.Scene.COURT);
                dlg.loadFromFile("court_intro.json");
                sound.playMusic("court", true);
                // ADD STATEMENTS HERE 
                //examples
                // Statement st = new Statement("Witness");
                // st.sentences.add("i was at home doing things");
                // st.objectionMap.put(0, "Knife");
                // st.correctionScripts.put(0, "fix.json");
                // stmts.add(st);
                Statement est = new Statement("Ella");
                est.sentences.add("I was tidying up Betty's room when I accidentally knocked over a candle on the windowsill.");
                est.sentences.add("I don't think anything got burned, though. ");
                est.objectionMap.put(1, "Letter");
                est.correctionScripts.put(1, "ella_correction.json");
                est.sentences.add("Well, I went down to the kitchens to get some cleaning supplies, and when I came back she was gone.");
                stmts.add(est);

                Statement cst = new Statement("Cassandra");
                cst.sentences.add("Well, I was coming home late from a party, so Betty was already asleep when I got home.");
                cst.sentences.add("I made sure to put away all of my jewelry before bed as well.");
                cst.objectionMap.put(1, "Pendant");
                cst.correctionScripts.put(1, "cassandra_correction1.json");
                cst.sentences.add("Then I went to the washroom to get ready to sleep.");
                cst.sentences.add("When I came back to our room, I saw someone taking Betty out the window! It was a bit hard to see, but there was a lit candle by the windowsill, and it definitely looked like the maid!");
                cst.objectionMap.put(3, "Candle");
                cst.correctionScripts.put(3, "cassandra_correction2.json");
                cst.sentences.add("In any case, I would personally have no reason to harm Betty or the Richman family!");
                cst.objectionMap.put(4, "Article");
                cst.correctionScripts.put(4, "cassandra_correction3.json");
                stmts.add(cst);
                
                break;
            }
            case VERDICT: {
                bg = loadImg("assets/backgrounds/verdict.png");
                dlg.loadFromFile("verdict.json");
                sound.playMusic("verdict", false);
                break;
            }
            default: break;
        }
    }

    // scene updation
    private void updateHome() {
    System.out.println("titleTimer: " + titleTimer);
    titleTimer--;
    if (titleTimer <= 0) {
        titleTimer = 180;
        switchTo(S.INTRODUCTION);
    }
}
    private void updateIntroduction() {
    if (dlg.isActive()) {
        dlg.update(input);
        // swap background if the current line specifies one
        String newBg = dlg.getCurrentBackground();
        if (newBg != null && !newBg.isEmpty()) {
            bg = loadImg("assets/backgrounds/" + newBg);
        }
        return;
    }
    switchTo(S.INVESTIGATION);
}

private void renderIntroduction(Graphics2D g) {
    drawBg(g);
    if (dlg.isActive()) {
        dlg.render(g);
        drawTextBox(g, dlg.getCurrentSpeaker(), dlg.getCurrentText(), dlg.getCharIndex());
    }
}

   private void updateInvestigation() {
    justCollected = false;

    if (dlg.isActive()) {
        dlg.update(input);
        String newBg = dlg.getCurrentBackground();
        if (newBg != null && !newBg.isEmpty()) {
            bg = loadImg("assets/backgrounds/" + newBg);
        }
        return;
    }

    for (int i = 0; i < hBounds.size(); i++) {
        if (hCollected.get(i)) continue;
        int[] b = hBounds.get(i);
        if (input.clickedIn(b[0], b[1], b[2], b[3])) {
            Evidence ev = hEvidence.get(i);
            state.addEvidence(ev);
            hCollected.set(i, true);
            justCollected = true;
            System.out.println("Loading: pickup_" + ev.getName().toLowerCase() + ".json");
            dlg.loadFromFile("pickup_" + ev.getName().toLowerCase() + ".json");
            return;
        }
    }

    // only switch to court if nothing was just collected and also not dialogue playing
    if (!justCollected && !dlg.isActive()) {
        boolean allDone = !hCollected.isEmpty();
        for (boolean c : hCollected) { if (!c) { allDone = false; break; } }
        if (allDone) {
            if (!concludingDlgPlayed) {
                concludingDlgPlayed = true;
                dlg.loadFromFile("investigation_end.json");
            } else {
                switchTo(S.COURT);
            }
        }
    }
}

    private void updateCourt() {
        if (objTimer > 0) { objTimer--; return; }
        if (correcting) {
            dlg.update(input);
            if (dlg.isFinished()) { correcting = false; advanceSentence(); }
            return;
        }
        if (dlg.isActive()) { dlg.update(input); return; }
        if (recordOpen)     { updateRecord(); return; }

        // after court intro finishes, just show the first sentence — no input needed
        if (!courtStarted && !stmts.isEmpty()) {
            courtStarted = true;
            return; // first sentence now visible, wait for player input
        }

        if (input.clickedIn(540,20,120,40) && !state.getEvidence().isEmpty()) {
            recordOpen = true; selEv = 0; return;
        }
        if (input.justPressed(java.awt.event.KeyEvent.VK_Z)
        || input.justPressed(java.awt.event.KeyEvent.VK_ENTER)
        || input.justPressed(java.awt.event.KeyEvent.VK_RIGHT)
        || input.justPressed(java.awt.event.KeyEvent.VK_DOWN)
        || input.clicked()) advanceSentence();
        if (state.isGameOver()) handleGameOver();
    }

    private void updateRecord() {
        ArrayList<Evidence> ev = (ArrayList<Evidence>) state.getEvidence();
        if (input.justPressed(java.awt.event.KeyEvent.VK_LEFT))
            selEv = Math.max(0, selEv - 1);
        if (input.justPressed(java.awt.event.KeyEvent.VK_RIGHT))
            selEv = Math.min(ev.size() - 1, selEv + 1);
        if (input.justPressed(java.awt.event.KeyEvent.VK_Z)
         || input.justPressed(java.awt.event.KeyEvent.VK_ENTER)) {
            recordOpen = false; checkObjection(ev.get(selEv));
        }
        if (input.justPressed(java.awt.event.KeyEvent.VK_X)
         || input.justPressed(java.awt.event.KeyEvent.VK_ESCAPE))
            recordOpen = false;
    }

    private void advanceSentence() {
        if (stmts.isEmpty()) return;
        sentIdx++;
        if (sentIdx >= stmts.get(stmtIdx).sentences.size()) {
            sentIdx = 0;
            stmtIdx++;
            if (stmtIdx >= stmts.size()) { switchTo(S.VERDICT); return; }
            state.saveCheckpoint(GameState.Scene.COURT);
            dlg.loadFromFile("statement_" + stmtIdx + "_intro.json");
        }
    }

    private void checkObjection(Evidence ev) {
        String script = ObjectionChecker.check(ev, stmts.get(stmtIdx), sentIdx);
        if (script != null) {
            objTimer = OBJ_DUR; correcting = true;
            sound.playSFX("objection");
            dlg.loadFromFile(script);
        } else {
            state.loseHP();
            sound.playSFX("wrong");
        }
    }

    private void handleGameOver() {
        state.resetHP();
        if (state.getCheckpoint() == GameState.Scene.INVESTIGATION) switchTo(S.INVESTIGATION);
        else switchTo(S.COURT);
    }

    private void updateVerdict() {
        dlg.update(input);
        if (dlg.isFinished()) switchTo(S.HOME);
    }

    private void drawEvidencePopup(Graphics2D g, Evidence ev) {
    // small card in top-right corner showing the collected evidence
    int px = 580, py = 20, pw = 210, ph = 110;

    g.setColor(new Color(10, 10, 40, 220));
    g.fillRoundRect(px, py, pw, ph, 10, 10);
    g.setColor(new Color(180, 180, 255));
    g.setStroke(new BasicStroke(1.5f));
    g.drawRoundRect(px, py, pw, ph, 10, 10);

    // "Evidence obtained" label
    g.setColor(new Color(180, 180, 255));
    g.setFont(new Font("SansSerif", Font.BOLD, 11));
    g.drawString("EVIDENCE OBTAINED", px + 10, py + 18);

    // evidence image
    if (ev.getImage() != null) {
        g.drawImage(ev.getImage(), px + 10, py + 26, 64, 64, null);
    } else {
        g.setColor(Color.GRAY);
        g.fillRect(px + 10, py + 26, 64, 64);
    }

    // evidence name and description
    g.setColor(Color.WHITE);
    g.setFont(new Font("SansSerif", Font.BOLD, 13));
    g.drawString(ev.getName(), px + 82, py + 44);
    g.setColor(Color.LIGHT_GRAY);
    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
    // wrap description if too long
    String desc = ev.getDescription().length() > 22
        ? ev.getDescription().substring(0, 22) + "..."
        : ev.getDescription();
    g.drawString(desc, px + 82, py + 60);
}

    // scene reneder ───────────────────────────────────────────────
    private void renderHome(Graphics2D g) {
        drawBg(g);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        String title = "";
        g.drawString(title, (816 - fm.stringWidth(title)) / 2, 280);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        String sub = "";
        g.drawString(sub, (816 - fm.stringWidth(sub)) / 2, 340);
    }

    private void renderInvestigation(Graphics2D g) {
    drawBg(g);
    g.setStroke(new BasicStroke(2));

    for (int i = 0; i < hBounds.size(); i++) {
        if (!hCollected.get(i)) {
            int[] b = hBounds.get(i);
            g.setColor(new Color(255,255,100,80)); g.fillRect(b[0],b[1],b[2],b[3]);
            g.setColor(new Color(255,220,0));       g.drawRect(b[0],b[1],b[2],b[3]);
        }
    }

    if (dlg.isActive()) {
        dlg.render(g);
        drawTextBox(g, dlg.getCurrentSpeaker(), dlg.getCurrentText(), dlg.getCharIndex());
    }
}
    private void renderCourt(Graphics2D g) {
        drawBg(g);
        drawSprite(g, leftSprite,  20,  200, 220, 240);
        drawSprite(g, rightSprite, 576, 200, 220, 240);
        drawHPBar(g);
        if (objTimer > 0) { drawObjBanner(g); return; }
        if (dlg.isActive()) {
            dlg.render(g);
            drawTextBox(g, dlg.getCurrentSpeaker(), dlg.getCurrentText(), dlg.getCharIndex());
        } else if (!stmts.isEmpty()) {
            String sent = stmts.get(stmtIdx).sentences.get(sentIdx);
            drawTextBox(g, stmts.get(stmtIdx).speakerName, sent, sent.length());
        }
        drawBtn(g, "Objection!", 540,20,120,40, new Color(180,30,30));
        if (recordOpen) drawRecord(g);
    }

    private void renderVerdict(Graphics2D g) {
        drawBg(g);
        if (dlg.isActive()) {
            dlg.render(g);
            drawTextBox(g, dlg.getCurrentSpeaker(), dlg.getCurrentText(), dlg.getCharIndex());
        }
    }

    // ── Drawing helpers ────────────────────────────────────────────────────
    private void drawBg(Graphics2D g) {
        if (bg != null) g.drawImage(bg, 0, 0, 816, 624, null);
        else { g.setColor(new Color(30,30,50)); g.fillRect(0, 0, 816, 624); }
    }

    private void drawBtn(Graphics2D g, String label, int x, int y, int w, int h, Color col) {
        g.setColor(col); g.fillRoundRect(x,y,w,h,10,10);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,20));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x+(w-fm.stringWidth(label))/2, y+h/2+fm.getAscent()/2-2);
    }

    private void drawTextBox(Graphics2D g, String speaker, String text, int ci) {
        int bx=20, by=460, bw=776, bh=140;
        g.setColor(new Color(10,10,30,220)); g.fillRoundRect(bx,by,bw,bh,12,12);
        g.setColor(new Color(180,180,255));  g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(bx,by,bw,bh,12,12);
        if (speaker != null && !speaker.isEmpty()) {
            g.setColor(new Color(10,10,30,220)); g.fillRoundRect(bx+10,by-28,140,28,8,8);
            g.setColor(new Color(180,180,255));  g.drawRoundRect(bx+10,by-28,140,28,8,8);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,14));
            g.drawString(speaker, bx+20, by-8);
        }
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.PLAIN,16));
        FontMetrics fm = g.getFontMetrics();
        drawWrapped(g, fm, text.substring(0, Math.min(ci, text.length())), bx+20,by+30,bw-40,24);
        if (ci >= text.length() && (System.currentTimeMillis()/500)%2==0) {
            g.setFont(new Font("SansSerif",Font.PLAIN,18));
            g.drawString("▼", bx+bw-30, by+bh-10);
        }
    }

    private void drawWrapped(Graphics2D g, FontMetrics fm, String text, int x, int y, int maxW, int lh) {
        StringBuilder line = new StringBuilder(); int cy = y;
        for (String w : text.split(" ")) {
            String test = line.length()==0 ? w : line+" "+w;
            if (fm.stringWidth(test)>maxW && line.length()>0) {
                g.drawString(line.toString(), x, cy); cy+=lh; line=new StringBuilder(w);
            } else line = new StringBuilder(test);
        }
        if (line.length()>0) g.drawString(line.toString(), x, cy);
    }

    private void drawHPBar(Graphics2D g) {
        BufferedImage full  = loadImg("assets/sprites/heart_full.png");
        BufferedImage empty = loadImg("assets/sprites/heart_empty.png");
        for (int i = 0; i < 5; i++) {
            BufferedImage h = i < state.getHP() ? full : empty;
            if (h != null) g.drawImage(h, 20+i*36, 20, 32, 32, null);
            else { g.setColor(i<state.getHP()?Color.RED:Color.DARK_GRAY); g.fillRect(20+i*36,20,30,30); }
        }
    }

    private void drawObjBanner(Graphics2D g) {
        float alpha = Math.min(1f, objTimer / (float)(OBJ_DUR/2));
        Composite c = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setFont(new Font("SansSerif",Font.BOLD,80)); g.setColor(new Color(220,40,40));
        String t = "OBJECTION!"; FontMetrics fm = g.getFontMetrics();
        g.drawString(t, (816-fm.stringWidth(t))/2, 340);
        g.setComposite(c);
    }

    private void drawSprite(Graphics2D g, String key, int x, int y, int w, int h) {
        if (key == null || key.isEmpty()) return;
        BufferedImage s = loadImg("assets/sprites/" + key + ".png");
        if (s != null) g.drawImage(s, x, y, w, h, null);
    }

    private void drawRecord(Graphics2D g) {
        java.util.List<Evidence> ev = state.getEvidence();
        if (ev.isEmpty()) return;
        int px=80,py=100,pw=656,ph=300;
        g.setColor(new Color(10,10,40,230)); g.fillRoundRect(px,py,pw,ph,14,14);
        g.setColor(new Color(180,180,255));  g.drawRoundRect(px,py,pw,ph,14,14);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,16));
        g.drawString("evidence!!!!!!!!!!!!!!!!!!!!!!!!", px+20, py+28);
        for (int i = 0; i < ev.size(); i++) {
            int ix = px+20+i*80;
            if (i==selEv) { g.setColor(new Color(100,120,200,180)); g.fillRoundRect(ix-4,py+46,72,72,8,8); }
            BufferedImage ei = ev.get(i).getImage();
            if (ei != null) g.drawImage(ei, ix, py+50, 64, 64, null);
            else { g.setColor(Color.GRAY); g.fillRect(ix, py+50, 64, 64); }
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.PLAIN,11));
            g.drawString(ev.get(i).getName(), ix, py+126);
        }
        g.setColor(Color.LIGHT_GRAY); g.setFont(new Font("SansSerif",Font.PLAIN,13));
        g.drawString(ev.get(selEv).getDescription(), px+20, py+240);
        g.setColor(new Color(150,150,150)); g.setFont(new Font("SansSerif",Font.PLAIN,12));
        g.drawString("← → navigate    Z/Enter = present    X/Esc = close", px+20, py+270);
    }

    private void drawSlider(Graphics2D g, String label, int x, int y, float val) {
        g.setFont(new Font("SansSerif",Font.PLAIN,14)); g.setColor(Color.LIGHT_GRAY);
        g.drawString(label, x-120, y+5);
        g.setColor(new Color(80,80,90));    g.fillRoundRect(x,y-4,SL_W,8,4,4);
        g.setColor(new Color(120,170,255)); g.fillRoundRect(x,y-4,(int)(SL_W*val),8,4,4);
        g.setColor(Color.WHITE);            g.fillOval(x+(int)(SL_W*val)-8,y-8,16,16);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString(Math.round(val*100)+"%", x+SL_W+12, y+5);
    }

    // ── Utilities ──────────────────────────────────────────────────────────
    private float sliderHit(int sy) {
        if (!input.clicked() || Math.abs(input.my()-sy) > 20) return -1;
        return Math.max(0, Math.min(1, (float)(input.mx()-SL_X)/SL_W));
    }

    // loads image from disk and stores it away. so each file is only read once
    private BufferedImage loadImg(String path) {
        if (imgCache.containsKey(path)) return imgCache.get(path);
        try {
            BufferedImage img = ImageIO.read(new File(path));
            imgCache.put(path, img);
            return img;
        } catch (IOException e) {
            System.err.println("Missing image: " + path);
            imgCache.put(path, null);
            return null;
        }
    }

    public void addHotspot(int x, int y, int w, int h, Evidence ev) {
        hBounds.add(new int[]{x,y,w,h}); hEvidence.add(ev); hCollected.add(false);
    }
    
}