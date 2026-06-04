package Model;

import java.util.*;

/** Trạng thái toàn bộ ván cờ: bàn cờ, lượt đi, quân đang chọn. */
public class GameModel {

    public enum Mode   { PVP, PV_AI }
    public enum Status { PLAYING, RED_WINS, BLACK_WINS }
    public enum Diff   { EASY, MEDIUM, HARD }
    private String endReason = "";//Phuoc
    private Board  board;
    private Mode   mode;
    private Status status;
    private boolean redTurn;        // true = lượt đỏ
    private Piece  selected;
    private List<Move> selMoves = new ArrayList<>();
    private AIPlayer ai;

    public void newGame(Mode mode, Diff diff) {
        this.mode   = mode;
        this.board  = new Board();
        this.board.init();
        this.redTurn = true;
        this.status  = Status.PLAYING;
        this.selected = null;
        this.selMoves = new ArrayList<>();
        this.ai = (mode == Mode.PV_AI) ? new AIPlayer(diff) : null;
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
        board.applyMove(m);
        clearSelection();
        redTurn = !redTurn;
        checkWin();
    }
    //Phuoc
    private boolean hasNoValidMoves(boolean isRed) {
        return board.validMoves(isRed).isEmpty();
    }
    //endPhuoc
    private void checkWin() {
        if (hasNoValidMoves(true)) {
            status = Status.BLACK_WINS;
            endReason = "Đỏ không còn nước đi hợp lệ";
            return;
        }

        if (hasNoValidMoves(false)) {
            status = Status.RED_WINS;
            endReason = "Đen không còn nước đi hợp lệ";
        }
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
    public int redCount()  { return board==null?0:board.count(true);  }
    public int blackCount(){ return board==null?0:board.count(false); }
    //Phuoc
    public String getEndReason() {return endReason;}
}