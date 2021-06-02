package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.util.Checker;
import com.github.shayna003.sudoker.widgets.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.prefs.keys.*;
import com.github.shayna003.sudoker.SudokuTab.QuickSaves.*;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import javax.print.attribute.*;

/**
 * Assume that there is always at least one tab in each ApplicationFrame.
 * @version 0.00 2021-5-8
 * @since 2020-11-1
 */
@SuppressWarnings("CanBeFinal")
public class ApplicationFrame extends JFrame
{
	public JTabbedPane historyTrees; // reference to the same TabbedPane containing HistoryTreePanels of this frame's tabs in HistoryTreeFrame
	public JScrollPane scrollPane; // in case things don't fit on screen
	
	public JMenuBar menuBar;
	
	public JMenu application_options;
	public JMenuItem preferences;
	public JMenuItem memoryMonitorItem;
	
	public JMenu newOptions;
	public JMenu editOptions;
	public JMenuItem undo;
	public JMenuItem redo;

	public JMenuItem seeHistoryTreeForThisTab;
	public AbstractAction quickSave; // brings up slots
	public AbstractAction quickLoad; // brings up slots
	public JMenuItem compareThisBoardWithAnother;

	public JMenu importOptions;
	public JMenu exportOptions;
	public JMenu printMenu;
	public JMenu viewOptionsMenu;
	
	public JMenu windowOptions;
	public JMenuItem musicPlayerItem;
	public JMenuItem historyTreeItem;
	public JMenuItem boardComparatorItem;
	public JMenuItem allSolutionsItem;
	public JMenuItem mergeAllTabsToThisWindow;
	
	public CloseableDndTabbedPane tabbedPane; // contains SudokuTabs
	
	public SudokuTab getSelectedTab()
	{
		return (SudokuTab) tabbedPane.getSelectedComponent();
	}

	/**
	 * @return Board of the selected SudokuTab
	 */
	public Board getCurrentBoard()
	{
		return getSelectedTab().board;
	}


	void initMenu()
	{
		initApplicationOptions();
		
		initNewOptions();
		initEditOptions();
		
		initImportOptions();
		initExportOptions();
		initPrintMenu();
		
		initViewOptions();
		initWindowOptions();
		
		menuBar = new JMenuBar();
		menuBar.add(application_options);
		menuBar.add(newOptions);
		menuBar.add(editOptions);
		menuBar.add(importOptions);
		menuBar.add(exportOptions);
		menuBar.add(printMenu);
		menuBar.add(viewOptionsMenu);
		menuBar.add(windowOptions);

		setJMenuBar(menuBar);
	}
	
