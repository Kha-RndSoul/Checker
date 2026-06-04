package Controller;

import Model.*;
import View.MainFrame;
import javax.swing.*;
import java.awt.*;

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

        boolean redWins = (st == GameModel.Status.RED_WINS);
        String winner = redWins ? "ĐỎ" : "ĐEN";
        Color winColor = redWins ? new Color(200, 40, 40) : new Color(40, 40, 40);
        String emoji = redWins ? "🔴" : "⚫";

        Timer t = new Timer(300, e -> {
            // ── Panel chính ──
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(new Color(245, 240, 230));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(winColor, 3, true),
                    BorderFactory.createEmptyBorder(20, 30, 20, 30)
            ));

            // Tiêu đề người thắng
            JLabel titleLbl = new JLabel(emoji + "  " + winner + " THẮNG!  " + emoji, SwingConstants.CENTER);
            titleLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
            titleLbl.setForeground(winColor);
            titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(titleLbl);
            panel.add(Box.createVerticalStrut(16));

            // Đường kẻ ngang
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(180, 160, 120));
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            panel.add(sep);
            panel.add(Box.createVerticalStrut(12));

            // Thống kê
            JLabel statsTitle = new JLabel("📊  THỐNG KÊ VÁN ĐẤU", SwingConstants.CENTER);
            statsTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
            statsTitle.setForeground(new Color(80, 60, 20));
            statsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(statsTitle);
            panel.add(Box.createVerticalStrut(10));

            // Bảng thống kê dạng lưới
            JPanel statsGrid = new JPanel(new GridLayout(3, 2, 8, 6));
            statsGrid.setBackground(new Color(245, 240, 230));
            statsGrid.setAlignmentX(Component.CENTER_ALIGNMENT);

            addStatRow(statsGrid, "⏱  Thời gian",   model.getElapsedTime(),
                    "", "");
            addStatRow(statsGrid, "🔴 Đỏ — Nước đi: " + model.getRedMoves(),
                    "Quân ăn: " + model.getRedCaptures(), "", "");
            addStatRow(statsGrid, "⚫ Đen — Nước đi: " + model.getBlackMoves(),
                    "Quân ăn: " + model.getBlackCaptures(), "", "");

            panel.add(statsGrid);
            panel.add(Box.createVerticalStrut(20));

            // Nút bấm
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            btnPanel.setBackground(new Color(245, 240, 230));

            JButton playAgainBtn = makeButton("▶  Chơi lại", new Color(50, 130, 50));
            JButton menuBtnDialog = makeButton("◀  Menu",    new Color(80, 80, 160));

            btnPanel.add(playAgainBtn);
            btnPanel.add(menuBtnDialog);
            panel.add(btnPanel);

            // Tạo dialog
            JDialog dialog = new JDialog(frame, "Kết thúc ván", true);
            dialog.setUndecorated(false);
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setResizable(false);

            playAgainBtn.addActionListener(ae -> { dialog.dispose(); startGame(lastMode, lastDiff); });
            menuBtnDialog.addActionListener(ae -> { dialog.dispose(); frame.showMenu(); });

            dialog.setVisible(true);
        });
        t.setRepeats(false);
        t.start();
    }

    // Helper: tạo 1 hàng thống kê (label trái + value phải)
    private void addStatRow(JPanel grid, String left, String right,
                            String unused1, String unused2) {
        JLabel l = new JLabel(left);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(60, 50, 30));

        JLabel r = new JLabel(right, SwingConstants.RIGHT);
        r.setFont(new Font("SansSerif", Font.BOLD, 13));
        r.setForeground(new Color(60, 50, 30));

        grid.add(l);
        grid.add(r);
    }

    // Helper: tạo nút bấm có màu tùy chỉnh
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
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