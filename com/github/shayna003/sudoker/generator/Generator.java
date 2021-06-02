package com.github.shayna003.sudoker.generator;

import com.github.shayna003.sudoker.Application;
import com.github.shayna003.sudoker.ApplicationFrame;
import com.github.shayna003.sudoker.Board;
import com.github.shayna003.sudoker.Sudoku;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.SettingsPanel;
import com.github.shayna003.sudoker.prefs.SingleSettingsFile;
import com.github.shayna003.sudoker.prefs.components.PrefsButtonGroup;
import com.github.shayna003.sudoker.prefs.components.PrefsCheckBox;
import com.github.shayna003.sudoker.prefs.components.PrefsComponent;
import com.github.shayna003.sudoker.prefs.components.PrefsNumberSpinner;
import com.github.shayna003.sudoker.solver.Solver;
import com.github.shayna003.sudoker.swingComponents.GBC;
import com.github.shayna003.sudoker.util.Checker;
import com.github.shayna003.sudoker.util.IO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since 6-2-2021
 * This class needs a lot of work to have practical functionalities.
 * Later on will be able to generate sudokus with difficulty range etc.
 * Note: you can't close this dialog unless generation completed or is canceled.
 */
public class Generator extends JDialog implements SettingsPanel
{
    public SingleSettingsFile settingsFile;
    ArrayList<PrefsComponent> prefsComponents;

    volatile boolean cancelGeneration;
    volatile Sudoku result;

    JRadioButton useFixedClues;
    PrefsNumberSpinner fixedClues;
    JRadioButton useClueRange;
    PrefsNumberSpinner minClues;
    PrefsNumberSpinner maxClues;
    PrefsButtonGroup clueRangeButtonGroup;

    public PrefsCheckBox lockClues;

    JButton clearLocations;
    public Board clueLocationBoard;

    JButton generateButton;
    JButton cancelGenerationButton; // for canceling the generation if it takes a while
    JButton cancelButton; // close this dialog

    int minNumberOfClues;
    int maxNumberOfClues;

