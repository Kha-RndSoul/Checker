package Model;

import java.util.*;

/** Trạng thái toàn bộ ván cờ: bàn cờ, lượt đi, quân đang chọn. */
public class GameModel {

    public enum Mode   { PVP, PV_AI }
    public enum Status { PLAYING, RED_WINS, BLACK_WINS }
    public enum Diff   { EASY, MEDIUM, HARD }

    // ── PHẦN PHÁT TRIỂN-23130072-Dũng
    // Cấu trúc Snapshot để chụp lại khoảnh khắc của bàn cờ trước mỗi nước đi
    private static class GameStateSnapshot {
        final Board boardSnapshot;
        final boolean redTurnSnapshot;
        final Status statusSnapshot;
        final int redMovesSnap, blackMovesSnap, redCapturesSnap, blackCapturesSnap;

        GameStateSnapshot(Board b, boolean rt, Status s, int rm, int bm, int rc, int bc) {
            this.boardSnapshot = b.copy();
            this.redTurnSnapshot = rt;
            this.statusSnapshot = s;
            this.redMovesSnap = rm;
            this.blackMovesSnap = bm;
            this.redCapturesSnap = rc;
            this.blackCapturesSnap = bc;
        }
    }
    private final Stack<GameStateSnapshot> undoStack = new Stack<>();
   

    private Board  board;
    private Mode   mode;
    private Status status;
    private boolean redTurn;        // true = lượt đỏ
    private Piece  selected;
    private List<Move> selMoves = new ArrayList<>();
    private AIPlayer ai;
    
    // Các biến phục vụ thống kê ván đấu
    private int redMoves = 0;
    private int blackMoves = 0;
    private int redCaptures = 0;
    private int blackCaptures = 0;
    private long startTime;

    public void newGame(Mode mode, Diff diff) {
        this.mode   = mode;
        this.board  = new Board();
        this.board.init();
        this.redTurn = true;
        this.status  = Status.PLAYING;
        this.selected = null;
        this.selMoves = new ArrayList<>();
        this.ai = (mode == Mode.PV_AI) ? new AIPlayer(diff) : null;

        // Reset dữ liệu thống kê & Lịch sử
        this.undoStack.clear();
        this.redMoves      = 0;
        this.blackMoves    = 0;
        this.redCaptures   = 0;
        this.blackCaptures = 0;
        this.startTime     = System.currentTimeMillis();
    }

    /** Chọn quân tại (r,c). Trả về true nếu chọn được. */
    public boolean select(int r, int c) {
        clearSelection();
        Piece p = board.get(r, c);
        if (p==null || p.isRed()!=redTurn) return false;
        List<Move> moves = board.movesForPiece(r, c, redTurn);
        if (moves.isEmpty()) return false;
        selected = p; selMoves = moves;
        return true;
    }

    /** Thực hiện nước đi đến (r,c) nếu hợp lệ. Trả về true nếu thành công. */
    public boolean moveTo(int r, int c) {
        for (Move m : selMoves)
            if (m.getToRow()==r && m.getToCol()==c) { applyMove(m); return true; }
        return false;
    }

    public void applyMove(Move m) {
        //  PHẦN PHÁT TRIỂN - 23130072-Dũng 
        // Đẩy trạng thái hiện tại vào lưu trữ lịch sử trước khi thay đổi dữ liệu bàn cờ
        undoStack.push(new GameStateSnapshot(board, redTurn, status, redMoves, blackMoves, redCaptures, blackCaptures));

        // Cập nhật thống kê nước đi
        int captured = (m.getCaptures() != null) ? m.getCaptures().size() : 0;
        if (redTurn) {
            redMoves++;
            redCaptures += captured;
        } else {
            blackMoves++;
            blackCaptures += captured;
        }
        
        board.applyMove(m);
        clearSelection();
        redTurn = !redTurn;
        checkWin();
    }

    // PHẦN PHÁT TRIỂN - 23130072-Dũng
    // Logic rút quân quay ngược thời gian phục vụ tính năng Hoàn tác
    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        GameStateSnapshot snap = undoStack.pop();
        this.board = snap.boardSnapshot;
        this.redTurn = snap.redTurnSnapshot;
        this.status = snap.statusSnapshot;
        this.redMoves = snap.redMovesSnap;
        this.blackMoves = snap.blackMovesSnap;
        this.redCaptures = snap.redCapturesSnap;
        this.blackCaptures = snap.blackCapturesSnap;

        clearSelection();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty(); }
    private void checkWin() {
        if (board.validMoves(true).isEmpty())  status = Status.BLACK_WINS;
        if (board.validMoves(false).isEmpty()) status = Status.RED_WINS;
    }

    public void clearSelection() { selected=null; selMoves=new ArrayList<>(); }

    public Move getAIMove() { return (ai!=null) ? ai.best(board, redTurn) : null; }
    
    //  UC12: Getters thống kê
    public int getRedMoves()      { return redMoves; }
    public int getBlackMoves()    { return blackMoves; }
    public int getRedCaptures()   { return redCaptures; }
    public int getBlackCaptures() { return blackCaptures; }

    public String getElapsedTime() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long min = elapsed / 60;
        long sec = elapsed % 60;
        return String.format("%02d:%02d", min, sec);
    }

    //  Getters 
    public Board  getBoard()    { return board; }
    public Status getStatus()   { return status; }
    public Mode   getMode()     { return mode; }
    public boolean isRedTurn()  { return redTurn; }
    public Piece  getSelected() { return selected; }
    public List<Move> getSelMoves() { return selMoves; }
    public int redCount()  { return board==null?0:board.count(true);  }
    public int blackCount(){ return board==null?0:board.count(false); }
}