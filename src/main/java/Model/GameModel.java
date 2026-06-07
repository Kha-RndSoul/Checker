package Model;

import java.util.*;

/**
 * GameModel – Trạng thái toàn bộ ván cờ: bàn cờ, lượt đi, quân đang chọn.
 *
 * UC12 – THÔNG BÁO KẾT QUẢ
 * Class này cung cấp toàn bộ dữ liệu mà GameController cần ở Bước 2:
 *   • getStatus()        – trạng thái ván (PLAYING / RED_WINS / BLACK_WINS)
 *   • getEndReasonText() – lý do kết thúc (bên nào hết nước đi)
 *   • getElapsedTime()   – thời gian đã chơi (mm:ss)
 *   • getRedMoves()      – tổng số nước đi của bên Đỏ
 *   • getBlackMoves()    – tổng số nước đi của bên Đen
 *   • getRedCaptures()   – tổng số quân ăn được của bên Đỏ
 *   • getBlackCaptures() – tổng số quân ăn được của bên Đen
 * Các getter trên tương ứng với GameResult DTO trong Sequence Diagram UC12.
 */
public class GameModel {

    public enum Mode      { PVP, PV_AI }
    public enum Status    { PLAYING, RED_WINS, BLACK_WINS }
    public enum Diff      { EASY, MEDIUM, HARD }
    public enum EndReason { NONE, RED_NO_VALID_MOVES, BLACK_NO_VALID_MOVES }

    // ── Snapshot dùng cho tính năng Hoàn tác ────────────────────────────────
    // Chụp lại toàn bộ trạng thái bàn cờ trước mỗi nước đi để có thể phục hồi
    private static class GameStateSnapshot {
        final Board   boardSnapshot;
        final boolean redTurnSnapshot;
        final Status  statusSnapshot;
        final int redMovesSnap, blackMovesSnap, redCapturesSnap, blackCapturesSnap;
        final List<String> moveLogSnapshot; // bản sao lịch sử tại thời điểm chụp

        GameStateSnapshot(Board b, boolean rt, Status s,
                          int rm, int bm, int rc, int bc, List<String> log) {
            this.boardSnapshot  = b.copy();
            this.redTurnSnapshot = rt;
            this.statusSnapshot  = s;
            this.redMovesSnap    = rm;
            this.blackMovesSnap  = bm;
            this.redCapturesSnap = rc;
            this.blackCapturesSnap = bc;
            this.moveLogSnapshot = new ArrayList<>(log);
        }
    }

    private final Stack<GameStateSnapshot> undoStack = new Stack<>();
    private List<String> moveLog = new ArrayList<>(); // lịch sử nước đi dạng ký hiệu đại số

    private Board   board;
    private Mode    mode;
    private Status  status;
    private boolean redTurn;   // true = lượt đỏ
    private Piece   selected;
    private List<Move> selMoves = new ArrayList<>();
    private AIPlayer ai;

    // ── Biến thống kê ván đấu – được trả về ở UC12-Bước 2 (GameResult DTO) ──
    private int  redMoves    = 0; // số nước đi của bên Đỏ
    private int  blackMoves  = 0; // số nước đi của bên Đen
    private int  redCaptures = 0; // số quân ăn được của bên Đỏ
    private int  blackCaptures = 0; // số quân ăn được của bên Đen
    private long startTime;       // mốc thời gian bắt đầu ván (ms)
    private EndReason endReason = EndReason.NONE;

