package Model;

import java.util.*;

/** Trạng thái toàn bộ ván cờ: bàn cờ, lượt đi, quân đang chọn. */
public class GameModel {

    public enum Mode   { PVP, PV_AI }
    public enum Status { PLAYING, RED_WINS, BLACK_WINS }
    public enum Diff   { EASY, MEDIUM, HARD }
    public enum EndReason {NONE, RED_NO_VALID_MOVES, BLACK_NO_VALID_MOVES}

    // ── PHẦN PHÁT TRIỂN-23130072-Dũng
    // Cấu trúc Snapshot để chụp lại khoảnh khắc của bàn cờ trước mỗi nước đi
    private static class GameStateSnapshot {
        final Board boardSnapshot;
        final boolean redTurnSnapshot;
        final Status statusSnapshot;
        final int redMovesSnap, blackMovesSnap, redCapturesSnap, blackCapturesSnap;
        final List<String> moveLogSnapshot; // Lưu bản sao lịch sử tại thời điểm chụp

        GameStateSnapshot(Board b, boolean rt, Status s, int rm, int bm, int rc, int bc, List<String> log) {
            this.boardSnapshot = b.copy();
            this.redTurnSnapshot = rt;
            this.statusSnapshot = s;
            this.redMovesSnap = rm;
            this.blackMovesSnap = bm;
            this.redCapturesSnap = rc;
            this.blackCapturesSnap = bc;
            this.moveLogSnapshot = new ArrayList<>(log); // Copy danh sách log cũ
        }
    }
    private final Stack<GameStateSnapshot> undoStack = new Stack<>();
    private List<String> moveLog = new ArrayList<>(); // Danh sách lưu trữ lịch sử nước đi dạng chuỗi ký tự


    private Board  board;
    private Mode   mode;
    private Status status;
    private boolean redTurn;        // true = lượt đỏ
    private Piece  selected;
    private List<Move> selMoves = new ArrayList<>();
    private AIPlayer ai;

    // ── UC12: Các biến tích lũy thống kê ván đấu ──
    // Được cập nhật trong applyMove() sau mỗi nước đi và đọc bởi
    // GameController.checkGameOver() khi xây dựng dialog kết quả.
    private int redMoves = 0;      // UC12: Tổng số nước đi của bên Đỏ
    private int blackMoves = 0;    // UC12: Tổng số nước đi của bên Đen
    private int redCaptures = 0;   // UC12: Tổng quân ăn được của bên Đỏ
    private int blackCaptures = 0; // UC12: Tổng quân ăn được của bên Đen
    private long startTime;        // UC12: Thời điểm bắt đầu ván (ms) để tính thời gian thi đấu
    private EndReason endReason = EndReason.NONE;

    public void newGame(Mode mode, Diff diff) {
        this.mode   = mode;
        this.board  = new Board();
        this.board.init();
        this.redTurn = true;
        this.status  = Status.PLAYING;
        this.selected = null;
        this.selMoves = new ArrayList<>();
        this.ai = (mode == Mode.PV_AI) ? new AIPlayer(diff) : null;
        this.endReason = EndReason.NONE;
        // UC12: Reset toàn bộ bộ đếm thống kê khi bắt đầu ván mới
        this.undoStack.clear();
        this.moveLog.clear(); // ── PHẦN PHÁT TRIỂN-23130072-Dũng: Xóa trắng log khi sang ván mới
        this.redMoves      = 0;
        this.blackMoves    = 0;
        this.redCaptures   = 0;
        this.blackCaptures = 0;
        this.startTime     = System.currentTimeMillis(); // UC12: Ghi nhận mốc thời gian khởi đầu ván
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
        //  ── PHẦN PHÁT TRIỂN-23130072-Dũng
        // Đẩy trạng thái hiện tại vào lưu trữ lịch sử trước khi thay đổi dữ liệu bàn cờ
        undoStack.push(new GameStateSnapshot(board, redTurn, status, redMoves, blackMoves, redCaptures, blackCaptures, moveLog));

        // Sinh ký tự viết cờ đại số và thêm vào bảng log nước đi
        String notation = generateNotation(m, redTurn);
        moveLog.add(notation);

        // ── UC12: Tích lũy thống kê nước đi sau mỗi lần applyMove ──
        // Số liệu này sẽ được đọc bởi getRedMoves(), getBlackMoves(),
        // getRedCaptures(), getBlackCaptures() khi dialog UC12 được dựng lên.
        int captured = (m.getCaptures() != null) ? m.getCaptures().size() : 0;
        if (redTurn) {
            redMoves++;            // UC12: Ghi nhận 1 nước đi của Đỏ
            redCaptures += captured; // UC12: Cộng dồn quân Đỏ ăn được
        } else {
            blackMoves++;            // UC12: Ghi nhận 1 nước đi của Đen
            blackCaptures += captured; // UC12: Cộng dồn quân Đen ăn được
        }

        board.applyMove(m);
        clearSelection();
        redTurn = !redTurn;
        checkWin(); // UC12: Sau mỗi nước đi → kiểm tra điều kiện thắng/thua
    }

