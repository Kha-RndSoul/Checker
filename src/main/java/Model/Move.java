package Model;

import java.util.*;

/**
 * Một nước đi: gồm danh sách các ô đi qua (steps) và quân bị ăn (captures).
 * steps[0] = ô xuất phát, steps[1..n] = ô đáp.
 */
public class Move {
    private final List<int[]> steps    = new ArrayList<>();
    private final List<int[]> captures = new ArrayList<>();

    public Move(int r, int c)           { steps.add(new int[]{r, c}); }
    public Move(Move o)                 { o.steps.forEach(s -> steps.add(s.clone()));
        o.captures.forEach(c -> captures.add(c.clone())); }

    public void addStep(int r, int c)    { steps.add(new int[]{r, c}); }
    public void addCapture(int r, int c) { captures.add(new int[]{r, c}); }

    public int getFromRow()  { return steps.get(0)[0]; }
    public int getFromCol()  { return steps.get(0)[1]; }
    public int getToRow()    { return steps.get(steps.size()-1)[0]; }
    public int getToCol()    { return steps.get(steps.size()-1)[1]; }
    public List<int[]> getCaptures() { return captures; }
    public boolean isCapture()       { return !captures.isEmpty(); }
}