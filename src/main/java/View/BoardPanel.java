package View;

import Model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * BoardPanel – Vẽ bàn cờ, quân cờ, highlight ô chọn và gợi ý nước đi.
 *
 * UC12 – liên quan:
 *   refresh(model) từ MainFrame gọi setModel() → repaint() sau mỗi nước đi (UC12-Bước 1).
 *   startAnimation() thực thi callback onComplete sau khi hoạt ảnh kết thúc;
 *   callback đó gọi checkGameOver() trong GameController → kích hoạt UC12-Bước 2 trở đi.
 */
public class BoardPanel extends JPanel {
    public interface ClickListener { void onClick(int row, int col); }

    private static final int CELL = 70;
    private static final Color LIGHT   = new Color(240, 217, 181);
    private static final Color DARK    = new Color(181, 136, 99);
    private static final Color C_RED   = new Color(200, 50, 50);
    private static final Color C_BLACK = new Color(35, 35, 35);
    private static final Color HINT    = new Color(50, 205, 50, 170);
    private static final Color SEL     = new Color(255, 255, 0, 130);

    // Khoảng trống viền bao quanh để hiển thị ký hiệu tọa độ đại số (a-h, 1-8)
    private static final int MARGIN = 24;

    private GameModel     model;
    private ClickListener listener;

    // Trạng thái hoạt ảnh trượt quân
    private Timer   animTimer;
    private Move    animMove;
    private boolean animPieceIsRed;
    private boolean animPieceIsKing;
    private double  animProgress = 0.0;

