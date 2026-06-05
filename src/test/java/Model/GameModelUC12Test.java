package Model;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UC12 – Thông báo kết quả ván đấu
 *
 * Bao gồm 3 nhóm kiểm thử:
 *   Nhóm 1 – Điều kiện kết thúc ván (checkWin)
 *   Nhóm 2 – Thống kê ván đấu (moves / captures / undo)
 *   Nhóm 3 – Định dạng thời gian (getElapsedTime)
 */
public class GameModelUC12Test {

    private GameModel model;

    // ────────────────────────────────────────────────
    //  Helper: xoá toàn bộ bàn cờ
    // ────────────────────────────────────────────────
    private void clearBoard() {
        Board board = model.getBoard();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board.remove(r, c);
    }

    // ────────────────────────────────────────────────
    //  Setup
    // ────────────────────────────────────────────────
    @BeforeEach
    void setUp() {
        model = new GameModel();
        model.newGame(GameModel.Mode.PVP, GameModel.Diff.EASY);
    }


    // ════════════════════════════════════════════════
    //  NHÓM 1 – Điều kiện kết thúc ván
    // ════════════════════════════════════════════════

    /**
     * TC01 – Trạng thái ban đầu phải là PLAYING.
     */
    @Test
    @DisplayName("TC01 – Trạng thái ban đầu là PLAYING")
    void TC01_initialStatus_isPlaying() {
        assertEquals(GameModel.Status.PLAYING, model.getStatus());
    }

    /**
     * TC02 – ĐỎ THẮNG khi đen hết quân trên bàn.
     * Xóa toàn bộ quân đen, đỏ thực hiện 1 nước đi bình thường
     * → checkWin phát hiện validMoves(false) rỗng → RED_WINS.
     */
    @Test
    @DisplayName("TC02 – Đỏ thắng khi đen hết quân")
    void TC02_redWins_whenBlackHasNoPieces() {
        clearBoard();
        // Đặt 2 quân đỏ có thể đi được (hàng 5, ô chẵn/lẻ theo luật checker)
        model.getBoard().set(5, 0, new Piece(5, 0, true));
        model.getBoard().set(5, 2, new Piece(5, 2, true));
        // Không có quân đen nào → validMoves(false) rỗng
        Move m = model.getBoard().validMoves(true).get(0);
        model.applyMove(m);
        assertEquals(GameModel.Status.RED_WINS, model.getStatus());
    }

    /**
     * TC03 – ĐEN THẮNG khi đỏ hết quân trên bàn.
     * Lượt đen đi trước bằng cách đỏ đi 1 nước rỗng trước,
     * sau đó xóa đỏ để đen thắng ngay lượt tiếp theo.
     */
    @Test
    @DisplayName("TC03 – Đen thắng khi đỏ hết quân")
    void TC03_blackWins_whenRedHasNoPieces() {
        clearBoard();
        // Đỏ đi 1 lượt trước để chuyển sang lượt đen
        model.getBoard().set(5, 0, new Piece(5, 0, true));
        model.getBoard().set(0, 1, new Piece(0, 1, false));
        model.getBoard().set(0, 3, new Piece(0, 3, false));
        Move redMove = model.getBoard().validMoves(true).get(0);
        model.applyMove(redMove);   // lượt đỏ xong → chuyển sang đen

        // Xóa nốt quân đỏ còn lại
        clearBoard();
        model.getBoard().set(0, 1, new Piece(0, 1, false));
        model.getBoard().set(0, 3, new Piece(0, 3, false));

        Move blackMove = model.getBoard().validMoves(false).get(0);
        model.applyMove(blackMove);
        assertEquals(GameModel.Status.BLACK_WINS, model.getStatus());
    }

    /**
     * TC04 – ĐỎ THẮNG bằng cách ăn quân đen cuối cùng.
     */
    @Test
    @DisplayName("TC04 – Đỏ thắng bằng nước ăn quân đen cuối cùng")
    void TC04_redWins_byCapturingLastBlackPiece() {
        clearBoard();
        // Đỏ (5,0) ăn đen (4,1) → đến (3,2); đen hết quân
        model.getBoard().set(5, 0, new Piece(5, 0, true));
        model.getBoard().set(4, 1, new Piece(4, 1, false));

        Move capture = model.getBoard().validMoves(true).stream()
                .filter(Move::isCapture)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm được nước ăn quân"));

        model.applyMove(capture);
        assertEquals(GameModel.Status.RED_WINS, model.getStatus());
    }

