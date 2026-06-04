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
        // Bỏ qua click khi đến lượt AI
        if (model.getMode()==GameModel.Mode.PV_AI && !model.isRedTurn()) return;

        // Nếu đã chọn quân → thử di chuyển
        if (model.getSelected() != null) {
            boolean moved = model.moveTo(row, col);
            frame.refresh(model);
            if (moved) { checkGameOver(); scheduleAI(); return; }
        }
        // Chọn quân mới
        model.select(row, col);
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
        Timer t = new Timer(300, e -> {
            int opt = JOptionPane.showOptionDialog(frame,
                    winner + " thắng!\nLý do: " + model.getEndReason() + "\nBạn muốn chơi tiếp?",
                    "Kết thúc ván", JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, null,
                    new String[]{"Chơi lại", "Menu"}, "Chơi lại");
            if (opt == 0) startGame(lastMode, lastDiff);
            else frame.showMenu();
        });
        t.setRepeats(false); t.start();
    }
}