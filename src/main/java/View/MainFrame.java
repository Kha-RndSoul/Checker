package View;

import Model.GameModel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Cửa sổ chính, dùng CardLayout để chuyển giữa menu và game. */
public class MainFrame extends JFrame {
    private final CardLayout cards  = new CardLayout();
    private final JPanel     root   = new JPanel(cards);
    private final MenuPanel  menu   = new MenuPanel();
    private final BoardPanel board  = new BoardPanel();
    private final JLabel     status = new JLabel("", SwingConstants.CENTER);
    private final JButton    menuBtn = new JButton("◀ Menu");

    // PHẦN PHÁT TRIỂN - 23130072-Dũng
    private final JButton    undoBtn = new JButton("↰ Hoàn tác");
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);

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

        // Thiết lập khu vực và ép nút Hoàn tác về phía bên phải thanh công cụ top
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBox.add(undoBtn);
        top.add(rightBox, BorderLayout.EAST);

        game.add(top,   BorderLayout.NORTH);
        game.add(board, BorderLayout.CENTER);

        // Cấu hình giao diện đồ họa cho bảng hiển thị Lịch sử đi cờ Đại số nằm bên tay phải
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
                new Font("SansSerif", Font.BOLD, 12)
        ));
        game.add(logScrollPane, BorderLayout.EAST); // Gắn bảng log vào sườn bên phải bàn cờ

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

        // Tự động kích hoạt/vô hiệu hóa nút bấm theo trạng thái của bộ nhớ stack
        undoBtn.setEnabled(m.canUndo());
        // Đồng bộ và tải lại toàn bộ danh sách lịch sử đi
        logModel.clear();
        List<String> moves = m.getMoveLog();
        for (int i = 0; i < moves.size(); i++) {
            // DŨNG
            String cleanMove = moves.get(i)
                    .replace("🔴", "")
                    .replace("⚫", "")
                    .replace("●", "")
                    .trim();
            logModel.addElement((i + 1) + ". " + cleanMove);
        }
        // Cơ chế cuộn thông minh: tự động đưa thanh cuộn xuống dòng nhật ký cuối cùng vừa đi
        int lastIndex = logModel.getSize() - 1;
        if (lastIndex >= 0) {
            logList.ensureIndexIsVisible(lastIndex);
        }

        String redCircle = "<font color='#C83232'>●</font>";   // Dấu chấm tròn HTML Đỏ
        String blackCircle = "<font color='#232323'>●</font>"; // Dấu chấm tròn HTML Đen

        String turnStr = m.isRedTurn() ? redCircle + " Lượt ĐỎ" : blackCircle + " Lượt ĐEN";
        String modeStr = m.getMode() == GameModel.Mode.PVP ? "[PvP]" : "[PvAI]";

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