    public BoardPanel() {
        setPreferredSize(new Dimension(CELL * 8 + 2 * MARGIN, CELL * 8 + 2 * MARGIN));
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (listener != null) {
                    // Trừ MARGIN trước khi quy đổi tọa độ pixel → ô cờ
                    int col = (e.getX() - MARGIN) / CELL;
                    int row = (e.getY() - MARGIN) / CELL;
                    // Đảm bảo click nằm trong phạm vi bàn cờ
                    if (col >= 0 && col < 8 && row >= 0 && row < 8) {
                        listener.onClick(row, col);
                    }
                }
            }
        });
    }

    /**
     * UC12-Bước 1: GC → MF : refresh(model) → setModel()
     * Cập nhật model và vẽ lại bàn cờ sau mỗi nước đi hợp lệ.
     */
    public void setModel(GameModel m) { this.model = m; repaint(); }

    public void setClickListener(ClickListener l) { this.listener = l; }

    /**
     * Thực thi hoạt ảnh trượt quân cờ từ vị trí xuất phát đến đích.
     * Sau khi hoạt ảnh hoàn tất, callback onComplete được gọi –
     * đó là nơi GameController gọi checkGameOver() để kích hoạt UC12-Bước 2.
     *
     * @param move       nước đi cần hiệu ứng hóa
     * @param isRed      màu quân đang di chuyển
     * @param isKing     quân có phải vương không
     * @param onComplete callback thực thi logic model sau khi hoạt ảnh xong
     */
    public void startAnimation(Move move, boolean isRed, boolean isKing, Runnable onComplete) {
        this.animMove        = move;
        this.animPieceIsRed  = isRed;
        this.animPieceIsKing = isKing;
        this.animProgress    = 0.0;

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();

        // Timer 15ms tạo hoạt ảnh chuyển động mịn (~67fps)
        animTimer = new Timer(15, e -> {
            animProgress += 0.08;
            if (animProgress >= 1.0) {
                animTimer.stop();
                animMove = null;
                if (onComplete != null) onComplete.run(); // kích hoạt checkGameOver() → UC12
            }
            repaint();
        });
        animTimer.start();
    }

    /** Trả về true nếu hoạt ảnh đang chạy (dùng để chặn undo trong lúc bay quân). */
    public boolean isAnimating() { return animMove != null; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (model == null || model.getBoard() == null) return;

        Board board = model.getBoard();

        // Vẽ nền gỗ nhạt làm khung viền bao quanh bàn cờ
        g2.setColor(new Color(232, 215, 190));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 1. Vẽ các ô vuông sáng/tối (dịch vào MARGIN)
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                g2.setColor((r + c) % 2 == 0 ? LIGHT : DARK);
                g2.fillRect(MARGIN + c * CELL, MARGIN + r * CELL, CELL, CELL);
            }

        // 2. Highlight ô đang được chọn (màu vàng); ẩn khi hoạt ảnh đang chạy
        Piece sel = model.getSelected();
        if (sel != null && !isAnimating()) {
            g2.setColor(SEL);
            g2.fillRect(MARGIN + sel.getCol() * CELL, MARGIN + sel.getRow() * CELL, CELL, CELL);
        }

        // 3. Vẽ gợi ý nước đi (chấm xanh); ẩn khi hoạt ảnh đang chạy
        if (!isAnimating()) {
            List<Move> hints = model.getSelMoves();
            if (hints != null)
                for (Move m : hints) {
                    g2.setColor(HINT);
                    int hc = m.getToCol(), hr = m.getToRow();
                    g2.fillOval(MARGIN + hc * CELL + CELL / 2 - 11,
                                MARGIN + hr * CELL + CELL / 2 - 11, 22, 22);
                }
        }

        // 4. Vẽ quân cờ tĩnh trên bàn
        int pad = 7;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board.get(r, c);
                if (p == null) continue;
                // Ẩn quân tại vị trí xuất phát khi đang bay (tránh render đôi)
                if (animMove != null && r == animMove.getFromRow() && c == animMove.getFromCol())
                    continue;
                // Ẩn quân bị ăn khi hoạt ảnh qua khỏi nửa đường
                if (animMove != null && animMove.isCapture() && animProgress > 0.5) {
                    if (animMove.getCaptures() != null) {
                        boolean isCaptured = false;
                        for (int[] cap : animMove.getCaptures())
                            if (r == cap[0] && c == cap[1]) { isCaptured = true; break; }
                        if (isCaptured) continue;
                    }
                }
                int x = MARGIN + c * CELL + pad, y = MARGIN + r * CELL + pad, sz = CELL - 2 * pad;
                drawSinglePiece(g2, p.isRed(), p.isKing(), x, y, sz);
            }

        // 5. Vẽ quân cờ đang trượt theo tiến trình hoạt ảnh (interpolation tuyến tính)
        if (animMove != null) {
            double cx = MARGIN + animMove.getFromCol() * CELL
                    + (animMove.getToCol() - animMove.getFromCol()) * CELL * animProgress;
            double cy = MARGIN + animMove.getFromRow() * CELL
                    + (animMove.getToRow() - animMove.getFromRow()) * CELL * animProgress;
            drawSinglePiece(g2, animPieceIsRed, animPieceIsKing,
                    (int) cx + pad, (int) cy + pad, CELL - 2 * pad);
        }

        // 6. Vẽ ký hiệu tọa độ đại số (cột a-h, hàng 1-8) quanh viền bàn cờ
        g2.setColor(new Color(90, 65, 40));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < 8; i++) {
            // Ký tự cột (a-h) phía dưới
            String colStr = String.valueOf((char) ('a' + i));
            int colW = fm.stringWidth(colStr), colH = fm.getAscent();
            int ccx  = MARGIN + i * CELL + CELL / 2;
            g2.drawString(colStr, ccx - colW / 2, MARGIN + CELL * 8 + MARGIN / 2 + colH / 2);

            // Ký tự hàng (1-8) phía trái
            String rowStr = String.valueOf(8 - i);
            int rowW = fm.stringWidth(rowStr), rowH = fm.getAscent();
            int ccy  = MARGIN + i * CELL + CELL / 2;
            g2.drawString(rowStr, MARGIN / 2 - rowW / 2, ccy + rowH / 2 - 2);
        }
    }

    /** Vẽ một quân cờ với hiệu ứng bóng, highlight sáng, viền và ký hiệu vương (K). */
    private void drawSinglePiece(Graphics2D g2, boolean isRed, boolean isKing, int x, int y, int sz) {
        // Bóng đổ
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(x + 3, y + 3, sz, sz);
        // Thân quân
        g2.setColor(isRed ? C_RED : C_BLACK);
        g2.fillOval(x, y, sz, sz);
        // Điểm highlight sáng
        g2.setColor(isRed ? new Color(240, 100, 100) : new Color(80, 80, 80));
        g2.fillOval(x + sz / 4, y + sz / 4, sz / 3, sz / 3);
        // Viền quân
        g2.setColor(new Color(0, 0, 0, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x, y, sz, sz);
        g2.setStroke(new BasicStroke(1f));
        // Ký hiệu vương
        if (isKing) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("Serif", Font.BOLD, 18));
            g2.drawString("K", x + sz / 2 - 6, y + sz / 2 + 7);
        }
    }
}