    public Generator()
    {
        setModal(true);

        prefsComponents = new ArrayList<>();

        fixedClues = new PrefsNumberSpinner("fixedClues", 17, 81, 1, 25, event ->
        {
            if (useFixedClues.isSelected())
            {
                minNumberOfClues = (Integer) fixedClues.getValue();
                maxNumberOfClues = minNumberOfClues;
                clueLocationBoard.maxClues = maxNumberOfClues;
            }
        }, 3);
        minClues = new PrefsNumberSpinner("minClues", 17, 81, 1, 25, event ->
        {
            minNumberOfClues = (Integer) minClues.getValue();
        }, 3);
        maxClues = new PrefsNumberSpinner("maxClues", 17, 81, 1, 30, event ->
        {
            if (useClueRange.isSelected())
            {
                maxNumberOfClues = (Integer) maxClues.getValue();
                clueLocationBoard.maxClues = maxNumberOfClues;
            }
        }, 3);

        prefsComponents.add(fixedClues);
        prefsComponents.add(minClues);
        prefsComponents.add(maxClues);

        useFixedClues = new JRadioButton("Use Fixed Number of Clues");
        useClueRange = new JRadioButton("Use Clue Number Range");
        clueRangeButtonGroup = new PrefsButtonGroup(event ->
        {
            setMaxNumberOfClues();
        }, "clueRadioSetting", 1, useFixedClues, useClueRange);
        prefsComponents.add(clueRangeButtonGroup);

        clearLocations = new JButton("Clear All Clue Locations");
        clearLocations.addActionListener(event ->
        {
            for (int r = 0; r < 9; r++)
            {
                for (int c = 0; c < 9; c++)
                {
                    clueLocationBoard.cellLocked[r][c] = false;
                }
            }
            clueLocationBoard.clueCount = 0;
            clueLocationBoard.repaint();
            clueLocationBoard.clueCountLabel.setText("Selected: 0");
        });

        clueLocationBoard = new Board();
        clueLocationBoard.initBoard(Board.FOR_GENERATOR);
        clueLocationBoard.initViewOptions(false);

        clueLocationBoard.sudoku = new Sudoku();
        clueLocationBoard.initArrays();

        lockClues = new PrefsCheckBox("lockGeneratedClues", "Lock Generated Clues", true);
        prefsComponents.add(lockClues);
        JPanel topPanel = new JPanel();
        topPanel.add(lockClues);

        JPanel clueSettingsPanel = new JPanel(new GridBagLayout());
        clueSettingsPanel.setBorder(BorderFactory.createTitledBorder("Clue Number Settings"));
        clueSettingsPanel.add(useFixedClues, new GBC(0, 0).setAnchor(GBC.WEST));
        clueSettingsPanel.add(fixedClues, new GBC(1, 0).setAnchor(GBC.WEST));

        clueSettingsPanel.add(useClueRange, new GBC(0, 1).setAnchor(GBC.WEST));
        clueSettingsPanel.add(new JLabel("Minimum: "), new GBC(1, 1).setAnchor(GBC.WEST));
        clueSettingsPanel.add(minClues, new GBC(2, 1).setAnchor(GBC.WEST));
        clueSettingsPanel.add(new JLabel("Maximum: "), new GBC(3, 1).setAnchor(GBC.WEST));
        clueSettingsPanel.add(maxClues, new GBC(4, 1).setAnchor(GBC.WEST));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(clueLocationBoard.clueCountLabel);
        buttonPanel.add(clearLocations);
        JPanel boardPanel = new JPanel();
        boardPanel.add(clueLocationBoard);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(boardPanel, BorderLayout.CENTER);
        centerPanel.setBorder(BorderFactory.createTitledBorder("Clue Location Settings (Selected Cells Will Have Clues)"));

        JPanel choiceButtonPanel = new JPanel();
        generateButton = new JButton("Generate");
        generateButton.addActionListener(event ->
        {
            if ((Integer) maxClues.getValue() < (Integer) minClues.getValue())
            {
                JOptionPane.showMessageDialog(Generator.this, "Maximum Clue number cannot be smaller than Minimum Clue Number.");
            }
            else if (clueLocationBoard.clueCount > clueLocationBoard.maxClues)
            {
                int difference = clueLocationBoard.clueCount - clueLocationBoard.maxClues;
                JOptionPane.showMessageDialog(Generator.this, "You need to either change the clue number setting or deselect " + difference + (difference > 1 ? " cells on the board to continue." : " cell on the board to continue."), "Too Many Cells Selected in Board", JOptionPane.INFORMATION_MESSAGE);
            }
            else // start generating
            {
                generateButton.setEnabled(false);
                generateButton.setText("Generating...");
                cancelGenerationButton.setEnabled(true);
                new GenerateWorker().execute();
            }
        });

        cancelGenerationButton = new JButton("Cancel Generation");
        cancelGenerationButton.addActionListener(event ->
        {
            cancelGeneration = false;
        });
        cancelGenerationButton.setEnabled(false);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event ->
        {
            if (generateButton.isEnabled())
            {
                saveSettings(settingsFile, true);
                setVisible(false);
            }
        });
        choiceButtonPanel.add(generateButton);
        choiceButtonPanel.add(cancelGenerationButton);
        choiceButtonPanel.add(cancelButton);

        JPanel topCompositePanel = new JPanel();
        topCompositePanel.setLayout(new BoxLayout(topCompositePanel, BoxLayout.Y_AXIS));
        topCompositePanel.add(topPanel);
        topCompositePanel.add(clueSettingsPanel);

