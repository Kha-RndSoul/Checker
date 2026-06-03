package Model;

import java.util.*;

public class GameModel {

    public enum Mode   { PVP, PV_AI }
    public enum Status { PLAYING, RED_WINS, BLACK_WINS }
    public enum Diff   { EASY, MEDIUM, HARD }

    // Dung Lớp Snapshot lưu trạng thái cũ để phục vụ hoàn tác
    private static class GameStateSnapshot {
        final Board boardSnapshot;
        final boolean redTurnSnapshot;
        final Status statusSnapshot;

        GameStateSnapshot(Board b, boolean rt, Status s) {
            this.boardSnapshot = b.copy();
            this.redTurnSnapshot = rt;
            this.statusSnapshot = s;
        }
    }

    private Board  board;
    private Mode   mode;
    private Status status;
    private boolean redTurn;
    private Piece  selected;
    private List<Move> selMoves = new ArrayList<>();
    private AIPlayer ai;

    // Dung ngăn xếp duy nhất cho Undo
    private final Stack<GameStateSnapshot> undoStack = new Stack<>();

    public void newGame(Mode mode, Diff diff) {
        this.mode   = mode;
        this.board  = new Board();
        this.board.init();
        this.redTurn = true;
        this.status  = Status.PLAYING;
        this.selected = null;
        this.selMoves = new ArrayList<>();
        this.ai = (mode == Mode.PV_AI) ? new AIPlayer(diff) : null;

        undoStack.clear(); // Xóa lịch sử khi vào ván mới
    }

    public boolean select(int r, int c) {
        clearSelection();
        Piece p = board.get(r, c);
        if (p==null || p.isRed()!=redTurn) return false;
        List<Move> moves = board.movesForPiece(r, c, redTurn);
        if (moves.isEmpty()) return false;
        selected = p; selMoves = moves;
        return true;
    }

    public boolean moveTo(int r, int c) {
        for (Move m : selMoves)
            if (m.getToRow()==r && m.getToCol()==c) { applyMove(m); return true; }
        return false;
    }

    public void applyMove(Move m) {
        // Lưu lại trạng thái hiện tại trước khi thực hiện nước đi mới
        undoStack.push(new GameStateSnapshot(board, redTurn, status));

        board.applyMove(m);
        clearSelection();
        redTurn = !redTurn;
        checkWin();
    }

    // Logic xử lý Hoàn tác
    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        GameStateSnapshot snap = undoStack.pop();
        this.board = snap.boardSnapshot;
        this.redTurn = snap.redTurnSnapshot;
        this.status = snap.statusSnapshot;
        clearSelection();
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    private void checkWin() {
        if (board.validMoves(true).isEmpty())  status = Status.BLACK_WINS;
        if (board.validMoves(false).isEmpty()) status = Status.RED_WINS;
    }

    public void clearSelection() { selected=null; selMoves=new ArrayList<>(); }
    public Move getAIMove() { return (ai!=null) ? ai.best(board, redTurn) : null; }

    // ── Getters ──────────────────────────────────────────────────────
    public Board  getBoard()    { return board; }
    public Status getStatus()   { return status; }
    public Mode   getMode()     { return mode; }
    public boolean isRedTurn()  { return redTurn; }
    public Piece  getSelected() { return selected; }
    public List<Move> getSelMoves() { return selMoves; }
}