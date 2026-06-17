package core;
import java.awt.event.*;
import java.util.*;

public class InputManager extends KeyAdapter implements MouseListener {
    private final Set<Integer> held = new HashSet<>(), justPressed = new HashSet<>();
    private int mx, my;
    private boolean clicked;

    @Override public void keyPressed(KeyEvent e) {
        if (!held.contains(e.getKeyCode())) justPressed.add(e.getKeyCode());
        held.add(e.getKeyCode());
    }
    @Override public void keyReleased(KeyEvent e) {
        held.remove(e.getKeyCode());
    }
    @Override public void mousePressed(MouseEvent e) {
        mx = e.getX(); my = e.getY(); clicked = true; 
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public boolean justPressed(int k) {
        return justPressed.contains(k);
    }
    public boolean held(int k) {
        return held.contains(k);
    }
    public boolean clicked() {
        return clicked; 
    }
    public int mx() {
        return mx; 
    }
    public int my() {
        return my; 
    }
    public boolean clickedIn(int x, int y, int w, int h) {
        return clicked && mx >= x && mx <= x+w && my >= y && my <= y+h;
    }
    public void endFrame() { justPressed.clear(); clicked = false; }
}
