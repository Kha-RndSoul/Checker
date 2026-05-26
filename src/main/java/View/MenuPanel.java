package View;

import Model.GameModel;
import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/** Màn hình chọn chế độ chơi và độ khó. */
public class MenuPanel extends JPanel {
    private final JRadioButton pvp   = new JRadioButton("2 Người (PvP)", true);
    private final JRadioButton pvai  = new JRadioButton("Vs AI (PvAI)");
    private final JRadioButton easy  = new JRadioButton("Dễ");
    private final JRadioButton med   = new JRadioButton("Trung bình", true);
    private final JRadioButton hard  = new JRadioButton("Khó");
    private final JPanel diffBox     = new JPanel();
    private BiConsumer<GameModel.Mode, GameModel.Diff> onStart;

    public MenuPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(10,20,10,20);

        JLabel title = new JLabel("CHECKERS", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setForeground(new Color(130,60,10));
        g.gridy=0; add(title, g);

        // Chế độ
        JPanel modeBox = new JPanel();
        modeBox.setBorder(BorderFactory.createTitledBorder("Chế độ chơi"));
        ButtonGroup mg = new ButtonGroup(); mg.add(pvp); mg.add(pvai);
        modeBox.add(pvp); modeBox.add(pvai);
        g.gridy=1; add(modeBox, g);

        // Độ khó
        diffBox.setBorder(BorderFactory.createTitledBorder("Độ khó AI"));
        ButtonGroup dg = new ButtonGroup(); dg.add(easy); dg.add(med); dg.add(hard);
        diffBox.add(easy); diffBox.add(med); diffBox.add(hard);
        diffBox.setVisible(false);
        g.gridy=2; add(diffBox, g);

        pvai.addActionListener(e -> diffBox.setVisible(true));
        pvp.addActionListener(e  -> diffBox.setVisible(false));

        JButton btn = new JButton("Bắt đầu ▶");
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(new Color(50,120,50)); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            if (onStart == null) return;
            GameModel.Mode m = pvai.isSelected() ? GameModel.Mode.PV_AI : GameModel.Mode.PVP;
            GameModel.Diff d = easy.isSelected() ? GameModel.Diff.EASY
                    : hard.isSelected() ? GameModel.Diff.HARD : GameModel.Diff.MEDIUM;
            onStart.accept(m, d);
        });
        g.gridy=3; add(btn, g);
    }

    public void setOnStart(BiConsumer<GameModel.Mode, GameModel.Diff> h) { onStart = h; }
}