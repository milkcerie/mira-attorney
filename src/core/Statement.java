package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statement {
    public final String speakerName;
    public final List<String> sentences = new ArrayList<>();
    public final Map<Integer, String> objectionMap = new HashMap<>();
    public final Map<Integer, String> correctionScripts = new HashMap<>();
    public Statement(String speakerName) {
        this.speakerName = speakerName;
    }
}