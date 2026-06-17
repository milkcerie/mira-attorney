package core;

public class ObjectionChecker {
    // return correction script filename if correct
    //return null if wrong
    public static String check(Evidence ev, Statement stmt, int sentenceIdx) {
        String correct = stmt.objectionMap.get(sentenceIdx);
        if (correct == null) return null;
        return ev.getName().equals(correct) ? stmt.correctionScripts.get(sentenceIdx) : null;
    }
}