package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// MSSV: 23130141 - Họ tên: Nguyễn Tuấn Kha
// DEVELOPMENT TESTING: UNIT TESTS CHO LOGIC UC04
public class GameControllerUC04Test {
    private GameModel model;

    @BeforeEach
    public void setUp() {
        // Khởi tạo một ván game mới trước mỗi ca kiểm thử
        model = new GameModel();
        model.newGame(GameModel.Mode.PVP, GameModel.Diff.MEDIUM);
    }

    @Test
    public void testUT_UC04_01_DeselectPiece() {
        // Giả lập chọn một quân cờ Đỏ hợp lệ ở hàng 5, cột 0
        boolean firstSelect = model.select(5, 0);
        assertTrue(firstSelect, "Quân cờ Đỏ hợp lệ phải được chọn thành công");
        assertNotNull(model.getSelected(), "Model phải ghi nhận có quân cờ đang được chọn");

        // Giả lập hành vi hủy chọn bằng cách xóa trạng thái selection
        model.clearSelection();
        assertNull(model.getSelected(), "Sau khi hủy chọn, biến selectedPiece trong Model bắt buộc phải trả về null");
    }

    @Test
    public void testUT_UC04_02_SelectEnemyPieceShouldFail() {
        // Đang là lượt Đỏ, cố tình chọn một quân cờ Đen của đối thủ
        boolean selectEnemy = model.select(2, 1);

        assertFalse(selectEnemy, "Hệ thống không được phép cho chọn quân cờ của đối phương");
    }

    @Test
    public void testUT_UC04_03_ViolationOfCaptureRule() {
        List<Move> allMoves = model.getBoard().validMoves(model.isRedTurn());
        assertNotNull(allMoves, "Danh sách nước đi trả về không được null để tránh lỗi hệ thống");

        boolean hasCapture = allMoves.stream().anyMatch(Move::isCapture);
        if (hasCapture) {
            System.out.println("Bàn cờ có nước ăn bắt buộc -> Logic ép ăn quân được kích hoạt.");
        }
    }
}