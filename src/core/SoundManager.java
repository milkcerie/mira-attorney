package core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundManager {
    private final Map<String, Clip> musicClips = new HashMap<>();
    private final Map<String, Clip> sfxClips   = new HashMap<>();

    private float  musicVolume  = 1.0f;
    private float  sfxVolume    = 1.0f;

    private String currentMusic = null;
    public void loadMusic(String name, String filepath) {
        musicClips.put(name, loadClip(filepath));
    }
    public void loadSFX(String name, String filepath) {
        sfxClips.put(name, loadClip(filepath));
    }
    private Clip loadClip(String filepath) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filepath));
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        }
        catch (UnsupportedAudioFileException e) {
            System.err.println("Unsupported audio format: " + filepath);
        } catch (IOException e) {
            System.err.println("Could not read audio file: " + filepath);
        } catch (LineUnavailableException e) {
            System.err.println("No audio line available for: " + filepath);
        }
        return null;
    }
    public void playMusic(String name, boolean loop) {
        stopMusic(); // always stop current track first

        Clip clip = musicClips.get(name);
        if (clip == null) return; // if track was not loaded do nothing

        // reset to  beginning in case the clip was played before.
        clip.setFramePosition(0);
        applyVolume(clip, musicVolume);

        if (loop) {
            // loop forever
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.start();
        }
        currentMusic = name;
    }
    public void stopMusic() {
        if (currentMusic == null) return;
        Clip clip = musicClips.get(currentMusic);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
        currentMusic = null;
    }
    public void playSFX(String name) {
        Clip clip = sfxClips.get(name);
        if (clip == null) return;
        clip.setFramePosition(0); // restart from the beginning
        applyVolume(clip, sfxVolume);
        clip.start();
    }
    public void setMusicVolume(float v) {
        musicVolume = v;
        // apply immediately to the currently playing track so the change happens 
        //while the slider is being dragged in real time
        if (currentMusic != null) {
            Clip clip = musicClips.get(currentMusic);
            if (clip != null) applyVolume(clip, v);
        }
    }
    public void setSFXVolume(float v) {
        sfxVolume = v;
    }
    private void applyVolume(Clip clip, float volume) {
        if (clip == null) return;

        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;

        FloatControl gain =
            (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

        if (volume <= 0.0f) {
            gain.setValue(gain.getMinimum());
        } else {
            // convert 0.0–1.0 linear scale to decibels
            float dB = 20.0f * (float) Math.log10(volume);

            dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
            gain.setValue(dB);
        }
    }
}