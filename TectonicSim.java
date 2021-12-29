

import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;


public class TectonicSim extends JFrame {
    
    public TectonicSim() {
        super("Tectonic Simulator");

        final int width  = 300;
        final int height = 300;

        final Simulation sim = new Simulation(width, height, 6);
        final SimulationPanel sPanel = new SimulationPanel(width, height);

        sPanel.setSim(sim);

        final JButton cycleButton = new JButton();
        cycleButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SimulationPanel.SimulationRenderMode> values = 
                    Arrays.asList(SimulationPanel.SimulationRenderMode.values());

                final SimulationPanel.SimulationRenderMode mode =
                    values.get((values.indexOf(sPanel.getMode()) + 1) % values.size());

                cycleButton.setText(mode.toString());
                sPanel.setMode(mode);
            }
        });
        cycleButton.setText(sPanel.getMode().toString());

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

        add(sPanel);
        add(cycleButton);
        pack();
        repaint();
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TectonicSim();
        });
    }
}