	void initApplicationOptions()
	{
		application_options = new JMenu(Application.application_name);
		application_options.setMnemonic(Application.application_name.charAt(0));

		JMenuItem aboutApplication = new JMenuItem("About " + Application.application_name, 'A');
		aboutApplication.addActionListener(event ->
		{
			Application.getAboutDialog().showUp();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("aboutApplication", KeyboardSettingsPanel.getMenuItemString(Application.application_name, "About " + Application.application_name), false, 0, 0, null, aboutApplication, 0);
		
		preferences = new JMenuItem("\u2699 Preferences...", 'P');
		Application.keyboardSettingsPanel.registerMenuShortcut("showPreferenceFrame", KeyboardSettingsPanel.getMenuItemString(Application.application_name, "\u2699 Preferences..."), true, KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, preferences, 0);
		
		preferences.addActionListener(event ->
		{
			Application.preferenceFrame.showUp(null);
		});
		
		memoryMonitorItem = new JMenuItem("Application Memory Usage", 'M');
		memoryMonitorItem.addActionListener(event ->
		{
			if (Application.memoryMonitorFrame == null)
			{
				Application.memoryMonitorFrame = MemoryMonitorPanel.getMemoryMonitorFrame();
				Application.memoryMonitorFrame.setLocationByPlatform(true);
				GeneralSettingsPanel.registerComponentAndSetFontSize(Application.memoryMonitorFrame);
			}
			Application.memoryMonitorFrame.setVisible(true);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("memoryMonitorItem", KeyboardSettingsPanel.getMenuItemString(Application.application_name, "Application Memory Usage"), true, KeyEvent.VK_M, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, memoryMonitorItem, 0);

		JMenuItem exitApplication = new JMenuItem(Application.exitProgram);
		exitApplication.setMnemonic('E');

		// I don't want this to collide with Apple's command q shortcut, because here quitting application can be canceled by the user
		Application.keyboardSettingsPanel.registerMenuShortcut("exitApplication", KeyboardSettingsPanel.getMenuItemString(Application.application_name, "\u238B Exit Program"), true, KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK, Application.exitProgram, null, 0);

		application_options.add(aboutApplication);
		
		application_options.addSeparator();
		application_options.add(preferences);
		
		application_options.addSeparator();
		application_options.add(memoryMonitorItem);
		
		application_options.addSeparator();
		application_options.add(exitApplication);
	}
	
	AbstractAction newEmptyTab = new AbstractAction("Empty Board")
	{
		public void actionPerformed(ActionEvent event)
		{
			addNewBoard(false, new Sudoku(), "New Tab with Empty Board");
		}
	};
	
	AbstractAction newEmptyWindow = new AbstractAction("Empty Board")
	{
		public void actionPerformed(ActionEvent event)
		{
			addNewBoard(true, new Sudoku(), "New Window with Empty Board");
		}
	};
	
	AbstractAction newTabFromClone = new AbstractAction("Clone Current Board")
	{
		public void actionPerformed(ActionEvent event)
		{
			addNewBoard(false, getCurrentBoard().clone("New Tab from Cloning"));
		}
	};
	
	AbstractAction newWindowFromClone = new AbstractAction("Clone Current Board")
	{
		public void actionPerformed(ActionEvent event)
		{
			addNewBoard(true, getCurrentBoard().clone("New Window from Cloning"));
		}
	};
	
	AbstractAction newTabFromQuickSave = new AbstractAction("From Quick Save...")
	{
		public void actionPerformed(ActionEvent event)
		{
			SudokuTab selectedTab = getSelectedTab();
			QuickSaveSlot slot = (QuickSaveSlot) JOptionPane.showInputDialog(ApplicationFrame.this, "Select a save slot to create a new Board from:", "Quick Saves", JOptionPane.INFORMATION_MESSAGE, null, selectedTab.quickSaves.saves, 0);

			if (slot != null)
			{
				if (slot.isEmpty)
				{
					JOptionPane.showMessageDialog(ApplicationFrame.this, "The selected save slot is empty.", "Can't Proceed", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					SudokuTab tab = addNewBoard(false, new Sudoku(), "New Tab from Quick Save " + slot.name);
					if (tab != null) tab.board.setBoardData(slot.data);
				}
			}
		}
	};
	
	AbstractAction newWindowFromQuickSave = new AbstractAction("From Quick Save...")
	{
		public void actionPerformed(ActionEvent event)
		{
			SudokuTab selectedTab = getSelectedTab();
			QuickSaveSlot slot = (QuickSaveSlot) JOptionPane.showInputDialog(ApplicationFrame.this, "Select a save slot to create a new Board from:", "Quick Saves", JOptionPane.INFORMATION_MESSAGE, null, selectedTab.quickSaves.saves, 0);

			if (slot != null)
			{
				if (slot.isEmpty)
				{
					JOptionPane.showMessageDialog(ApplicationFrame.this, "The selected save slot is empty.", "Can't Proceed", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					SudokuTab tab = addNewBoard(true, new Sudoku(), "New Window from Quick Save " + slot.name);
					if (tab != null) tab.board.setBoardData(slot.data);
				}
			}
		}
	};
	
	AbstractAction newTabFromGenerate = new AbstractAction("Generate...")
	{
		public void actionPerformed(ActionEvent event)
		{
			Sudoku sudoku = Application.getGenerator().showGenerateDialog(ApplicationFrame.this);
			if (sudoku != null)
			{
				SudokuTab tab = Application.addTab(ApplicationFrame.this, sudoku, "New Tab from Generating a Puzzle with "+ Checker.solvedCellCount(sudoku) + " clues.");
				if (tab != null)
				{
					if (Application.generator.lockClues.isSelected())
					{
						for (int r = 0; r < 9; r++)
						{
							for (int c = 0; c < 9; c++)
							{
								if (tab.board.sudoku.status[r][c] > 0) tab.board.cellLocked[r][c] = true;
							}
						}
					}
				}
			}
		}
	};
	
	AbstractAction newWindowFromGenerate = new AbstractAction("Generate...")
	{
		public void actionPerformed(ActionEvent event)
		{
			Sudoku sudoku = Application.getGenerator().showGenerateDialog(ApplicationFrame.this);
			if (sudoku != null)
			{
				SudokuTab tab = Application.createNewWindowWithTab(ApplicationFrame.this, sudoku, "New Window from Generating a Puzzle with "+ Checker.solvedCellCount(sudoku) + " clues.");
				if (tab != null)
				{
					if (Application.generator.lockClues.isSelected())
					{
						for (int r = 0; r < 9; r++)
						{
							for (int c = 0; c < 9; c++)
							{
								if (tab.board.sudoku.status[r][c] > 0) tab.board.cellLocked[r][c] = true;
							}
						}
					}
					configureNewWindow(tab.owner);
				}
			}
		}
	};
	
	AbstractAction newTabFromString = new AbstractAction("From Text...")
	{
		public void actionPerformed(ActionEvent event)
		{
			Application.getImporter().newBoardFromString(ApplicationFrame.this, Board.NEW_TAB_FROM_STRING, false);
		}
	};
	
	AbstractAction newWindowFromString = new AbstractAction("From Text...")
	{
		public void actionPerformed(ActionEvent event)
		{
			SudokuTab tab = Application.getImporter().newBoardFromString(ApplicationFrame.this, Board.NEW_WINDOW_FROM_STRING, true);
			if (tab != null) configureNewWindow(tab.owner);
		}
	};
	
	AbstractAction newTabFromFile = new AbstractAction("From File...")
	{
		public void actionPerformed(ActionEvent event)
		{
			Application.getImporter().newBoardFromFile(ApplicationFrame.this, Board.NEW_TAB_FROM_FILE, false);
		}
	};
	
	AbstractAction newWindowFromFile = new AbstractAction("From File...")
	{
		public void actionPerformed(ActionEvent event)
		{
			SudokuTab tab = Application.getImporter().newBoardFromFile(ApplicationFrame.this, Board.NEW_WINDOW_FROM_FILE, true);
			if (tab != null) configureNewWindow(tab.owner);
		}
	};
	
	void initNewOptions()
	{
		newOptions = new JMenu("New");
		newOptions.setMnemonic('N');
		
		JMenu newTab = new JMenu("Tab");
		newTab.setMnemonic('T');
		JMenu newWindow = new JMenu("Window");
		newWindow.setMnemonic('W');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newEmptyTab", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "Empty"), true, KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), newEmptyTab, null, 0);
		newTab.add(newEmptyTab).setMnemonic('E');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newTabFromClone", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "Clone Current Board"), true, KeyEvent.VK_C, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, newTabFromClone, null, 0);
		newTab.add(newTabFromClone).setMnemonic('C');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newTabFromQuickSave", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "From Quick Save..."), true, KeyEvent.VK_L, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, newTabFromQuickSave, null, 0);
		newTab.add(newTabFromQuickSave).setMnemonic('Q');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newTabFromGenerate", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "Generate..."), true, KeyEvent.VK_G, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, newTabFromGenerate, null, 0);
		newTab.add(newTabFromGenerate).setMnemonic('G');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newTabFromString", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "From Text..."), true, KeyEvent.VK_I, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, newTabFromString, null, 0);
		newTab.add(newTabFromString).setMnemonic('S');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newTabFromFile", KeyboardSettingsPanel.getMenuItemString("New", "Tab", "From File..."), true, KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, newTabFromFile, null, 0);
		JMenuItem tmp = newTab.add(newTabFromFile);
		tmp.setMnemonic('F');
		tmp.setDisplayedMnemonicIndex(5);

		Application.keyboardSettingsPanel.registerMenuShortcut("newEmptyWindow", KeyboardSettingsPanel.getMenuItemString("New", "Window", "Empty"), true, KeyEvent.VK_N, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, newEmptyWindow, null, 0);
		newWindow.add(newEmptyWindow).setMnemonic('E');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newWindowFromClone", KeyboardSettingsPanel.getMenuItemString("New", "Window", "Clone Current Board"), false, 0,0, newWindowFromClone, null, 0);
		newWindow.add(newWindowFromClone).setMnemonic('C');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newWindowFromQuickSave", KeyboardSettingsPanel.getMenuItemString("New", "Window", "From Quick Save..."), false, 0,0, newWindowFromQuickSave, null, 0);
		newWindow.add(newWindowFromQuickSave).setMnemonic('Q');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newWindowFromGenerate", KeyboardSettingsPanel.getMenuItemString("New", "Window", "Generate..."), false, 0,0, newWindowFromGenerate, null, 0);
		newWindow.add(newWindowFromGenerate).setMnemonic('G');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newWindowFromString", KeyboardSettingsPanel.getMenuItemString("New", "Window", "From Text..."), false, 0,0, newWindowFromString, null, 0);
		newWindow.add(newWindowFromString).setMnemonic('S');
		
		Application.keyboardSettingsPanel.registerMenuShortcut("newWindowFromFile", KeyboardSettingsPanel.getMenuItemString("New", "Window", "From File..."), false, 0,0, newWindowFromFile, null, 0);
		tmp = newWindow.add(newWindowFromFile);
		tmp.setMnemonic('F');
		tmp.setDisplayedMnemonicIndex(5);

		newOptions.add(newTab);
		newOptions.add(newWindow);
	}
	
	public static int getKeyCodeForDigit(int d)
	{
		int keyCode;
		switch (d) 
		{
			case 1: keyCode = KeyEvent.VK_1;
				break;
			case 2: keyCode = KeyEvent.VK_2;
				break;
			case 3: keyCode = KeyEvent.VK_3;
				break;
			case 4: keyCode = KeyEvent.VK_4;
				break;
			case 5: keyCode = KeyEvent.VK_5;
				break;
			case 6: keyCode = KeyEvent.VK_6;
				break;
			case 7: keyCode = KeyEvent.VK_7;
				break;
			case 8: keyCode = KeyEvent.VK_8;
				break;
			case 9: keyCode = KeyEvent.VK_9;
				break;
			default:
				keyCode = KeyEvent.VK_0;
		}
		return keyCode;
	}
	
	JMenuItem makeQuickSaveSlotItem(int s)
	{
		JMenuItem item = new JMenuItem("To Slot " + s);
		int keyCode = getKeyCodeForDigit(s);
		item.setMnemonic(keyCode);
		item.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			selectedTab.quickSaves.saveToSlot(new BoardData(selectedTab.board), s - 1);
		});
		
		Application.keyboardSettingsPanel.registerMenuShortcut("quickSaveSlot" + s, KeyboardSettingsPanel.getMenuItemString("Edit", "Quick Save", "To Slot " + s), true, keyCode, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, item, 0);
		return item;
	}
	
	JMenuItem makeQuickLoadSlotItem(int s)
	{
		JMenuItem item = new JMenuItem("From Slot " + s);
		int keyCode = getKeyCodeForDigit(s);
		item.setMnemonic(keyCode);
		item.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			selectedTab.quickSaves.loadFromSlot(s - 1);
		});
		
		Application.keyboardSettingsPanel.registerMenuShortcut("quickLoadSlot" + s, KeyboardSettingsPanel.getMenuItemString("Edit", "Quick Load", "From Slot " + s), true, keyCode, InputEvent.META_DOWN_MASK, null, item, 0);
		return item;
	}
	
	void initEditOptions()
	{
		editOptions = new JMenu("Edit");
		editOptions.setMnemonic('E');
		
		// dummy values for now
		undo = new JMenuItem("Undo", 'U');
		// so that this item appears in the correct order in KeyboardSettingsPanel
		Application.keyboardSettingsPanel.registerMenuShortcut("undo", KeyboardSettingsPanel.getMenuItemString("Edit", "Undo"), true, KeyEvent.VK_Z, InputEvent.META_DOWN_MASK, null, null, 0);
		
		redo = new JMenuItem("Redo", 'R');
		// so that this item appears in the correct order in KeyboardSettingsPanel
		Application.keyboardSettingsPanel.registerMenuShortcut("redo", KeyboardSettingsPanel.getMenuItemString("Edit", "Redo"), true, KeyEvent.VK_Z, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, null, 0);
		
		seeHistoryTreeForThisTab = new JMenuItem("See History Tree For This Tab", 'H');
		seeHistoryTreeForThisTab.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			historyTrees.setSelectedComponent(selectedTab.historyTreePanel);
			Application.historyTreeFrame.windows.setSelectedComponent(historyTrees);
			Application.historyTreeFrame.setVisible(true);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("seeHistory", KeyboardSettingsPanel.getMenuItemString("Edit", "See History Tree For This Tab"), true, KeyEvent.VK_H, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, seeHistoryTreeForThisTab, 0);

		editOptions.add(undo);
		editOptions.add(redo);
		editOptions.add(seeHistoryTreeForThisTab);
		editOptions.addSeparator();

		quickSave = new AbstractAction("See Slots...")
		{
			public void actionPerformed(ActionEvent event)
			{
				SudokuTab selectedTab = getSelectedTab();
				QuickSaveSlot slot = (QuickSaveSlot) JOptionPane.showInputDialog(ApplicationFrame.this, "Select a save slot to save to: ", "Quick Save", JOptionPane.INFORMATION_MESSAGE, null, selectedTab.quickSaves.saves, 0);
				if (slot != null)
				{
					selectedTab.quickSaves.saveToSlot(new BoardData(selectedTab.board), slot);
				}
			}
		};
		Application.keyboardSettingsPanel.registerMenuShortcut("quickSave", KeyboardSettingsPanel.getMenuItemString("Edit", "Quick Save", "See Slots..."), true, KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), quickSave, null, 0);
		
		JMenu quickSaveMenu = new JMenu("Quick Save");
		quickSaveMenu.setMnemonic('S');
		quickSaveMenu.add(quickSave).setMnemonic('S');
		quickSaveMenu.addSeparator();
		
		for (int i = 1; i < 11; i++)
		{
			quickSaveMenu.add(makeQuickSaveSlotItem(i));
		}

		quickLoad = new AbstractAction("See Slots...")
		{
			public void actionPerformed(ActionEvent event)
			{
				SudokuTab selectedTab = getSelectedTab();
				QuickSaveSlot slot = (QuickSaveSlot) JOptionPane.showInputDialog(ApplicationFrame.this, "Select a save slot to load from: ", "Quick Load", JOptionPane.INFORMATION_MESSAGE, null, selectedTab.quickSaves.saves, 0);
				if (slot != null)
				{
					selectedTab.quickSaves.loadFromSlot(slot);
				}
			}
		};
		Application.keyboardSettingsPanel.registerMenuShortcut("quickLoad", KeyboardSettingsPanel.getMenuItemString("Edit", "Quick Load", "See Slots..."), true, KeyEvent.VK_L, InputEvent.META_DOWN_MASK, quickLoad, null, 0);

		JMenu quickLoadMenu = new JMenu("Quick Load");
		quickLoadMenu.setMnemonic('L');
		quickLoadMenu.add(quickLoad).setMnemonic('S');
		quickLoadMenu.addSeparator();

		// for clearer key bindings table
		for (int i = 1; i < 11; i++)
		{
			quickLoadMenu.add(makeQuickLoadSlotItem(i));
		}
		
		editOptions.add(quickSaveMenu);
		editOptions.add(quickLoadMenu);
		editOptions.addSeparator();
		
		JMenuItem lockAllSolvedCells = new JMenuItem("Lock All Cells with only 1 Candidate", 'L');
		lockAllSolvedCells.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					if (board.sudoku.status[r][c] > 0) board.cellLocked[r][c] = true;
				}
			}
			board.counter.calculateCounts();
			board.cellEditor.setEnabled(board.selectedCell != null);
			board.repaint();
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Locked All Cells with only 1 Candidate for Board", EditType.MASS_LOCK_CHANGE, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("lockAllSolvedCells", KeyboardSettingsPanel.getMenuItemString("Edit", "Lock All Cells with only 1 Candidate"), true, KeyEvent.VK_S, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, lockAllSolvedCells, 0);
		
