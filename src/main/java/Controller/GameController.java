package Controller;

import Model.*;
import View.MainFrame;
import javax.swing.*;

/**
 * CONTROLLER — GameController
 * Kết nối Model và View, xử lý toàn bộ tương tác người dùng.
 */
public class GameController {
    private final GameModel model = new GameModel();
    private MainFrame frame;
    private GameModel.Mode lastMode;
    private GameModel.Diff lastDiff;

    public void start() {
        frame = new MainFrame();
        frame.getMenu().setOnStart(this::startGame);
        frame.getBoard().setClickListener(this::handleClick);
        frame.getMenuBtn().addActionListener(e -> frame.showMenu());
        frame.showMenu();
        frame.setVisible(true);
    }

    private void startGame(GameModel.Mode mode, GameModel.Diff diff) {
        lastMode = mode; lastDiff = diff;
        model.newGame(mode, diff);
        frame.refresh(model);
        frame.showGame();
    }

    private void handleClick(int row, int col) {
        if (model.getStatus() != GameModel.Status.PLAYING) return;
        if (model.getMode() == GameModel.Mode.PV_AI && !model.isRedTurn()) return;

        if (model.getSelected() != null) {
            boolean moved = model.moveTo(row, col);
            frame.refresh(model);
            if (moved) { checkGameOver(); scheduleAI(); return; }
        }

        // Lấy thông tin quân cờ tại ô vừa click trước khi trạng thái selection bị thay đổi/xóa
        Model.Piece clickedPiece = model.getBoard().get(row, col);

        boolean success = model.select(row, col);

        // PHẦN PHÁT TRIỂN TIẾP - MSSV: 23130141 - Họ tên: Nguyễn Tuấn Kha
        // Nâng cấp UC04: Xử lý các luồng ngoại lệ khi chọn quân cờ không thành công
        if (!success) {
            if (clickedPiece != null) {
                //Click trúng quân cờ của đối thủ
                if (clickedPiece.isRed() != model.isRedTurn()) {
                    JOptionPane.showMessageDialog(frame,
                            "Không thể lựa chọn quân cờ này! Đây là quân cờ của đối thủ.",
                            "Chọn sai quân cờ",
                            JOptionPane.ERROR_MESSAGE);
                }
                //Click vào quân không phải quân có nước bắt buôc ăn
                else {
                    java.util.List<Model.Move> allMoves = model.getBoard().validMoves(model.isRedTurn());
                    boolean hasCapture = allMoves.stream().anyMatch(Model.Move::isCapture);

                    if (hasCapture) {
                        // Hiển thị hộp thoại cảnh báo người chơi
                        JOptionPane.showMessageDialog(frame,
                                "Bạn không thể chọn quân này! Bắt buộc phải thực hiện nước ăn quân.",
                                "Chọn quân sai luật",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }

        frame.refresh(model);
    }

    private void scheduleAI() {
        if (model.getMode() != GameModel.Mode.PV_AI) return;
        if (model.isRedTurn() || model.getStatus() != GameModel.Status.PLAYING) return;
        Timer t = new Timer(500, e -> {
            Move m = model.getAIMove();
            if (m != null) model.applyMove(m);
            frame.refresh(model);
            checkGameOver();
        });
        t.setRepeats(false); t.start();
    }

    private void checkGameOver() {
        GameModel.Status st = model.getStatus();
        if (st == GameModel.Status.PLAYING) return;
        String winner = (st == GameModel.Status.RED_WINS) ? "ĐỎ 🔴" : "ĐEN ⚫";
        String stats = buildStatsMessage();
        Timer t = new Timer(300, e -> {
            int opt = JOptionPane.showOptionDialog(frame,
                    winner + " thắng!\n\n" + stats + "\nBạn muốn chơi tiếp?",
                    "Kết thúc ván", JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, null,
                    new String[]{"Chơi lại", "Menu"}, "Chơi lại");
            if (opt == 0) startGame(lastMode, lastDiff);
            else frame.showMenu();
        });
        t.setRepeats(false); t.start();
    }
    private String buildStatsMessage() {
        return "─────────────────────────────\n"
                + "  📊 THỐNG KÊ VÁN ĐẤU\n"
                + "─────────────────────────────\n"
                + String.format("  ⏱  Thời gian    : %s%n", model.getElapsedTime())
                + String.format("  🔴 Đỏ  — Nước đi: %d  |  Quân ăn: %d%n",
                model.getRedMoves(), model.getRedCaptures())
                + String.format("  ⚫ Đen — Nước đi: %d  |  Quân ăn: %d%n",
                model.getBlackMoves(), model.getBlackCaptures())
                + "─────────────────────────────\n";
    }
}