package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    private int hp = 5;
    private Scene checkpoint = Scene.HOME;                      // ADD THIS
    private final Map<String, Boolean> flags = new HashMap<>(); // ADD THIS

    public void loseHP()         { hp--; }
    public boolean isGameOver()  { return hp <= 0; }
    public int getHP()           { return hp; }
    public void resetHP()        { hp = 5; }

    public enum Scene {
        HOME, INVESTIGATION, COURT, VERDICT
    }

    public void saveCheckpoint(Scene scene) { checkpoint = scene; }
    public Scene getCheckpoint()            { return checkpoint; }

    public void setFlag(String name)    { flags.put(name, true); }
    public boolean hasFlag(String name) { return flags.getOrDefault(name, false); }
    public void clearFlag(String name)  { flags.remove(name); }

    private final List<Evidence> collectedEvidence = new ArrayList<>();

    public void addEvidence(Evidence e) {
        if (!hasEvidence(e.getName())) collectedEvidence.add(e);
    }
    public boolean hasEvidence(String name) {
        for (Evidence e : collectedEvidence) {
            if (e.getName().equals(name)) return true;
        }
        return false;
    }
    public List<Evidence> getEvidence() { return collectedEvidence; }

    private float musicVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private float textScrollSpeed = 1.0f;

    public float getMusicVolume() { 
        return musicVolume; 
    }
    public float getSFXVolume() {
         return sfxVolume; 
    }
    public float getTextScrollSpeed() { 
        return textScrollSpeed; 
    }

    public void setMusicVolume(float v) {
        musicVolume = clamp(v); 
    }
    public void setSFXVolume(float v) {
        sfxVolume = clamp(v);
    }
    public void setTextScrollSpeed(float v) {
        textScrollSpeed = Math.max(0.1f, Math.min(5.0f, v)); 
    }

    private float clamp(float v) { 
        return Math.max(0.0f, Math.min(1.0f, v)); 
    }
}