    // ── PHẦN PHÁT TRIỂN-23130072-Dũng
    // Logic rút quân quay ngược thời gian phục vụ tính năng Hoàn tác
    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        GameStateSnapshot snap = undoStack.pop();
        this.board = snap.boardSnapshot;
        this.redTurn = snap.redTurnSnapshot;
        this.status = snap.statusSnapshot;
        this.endReason = EndReason.NONE;
        this.redMoves = snap.redMovesSnap;
        this.blackMoves = snap.blackMovesSnap;
        this.redCaptures = snap.redCapturesSnap;
        this.blackCaptures = snap.blackCapturesSnap;
        this.moveLog = new ArrayList<>(snap.moveLogSnapshot); // Khôi phục lại nhật ký log nước đi cũ

        clearSelection();
        return true;
    }

    // ── PHẦN PHÁT TRIỂN-23130072-Dũng
    // Thuật toán chuyển đổi dữ liệu tọa độ (Row, Col) sang Hệ tọa độ Đại số (a1 -> h8)
    private String generateNotation(Move m, boolean isRed) {
        char startCol = (char) ('a' + m.getFromCol());
        char endCol   = (char) ('a' + m.getToCol());

        // Mảng 2D dòng 0 ở trên cùng màn hình tương ứng với hàng 8 của bàn cờ thực tế
        int startRow = 8 - m.getFromRow();
        int endRow   = 8 - m.getToRow();

        String startSq = startCol + String.valueOf(startRow);
        String endSq   = endCol + String.valueOf(endRow);

        String emoji = isRed ? "🔴" : "⚫";
        String separator = m.isCapture() ? " x " : " - ";

        return emoji + " " + startSq + separator + endSq;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty(); }

    /**
     * UC12 [Bước hỗ trợ – checkWin]: Sau mỗi nước đi, kiểm tra xem
     * có bên nào hết nước đi hợp lệ không. Nếu có → cập nhật Status
     * và EndReason để GameController.checkGameOver() đọc và hiển thị
     * dialog kết quả UC12.
     */
    private void checkWin() {
        endReason = EndReason.NONE;

        // UC12: Kiểm tra bên Đỏ còn nước đi không; nếu không → Đen thắng
        if (hasNoValidMoves(true)) {
            status = Status.BLACK_WINS;
            endReason = EndReason.RED_NO_VALID_MOVES;
            return;
        }

        // UC12: Kiểm tra bên Đen còn nước đi không; nếu không → Đỏ thắng
        if (hasNoValidMoves(false)) {
            status = Status.RED_WINS;
            endReason = EndReason.BLACK_NO_VALID_MOVES;
        }
    }

    public void clearSelection() { selected=null; selMoves=new ArrayList<>(); }

    public Move getAIMove() { return (ai!=null) ? ai.best(board, redTurn) : null; }

    //UC11: Kiểm tra xem bên isRed có còn nước đi hợp lệ nào không
    public boolean hasNoValidMoves(boolean isRed) {
        return board != null && board.hasNoValidMoves(isRed);
    }

    // ── UC12: Các getter thống kê ─────────────────────────────────────
    // Được gọi bởi GameController.checkGameOver() để điền vào bảng
    // thống kê trong dialog thông báo kết quả UC12.

    /** UC12: Trả về tổng số nước đi của bên Đỏ trong ván. */
    public int getRedMoves()      { return redMoves; }

    /** UC12: Trả về tổng số nước đi của bên Đen trong ván. */
    public int getBlackMoves()    { return blackMoves; }

    /** UC12: Trả về số quân bên Đỏ đã ăn được trong ván. */
    public int getRedCaptures()   { return redCaptures; }

    /** UC12: Trả về số quân bên Đen đã ăn được trong ván. */
    public int getBlackCaptures() { return blackCaptures; }

    public List<String> getMoveLog() { return moveLog; } // Getter lấy danh sách log nước đi

    /**
     * UC12: Tính và trả về chuỗi thời gian thi đấu định dạng "MM:SS".
     * Được gọi khi dựng bảng thống kê trong dialog kết quả.
     */
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
    public EndReason getEndReason() {return endReason;}

    /**
     * UC12: Trả về chuỗi mô tả lý do kết thúc ván đấu.
     * Được đọc bởi GameController.checkGameOver() để hiển thị
     * dòng "Lý do: ..." trong dialog thông báo kết quả.
     */
    public String getEndReasonText() {
        switch (endReason) {
            case RED_NO_VALID_MOVES:
                return "Đỏ không còn nước đi hợp lệ.";
            case BLACK_NO_VALID_MOVES:
                return "Đen không còn nước đi hợp lệ.";
            default:
                return "";
        }
    }
}