        setLayout(new BorderLayout());
        add(topCompositePanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(choiceButtonPanel, BorderLayout.SOUTH);

        settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "generator_settings.xml"));
        loadSettings(settingsFile);
        setTitle("Generate New Sudoku Puzzle With Only 1 Solution");
        pack();

        GeneralSettingsPanel.registerComponentAndSetFontSize(this);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (generateButton.isEnabled())
                {
                    saveSettings(settingsFile, true);
                    setVisible(false);
                }
            }
        });
    }

    public Sudoku showGenerateDialog(ApplicationFrame owner)
    {
        result = null;
        setLocationRelativeTo(owner);
        setVisible(true);
        return result;
    }

    @Override
    public void saveSettings(SingleSettingsFile file, boolean saveToFile)
    {
        for (PrefsComponent c : prefsComponents)
        {
            c.saveSettings(file.node);
        }
        if (saveToFile) file.save();
    }

    @Override
    public void loadSettings(SingleSettingsFile file)
    {
        for (PrefsComponent c : prefsComponents)
        {
            c.loadSettings(file.node);
        }
        setMaxNumberOfClues();
    }

    @Override
    public SingleSettingsFile getSettingsFile()
    {
        return settingsFile;
    }

    @Override
    public void applyChanges() {}

    void setMaxNumberOfClues()
    {
        if (useFixedClues.isSelected())
        {
            minNumberOfClues = (Integer) fixedClues.getValue();
            maxNumberOfClues = minNumberOfClues;
            clueLocationBoard.maxClues = maxNumberOfClues;
        }
        else
        {
            minNumberOfClues = (Integer) minClues.getValue();
            maxNumberOfClues = (Integer) maxClues.getValue();
            clueLocationBoard.maxClues = maxNumberOfClues;
        }
    }

    /**
     * Generates a puzzle with the specified clue range and clue location settings.
     * @return the generated puzzle, or null if user canceled the generation.
     */
    public Sudoku generate()
    {
        cancelGeneration = false;
        Sudoku sudoku = new Sudoku();
        Random random = new Random();

        int numberOfCellSelected = 0;
        int usedClueLocationsFromBoard = 0; // for clue location settings
        int targetNumberOfClues = minNumberOfClues < maxNumberOfClues ? minNumberOfClues + random.nextInt(maxNumberOfClues - maxNumberOfClues + 1) : minNumberOfClues;

        // to record indexes of cells with clues set
        ArrayList<Integer> rowIndexes = new ArrayList<>(targetNumberOfClues);
        ArrayList<Integer> colIndexes = new ArrayList<>(targetNumberOfClues);

        if (clueLocationBoard.clueCount > 0)
        {
            int counter = 0;

            for (int r = 0; r < 9; r++)
            {
                for (int c = 0; c < 9; c++)
                {
                    if (clueLocationBoard.cellLocked[r][c])
                    {
                        rowIndexes.add(r);
                        colIndexes.add(c);
                        counter++;
                        if (counter == clueLocationBoard.clueCount) break;
                    }
                }
            }
        }

        while (numberOfCellSelected < targetNumberOfClues)
        {
            if (cancelGeneration)
            {
                return null;
            }
            if (usedClueLocationsFromBoard < clueLocationBoard.clueCount)
            {
                do
                {
                    sudoku.setValueAt(rowIndexes.get(usedClueLocationsFromBoard), colIndexes.get(usedClueLocationsFromBoard), 1 + random.nextInt(9));
                }
                while (!Checker.checkValid(sudoku, rowIndexes.get(usedClueLocationsFromBoard), colIndexes.get(usedClueLocationsFromBoard)));
                usedClueLocationsFromBoard++;
                numberOfCellSelected++;
            }
            else
            {
                int r;
                int c;
                do
                {
                    r = 1 + random.nextInt(8);
                    c = 1 + random.nextInt(8);
                }
                while (sudoku.status[r][c] > 0);

                do
                {
                    sudoku.setValueAt(r, c, 1 + random.nextInt(9));
                }
                while (!Checker.checkValid(sudoku, r, c));
                rowIndexes.add(r);
                colIndexes.add(c);
                numberOfCellSelected++;
            }
        }

        while (Solver.solve(sudoku, null, Solver.RETURN_IF_FINDS_SECOND_SOLUTION) != 1)
        {
            int index = random.nextInt(targetNumberOfClues);
            do
            {
                if (cancelGeneration)
                {
                    return null;
                }
                sudoku.setValueAt(rowIndexes.get(index), colIndexes.get(index), 1 + random.nextInt(9));
            }
            while (!Checker.checkValid(sudoku, rowIndexes.get(index), colIndexes.get(index)));
        }

        result = sudoku;
        return result;
    }

    private class GenerateWorker extends SwingWorker<Sudoku, Object>
    {
        @Override
        public Sudoku doInBackground()
        {
            return generate();
        }

        /**
         * Executes in the event dispatch thread
         */
        public void process(List<Object> data)
        {
        }

        /**
         * Executes in the event dispatch thread
         */
        public void done()
        {
            if (result != null)
            {
                saveSettings(settingsFile, true);
                setVisible(false);
            }

            cancelGeneration = false;
            cancelGenerationButton.setEnabled(false);
            generateButton.setText("Generate");
            generateButton.setEnabled(true);
        }
    }
}