    /**
     * TC05 – Game vẫn PLAYING khi cả 2 bên còn quân và nước đi.
     */
    @Test
    @DisplayName("TC05 – Vẫn PLAYING khi 2 bên còn quân")
    void TC05_statusPlaying_whenBothSidesHaveMoves() {
        // Chỉ đi 1 nước bình thường, chưa ai thắng
        Move m = model.getBoard().validMoves(true).get(0);
        model.applyMove(m);
        assertEquals(GameModel.Status.PLAYING, model.getStatus());
    }


    // ════════════════════════════════════════════════
    //  NHÓM 2 – Thống kê ván đấu
    // ════════════════════════════════════════════════

    /**
     * TC06 – Mọi counter = 0 ngay sau newGame.
     */
    @Test
    @DisplayName("TC06 – Mọi counter = 0 sau newGame")
    void TC06_allStatsZero_afterNewGame() {
        assertEquals(0, model.getRedMoves(),      "redMoves phải = 0");
        assertEquals(0, model.getBlackMoves(),    "blackMoves phải = 0");
        assertEquals(0, model.getRedCaptures(),   "redCaptures phải = 0");
        assertEquals(0, model.getBlackCaptures(), "blackCaptures phải = 0");
    }

    /**
     * TC07 – redMoves tăng 1 sau 1 nước đi bình thường của đỏ.
     */
    @Test
    @DisplayName("TC07 – redMoves tăng sau nước đi của đỏ")
    void TC07_redMoves_incrementsAfterRedMove() {
        Move m = model.getBoard().validMoves(true).get(0);
        model.applyMove(m);
        assertEquals(1, model.getRedMoves());
        assertEquals(0, model.getBlackMoves());
    }

    /**
     * TC08 – blackMoves tăng 1 sau 1 nước đi của đen.
     */
    @Test
    @DisplayName("TC08 – blackMoves tăng sau nước đi của đen")
    void TC08_blackMoves_incrementsAfterBlackMove() {
        model.applyMove(model.getBoard().validMoves(true).get(0));   // đỏ đi
        model.applyMove(model.getBoard().validMoves(false).get(0));  // đen đi
        assertEquals(1, model.getRedMoves());
        assertEquals(1, model.getBlackMoves());
    }

    /**
     * TC09 – redCaptures tăng 1 khi đỏ ăn 1 quân đen.
     */
    @Test
    @DisplayName("TC09 – redCaptures = 1 khi đỏ ăn 1 quân")
    void TC09_redCaptures_singleCapture() {
        clearBoard();
        // Đỏ (5,0) ăn đen (4,1) → đến (3,2)
        model.getBoard().set(5, 0, new Piece(5, 0, true));
        model.getBoard().set(4, 1, new Piece(4, 1, false));
        // Thêm 1 quân đen để game chưa kết thúc ngay
        model.getBoard().set(0, 1, new Piece(0, 1, false));

        Move capture = model.getBoard().validMoves(true).stream()
                .filter(Move::isCapture)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm thấy nước ăn quân"));

        model.applyMove(capture);
        assertEquals(1, model.getRedCaptures());
        assertEquals(0, model.getBlackCaptures());
    }

    /**
     * TC10 – redCaptures = 2 khi đỏ ăn 2 quân liên tiếp (multi-jump).
     */
    @Test
    @DisplayName("TC10 – redCaptures = 2 khi đỏ multi-jump ăn 2 quân")
    void TC10_redCaptures_multiJump() {
        clearBoard();
        // Đỏ (6,0) → ăn đen (5,1) → (4,2) → ăn đen (3,3) → (2,4)
        model.getBoard().set(6, 0, new Piece(6, 0, true));
        model.getBoard().set(5, 1, new Piece(5, 1, false));
        model.getBoard().set(3, 3, new Piece(3, 3, false));
        // Thêm 1 quân đen ở xa để game không kết thúc ngay
        model.getBoard().set(0, 7, new Piece(0, 7, false));

        Move multiCapture = model.getBoard().validMoves(true).stream()
                .filter(m -> m.getCaptures().size() >= 2)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm thấy nước multi-jump"));

        model.applyMove(multiCapture);
        assertEquals(2, model.getRedCaptures());
    }

