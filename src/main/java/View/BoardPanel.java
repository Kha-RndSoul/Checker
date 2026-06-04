package View;

import Model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/** Vẽ bàn cờ, quân cờ, highlight ô chọn và gợi ý nước đi. */
public class BoardPanel extends JPanel {
    public interface ClickListener { void onClick(int row, int col); }

    private static final int CELL = 70;
    private static final Color LIGHT   = new Color(240, 217, 181);
    private static final Color DARK    = new Color(181, 136, 99);
    private static final Color C_RED   = new Color(200, 50, 50);
    private static final Color C_BLACK = new Color(35, 35, 35);
    private static final Color HINT    = new Color(50, 205, 50, 170);
    private static final Color SEL     = new Color(255, 255, 0, 130);

    private GameModel model;
    private ClickListener listener;

    // ── PHẦN PHÁT TRIỂN - MSSV: [Điền MSSV] - Họ tên: Dũng ─────────────────
    private Timer animTimer;
    private Move animMove;
    private boolean animPieceIsRed;
    private boolean animPieceIsKing;
    private double animProgress = 0.0;
    // ───────────────────────────────────────────────────────────────────────

    public BoardPanel() {
        setPreferredSize(new Dimension(CELL*8, CELL*8));
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (listener != null)
                    listener.onClick(e.getY()/CELL, e.getX()/CELL);
            }
        });
    }

    public void setModel(GameModel m) { this.model = m; repaint(); }
    public void setClickListener(ClickListener l) { this.listener = l; }

    // ── PHẦN PHÁT TRIỂN - MSSV: [Điền MSSV] - Họ tên: Dũng ─────────────────
    public void startAnimation(Move move, boolean isRed, boolean isKing, Runnable onComplete) {
        this.animMove = move;
        this.animPieceIsRed = isRed;
        this.animPieceIsKing = isKing;
        this.animProgress = 0.0;

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();

        // Kích hoạt bộ hẹn giờ chu kỳ lặp siêu tốc 15ms tạo hiệu ứng chuyển động mịn
        animTimer = new Timer(15, e -> {
            animProgress += 0.08;
            if (animProgress >= 1.0) {
                animTimer.stop();
                animMove = null;
                if (onComplete != null) onComplete.run(); // Thực thi tiếp logic model khi kết thúc
            }
            repaint();
        });
        animTimer.start();
    }

    public boolean isAnimating() { return animMove != null; }
    // ───────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (model == null || model.getBoard() == null) return;

        Board board = model.getBoard();

        // 1. Vẽ ô vuông
        for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
            g2.setColor((r+c)%2==0 ? LIGHT : DARK);
            g2.fillRect(c*CELL, r*CELL, CELL, CELL);
        }

        // 2. Highlight ô đang chọn
        // ── SỬA ĐỔI BỞI DŨNG: Ẩn ô vàng khi đang bay quân để tránh lỗi hình ảnh render ──
        Piece sel = model.getSelected();
        if (sel != null && !isAnimating()) {
            g2.setColor(SEL);
            g2.fillRect(sel.getCol()*CELL, sel.getRow()*CELL, CELL, CELL);
        }

        // 3. Gợi ý nước đi
        // ── SỬA ĐỔI BỞI DŨNG: Ẩn các gợi ý nước đi màu xanh khi đang chạy hoạt ảnh ──
        if (!isAnimating()) {
            List<Move> hints = model.getSelMoves();
            if (hints != null) for (Move m : hints) {
                g2.setColor(HINT);
                int hc=m.getToCol(), hr=m.getToRow();
                g2.fillOval(hc*CELL+CELL/2-11, hr*CELL+CELL/2-11, 22, 22);
            }
        }

        // 4. Vẽ quân cờ cố định tĩnh trên bàn
        int pad = 7;
        for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
            Piece p = board.get(r, c);
            if (p == null) continue;

            // ── SỬA ĐỔI BỞI DŨNG: Ẩn quân cờ cũ tại vị trí xuất phát khi nó đang thực hiện chuyển động bay ──
            if (animMove != null && r == animMove.getFromRow() && c == animMove.getFromCol()) {
                continue;
            }

            // ── SỬA ĐỔI BỞI DŨNG: Ẩn quân cờ bị ăn ở giữa chặng đường chuyển động bay qua ──
            if (animMove != null && animMove.isCapture() && animProgress > 0.5) {
                if (animMove.getCaptures() != null) {
                    boolean isCapturedTile = false;
                    for (int[] cap : animMove.getCaptures()) {
                        if (r == cap[0] && c == cap[1]) { isCapturedTile = true; break; }
                    }
                    if (isCapturedTile) continue;
                }
            }

            int x = c * CELL + pad, y = r * CELL + pad, sz = CELL - 2 * pad;
            drawSinglePiece(g2, p.isRed(), p.isKing(), x, y, sz);
        }

        // 5. Vẽ quân cờ ảo tịnh tiến trượt trên màn hình dựa vào tiến trình progress
        // ── PHẦN PHÁT TRIỂN - MSSV: [Điền MSSV] - Họ tên: Dũng ─────────────────
        if (animMove != null) {
            double currentX = animMove.getFromCol() * CELL + (animMove.getToCol() - animMove.getFromCol()) * CELL * animProgress;
            double currentY = animMove.getFromRow() * CELL + (animMove.getToRow() - animMove.getFromRow()) * CELL * animProgress;
            drawSinglePiece(g2, animPieceIsRed, animPieceIsKing, (int)currentX + pad, (int)currentY + pad, CELL - 2 * pad);
        }
        // ───────────────────────────────────────────────────────────────────────
    }

    // ── PHẦN PHÁT TRIỂN - MSSV: [Điền MSSV] - Họ tên: Dũng ─────────────────
    // Hàm rút gọn helper tránh trùng lặp code giao diện khi phải vẽ quân cờ động và tĩnh
    private void drawSinglePiece(Graphics2D g2, boolean isRed, boolean isKing, int x, int y, int sz) {
        // bóng
        g2.setColor(new Color(0,0,0,50));
        g2.fillOval(x+3, y+3, sz, sz);
        // thân
        g2.setColor(isRed ? C_RED : C_BLACK);
        g2.fillOval(x, y, sz, sz);
        // highlight sáng
        g2.setColor(isRed ? new Color(240,100,100) : new Color(80,80,80));
        g2.fillOval(x+sz/4, y+sz/4, sz/3, sz/3);
        // viền
        g2.setColor(new Color(0,0,0,100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x, y, sz, sz);
        g2.setStroke(new BasicStroke(1f));
        // vương
        if (isKing) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("Serif", Font.BOLD, 18));
            g2.drawString("K", x+sz/2-6, y+sz/2+7);
        }
    }
    // ───────────────────────────────────────────────────────────────────────
}