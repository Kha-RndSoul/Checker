package View;

import Model.GameModel;
import javax.swing.*;
import java.awt.*;

/** Cửa sổ chính, dùng CardLayout để chuyển giữa menu và game. */
public class MainFrame extends JFrame {
    private final CardLayout cards  = new CardLayout();
    private final JPanel     root   = new JPanel(cards);
    private final MenuPanel  menu   = new MenuPanel();
    private final BoardPanel board  = new BoardPanel();
    private final JLabel     status = new JLabel("", SwingConstants.CENTER);
    private final JButton    menuBtn = new JButton("◀ Menu");

    // PHẦN PHÁT TRIỂN -23130072-Dũng
    private final JButton    undoBtn = new JButton("↰ Hoàn tác");

    public MainFrame() {
        super("Checkers Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Game panel
        JPanel game = new JPanel(new BorderLayout(0, 4));
        game.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        status.setFont(new Font("SansSerif", Font.BOLD, 16));
        JPanel top = new JPanel(new BorderLayout());
        top.add(menuBtn, BorderLayout.WEST);
        top.add(status,  BorderLayout.CENTER);

        // ── PHẦN PHÁT TRIỂN -23130072-Dũng
        // Thiết lập khu vực và ép nút Hoàn tác về phía bên phải thanh công cụ top
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBox.add(undoBtn);
        top.add(rightBox, BorderLayout.EAST);

        game.add(top,   BorderLayout.NORTH);
        game.add(board, BorderLayout.CENTER);

        root.add(menu, "MENU");
        root.add(game, "GAME");
        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    public void showMenu() { cards.show(root, "MENU"); pack(); }
    public void showGame() { cards.show(root, "GAME"); pack(); }

    public void refresh(GameModel m) {
        board.setModel(m);
        if (m == null) return;

        // ── PHẦN PHÁT TRIỂN -23130072-Dũng
        // Tự động kích hoạt/vô hiệu hóa nút bấm theo trạng thái của bộ nhớ stack
        undoBtn.setEnabled(m.canUndo());

        String turn = m.isRedTurn() ? " Lượt ĐỎ" : " Lượt ĐEN";
        String mode = m.getMode() == GameModel.Mode.PVP ? "[PvP]" : "[PvAI]";
        status.setText(mode + "  " + turn
                + "   ĐỎ " + m.redCount() + "   ĐEN " + m.blackCount());
    }

    public MenuPanel  getMenu()    { return menu; }
    public BoardPanel getBoard()   { return board; }
    public JButton    getMenuBtn() { return menuBtn; }

    // ── PHẦN PHÁT TRIỂN -23130072-Dũng
    public JButton    getUndoBtn() { return undoBtn; }
}