		JMenuItem unlockAllCells = new JMenuItem("Unlock All Cells", 'U');
		unlockAllCells.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					board.cellEditor.clearLocks(board.cells[r][c]);
				}
			}
			board.counter.setLockedCount(0);
			board.cellEditor.setEnabled(board.selectedCell != null);
			board.repaint();
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Unlocked All Cells for Board", EditType.MASS_LOCK_CHANGE, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("unlockAllCells", KeyboardSettingsPanel.getMenuItemString("Edit", "Unlocked All Cells for Board"), true, KeyEvent.VK_U, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, unlockAllCells, 0);
		
		editOptions.add(lockAllSolvedCells);
		editOptions.add(unlockAllCells);
		editOptions.addSeparator();
		
		JMenuItem clearBoard = new JMenuItem("Clear this Board Entirely", 'C');
		clearBoard.setToolTipText("Clear all Candidates, Pencil Marks and Notes, and Unlock All Cells");
		clearBoard.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					board.cellEditor.clearCandidates(board.cells[r][c]);
					board.cellEditor.clearPencilMarks(board.cells[r][c]);
					board.cellEditor.clearNotes(board.cells[r][c]);
					board.cellEditor.clearLocks(board.cells[r][c]);
				}
			}
			board.counter.setCandidateCount(729);
			board.counter.setSolvedCount(0);
			board.counter.setLockedCount(0);
			board.cellEditor.setEnabled(board.selectedCell != null);
			board.repaint();
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Cleared all Candidates, Pencil Marks and Notes, and Unlocked All Cells for Board", EditType.CLEAR, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("clearBoard", KeyboardSettingsPanel.getMenuItemString("Edit", "Clear this Board Entirely"), true, KeyEvent.VK_BACK_SPACE, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, clearBoard, 0);
		
		JMenuItem clearCandidates = new JMenuItem("Clear All Candidates of this Board", 'C');
		clearCandidates.setDisplayedMnemonicIndex(10);
		clearCandidates.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					board.cellEditor.clearCandidates(board.cells[r][c]);
				}
			}
			board.counter.setCandidateCount(729);
			board.counter.setSolvedCount(0);
			board.cellEditor.setEnabled(board.selectedCell != null);
			board.repaint();
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Cleared All Candidates for Board", EditType.CLEAR, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("clearCandidates", KeyboardSettingsPanel.getMenuItemString("Edit", "Clear All Candidates of this Board"), true, KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, clearCandidates, 0);
		
		JMenuItem clearPencilMarks = new JMenuItem("Clear All Pencil Marks of this Board", 'P');
		clearPencilMarks.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					board.cellEditor.clearPencilMarks(board.cells[r][c]);
				}
			}
			board.cellEditor.setEnabled(board.selectedCell != null);
			board.repaint();
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Cleared All Pencil Marks of Board", EditType.CLEAR, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("clearPencilMarks", KeyboardSettingsPanel.getMenuItemString("Edit", "Clear All Pencil Marks of this Board"), false, 0, 0, null, clearPencilMarks, 0);
		
		JMenuItem clearNotes = new JMenuItem("Clear All Notes of this Board", 'N');
		clearNotes.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			Board board = selectedTab.board;
			board.cellEditor.endEdit();
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					board.cellEditor.clearNotes(board.cells[r][c]);
				}
			}
			board.cellEditor.setEnabled(board.selectedCell != null);
			selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Cleared All Notes of Board", EditType.CLEAR, board));
			board.cellEditor.startEditIfCellSelected();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("clearNotes", KeyboardSettingsPanel.getMenuItemString("Edit", "Clear All Notes of this Board"), false, 0, 0, null, clearNotes, 0);

		JMenuItem cloneAnotherBoard = new JMenuItem("Clone Another Board...", 'C');
		cloneAnotherBoard.addActionListener(event ->
		{
			SudokuTab tab = Application.openWindowsAndTabs.showTabChooserDialog(ApplicationFrame.this);
			SudokuTab selectedTab = getSelectedTab();
			if (tab != null && tab != selectedTab)
			{
				selectedTab.board.cellEditor.endEdit();
				selectedTab.board.setBoardData(new BoardData(tab.board));
				selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Cloned Data from " + tab.owner.getTitle() + ", " +  tab.getName(), EditType.CLONE, selectedTab.board));
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("cloneAnotherBoard", KeyboardSettingsPanel.getMenuItemString("Edit", "Clone Another Board..."), false, 0, 0, null, cloneAnotherBoard, 0);

		compareThisBoardWithAnother = new JMenuItem("Compare this Board with Another...", 'C');
		compareThisBoardWithAnother.addActionListener(event ->
		{
			SudokuTab tab = Application.openWindowsAndTabs.showTabChooserDialog(ApplicationFrame.this);
			if (tab != null)
			{
				BoardComparator newComparator = Application.getBoardComparatorFrame().makeNewComparator();
				newComparator.board1.setChosenTab(getSelectedTab());
				newComparator.board2.setChosenTab(tab);
				Application.getBoardComparatorFrame().linkBoard1ToTree.setEnabled(true);
				Application.getBoardComparatorFrame().linkBoard2ToTree.setEnabled(true);
				Application.getBoardComparatorFrame().setVisible(true);
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("compareBoardWithOther", KeyboardSettingsPanel.getMenuItemString("Edit", "Compare this Board with Another..."), true, KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, compareThisBoardWithAnother, 0);

		editOptions.add(clearBoard);
		editOptions.add(clearCandidates);
		editOptions.add(clearPencilMarks);
		editOptions.add(clearNotes);

		editOptions.addSeparator();
		editOptions.add(cloneAnotherBoard);

		editOptions.addSeparator();
		editOptions.add(compareThisBoardWithAnother);
	}
	
	void initImportOptions()
	{
		importOptions = new JMenu("Import");
		importOptions.setMnemonic('I');

		JMenuItem generate = new JMenuItem("Generate...", 'G');
		generate.addActionListener(event ->
		{
			Sudoku sudoku = Application.getGenerator().showGenerateDialog(ApplicationFrame.this);
			if (sudoku != null)
			{
				SudokuTab selectedTab = getSelectedTab();
				selectedTab.board.cellEditor.endEdit();
				selectedTab.board.setSudoku(sudoku);
				selectedTab.historyTreePanel.historyTree.addNodeForEdit(new Edit("Generated a Puzzle with " + Checker.solvedCellCount(sudoku) + " clues.", EditType.GENERATE, selectedTab.board));
				if (Application.miscellaneousSettingsPanel.restartTimerUponImport.isSelected()) selectedTab.stopwatch.restart();
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("generate", KeyboardSettingsPanel.getMenuItemString("Import", "Generate..."), true, KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, generate, 0);
		
		JMenuItem fromString = new JMenuItem("From Text...", 'T');
		fromString.addActionListener(event ->
		{
			Application.getImporter().importFromString(getSelectedTab().board, ApplicationFrame.this);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("importString", KeyboardSettingsPanel.getMenuItemString("Import", "From Text..."), true, KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, fromString, 0);
		
		JMenuItem fromFile = new JMenuItem("From File...", 'F');
		fromFile.addActionListener(event ->
		{
			Application.getImporter().importFromFile(getSelectedTab().board, ApplicationFrame.this);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("importFile", KeyboardSettingsPanel.getMenuItemString("Import", "From File..."), true, KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, fromFile, 0);
		fromFile.setDisplayedMnemonicIndex(5);
		
		importOptions.add(generate);
		importOptions.add(fromString);
		importOptions.add(fromFile);
	}
	
	void initExportOptions()
	{
		exportOptions = new JMenu("Export");
		exportOptions.setMnemonic('X');
		
		JMenuItem showExportOptions = new JMenuItem("Show Export Options...", 'S');
		showExportOptions.addActionListener(event ->
		{
			Application.getExporter().showDialog(getSelectedTab().board);
		});
		
		Application.keyboardSettingsPanel.registerMenuShortcut("exportOptions", KeyboardSettingsPanel.getMenuItemString("Export", "Show Export Options..."), true, KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, showExportOptions, 0);

		exportOptions.add(showExportOptions);
	}
	
	HashPrintRequestAttributeSet attributes;
	void initPrintMenu()
	{
		printMenu = new JMenu("Print");
		printMenu.setMnemonic('P');
		
		JMenuItem printCurrentBoard = new JMenuItem("Print Current Board...", 'P');
		
		printCurrentBoard.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			try 
			{
				PrinterJob job = PrinterJob.getPrinterJob();
				job.setPrintable(selectedTab.board);
				
				if (attributes == null) attributes = new HashPrintRequestAttributeSet();
				if (job.printDialog(attributes))
				{
					job.print(attributes);
				}
			}
			catch (PrinterException e)
			{
				JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE, null);
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("printCurrentBoard", KeyboardSettingsPanel.getMenuItemString("Print", "Print Current Board..."), true, KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, printCurrentBoard, 0);
		printMenu.add(printCurrentBoard);
	}
	
	void initViewOptions()
	{
		viewOptionsMenu = new JMenu("View");
		viewOptionsMenu.setMnemonic('V');
		//263E
		JMenuItem enterDarkMode = new JMenuItem("\u263E Enter Dark Mode", 'D');
		enterDarkMode.setToolTipText("By Changing the Look And Feel");
		enterDarkMode.addActionListener(event ->
		{	
			Application.generalSettingsPanel.metalThemeCombo.setSelectedItem("DarkMetal");
			Application.generalSettingsPanel.lookAndFeelCombo.setSelectedItem("Metal");
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("enterDarkMode", KeyboardSettingsPanel.getMenuItemString("View", "\u263E Enter Dark Mode"), true, KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, enterDarkMode, 0);
		
		JMenuItem minimize = new JMenuItem("Minimize this Window", 'M');
		JMenuItem maximize = new JMenuItem("Maximize this Window", 'M');
		JMenuItem setSizeToNormal = new JMenuItem("Set Size to Normal", 'N');
		setSizeToNormal.setToolTipText("If it's currently maximized");
		JMenuItem resizeToFit = new JMenuItem("Resize this Window to Preferred Size", 'R');
		JMenuItem centerWindow = new JMenuItem("Center this Window", 'C');
		
		maximize.addActionListener(event -> 
		{
			setExtendedState(JFrame.MAXIMIZED_BOTH);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("maximize", KeyboardSettingsPanel.getMenuItemString("View", "Maximize this Window"), false, 0, 0, null, maximize, 0);
		
		minimize.addActionListener(event -> 
		{
			setExtendedState(JFrame.ICONIFIED);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("minimize", KeyboardSettingsPanel.getMenuItemString("View", "Minimize this Window"), false, 0, 0, null, minimize, 0);
		
		setSizeToNormal.addActionListener(event -> 
		{
			setExtendedState(JFrame.NORMAL);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("setSizeToNormal", KeyboardSettingsPanel.getMenuItemString("View", "Set Size to Normal"), false, 0, 0, null, setSizeToNormal, 0);
		
		resizeToFit.addActionListener(event -> 
		{
			pack();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("resizeToFit", KeyboardSettingsPanel.getMenuItemString("View", "Resize this Window to Preferred Size"), false, 0, 0, null, resizeToFit, 0);
		
		centerWindow.addActionListener(event ->
		{
			setLocationRelativeTo(null);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("centerWindow", KeyboardSettingsPanel.getMenuItemString("View", "Center this Window"), false, 0, 0, null, centerWindow, 0);
		
		JMenuItem bringAllToFront = new JMenuItem("Bring All Windows To Front", 'F');
		bringAllToFront.addActionListener(event ->
		{
			for (ApplicationFrame frame : Application.openWindows)
			{
				frame.toFront();
			}

			if (Application.allSolutionsFrame != null) Application.allSolutionsFrame.toFront();
			if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.toFront();
			Application.historyTreeFrame.toFront();
			Application.preferenceFrame.toFront();
			if (Application.memoryMonitorFrame != null) Application.memoryMonitorFrame.toFront();
			if (Application.musicPlayerFrame != null) Application.musicPlayerFrame.toFront();
			if (Application.aboutDialog != null) Application.aboutDialog.toFront();
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("bringAllToFront", KeyboardSettingsPanel.getMenuItemString("View", "Bring All Windows To Front"), false, 0, 0, null, bringAllToFront, 0);
		
		viewOptionsMenu.add(enterDarkMode);
		
		viewOptionsMenu.addSeparator();
		
		viewOptionsMenu.add(maximize);
		viewOptionsMenu.add(minimize);
		viewOptionsMenu.add(setSizeToNormal);
		viewOptionsMenu.add(resizeToFit);
		viewOptionsMenu.add(centerWindow);
		
		viewOptionsMenu.addSeparator();
		
		viewOptionsMenu.add(bringAllToFront);
	}
	
	void initWindowOptions()
	{
		windowOptions = new JMenu("Window");
		windowOptions.setMnemonic('W');

		JMenuItem renameTab = new JMenuItem("Rename This Tab", 'R');
		renameTab.addActionListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			String newName = Application.getNameForTab(this, selectedTab.getName());
			if (newName != null)
			{
				selectedTab.setName(newName);
				selectedTab.historyTreePanel.setName(selectedTab.getName());
				int selectedIndex = tabbedPane.getSelectedIndex();
				((CloseableDndTabbedPane.TabComponent) tabbedPane.getTabComponentAt(selectedIndex)).setTitle(selectedTab.getName());
				historyTrees.setTitleAt(historyTrees.indexOfComponent(selectedTab.historyTreePanel), selectedTab.getName());
				Application.openWindowsAndTabs.windowChanged(ApplicationFrame.this);
				if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.updateChosenBoardInfos();
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("renameTab", KeyboardSettingsPanel.getMenuItemString("Window", "Rename This Tab"), true, KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, renameTab, 0);

		JMenuItem renameWindow = new JMenuItem("Rename This Window", 'R');
		renameWindow.addActionListener(event ->
		{
			String newName = Application.getNameForWindow(this, this.getTitle());
			if (newName != null)
			{
				setTitle(newName);
				historyTrees.setName(getTitle());
				Application.historyTreeFrame.windows.setTitleAt(Application.historyTreeFrame.windows.indexOfComponent(historyTrees), getTitle());
				Application.openWindowsAndTabs.windowChanged(ApplicationFrame.this);
				if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.updateChosenBoardInfos();
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("renameWindow", KeyboardSettingsPanel.getMenuItemString("Window", "Rename This Window"), true, KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, null, renameWindow, 0);

		JMenuItem closeThisTab = new JMenuItem("Close This Tab...", 'T');
		closeThisTab.setDisplayedMnemonicIndex(11);
		closeThisTab.addActionListener(event ->
		{
			int result = JOptionPane.showOptionDialog(ApplicationFrame.this, "Do you want to save data for this tab before closing it?", "Closing Tab", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Yes", "No", "Cancel"}, "Cancel");
			if (result == 0 || result == 1) // yes or no
			{
				Application.closeTab(getSelectedTab(), result == 0);
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("closeThisTab", KeyboardSettingsPanel.getMenuItemString("Window", "Close This Tab"), false, 0, 0, null, closeThisTab, 0);


		JMenuItem closeThisWindow = new JMenuItem("Close This Window...", 'W');
		closeThisWindow.addActionListener(event ->
		{
			Application.closeWindow(ApplicationFrame.this, true, false);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("closeThisWindow", KeyboardSettingsPanel.getMenuItemString("Window", "Close This Window"), false, 0, 0, null, closeThisWindow, 0);

		JMenuItem moveTabToAnotherWindow = new JMenuItem("Move This Tab to Another Window...", 'M');
		moveTabToAnotherWindow.addActionListener(event ->
		{
			ApplicationFrame targetFrame = Application.openWindowsAndTabs.showWindowChooserDialog(ApplicationFrame.this);
			
			if (targetFrame != null && targetFrame != ApplicationFrame.this)
			{
				SudokuTab selectedTab = getSelectedTab();
				int selectedIndex = tabbedPane.getSelectedIndex();
				targetFrame.tabbedPane.performDnd(new CloseableDndTabbedPane.TabData(selectedTab, tabbedPane, selectedTab.getName(), tabbedPane.getToolTipTextAt(selectedIndex), selectedIndex), targetFrame.tabbedPane.getTabCount(), null);
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("moveTabToAnotherWindow", KeyboardSettingsPanel.getMenuItemString("Window", "Move This Tab to Another Window..."), true, KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK, null, moveTabToAnotherWindow, 0);
		
		JMenuItem moveTabToNewWindow = new JMenuItem("Move This Tab to a New Window...", 'M');
		moveTabToNewWindow.addActionListener(event ->
		{
			if (tabbedPane.getTabCount() == 1)
			{
				JOptionPane.showMessageDialog(ApplicationFrame.this, "This window only has 1 tab.", "Cannot Proceed", JOptionPane.INFORMATION_MESSAGE);
			}
			else 
			{		
				ApplicationFrame newFrame = Application.createNewWindow(ApplicationFrame.this);

				if (newFrame != null)
				{
					SudokuTab selectedTab = getSelectedTab();
					tabbedPane.remove(tabbedPane.getSelectedIndex());
					historyTrees.remove(selectedTab.historyTreePanel);
					Application.openWindowsAndTabs.windowChanged(ApplicationFrame.this);

					newFrame.tabbedPane.add(selectedTab);
					selectedTab.owner = newFrame;
					newFrame.historyTrees.addTab(selectedTab.getName(), null, selectedTab.historyTreePanel, "History Tree for this Board");
					Application.openWindowsAndTabs.addWindow(newFrame); // refreshes the tree

					configureNewWindow(newFrame);
					if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.updateChosenBoardInfos();
				}
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("moveTabToNewWindow", KeyboardSettingsPanel.getMenuItemString("Window", "Move This Tab to a New Window..."), true, KeyEvent.VK_W, InputEvent.META_DOWN_MASK, null, moveTabToNewWindow, 0);
		
		mergeAllTabsToThisWindow = new JMenuItem("Merge All Tabs To This Window", 'M');
		mergeAllTabsToThisWindow.addActionListener(event ->
		{
			assert Application.openWindows.size() > 0;
			if (Application.openWindows.size() == 1)
			{
				JOptionPane.showMessageDialog(ApplicationFrame.this, "There is only 1 open window.", "Cannot Proceed", JOptionPane.INFORMATION_MESSAGE);
			}
			else 
			{
				for (ApplicationFrame frame : Application.openWindows)
				{
					if (frame != ApplicationFrame.this)
					{
						SudokuTab removedTab;
						
						while (frame.tabbedPane.getTabCount() > 0)
						{
							removedTab = (SudokuTab) frame.tabbedPane.getComponentAt(0);
							frame.tabbedPane.remove(0);
							
							historyTrees.addTab(removedTab.getName(), null, removedTab.historyTreePanel, "History Tree for this Board");
							tabbedPane.add(removedTab);
							removedTab.owner = ApplicationFrame.this;
						}
						Application.closeWindow(frame, true, false);
					}
				}
				Application.openWindowsAndTabs.rootChanged();
				if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.updateChosenBoardInfos();
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("mergeAllTabsToThisWindow", KeyboardSettingsPanel.getMenuItemString("Window", "Merge All Tabs To This Window"), true, KeyEvent.VK_W, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, mergeAllTabsToThisWindow, 0);

		musicPlayerItem = new JMenuItem("\u266b Music Player", 'M');
		musicPlayerItem.addActionListener(event ->
		{
			Application.getMusicPlayerFrame().setVisible(true);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("musicPlayer", KeyboardSettingsPanel.getMenuItemString("Window", "\u266b Music Player"), true, KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, musicPlayerItem, 0);


		historyTreeItem = new JMenuItem("History Trees", 'H');
		historyTreeItem.addActionListener(event ->
		{
			Application.historyTreeFrame.setVisible(true);
		});
		// shift command h is "see history tree for this tab", command h collides with Mac's hide application
		Application.keyboardSettingsPanel.registerMenuShortcut("historyTreeItem", KeyboardSettingsPanel.getMenuItemString("Window", "History Trees"), true, KeyEvent.VK_H, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, null, historyTreeItem, 0);
		
		boardComparatorItem = new JMenuItem("Board Comparator", 'B');
		boardComparatorItem.addActionListener(event ->
		{
			Application.getBoardComparatorFrame().setVisible(true);
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("boardComparatorItem", KeyboardSettingsPanel.getMenuItemString("Window", "Board Comparator"), true, KeyEvent.VK_B, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, null, boardComparatorItem, 0);

		allSolutionsItem = new JMenuItem("All Solutions Tables", 'A');
		allSolutionsItem.addActionListener(event ->
		{
			if (Application.allSolutionsFrame == null || Application.allSolutionsFrame.getSelectedTab() == null)
			{
				JOptionPane.showMessageDialog(ApplicationFrame.this, "There is currently no All Solutions Table. To use this item, click \"Solution Count\" of a Board's Solver.");
			}
			else
			{
				Application.allSolutionsFrame.setVisible(true);
			}
		});
		Application.keyboardSettingsPanel.registerMenuShortcut("allSolutionsItem", KeyboardSettingsPanel.getMenuItemString("Window", "All Solutions Tables"), true, KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, allSolutionsItem, 0);


		windowOptions.add(renameTab);
		windowOptions.add(renameWindow);
		windowOptions.addSeparator();

		windowOptions.add(closeThisTab);
		windowOptions.add(closeThisWindow);
		windowOptions.addSeparator();
		
		windowOptions.add(moveTabToAnotherWindow);
		windowOptions.add(moveTabToNewWindow);
		windowOptions.add(mergeAllTabsToThisWindow);
		windowOptions.addSeparator();

		windowOptions.add(musicPlayerItem);
		windowOptions.add(historyTreeItem);
		windowOptions.add(boardComparatorItem);
		windowOptions.add(allSolutionsItem);
	}
	
	/**
	 * @param createNewWindow if true, create Board in new ApplicationFrame, else create a new tab
	 */
	public SudokuTab addNewBoard(boolean createNewWindow, Sudoku sudoku, String creationEventDescription)
	{
		if (createNewWindow)
		{
			SudokuTab tab = Application.createNewWindowWithTab(ApplicationFrame.this, sudoku, creationEventDescription);
			if (tab != null) configureNewWindow(tab.owner);
			return tab;
		}
		else 
		{
			// can be null
			return Application.addTab(ApplicationFrame.this, sudoku, creationEventDescription);
		}
	}

	/**
	 * @param createNewWindow if true, add Board in new ApplicationFrame, else create a new tab
	 */
	public SudokuTab addNewBoard(boolean createNewWindow, Board board)
	{
		if (createNewWindow)
		{
			SudokuTab tab = Application.createNewWindowWithTab(ApplicationFrame.this, board);
			if (tab != null) configureNewWindow(tab.owner);
			return tab;
		}
		else 
		{
			// can be null
			return Application.addTab(ApplicationFrame.this, board);
		}
	}

	void configureNewWindow(ApplicationFrame frame)
	{
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}
	
	/**
	 * Things don't fit on the screen if you call JFrame.setDefaultLookAndFeelDecorated(true) 
	 * When the look and feel is Metal
	 */
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets i = getToolkit().getScreenInsets(getGraphicsConfiguration());
		int insetsWidth = i.left + i.right;
		int insetsHeight = i.top + i.bottom;
		return new Dimension(d.width + insetsWidth > screenSize.width ? screenSize.width - insetsWidth : d.width, d.height + insetsHeight > screenSize.height ? screenSize.height - insetsHeight : d.height);
	}
	
	public ApplicationFrame()
	{
		tabbedPane = new CloseableDndTabbedPane(this);
		tabbedPane.addChangeListener(event ->
		{
			SudokuTab selectedTab = getSelectedTab();
			if (selectedTab != null)
			{
				undo.setAction(selectedTab.historyTreePanel.historyTree.undo);
				redo.setAction(selectedTab.historyTreePanel.historyTree.redo);
			}
		});
		scrollPane = new JScrollPane(tabbedPane);
		add(scrollPane);
		
		initMenu();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			/**
			 * Called when the user tries to close the window from the window's System menu
			 */
			@Override
			public void windowClosing(WindowEvent event)
			{
				Application.closeWindow(ApplicationFrame.this, true, false);
			}
		});
		
		GeneralSettingsPanel.registerComponentAndSetFontSize(ApplicationFrame.this);
		pack();
	}
	
	@Override
	public String toString()
	{
		return getTitle();
	}
}
			