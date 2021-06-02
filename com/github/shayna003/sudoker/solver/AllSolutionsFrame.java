package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.Application;
import com.github.shayna003.sudoker.ApplicationFrame;
import com.github.shayna003.sudoker.Sudoku;
import com.github.shayna003.sudoker.SudokuTab;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.util.GridUtil;
import com.github.shayna003.sudoker.util.IO;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * @since 5-23-2021
 */
public class AllSolutionsFrame extends JFrame
{
    JTabbedPane tabbedPane;
    JMenu optionsMenu;
    AbstractAction closeThisTab;
    AbstractAction copySelectedSudokuString;
    AbstractAction makeNewTabWithSelectedSudoku;
    AbstractAction makeNewWindowWithSelectedSudoku;
    AbstractAction maxSolutionSettings;

    public AllSolutionsFrame()
    {
        tabbedPane = new JTabbedPane();

        JMenuBar bar = new JMenuBar();
        optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('O');

        closeThisTab = new AbstractAction("Close this tab")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                tabbedPane.remove(tabbedPane.getSelectedIndex());
            }
        };
        closeThisTab.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        closeThisTab.setEnabled(false);
        Application.keyboardSettingsPanel.registerOtherShortcut("closeAllSolutionsTab", KeyboardSettingsPanel.getMenuItemString("All Solutions Tables", "Close This Tab"), false, 0, 0, closeThisTab, tabbedPane, JComponent.WHEN_IN_FOCUSED_WINDOW);

        copySelectedSudokuString = new AbstractAction("Copy Selected Sudoku Text")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                StringSelection selection = new StringSelection(IO.getCompact81CandidatesString(getSelectedTab().getSelectedSudoku()));
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
        };
        copySelectedSudokuString.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        copySelectedSudokuString.setEnabled(false);
        // command + c can trigger copying the selected row in the table.
        // changing the table selection behaviour can make it hard to see the number of the solution.
        // therefore the shortcut is shift command c
        Application.keyboardSettingsPanel.registerOtherShortcut("copyAllSolutionsSudokuString", KeyboardSettingsPanel.getMenuItemString("All Solutions Tables", "Copy Selected Sudoku Text"), true, KeyEvent.VK_C, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, copySelectedSudokuString, tabbedPane, JComponent.WHEN_IN_FOCUSED_WINDOW);

        makeNewTabWithSelectedSudoku = new AbstractAction("Create New Tab with Selected Sudoku")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ApplicationFrame frame;
                if (Application.openWindows.size() > 1)
                {
                    frame = Application.openWindowsAndTabs.showWindowChooserDialog(AllSolutionsFrame.this);
                }
                else
                {
                    frame = Application.openWindows.get(0);
                }
                if (frame != null)
                {
                    frame.addNewBoard(false, getSelectedTab().getSelectedSudoku(), "New Tab from " + getSelectedTab().getName() + "'s solution #" + (getSelectedTab().table.getSelectedRow() + 1) + " of all solutions");
                }
            }
        };
        makeNewTabWithSelectedSudoku.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
        makeNewTabWithSelectedSudoku.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, 11);
        makeNewTabWithSelectedSudoku.setEnabled(false);
        Application.keyboardSettingsPanel.registerOtherShortcut("newTabFromSelectedSolution", KeyboardSettingsPanel.getMenuItemString("All Solutions Tables", "Create New Tab with Selected Sudoku"), true, KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), makeNewTabWithSelectedSudoku, tabbedPane, JComponent.WHEN_IN_FOCUSED_WINDOW);

        makeNewWindowWithSelectedSudoku = new AbstractAction("Create New Window with Selected Sudoku")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Application.openWindows.get(0).addNewBoard(true, getSelectedTab().getSelectedSudoku(), "New Window from " + getSelectedTab().getName() + "'s solution #" + (getSelectedTab().table.getSelectedRow() + 1) + " of all solutions");

            }
        };
        makeNewWindowWithSelectedSudoku.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_W);
        makeNewWindowWithSelectedSudoku.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, 11);
        makeNewWindowWithSelectedSudoku.setEnabled(false);
        Application.keyboardSettingsPanel.registerOtherShortcut("newWindowFromSelectedSolution", KeyboardSettingsPanel.getMenuItemString("All Solutions Tables", "Create New Window with Selected Sudoku"), true, KeyEvent.VK_N, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, makeNewWindowWithSelectedSudoku, tabbedPane, JComponent.WHEN_IN_FOCUSED_WINDOW);

        maxSolutionSettings = new AbstractAction("\u2699 Settings for All Solutions Maximum Count")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Application.preferenceFrame.showUp(Application.solverSettingsPanel, Application.solverSettingsPanel.maxSolutionsForSolveAll);
            }
        };
        maxSolutionSettings.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        Application.keyboardSettingsPanel.registerOtherShortcut("maxSolutionCountSettings", KeyboardSettingsPanel.getMenuItemString("All Solutions Tables", "\u2699 Settings for All Solutions Maximum Count"), false, 0, 0, maxSolutionSettings, tabbedPane, JComponent.WHEN_IN_FOCUSED_WINDOW);

        optionsMenu.add(closeThisTab);
        optionsMenu.addSeparator();

        optionsMenu.add(copySelectedSudokuString);
        optionsMenu.add(makeNewTabWithSelectedSudoku);
        optionsMenu.add(makeNewWindowWithSelectedSudoku);

        optionsMenu.addSeparator();
        optionsMenu.add(maxSolutionSettings);
        bar.add(optionsMenu);
        setJMenuBar(bar);

        tabbedPane.addChangeListener(event ->
        {
            closeThisTab.setEnabled(tabbedPane.getTabCount() > 0);
            AllSolutionsTab tab = (AllSolutionsTab) tabbedPane.getSelectedComponent();

            boolean enabled = tab != null && tab.table.getSelectedRow() >= 0;
            copySelectedSudokuString.setEnabled(enabled);
            makeNewTabWithSelectedSudoku.setEnabled(enabled);
            makeNewWindowWithSelectedSudoku.setEnabled(enabled);
        });

        add(tabbedPane, BorderLayout.CENTER);
        setTitle("All Solutions Tables");
        setLocationByPlatform(true);

        GeneralSettingsPanel.registerComponentAndSetFontSize(this);
    }

    public AllSolutionsTab getSelectedTab()
    {
        return (AllSolutionsTab) tabbedPane.getSelectedComponent();
    }

    public void addTab(SudokuTab tab, ArrayList<int[][]> data)
    {
        tabbedPane.addTab("All Solutions for " +  tab.getName(), new AllSolutionsTab(tab, data));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        if (tabbedPane.getTabCount() == 1) pack();
    }

    class AllSolutionsTab extends JPanel
    {
        JPopupMenu popupMenu;
        SudokuTab tab;
        ArrayList<int[][]> data;
        AllSolutionsTable table;
        JLabel label;

        public String getName()
        {
            return tab.owner.getTitle() + " " +  tab.getName();
        }

        public Sudoku getSelectedSudoku()
        {
            if (table.getSelectedRow() < 0) return null;
            else return new Sudoku(GridUtil.copyOf(data.get(table.getSelectedRow())));
        }

        public AllSolutionsTab(SudokuTab tab, ArrayList<int[][]> data)
        {
            super(new BorderLayout());

            this.tab = tab;
            this.data = data;
            table = new AllSolutionsTable();

            popupMenu = new JPopupMenu();
            GeneralSettingsPanel.registerComponentAndSetFontSize(popupMenu);

            popupMenu.add(closeThisTab);
            popupMenu.addSeparator();

            popupMenu.add(copySelectedSudokuString);
            popupMenu.add(makeNewTabWithSelectedSudoku);
            popupMenu.add(makeNewWindowWithSelectedSudoku);

            popupMenu.addSeparator();
            popupMenu.add(maxSolutionSettings);
            table.setComponentPopupMenu(popupMenu);

            if (data.size() > (Integer) Application.solverSettingsPanel.maxSolutionsForSolveAll.getValue())
            {
                data.remove(data.size() - 1);
                int max = (Integer) Application.solverSettingsPanel.maxSolutionsForSolveAll.getValue();
                label = new JLabel("More than " + max + " Solutions Found, here are the first " + max + ":");
            }
            else if (data.size() == 1)
            {
                label = new JLabel("Only 1 Solution Found:");
            }
            else
            {
                label = new JLabel(data.size() + " Total Solutions Found:");
            }

            JScrollPane pane = new JScrollPane(table);
            // doesn't work
            // pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            // pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            add(label, BorderLayout.NORTH);
            add(pane, BorderLayout.CENTER);
        }

        class AllSolutionsTable extends JTable
        {
            TableModel model;

            public AllSolutionsTable()
            {
                model = new AbstractTableModel()
                {
                    @Override
                    public int getRowCount()
                    {
                        return data.size();
                    }

                    @Override
                    public int getColumnCount()
                    {
                        return 2;
                    }

                    @Override
                    public Object getValueAt(int rowIndex, int columnIndex)
                    {
                        if (columnIndex == 0) return rowIndex + 1;
                        else return IO.getCompact81CandidatesString(data.get(rowIndex));
                    }

                    @Override
                    public String getColumnName(int c)
                    {
                        return c == 0 ? "#" : "Solution";
                    }
                };
                setModel(model);
                getSelectionModel().addListSelectionListener(event ->
                {
                    boolean enabled = getSelectedRow() >= 0;
                    copySelectedSudokuString.setEnabled(enabled);
                    makeNewTabWithSelectedSudoku.setEnabled(enabled);
                    makeNewWindowWithSelectedSudoku.setEnabled(enabled);
                });

                getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                Rectangle2D bounds = getFont().getStringBounds(String.valueOf(data.size()), getFontMetrics(getFont()).getFontRenderContext());
                TableColumn c = getColumn("#");
                int preferredWidthForNameColumn = (int) (bounds.getWidth() * 1.1 + 5);
                c.setPreferredWidth(preferredWidthForNameColumn);
                c.setMaxWidth(preferredWidthForNameColumn);
                c.setMinWidth(preferredWidthForNameColumn);
            }
        }
    }
}
