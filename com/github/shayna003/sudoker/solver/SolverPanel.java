package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.history.Edit;
import com.github.shayna003.sudoker.history.EditType;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.util.Checker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @since 5-23-2021
 */
public class SolverPanel extends JPanel
{
    SudokuTab owner;

    JPanel buttonsPanel;
    JButton checkValidity;
    JButton checkValidityAndSolutions;
    JButton quickSolve;
    JButton solveAll;
    JButton takeStep;
    JButton options;

    JPopupMenu optionsPopup;
    JMenuItem clearValidityHighlights;
    JMenuItem clearSolverHighlights;
    JMenuItem clearOutputConsole;

    JMenuItem solverSettings;
    JMenuItem solverColorSettings;

    public JTextArea output;
    public JPanel outputPanel;
    public SolvingTechniqueTree solvingTree;

    public SolverPanel(SudokuTab owner)
    {
        this.owner = owner;
        if (owner.board.solverHighlights == null) owner.board.solverHighlights = new int[9][9][9];

        solvingTree = Application.solverSettingsPanel.modelSolvingTree.clone();
        solvingTree.board = owner.board;

        AbstractAction checkValidityAction = new AbstractAction("Check Validity")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                owner.board.cellEditor.endEdit();
                runValidityCheck(true);
            }
        };
        checkValidity = new JButton(checkValidityAction);
        checkValidity.setToolTipText("Check for conflicts on the Board and cells with no possible candidates");
        Application.keyboardSettingsPanel.registerOtherShortcut("checkValidity", KeyboardSettingsPanel.getMenuItemString("Solver", "Check Validity"), true, KeyEvent.VK_V, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, checkValidityAction, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction checkValidityAndSolutionsAction = new AbstractAction("Check Validity Fully")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                owner.board.cellEditor.endEdit();
                runValidityAndSolutionCountCheck();
            }
        };
        checkValidityAndSolutions = new JButton(checkValidityAndSolutionsAction);
        checkValidityAndSolutions.setToolTipText("Check Validity + check for number of solutions");
        Application.keyboardSettingsPanel.registerOtherShortcut("checkValidityFully", KeyboardSettingsPanel.getMenuItemString("Solver", "Check Validity Fully"), false, 0, 0, checkValidityAndSolutionsAction, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction quickSolveAction = new AbstractAction("Quick Solve")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                owner.board.cellEditor.endEdit();
                if (shouldStartSolving())
                {
                    ArrayList<int[][]> answers = new ArrayList<>(1);
                    int result = Solver.solve(owner.board.sudoku, answers, Solver.FIND_ONE_SOLUTION);
                    if (result > 0)
                    {
                        assert result == 1 : result;
                        owner.board.setSudoku(answers.get(0));
                        owner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Imported Sudoku from Text", EditType.QUICK_SOLVE, owner.board));
                    }
                    else
                    {
                        appendMessage("This board has no valid solutions.", true);
                    }
                }
            }
        };
        quickSolve = new JButton(quickSolveAction);
        quickSolve.setToolTipText("Solve Instantly");
        Application.keyboardSettingsPanel.registerOtherShortcut("quickSolve", KeyboardSettingsPanel.getMenuItemString("Solver", "Quick Solve"), false, 0, 0, quickSolveAction, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction solutionCountAction = new AbstractAction("Solution Count")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                owner.board.cellEditor.endEdit();
                if (shouldStartSolving())
                {
                    ArrayList<int[][]> results = new ArrayList<>();
                    int found = Solver.solve(owner.board.sudoku, results, Solver.KEEP_ON_LOOKING_UNTIL_MAX_REACHED);
                    if (found > 0)
                    {
                        Application.getAllSolutionsFrame().addTab(owner, results);
                        Application.allSolutionsFrame.setVisible(true);
                    }
                    else
                    {
                        appendMessage("This board has no valid solutions.", true);
                    }
                }
            }
        };
        solveAll = new JButton(solutionCountAction);
        solveAll.setToolTipText("Find all solutions of this puzzle");
        Application.keyboardSettingsPanel.registerOtherShortcut("solutionCount", KeyboardSettingsPanel.getMenuItemString("Solver", "Solution Count"), false, 0, 0, solutionCountAction, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction takeStepAction = new AbstractAction("Take Step")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                owner.board.cellEditor.endEdit();
                if (shouldStartSolving())
                {
                    takeStep();
                }
            }
        };
        takeStep = new JButton(takeStepAction);
        takeStep.setToolTipText("Take one step using the checked solving techniques");
        Application.keyboardSettingsPanel.registerOtherShortcut("takeStep", KeyboardSettingsPanel.getMenuItemString("Solver", "Take Step"), true, KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), takeStepAction, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        options = new JButton("Options...");
        options.addActionListener(event ->
        {
            Point point = SwingUtilities.convertPoint(buttonsPanel, options.getLocation(), this);
            optionsPopup.show(this, point.x, point.y + options.getHeight());
        });

        AbstractAction clearValidityHighlightsAction = new AbstractAction("Clear Validity Highlights")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (owner.board.validity != null)
                {
                    for (int r = 0; r < 9; r++) Arrays.fill(owner.board.validity[r], VALID);
                    owner.board.repaint();
                }
            }
        };
        clearValidityHighlightsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);
        clearValidityHighlights = new JMenuItem(clearValidityHighlightsAction);
        Application.keyboardSettingsPanel.registerOtherShortcut("clearValidityHighlights", KeyboardSettingsPanel.getMenuItemString("Solver", "Clear Validity Highlights"), false, 0, 0, clearValidityHighlightsAction, null, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction clearSolverHighlightsAction = new AbstractAction("Clear Solver Highlights and Reset Take Step")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                clearSolverHighlightsCompletely();
            }
        };
        clearSolverHighlightsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        clearSolverHighlights = new JMenuItem(clearSolverHighlightsAction);
        Application.keyboardSettingsPanel.registerOtherShortcut("clearSolverHighlights", KeyboardSettingsPanel.getMenuItemString("Solver", "Clear Solver Highlights"), false, 0, 0, clearSolverHighlightsAction, null, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction clearOutputConsoleAction = new AbstractAction("Clear Output Messages")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                output.setText("");
            }
        };
        clearOutputConsoleAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
        clearOutputConsole = new JMenuItem(clearOutputConsoleAction);
        Application.keyboardSettingsPanel.registerOtherShortcut("clearOutputConsole", KeyboardSettingsPanel.getMenuItemString("Solver", "Clear Output Messages"), false, 0, 0, clearOutputConsoleAction, null, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction solverSettingsAction = new AbstractAction("\u2699 Solver Settings")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Application.preferenceFrame.showUp(Application.solverSettingsPanel);
            }
        };
        solverSettingsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        solverSettings = new JMenuItem(solverSettingsAction);
        Application.keyboardSettingsPanel.registerOtherShortcut("solverSettings", KeyboardSettingsPanel.getMenuItemString("Solver", "\u2699 Solver Settings"), false, 0, 0, solverSettingsAction, null, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        AbstractAction solverColorSettingsAction = new AbstractAction("\u2699 Solver Color Settings")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Application.preferenceFrame.showUp(Application.themesPanel, Application.themesPanel.onlyCandidateColor);
            }
        };
        solverColorSettingsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        solverColorSettings = new JMenuItem(solverColorSettingsAction);
        Application.keyboardSettingsPanel.registerOtherShortcut("solverColorSettings", KeyboardSettingsPanel.getMenuItemString("Solver", "\u2699 Solver Color Settings"), false, 0, 0, solverColorSettingsAction, null, SolverPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);

        optionsPopup = new JPopupMenu();
        GeneralSettingsPanel.registerComponentAndSetFontSize(optionsPopup);
        optionsPopup.add(clearValidityHighlights);
        optionsPopup.add(clearSolverHighlights);
        optionsPopup.add(clearOutputConsole);
        optionsPopup.addSeparator();
        optionsPopup.add(solverSettings);
        optionsPopup.add(solverColorSettings);

        buttonsPanel = new JPanel(new GridLayout(3, 2));
        buttonsPanel.add(checkValidity);
        buttonsPanel.add(checkValidityAndSolutions);
        buttonsPanel.add(quickSolve);
        buttonsPanel.add(solveAll);
        buttonsPanel.add(takeStep);
        buttonsPanel.add(options);
        buttonsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        output = new JTextArea(10, 10);
        output.setEditable(false);

        JScrollPane outputPane = new JScrollPane(output);
        outputPanel = new JPanel(new BorderLayout(0, 0));
        outputPanel.add(outputPane, BorderLayout.CENTER);
        outputPanel.setBorder(BorderFactory.createTitledBorder("Solver Output Messages"));

        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.NORTH);
        add(new JScrollPane(solvingTree), BorderLayout.SOUTH);
    }

    boolean shouldStartSolving()
    {
        String feedBack = runValidityCheck(false);
        if (feedBack.length() > 0) // board is invalid
        {
            appendMessage("Cannot Solve. The Board is invalid:", true);
            appendMessage(feedBack, false);
            return false;
        }
        else if (owner.board.counter.totalSolvedCells == 81)
        {
            appendMessage("This board is already solved and valid.", true);
            return false;
        }
        else
        {
            return true;
        }
    }

    // for cell validity
    public static final int NO_CANDIDATES = -1;
    public static final int REPEATED = -2;
    public static final int VALID = 0;

    // for solver highlights
    public static final int NORMAL_CANDIDATE = 0;
    public static final int ELIMINATED_CANDIDATE = -1;
    public static final int ONLY_CANDIDATE = -2;

    /**
     * Appends a message to the Solver Output TextArea
     */
    void appendMessage(String s, boolean appendExtraLineSeparator)
    {
        if (output.getText().length() > 0)
        {
            StringBuilder b;
            if (appendExtraLineSeparator && Application.solverSettingsPanel.clearSolverOutputWhenAppendingMessage.isSelected())
            {
                b = new StringBuilder();
            }
            else
            {
                b = new StringBuilder(output.getText());
                b.append(System.lineSeparator());
                if (appendExtraLineSeparator) b.append(System.lineSeparator());
            }
            b.append(s);
            output.setText(b.toString());
        }
        else
        {
            output.setText(s);
        }
    }

    public void runValidityAndSolutionCountCheck()
    {
        StringBuilder b = new StringBuilder();
        String messages = runValidityCheck(false);

        if (messages.length() > 0)
        {
            b.append("Board is invalid: ");
            b.append(System.lineSeparator());
            b.append(messages);
            appendMessage(b.toString(), true);
            return;
        }

        if (owner.board.counter.totalSolvedCells == 81)
        {
            appendMessage("Board is solved and valid!", true);
            return;
        }

        int solutionCount = Solver.solve(owner.board.sudoku, null, Solver.RETURN_IF_FINDS_SECOND_SOLUTION);
        if (solutionCount <= 0)
        {
            b.append("Board is invalid: ");
            b.append(System.lineSeparator());
            b.append("Even though the board is currently valid, there are no solutions for this puzzle.");
        }
        else if (solutionCount == 1)
        {
            b.append("Board is valid and has only 1 solution.");
        }
        else
        {
            b.append("Board is valid, but has more than 1 solution.");
        }
        appendMessage(b.toString(), true);
    }

    /*
     * @return if board is invalid, return the messages describing what is invalid
     */
    public String runValidityCheck(boolean appendFinalMessages)
    {
        StringBuilder b = new StringBuilder();
        if (owner.board.validity == null)
        {
            owner.board.validity = new int[9][9];
        }
        else
        {
            for (int r = 0; r < 9; r++) Arrays.fill(owner.board.validity[r], VALID);
        }

        Sudoku sudoku = owner.board.sudoku;

        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                if (owner.board.validity[r][c] == VALID)
                {
                    if (sudoku.status[r][c] == 0)
                    {
                        owner.board.validity[r][c] = NO_CANDIDATES;
                        b.append("Cell ");
                        b.append(Application.digitsAndIndexesPanel.getStringIndex(r, c));
                        b.append(" has no possible candidates.");
                        b.append(System.lineSeparator());
                    }
                    else if (sudoku.status[r][c] > 0)
                    {
                        // check for repeats in same row
                        int occurrences = 1;
                        for (int ci = 0; ci < 9; ci++)
                        {
                            if (ci != c && sudoku.status[r][ci] == sudoku.status[r][c])
                            {
                                occurrences++;
                                owner.board.validity[r][ci] = REPEATED;
                            }
                        }
                        if (occurrences > 1)
                        {
                            owner.board.validity[r][c] = REPEATED;
                            b.append("Candidate ");
                            b.append(Application.digitsAndIndexesPanel.getDigit(sudoku.status[r][c]));
                            b.append(" occurs ");
                            b.append(occurrences);
                            b.append(" times in row ");
                            b.append(Application.digitsAndIndexesPanel.getRowIndex(r));
                            b.append(".");
                            b.append(System.lineSeparator());
                            occurrences = 1;
                        }

                        // check for repeats in same column
                        for (int ri = 0; ri < 9; ri++)
                        {
                            if (ri != r && sudoku.status[ri][c] == sudoku.status[r][c])
                            {
                                occurrences++;
                                owner.board.validity[ri][c] = REPEATED;
                            }
                        }
                        if (occurrences > 1)
                        {
                            owner.board.validity[r][c] = REPEATED;
                            b.append("Candidate ");
                            b.append(Application.digitsAndIndexesPanel.getDigit(sudoku.status[r][c]));
                            b.append(" occurs ");
                            b.append(occurrences);
                            b.append(" times in column ");
                            b.append(Application.digitsAndIndexesPanel.getColIndex(c));
                            b.append(".");
                            b.append(System.lineSeparator());
                            occurrences = 1;
                        }

                        // check for repeats in same box
                        int boxr = Math.floorDiv(r, 3);
                        int boxc = Math.floorDiv(c, 3);

                        for (int ri = 0; ri < 3; ri++)
                        {
                            for (int ci = 0; ci < 3; ci++)
                            {
                                if (boxr * 3 + ri != r || boxc * 3 + ci != c)
                                {
                                    if (sudoku.status[boxr * 3 + ri][boxc * 3 + ci] == sudoku.status[r][c])
                                    {
                                        occurrences++;
                                        owner.board.validity[boxr * 3 + ri][boxc * 3 + ci] = REPEATED;
                                    }
                                }
                            }
                        }
                        if (occurrences > 1)
                        {
                            owner.board.validity[r][c] = REPEATED;
                            b.append("Candidate ");
                            b.append(Application.digitsAndIndexesPanel.getDigit(sudoku.status[r][c]));
                            b.append(" occurs ");
                            b.append(occurrences);
                            b.append(" times in box ");
                            b.append(Application.digitsAndIndexesPanel.getBoxIndex(boxr, boxc));
                            b.append(".");
                            b.append(System.lineSeparator());
                        }
                    }
                }
            }
        }
        if (b.length() > 0)
        {
            // remove the last line separator
            b.delete(b.length() - System.lineSeparator().length(), b.length());
            if (appendFinalMessages)
            {
                appendMessage("Board is invalid: ", true);
                appendMessage(b.toString(), false);
            }
            owner.board.repaint();
        }
        else
        {
            if (appendFinalMessages)
            {
                if (Checker.solvedCellCount(owner.board.sudoku) == 81)
                {
                    appendMessage("Board is solved and valid!", true);
                }
                else
                {
                    appendMessage("Board is currently valid:", true);
                    appendMessage("There are no repeated candidates in the same unit, or cells with no possible candidates.", false);
                }
            }
        }
        return b.toString();
    }

    public void takeStep()
    {
        if (!owner.board.viewOptions.showAllCandidates.isSelected())
        {
            owner.board.viewOptions.showAllCandidates.setSelected(true);
        }

        String results = solvingTree.takeStep();
        if (results.length() > 0) appendMessage(results, true);
    }

    public void clearSolverHighlightsCompletely()
    {
        solvingTree.restartTraversal();
        clearSolverHighlights();

        if (solvingTree.takeStepResult != null)
        {
            solvingTree.takeStepResult = null;
        }
        owner.board.repaint();
    }

    public void clearSolverHighlights()
    {
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                Arrays.fill(owner.board.solverHighlights[r][c], SolverPanel.NORMAL_CANDIDATE);
            }
        }
    }
}