    /**
     * Khởi tạo ván mới: reset bàn cờ, lượt đi, thống kê và lịch sử.
     * UC12: reset toàn bộ số liệu thống kê để chuẩn bị cho ván kế tiếp
     * (được gọi từ GameController sau khi người chơi chọn "Chơi lại").
     */
    public void newGame(Mode mode, Diff diff) {
        this.mode    = mode;
        this.board   = new Board();
        this.board.init();
        this.redTurn  = true;
        this.status   = Status.PLAYING;
        this.selected = null;
        this.selMoves = new ArrayList<>();
        this.ai       = (mode == Mode.PV_AI) ? new AIPlayer(diff) : null;
        this.endReason = EndReason.NONE;
        // Reset dữ liệu thống kê và lịch sử – chuẩn bị GameResult DTO cho ván mới
        this.undoStack.clear();
        this.moveLog.clear();
        this.redMoves      = 0;
        this.blackMoves    = 0;
        this.redCaptures   = 0;
        this.blackCaptures = 0;
        this.startTime     = System.currentTimeMillis();
    }

    /** Chọn quân tại (r,c). Trả về true nếu chọn hợp lệ. */
    public boolean select(int r, int c) {
        clearSelection();
        Piece p = board.get(r, c);
        if (p == null || p.isRed() != redTurn) return false;
        List<Move> moves = board.movesForPiece(r, c, redTurn);
        if (moves.isEmpty()) return false;
        selected = p;
        selMoves = moves;
        return true;
    }

    /**
     * Thực hiện nước đi đến (r,c) nếu hợp lệ.
     * Trả về true nếu thành công – GameController sẽ gọi checkGameOver() ngay sau.
     */
    public boolean moveTo(int r, int c) {
        for (Move m : selMoves)
            if (m.getToRow() == r && m.getToCol() == c) { applyMove(m); return true; }
        return false;
    }

    /**
     * Áp dụng nước đi lên bàn cờ, cập nhật thống kê và kiểm tra thắng/thua.
     *
     * UC12: mỗi lần applyMove() hoàn thành, các biến redMoves/blackMoves/
     * redCaptures/blackCaptures được cập nhật – đây là nguồn dữ liệu cho
     * GameResult DTO trả về ở UC12-Bước 2.
     * checkWin() bên trong sẽ cập nhật status và endReason nếu ván kết thúc.
     */
    public void applyMove(Move m) {
        // Lưu snapshot trước khi thay đổi (phục vụ tính năng Hoàn tác)
        undoStack.push(new GameStateSnapshot(
                board, redTurn, status,
                redMoves, blackMoves, redCaptures, blackCaptures, moveLog));

        // Sinh ký hiệu đại số và thêm vào lịch sử
        String notation = generateNotation(m, redTurn);
        moveLog.add(notation);

        // UC12: cập nhật số nước đi và số quân ăn được cho từng bên
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

        // UC12: sau khi đổi lượt, kiểm tra bên mới có còn nước đi không
        // → cập nhật status (RED_WINS / BLACK_WINS) và endReason
        checkWin();
    }

