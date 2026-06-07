package View;

import Model.GameModel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * MainFrame – Cửa sổ chính, dùng CardLayout để chuyển giữa menu và game.
 *
 * UC12 – THÔNG BÁO KẾT QUẢ
 * Sau khi ResultDialog đóng lại, GameController gọi một trong hai phương thức:
 *   • showGame() – UC12-Bước 4 (nhánh "Chơi lại"): hiển thị bàn cờ mới
 *   • showMenu() – UC12-Bước 4 (nhánh "Menu")    : trở về màn hình menu chính
 * refresh(model) được gọi ở UC12-Bước 1 sau mỗi nước đi để cập nhật View.
 */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel     root  = new JPanel(cards);
    private final MenuPanel  menu  = new MenuPanel();
    private final BoardPanel board = new BoardPanel();
    private final JLabel     status = new JLabel("", SwingConstants.CENTER);
    private final JButton    menuBtn = new JButton("◀ Menu");

    // Nút Hoàn tác và bảng lịch sử nước đi (ký hiệu đại số)
    private final JButton    undoBtn  = new JButton("↰ Hoàn tác");
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);

    public MainFrame() {
        super("Checkers Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // ── Dựng panel game ──────────────────────────────────────────────────
        JPanel game = new JPanel(new BorderLayout(0, 4));
        game.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        status.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.add(menuBtn, BorderLayout.WEST);
        top.add(status,  BorderLayout.CENTER);

        // Ép nút Hoàn tác sang bên phải thanh công cụ
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBox.add(undoBtn);
        top.add(rightBox, BorderLayout.EAST);

        game.add(top,   BorderLayout.NORTH);
        game.add(board, BorderLayout.CENTER);

        // ── Bảng lịch sử nước đi (ký hiệu đại số a-h) ──────────────────────
        logList.setFont(new Font("SansSerif", Font.BOLD, 13));
        logList.setBackground(new Color(245, 240, 230));
        logList.setSelectionBackground(new Color(200, 180, 140));

        JScrollPane logScrollPane = new JScrollPane(logList);
        logScrollPane.setPreferredSize(new Dimension(160, 0));
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 160, 120), 2),
                "Lịch sử đi (a-h)",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12)));
        game.add(logScrollPane, BorderLayout.EAST);

        root.add(menu, "MENU");
        root.add(game, "GAME");
        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * UC12-Bước 4 (nhánh "Menu"): GC → MF : showMenu()
     * Chuyển CardLayout về màn hình menu chính sau khi kết thúc ván.
     */
    public void showMenu() { cards.show(root, "MENU"); pack(); }

    /**
     * UC12-Bước 4 (nhánh "Chơi lại"): GC → MF : showGame()
     * Chuyển CardLayout sang màn hình bàn cờ để bắt đầu ván mới.
     */
    public void showGame() { cards.show(root, "GAME"); pack(); }

    /**
     * UC12-Bước 1: GC → MF : refresh(model)
     * Cập nhật toàn bộ View sau mỗi nước đi: bàn cờ, thanh trạng thái,
     * lịch sử nước đi và trạng thái nút Hoàn tác.
     */
    public void refresh(GameModel m) {
        board.setModel(m);
        if (m == null) return;

        // Kích hoạt / vô hiệu hóa nút Hoàn tác theo trạng thái undo stack
        undoBtn.setEnabled(m.canUndo());

        // Đồng bộ bảng lịch sử nước đi
        logModel.clear();
        List<String> moves = m.getMoveLog();
        for (int i = 0; i < moves.size(); i++) {
            String cleanMove = moves.get(i)
                    .replace("🔴", "")
                    .replace("⚫", "")
                    .replace("●", "")
                    .trim();
            logModel.addElement((i + 1) + ". " + cleanMove);
        }
        // Tự động cuộn xuống nước đi mới nhất
        int lastIndex = logModel.getSize() - 1;
        if (lastIndex >= 0) logList.ensureIndexIsVisible(lastIndex);

        // Cập nhật thanh trạng thái: lượt đi, chế độ, số quân còn lại
        String redCircle   = "<font color='#C83232'>●</font>";
        String blackCircle = "<font color='#232323'>●</font>";
        String turnStr  = m.isRedTurn() ? redCircle + " Lượt ĐỎ" : blackCircle + " Lượt ĐEN";
        String modeStr  = m.getMode() == GameModel.Mode.PVP ? "[PvP]" : "[PvAI]";
        status.setText("<html>" + modeStr + " &nbsp;&nbsp;&nbsp;&nbsp; " + turnStr
                + " &nbsp;&nbsp;&nbsp;&nbsp; | &nbsp;&nbsp;&nbsp;&nbsp; "
                + redCircle + " ĐỎ: " + m.redCount() + " &nbsp;&nbsp;&nbsp;&nbsp; "
                + blackCircle + " ĐEN: " + m.blackCount() + "</html>");
    }

    public MenuPanel  getMenu()    { return menu; }
    public BoardPanel getBoard()   { return board; }
    public JButton    getMenuBtn() { return menuBtn; }
    public JButton    getUndoBtn() { return undoBtn; }
}
