package Controller;

import Model.*;
import View.MainFrame;
import javax.swing.*;
import java.awt.*;

/**
 * UC12 – THÔNG BÁO KẾT QUẢ
 * Luồng chính: Bước 1 (nhận nước đi) → Bước 2 (kiểm tra kết thúc)
 *            → Bước 3 (hiển thị kết quả) → Bước 4 (người chơi chọn hành động)
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
        frame.getUndoBtn().addActionListener(e -> handleUndo());
        frame.showMenu();
        frame.setVisible(true);
    }

    // UC12 - Bước 4: khởi tạo ván mới khi người chơi chọn "Chơi lại"
    private void startGame(GameModel.Mode mode, GameModel.Diff diff) {
        lastMode = mode;
        lastDiff = diff;
        model.newGame(mode, diff);
        frame.refresh(model);
        frame.showGame();
        tryAutoSelectUniquePiece();
    }

    // UC12 - Bước 1: nhận nước đi từ người chơi, cập nhật bàn cờ và kiểm tra kết thúc ván
    private void handleClick(int row, int col) {
        if (model.getStatus() != GameModel.Status.PLAYING) return;
        if (model.getMode() == GameModel.Mode.PV_AI && !model.isRedTurn()) return;
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return;

        Model.Piece clickedPiece  = model.getBoard().get(row, col);
        Model.Piece selectedPiece = model.getSelected();

        // Hủy chọn khi click lại chính quân đang chọn
        if (selectedPiece != null && selectedPiece.getRow() == row && selectedPiece.getCol() == col) {
            model.clearSelection();
            frame.refresh(model);
            return;
        }

        if (selectedPiece != null) {
            boolean moved = model.moveTo(row, col); // UC12 - Bước 1: thực hiện nước đi của người chơi
            frame.refresh(model);                   // UC12 - Bước 1: cập nhật giao diện sau nước đi
            if (moved) {
                checkGameOver();                    // UC12 - Bước 1: kiểm tra ván có kết thúc không
                scheduleAI();
                tryAutoSelectUniquePiece();
                return;
            }
        }

        boolean success = model.select(row, col);

        // Thông báo lỗi khi chọn quân không hợp lệ
        if (!success) {
            if (clickedPiece != null) {
                if (clickedPiece.isRed() != model.isRedTurn()) {
                    JOptionPane.showMessageDialog(frame,
                            "Không thể lựa chọn quân cờ này! Đây là quân cờ của đối thủ.",
                            "Chọn sai quân cờ", JOptionPane.ERROR_MESSAGE);
                } else {
                    java.util.List<Model.Move> allMoves = model.getBoard().validMoves(model.isRedTurn());
                    boolean hasCapture = allMoves.stream().anyMatch(Model.Move::isCapture);
                    if (hasCapture) {
                        JOptionPane.showMessageDialog(frame,
                                "Bạn không thể chọn quân này! Bắt buộc phải thực hiện nước ăn quân.",
                                "Chọn quân sai luật", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }

        frame.refresh(model);
    }

    // UC12 - Bước 1: nhận nước đi từ AI (kèm hoạt ảnh), cập nhật bàn cờ và kiểm tra kết thúc ván
    private void executeMoveWithAnimation(Move m) {
        Piece activePiece = model.getBoard().get(m.getFromRow(), m.getFromCol());
        if (activePiece == null) return;

        boolean isRed  = activePiece.isRed();
        boolean isKing = activePiece.isKing();

        model.clearSelection();
        frame.refresh(model);

        frame.getBoard().startAnimation(m, isRed, isKing, () -> {
            model.applyMove(m);   // UC12 - Bước 1: thực hiện nước đi của AI
            frame.refresh(model); // UC12 - Bước 1: cập nhật giao diện sau nước đi
            checkGameOver();      // UC12 - Bước 1: kiểm tra ván có kết thúc không
            scheduleAI();
            tryAutoSelectUniquePiece();
        });
    }

    private void handleUndo() {
        if (frame.getBoard().isAnimating()) return;
        if (model.undo()) {
            if (model.getMode() == GameModel.Mode.PV_AI && !model.isRedTurn()) {
                model.undo();
            }
            frame.refresh(model);
            tryAutoSelectUniquePiece();
        }
    }

    private void scheduleAI() {
        if (model.getMode() != GameModel.Mode.PV_AI) return;
        if (model.isRedTurn() || model.getStatus() != GameModel.Status.PLAYING) return;
        Timer t = new Timer(400, e -> {
            Move aiMove = model.getAIMove();
            if (aiMove != null) executeMoveWithAnimation(aiMove);
        });
        t.setRepeats(false);
        t.start();
    }

    // UC12 - Bước 2, 3, 4: kiểm tra trạng thái ván, hiển thị kết quả và xử lý lựa chọn của người chơi
    private void checkGameOver() {

        // UC12 - Bước 2: lấy trạng thái ván từ model
        GameModel.Status st = model.getStatus();
        if (st == GameModel.Status.PLAYING) return; // ván chưa kết thúc → thoát

        // UC12 - Bước 2: xác định người thắng và thu thập thống kê ván đấu
        boolean redWins  = (st == GameModel.Status.RED_WINS);
        String  winner   = redWins ? "ĐỎ" : "ĐEN";
        Color   winColor = redWins ? new Color(200, 40, 40) : new Color(40, 40, 40);
        String  emoji    = redWins ? "🔴" : "⚫";
        String  reason   = model.getEndReasonText();

        // UC12 - Bước 3: trì hoãn 300ms rồi hiển thị hộp thoại kết quả
        Timer t = new Timer(300, e -> {

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(new Color(245, 240, 230));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(winColor, 3, true),
                    BorderFactory.createEmptyBorder(20, 30, 20, 30)));

            // UC12 - Bước 3: hiển thị tiêu đề thắng cuộc
            JLabel titleLbl = new JLabel(emoji + "  " + winner + " THẮNG!  " + emoji, SwingConstants.CENTER);
            titleLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
            titleLbl.setForeground(winColor);
            titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(titleLbl);
            panel.add(Box.createVerticalStrut(16));

            // UC12 - Bước 3: hiển thị lý do kết thúc ván
            if (reason != null && !reason.isEmpty()) {
                JLabel reasonLbl = new JLabel("Lý do: " + reason, SwingConstants.CENTER);
                reasonLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
                reasonLbl.setForeground(new Color(80, 60, 20));
                reasonLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(reasonLbl);
                panel.add(Box.createVerticalStrut(12));
            }

            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(180, 160, 120));
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            panel.add(sep);
            panel.add(Box.createVerticalStrut(12));

            // UC12 - Bước 3: hiển thị bảng thống kê ván đấu (thời gian, số nước đi, số quân ăn)
            JLabel statsTitle = new JLabel("📊  THỐNG KÊ VÁN ĐẤU", SwingConstants.CENTER);
            statsTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
            statsTitle.setForeground(new Color(80, 60, 20));
            statsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(statsTitle);
            panel.add(Box.createVerticalStrut(10));

            JPanel statsGrid = new JPanel(new GridLayout(3, 2, 8, 6));
            statsGrid.setBackground(new Color(245, 240, 230));
            statsGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
            addStatRow(statsGrid, "⏱  Thời gian",              model.getElapsedTime(), "", "");
            addStatRow(statsGrid, "🔴 Đỏ — Nước đi: "  + model.getRedMoves(),   "Quân ăn: " + model.getRedCaptures(),   "", "");
            addStatRow(statsGrid, "⚫ Đen — Nước đi: " + model.getBlackMoves(), "Quân ăn: " + model.getBlackCaptures(), "", "");
            panel.add(statsGrid);
            panel.add(Box.createVerticalStrut(20));

            // UC12 - Bước 3: hiển thị nút [Chơi lại] và [Menu]
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            btnPanel.setBackground(new Color(245, 240, 230));
            JButton playAgainBtn  = makeButton("▶  Chơi lại", new Color(50, 130, 50));
            JButton menuBtnDialog = makeButton("◀  Menu",     new Color(80, 80, 160));
            btnPanel.add(playAgainBtn);
            btnPanel.add(menuBtnDialog);
            panel.add(btnPanel);

            JDialog dialog = new JDialog(frame, "Kết thúc ván", true);
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setResizable(false);

            // UC12 - Bước 4: người chơi chọn "Chơi lại" → khởi tạo ván mới
            playAgainBtn.addActionListener(ae -> {
                dialog.dispose();
                startGame(lastMode, lastDiff);
            });

            // UC12 - Bước 4: người chơi chọn "Menu" → trở về màn hình menu
            menuBtnDialog.addActionListener(ae -> {
                dialog.dispose();
                frame.showMenu();
            });

            dialog.setVisible(true);
        });
        t.setRepeats(false);
        t.start();
    }

    private void addStatRow(JPanel grid, String left, String right, String unused1, String unused2) {
        JLabel l = new JLabel(left);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(60, 50, 30));
        JLabel r = new JLabel(right, SwingConstants.RIGHT);
        r.setFont(new Font("SansSerif", Font.BOLD, 13));
        r.setForeground(new Color(60, 50, 30));
        grid.add(l);
        grid.add(r);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        return btn;
    }

    // Tự động chọn quân nếu lượt đó chỉ có duy nhất một quân có thể đi
    private void tryAutoSelectUniquePiece() {
        if (model.getStatus() != GameModel.Status.PLAYING) return;
        if (model.getMode() == GameModel.Mode.PV_AI && !model.isRedTurn()) return;

        java.util.List<Model.Move> validMoves = model.getBoard().validMoves(model.isRedTurn());
        if (validMoves.isEmpty()) return;

        int firstRow = validMoves.get(0).getFromRow();
        int firstCol = validMoves.get(0).getFromCol();

        boolean isUniquePiece = validMoves.stream()
                .allMatch(m -> m.getFromRow() == firstRow && m.getFromCol() == firstCol);

        if (isUniquePiece) {
            model.select(firstRow, firstCol);
            frame.refresh(model);
        }
    }
}
