package com.tectonics.gui;

import java.awt.event.ActionEvent;
import java.awt.BorderLayout;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import com.tectonics.Simulation;


public class TectonicSim extends JFrame {
    
    private final SimulationPanel mSimPanel;

    private final JPanel mControlPanel;

    private boolean mIsRunning = false;
    
    private int tick = 0;

    public TectonicSim() {
        super("Tectonic Simulator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final int width  = 800;
        final int height = 600;


        // Components

        final Simulation sim = new Simulation(width, height, 6);
        mSimPanel = new SimulationPanel(width, height);
        mSimPanel.setSim(sim);

        final JComboBox<SimulationPanel.SimulationRenderMode> renderModeBox =
            new JComboBox<>(SimulationPanel.SimulationRenderMode.values());
        renderModeBox.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mSimPanel.setMode((SimulationPanel.SimulationRenderMode) renderModeBox.getSelectedItem());
            }
        });
    
        final JLabel tickLabel = new JLabel("Tick:  0");

        final JButton restartButton = new JButton();
        restartButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SwingWorker<>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        mSimPanel.setSim(new Simulation(width, height, 6));
                        tick = 0;
                        tickLabel.setText("Tick:  0");
                        return 0;
                    }
                }.execute();
            }
        });
        restartButton.setText("Restart Sim");

        final JButton playButton = new JButton();
        playButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SwingWorker<>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        playButton.setText(mIsRunning ? "Play" : "Pause");
                        mIsRunning = !mIsRunning;
                        while(mIsRunning) {
                            mSimPanel.update();
                            Thread.sleep(50);
                            ++tick;
                            tickLabel.setText("Tick:  " + tick);
                        }
                        return 0;
                    }
                }.execute();
            }
        });
        playButton.setText("Play");


        // Layout
        
        mControlPanel = new JPanel();
        mControlPanel.setLayout(new BoxLayout(mControlPanel, BoxLayout.Y_AXIS));

        mControlPanel.add(renderModeBox);
        mControlPanel.add(restartButton);
        mControlPanel.add(tickLabel);
        mControlPanel.add(playButton);


        setLayout(new BorderLayout());

        add(mSimPanel, BorderLayout.CENTER);
        add(mControlPanel, BorderLayout.WEST);
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