    /**
     * TC11 – blackCaptures tăng 1 khi đen ăn 1 quân đỏ.
     */
    @Test
    @DisplayName("TC11 – blackCaptures = 1 khi đen ăn 1 quân")
    void TC11_blackCaptures_singleCapture() {
        clearBoard();
        model.getBoard().set(7, 0, new Piece(7, 0, true));   // Đỏ A
        model.getBoard().set(3, 6, new Piece(3, 6, true));   // Đỏ B
        model.getBoard().set(2, 7, new Piece(2, 7, false));  // Đen C

        // Đỏ đi bình thường (lượt 1) — phải chọn đúng Đỏ A, không được di chuyển Đỏ B
        List<Move> redMoves = model.getBoard().validMoves(true);
        assertFalse(redMoves.isEmpty(), "Đỏ phải có nước đi");
        Move redMove = redMoves.stream()
                .filter(m -> m.getFromRow() == 7 && m.getFromCol() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Đỏ A không có nước đi"));
        assertFalse(redMove.isCapture(), "Đỏ không được có nước ăn");
        model.applyMove(redMove);

        // Đen ăn Đỏ B (lượt 2)
        Move blackCapture = model.getBoard().validMoves(false).stream()
                .filter(Move::isCapture)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm được nước ăn của đen"));
        model.applyMove(blackCapture);

        assertEquals(1, model.getBlackCaptures());
        assertEquals(0, model.getRedCaptures());
    }



    /**
     * TC12 – Undo sau 1 nước đỏ → redMoves trở về 0.
     */
    @Test
    @DisplayName("TC12 – Undo rollback redMoves về 0")
    void TC12_undo_rollbacksRedMoves() {
        Move m = model.getBoard().validMoves(true).get(0);
        model.applyMove(m);
        assertEquals(1, model.getRedMoves());

        model.undo();
        assertEquals(0, model.getRedMoves());
    }

    /**
     * TC13 – Undo sau nước ăn quân → redCaptures trở về 0.
     */
    @Test
    @DisplayName("TC13 – Undo rollback redCaptures về 0")
    void TC13_undo_rollbacksRedCaptures() {
        clearBoard();
        model.getBoard().set(5, 0, new Piece(5, 0, true));
        model.getBoard().set(4, 1, new Piece(4, 1, false));
        model.getBoard().set(0, 1, new Piece(0, 1, false));

        Move capture = model.getBoard().validMoves(true).stream()
                .filter(Move::isCapture).findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm thấy nước ăn"));
        model.applyMove(capture);
        assertEquals(1, model.getRedCaptures());

        model.undo();
        assertEquals(0, model.getRedCaptures());
    }

    /**
     * TC14 – canUndo() trả về false ngay sau newGame.
     */
    @Test
    @DisplayName("TC14 – canUndo = false sau newGame")
    void TC14_canUndo_falseAfterNewGame() {
        assertFalse(model.canUndo());
    }

    /**
     * TC15 – canUndo() trả về true sau 1 nước đi.
     */
    @Test
    @DisplayName("TC15 – canUndo = true sau 1 nước đi")
    void TC15_canUndo_trueAfterMove() {
        model.applyMove(model.getBoard().validMoves(true).get(0));
        assertTrue(model.canUndo());
    }


    // ════════════════════════════════════════════════
    //  NHÓM 3 – Định dạng thời gian
    // ════════════════════════════════════════════════

    /**
     * TC16 – getElapsedTime() phải có định dạng mm:ss.
     */
    @Test
    @DisplayName("TC16 – getElapsedTime() đúng định dạng mm:ss")
    void TC16_elapsedTime_formatIsCorrect() {
        String time = model.getElapsedTime();
        assertNotNull(time);
        assertTrue(time.matches("\\d{2}:\\d{2}"),
                "Thời gian phải theo dạng mm:ss, thực tế: " + time);
    }

    /**
     * TC17 – getElapsedTime() trả về "00:00" ngay sau newGame.
     */
    @Test
    @DisplayName("TC17 – getElapsedTime() = 00:00 ngay sau newGame")
    void TC17_elapsedTime_isZero_rightAfterNewGame() {
        assertEquals("00:00", model.getElapsedTime());
    }

    /**
     * TC18 – getElapsedTime() sau 1 giây phải là "00:01".
     */
    @Test
    @DisplayName("TC18 – getElapsedTime() = 00:01 sau ~1 giây")
    void TC18_elapsedTime_afterOneSecond() throws InterruptedException {
        Thread.sleep(1100); // chờ hơn 1 giây
        assertEquals("00:01", model.getElapsedTime());
    }

    /**
     * TC19 – newGame reset lại bộ đếm thời gian.
     */
    @Test
    @DisplayName("TC19 – newGame reset thời gian về 00:00")
    void TC19_newGame_resetsTimer() throws InterruptedException {
        Thread.sleep(1100);
        // Bắt đầu ván mới → thời gian phải về 0
        model.newGame(GameModel.Mode.PVP, GameModel.Diff.EASY);
        assertEquals("00:00", model.getElapsedTime());
    }
}