    /**
     * Hoàn tác nước đi gần nhất: phục hồi toàn bộ trạng thái bàn cờ
     * bao gồm cả số liệu thống kê (redMoves, blackMoves, redCaptures, blackCaptures).
     */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        GameStateSnapshot snap = undoStack.pop();
        this.board         = snap.boardSnapshot;
        this.redTurn       = snap.redTurnSnapshot;
        this.status        = snap.statusSnapshot;
        this.endReason     = EndReason.NONE;
        this.redMoves      = snap.redMovesSnap;
        this.blackMoves    = snap.blackMovesSnap;
        this.redCaptures   = snap.redCapturesSnap;
        this.blackCaptures = snap.blackCapturesSnap;
        this.moveLog       = new ArrayList<>(snap.moveLogSnapshot);
        clearSelection();
        return true;
    }

    /**
     * Chuyển đổi tọa độ (row, col) sang ký hiệu cờ đại số (a1–h8).
     * Hàng 0 trên màn hình tương ứng với hàng 8 trên bàn cờ thực tế.
     */
    private String generateNotation(Move m, boolean isRed) {
        char startCol = (char) ('a' + m.getFromCol());
        char endCol   = (char) ('a' + m.getToCol());
        int  startRow = 8 - m.getFromRow();
        int  endRow   = 8 - m.getToRow();
        String startSq  = startCol + String.valueOf(startRow);
        String endSq    = endCol   + String.valueOf(endRow);
        String emoji    = isRed ? "🔴" : "⚫";
        String separator = m.isCapture() ? " x " : " - ";
        return emoji + " " + startSq + separator + endSq;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    /**
     * Kiểm tra điều kiện thắng/thua sau mỗi nước đi.
     *
     * UC12-Bước 2 (nội bộ): nếu một bên hết nước đi hợp lệ,
     * status được cập nhật thành RED_WINS hoặc BLACK_WINS,
     * và endReason ghi nhận bên nào là nguyên nhân kết thúc.
     * GameController đọc status này trong checkGameOver().
     */
    private void checkWin() {
        endReason = EndReason.NONE;
        // Đỏ hết nước đi → Đen thắng
        if (hasNoValidMoves(true)) {
            status    = Status.BLACK_WINS;
            endReason = EndReason.RED_NO_VALID_MOVES;
            return;
        }
        // Đen hết nước đi → Đỏ thắng
        if (hasNoValidMoves(false)) {
            status    = Status.RED_WINS;
            endReason = EndReason.BLACK_NO_VALID_MOVES;
        }
    }

    public void clearSelection() { selected = null; selMoves = new ArrayList<>(); }

    public Move getAIMove() { return (ai != null) ? ai.best(board, redTurn) : null; }

    /** Kiểm tra bên isRed có còn nước đi hợp lệ không – dùng trong checkWin(). */
    public boolean hasNoValidMoves(boolean isRed) {
        return board != null && board.hasNoValidMoves(isRed);
    }

    // ── Getters thống kê ván đấu – nguồn dữ liệu GameResult DTO (UC12-Bước 2) ──

    /** UC12: tổng số nước đi của bên Đỏ trong ván hiện tại. */
    public int getRedMoves()      { return redMoves; }

    /** UC12: tổng số nước đi của bên Đen trong ván hiện tại. */
    public int getBlackMoves()    { return blackMoves; }

    /** UC12: tổng số quân bên Đỏ đã ăn được trong ván hiện tại. */
    public int getRedCaptures()   { return redCaptures; }

    /** UC12: tổng số quân bên Đen đã ăn được trong ván hiện tại. */
    public int getBlackCaptures() { return blackCaptures; }

    /** UC12: thời gian đã trôi qua kể từ khi bắt đầu ván, định dạng mm:ss. */
    public String getElapsedTime() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long min = elapsed / 60;
        long sec = elapsed % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public List<String> getMoveLog() { return moveLog; }

    // ── Getters trạng thái ván ──────────────────────────────────────────────

    public Board    getBoard()    { return board; }

    /**
     * UC12-Bước 2: GC → GM : getStatus() / GM --> GC : status
     * Trả về PLAYING, RED_WINS hoặc BLACK_WINS.
     */
    public Status   getStatus()   { return status; }

    public Mode     getMode()     { return mode; }
    public boolean  isRedTurn()   { return redTurn; }
    public Piece    getSelected() { return selected; }
    public List<Move> getSelMoves() { return selMoves; }
    public int redCount()   { return board == null ? 0 : board.count(true);  }
    public int blackCount() { return board == null ? 0 : board.count(false); }
    public EndReason getEndReason() { return endReason; }

    /**
     * UC12-Bước 2: một phần của GameResult DTO – lý do kết thúc ván.
     * Trả về chuỗi mô tả nguyên nhân để hiển thị trong hộp thoại kết quả.
     */
    public String getEndReasonText() {
        switch (endReason) {
            case RED_NO_VALID_MOVES:   return "Đỏ không còn nước đi hợp lệ.";
            case BLACK_NO_VALID_MOVES: return "Đen không còn nước đi hợp lệ.";
            default:                   return "";
        }
    }
}
