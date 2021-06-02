package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.Board.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.history.HistoryTree.*;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class can compare the differences of two boards
 * @since 4-16-2021
 * Started working on 5-5-2021
 */
@SuppressWarnings("CanBeFinal")
public class BoardComparator extends JPanel
{	
	/**
	 * Compare notes of one cell of the two boards
	 */
	void computeDifferencesInNotes(int r, int c)
	{
		String notes1 = board1.board.notes[r][c];
		String notes2 = board2.board.notes[r][c];
		
		if (notes1.length() == 0 && notes2.length() > 0)
		{
			board1.board.note_differences[r][c] = Board.DOES_NOT_HAVE;
			board2.board.note_differences[r][c] = Board.HAS_UNIQUE;
		}
		else if (notes1.length() > 0 && notes2.length() == 0)
		{
			board1.board.note_differences[r][c] = Board.HAS_UNIQUE;
			board2.board.note_differences[r][c] = Board.DOES_NOT_HAVE;
		}
		else if (notes1.equals(notes2))
		{
			board1.board.note_differences[r][c] = Board.SAME;
			board2.board.note_differences[r][c] = Board.SAME;
		}
		else 
		{
			board1.board.note_differences[r][c] = Board.DIFFERENT;
			board2.board.note_differences[r][c] = Board.DIFFERENT;
		}
	}
	
	/**
	 * Compare 1. solved value or 2. pencil marks
	 * of one cell of the two boards
	 * Currently this needs to be called after computeDifferencesInCandidates.
	 */
	void computeDifferencesInPencilMarks(int r, int c)
	{
		int status1 = board1.board.sudoku.status[r][c];
		int status2 = board2.board.sudoku.status[r][c];
		
		// first compare solved values
		if (status1 > 0 ^ status2>= 0) // one cell is solved, the other isn't
		{
			board1.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
			board2.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
		}
		else if (status1 > 0 && status2 > 0) // two solved values are different
		{
			if (status1 != status2)
			{
				board1.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
				board2.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
			}
			else 
			{
				board1.board.pencil_mark_differences[r][c] = Board.SAME;
				board2.board.pencil_mark_differences[r][c] = Board.SAME;
			}
		}
		else // compare pencil marks
		{
			String pencilMarks1 = board1.board.pencilMarks[r][c];
			String pencilMarks2 = board2.board.pencilMarks[r][c];
			
			if (pencilMarks1.length() > 0 && pencilMarks2.length() == 0)
			{
				board1.board.pencil_mark_differences[r][c] = Board.HAS_UNIQUE;
				board2.board.pencil_mark_differences[r][c] = Board.DOES_NOT_HAVE;
			}
			else if (pencilMarks1.length() == 0 && pencilMarks2.length() > 0)
			{
				board1.board.pencil_mark_differences[r][c] = Board.DOES_NOT_HAVE;
				board2.board.pencil_mark_differences[r][c] = Board.HAS_UNIQUE;
			}
			else if (pencilMarks1.equals(pencilMarks2))
			{
				board1.board.pencil_mark_differences[r][c] = Board.SAME;
				board2.board.pencil_mark_differences[r][c] = Board.SAME;
			}
			else 
			{
				board1.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
				board2.board.pencil_mark_differences[r][c] = Board.DIFFERENT;
			}
		}
	}
	
	/**
	 * Compare candidates of one cell of the two boards
	 */
	void computeDifferencesInCandidates(int r, int c)
	{
		int status1 = board1.board.sudoku.status[r][c];
		int status2 = board2.board.sudoku.status[r][c];
		
		if (status1 > 0 && status2 > 0) // if both cells are solved
		{
			if (status1 != status2) // wo solved values are different
			{
				for (int n = 0; n < 9; n++)
				{
					board1.board.candidate_differences[r][c][n] = Board.SAME;
					board2.board.candidate_differences[r][c][n] = Board.SAME;
				}
				
				board1.board.candidate_differences[r][c][status1 - 1] = Board.DIFFERENT;
				board2.board.candidate_differences[r][c][status2 - 1] = Board.DIFFERENT;
			}
			else 
			{
				for (int n = 0; n < 9; n++)
				{
					board1.board.candidate_differences[r][c][n] = Board.SAME;
					board2.board.candidate_differences[r][c][n] = Board.SAME;
				}
			}
		}
		else // compare candidates
		{	
			int[] cell1 = board1.board.sudoku.grid[r][c];
			int[] cell2 = board2.board.sudoku.grid[r][c];
			
			for (int n = 0; n < 9; n++)
			{
				if (cell1[n] > 0 && cell2[n] == 0)
				{
					board1.board.candidate_differences[r][c][n] = Board.HAS_UNIQUE;
					board2.board.candidate_differences[r][c][n] = Board.DOES_NOT_HAVE;
				}
				else if (cell1[n] == 0 && cell2[n] > 0)
				{
					board1.board.candidate_differences[r][c][n] = Board.DOES_NOT_HAVE;
					board2.board.candidate_differences[r][c][n] = Board.HAS_UNIQUE;
				}
				else
				{
					board1.board.candidate_differences[r][c][n] = Board.SAME;
					board2.board.candidate_differences[r][c][n] = Board.SAME;
				}
			}
		}
	}
	
	/**
	 * Compare lock states of one cell of the two boards
	 */
	void computeDifferencesInLocks(int r, int c)
	{
		boolean locked1 = board1.board.cellLocked[r][c];
		boolean locked2 = board2.board.cellLocked[r][c];
		
		if (locked1 && !locked2)
		{
			board1.board.lock_differences[r][c] = Board.HAS_UNIQUE;
			board2.board.lock_differences[r][c] = Board.DOES_NOT_HAVE;
		}
		else if (locked2 && !locked1)
		{
			board1.board.lock_differences[r][c] = Board.DOES_NOT_HAVE;
			board2.board.lock_differences[r][c] = Board.HAS_UNIQUE;
		}
		else 
		{
			board1.board.lock_differences[r][c] = Board.SAME;
			board2.board.lock_differences[r][c] = Board.SAME;
		}
	}
	
	
	/**
	 * Compute the differences between two boards
	 */
	void computeDifferences()
	{
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				computeDifferencesInCandidates(r, c);
				computeDifferencesInPencilMarks(r, c);
				computeDifferencesInNotes(r, c);
				computeDifferencesInLocks(r, c);
			}
		}
		board1.board.repaint();
		board2.board.repaint();
	}
	
	@SuppressWarnings("CanBeFinal")
	public class BoardViewPane extends JPanel
	{
		// has 4 view modes: show differences in candidates, pencil marks, notes, and locks
		public Board board; // only for viewing purposes, cannot edit
		public SudokuTab chosenTab;
		Counter counter;
		ViewOptions viewOptions;
		JRadioButton compareCandidates; // candidates and solved cells
		JRadioButton comparePencilMarks; // pencil marks and solved cells
		JRadioButton compareNotes;
		JRadioButton compareLocks;
		
		// for viewing the pencil marks and notes of selected cell
		JTextArea pencilMarks;
		JTextArea notes;
		JCheckBox locked;
		
		public boolean linkHistoryTreeSelectedNode;
		
		public int stepNumber;
		public int creationOrder;
		
		void initArrays()
		{
			board.candidate_differences = new int[9][9][9];
			board.note_differences = new int[9][9];
			board.pencil_mark_differences = new int[9][9];
			board.lock_differences = new int[9][9];
		}
		
		public void setChosenTab(SudokuTab selectedTab)
		{
			chosenTab = selectedTab;
			Board selectedBoard = selectedTab.board;
			setChosenBoard(selectedBoard);
			linkBoard();
		}
		
		public void chooseBoard()
		{
			SudokuTab selectedTab = Application.openWindowsAndTabs.showTabChooserDialog(Application.boardComparatorFrame);
			if (selectedTab != null && selectedTab.board != this.board)
			{
				setChosenTab(selectedTab);
			}
		}
		
		public String getChosenBoardInfo()
		{
			if (chosenTab == null)
			{
				return "Empty -- Choose A Board...";
			}
			else 
			{
				return chosenTab.owner.getName() + " | " + chosenTab.getName() + " | Step " + stepNumber + " (creation order " + creationOrder + ")";
			}
		}
		
		void updateStepInformation()
		{
			if (chosenTab != null)
			{
				HistoryStamp currentStamp = chosenTab.historyTreePanel.historyTree.getCurrentStamp();
				stepNumber = currentStamp.stepNumber;
				creationOrder = currentStamp.creationOrder;
			}
		}
		
		/**
		 * Only called by a HistoryTree
		 */
		public void setChosenBoard(BoardData data)
		{
			updateStepInformation();
			Application.boardComparatorFrame.setChosenInformation(this, getChosenBoardInfo());
			board.setSudokuAndNotesAndLocks(data, false);
			computeDifferences();
		}
		
		public void updateChosenBoardInfo()
		{
			Application.boardComparatorFrame.setChosenInformation(this, getChosenBoardInfo());
		}
		
		public void setChosenBoard(Board newBoard)
		{
			if (newBoard != null)
			{
				board.setSudokuAndNotesAndLocks(newBoard, false);
			}
			else 
			{
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
			}
			updateStepInformation();
			Application.boardComparatorFrame.setChosenInformation(this, getChosenBoardInfo());
			computeDifferences();
		}
		
		public void tabWasClosed(SudokuTab tab)
		{
			if (this.board != null && this.chosenTab == tab)
			{
				chosenTab = null;
				setChosenBoard((Board) null);
				
				if (Application.boardComparatorFrame.getSelectedComparator() == BoardComparator.this)
				{
					if (BoardViewPane.this == board1) Application.boardComparatorFrame.linkBoard1ToTree.setEnabled(false);
					else Application.boardComparatorFrame.linkBoard2ToTree.setEnabled(false);
				}
			}
		}
		
		/**
		 * Not used.
		 */
		public Board getOtherBoard()
		{
			return this == board1 ? board2.board : board1.board;
		}
		
		public void linkBoard()
		{
			if (chosenTab != null && linkHistoryTreeSelectedNode)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					
					for (int t = 0; t < window.getTabCount(); t++)
					{
						HistoryTreePanel historyTreePanel = (HistoryTreePanel) window.getComponentAt(t);
						if (historyTreePanel.historyTree.setLinkedBoardComparatorBoard(BoardViewPane.this)) break;
					}
				}
			}
		}
		
		public BoardViewPane()
		{
			board = new Board();
			board.initBoard(Board.FOR_BOARD_COMPARATOR);
			board.sudoku = new Sudoku();
			board.initArrays();
			board.makeCounter();
			board.initViewOptions(false);
			viewOptions = board.viewOptions;
			compareCandidates = viewOptions.showAllCandidates;
			
			comparePencilMarks = viewOptions.showPencilMarks;
			compareNotes = viewOptions.showBlank;
			compareNotes.setText("Notes");
			
			viewOptions.showLockDifferences = new JRadioButton("Locks");
			viewOptions.viewButtonGroup.add(viewOptions.showLockDifferences);
			compareLocks = viewOptions.showLockDifferences;
			compareLocks.addActionListener(event ->
			{
				counter.setCandidateCount(counter.totalCandidates);
				board.repaint();
			});
			counter = board.counter;
			counter.calculateCounts();
			initArrays();
			
			board.initCellEditor(false);
			pencilMarks = board.cellEditor.pencilMarksEditor;
			pencilMarks.setEditable(false);
			
			notes = board.cellEditor.notesEditor;
			notes.setEditable(false);
			
			locked = board.cellEditor.lockCell;
			locked.setEnabled(false);
			
			// add different actionListeners for rotate and flip buttons
			board.viewOptions.rotateClockwiseAction = new AbstractAction("\u21BB")
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					board.rotate(Sudoku.ROTATE_CLOCKWISE);
					board.cellEditor.setEnabled(board.selectedCell != null);
					computeDifferences();
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("rotateComparatorBoardClockwise", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Rotate Board 90 Degrees Clockwise"), false, 0, 0, board.viewOptions.rotateClockwiseAction, board, JComponent.WHEN_IN_FOCUSED_WINDOW);
			viewOptions.rotateClockwise.setAction(board.viewOptions.rotateClockwiseAction);
			
			viewOptions.rotateAntiClockwise.addActionListener(event ->
			{
				board.rotate(Sudoku.ROTATE_ANTI_CLOCKWISE);
				board.cellEditor.setEnabled(board.selectedCell != null);
				computeDifferences();
			});
			
			viewOptions.flipVertical.addActionListener(event ->
			{
				board.flip(Sudoku.VERTICAL_FLIP);
				board.cellEditor.setEnabled(board.selectedCell != null);
				computeDifferences();
			});
			
			viewOptions.flipHorizontal.addActionListener(event ->
			{
				board.flip(Sudoku.HORIZONTAL_FLIP);
				board.cellEditor.setEnabled(board.selectedCell != null);
				computeDifferences();
			});
			
			JToolBar rotateAndIndexes = new JToolBar();
			rotateAndIndexes.add(new JLabel("Rotate and Flip Board: "));
			rotateAndIndexes.add(viewOptions.rotateClockwise);
			rotateAndIndexes.add(viewOptions.rotateAntiClockwise);
			rotateAndIndexes.add(viewOptions.flipVertical);
			rotateAndIndexes.add(viewOptions.flipHorizontal);
			rotateAndIndexes.addSeparator();
			rotateAndIndexes.add(new JLabel("Show Indexes: "));
			rotateAndIndexes.add(viewOptions.showRowIndexes);
			rotateAndIndexes.add(viewOptions.showColIndexes);
			rotateAndIndexes.add(viewOptions.showBoxIndexes);
			
			JToolBar viewMode = new JToolBar();
			viewMode.add(new JLabel("Compare Differences in: "));
			viewMode.add(compareCandidates);
			viewMode.add(comparePencilMarks);
			viewMode.add(compareNotes);
			viewMode.add(compareLocks);
			
			JPanel selectedCellInfoPanel = new JPanel(new GridBagLayout());
			selectedCellInfoPanel.setBorder(BorderFactory.createTitledBorder("Selected Cell Information"));
			
			selectedCellInfoPanel.add(new JLabel("Notes: "), new GBC(0, 0, 1, 2));
			selectedCellInfoPanel.add(notes, new GBC(1, 0, 1, 2));
			selectedCellInfoPanel.add(locked, new GBC(2, 0, 2, 1).setAnchor(GBC.EAST));
			selectedCellInfoPanel.add(new JLabel(" Pencil Marks: "), new GBC(2, 1));
			selectedCellInfoPanel.add(pencilMarks, new GBC(3, 1));
			
			setLayout(new BorderLayout());
			JPanel northPanel = new JPanel(new BorderLayout());
			northPanel.add(rotateAndIndexes, BorderLayout.NORTH);
			northPanel.add(viewMode, BorderLayout.SOUTH);
			
			JPanel southPanel = new JPanel();
			counter.setBorder(BorderFactory.createTitledBorder("Counter"));
			southPanel.add(selectedCellInfoPanel);
			southPanel.add(counter);
			
			JPanel boardPanel = new JPanel(new BorderLayout());
			JPanel boardContainerPanel = new JPanel(); // for correct resize behaviours
			boardContainerPanel.add(board);
			boardPanel.add(boardContainerPanel, BorderLayout.CENTER);
			
			add(northPanel, BorderLayout.NORTH);
			add(boardPanel, BorderLayout.CENTER);
			add(southPanel, BorderLayout.SOUTH);
			
			loadPrefs();
			board.initializing = false;
		}
		
		void loadPrefs()
		{
			AbstractButton viewMode = viewOptions.viewButtonGroup.getButton(Application.boardSettingsPanel.compareOptionsGroup.selectedButton);
			viewMode.setSelected(true);
			for (ActionListener listener : viewMode.getActionListeners())
			{
				listener.actionPerformed(null);
			}
			
			viewOptions.showRowIndexes.setSelected(Application.boardSettingsPanel.comparatorShowRowIndexes.isSelected());
			viewOptions.showColIndexes.setSelected(Application.boardSettingsPanel.comparatorShowColIndexes.isSelected());
			viewOptions.showBoxIndexes.setSelected(Application.boardSettingsPanel.comparatorShowBoxIndexes.isSelected());
			
			linkHistoryTreeSelectedNode = Application.boardSettingsPanel.defaultLinkEnabled.isSelected();
		}
	}
	
	public BoardViewPane board1;
	public BoardViewPane board2;

	public JPanel boardPanel;
	public void swapBoards()
	{
		Component removed = boardPanel.getComponent(0);
		boardPanel.remove(0);
		boardPanel.add(removed);
		board1 = board2;
		board2 = (BoardViewPane) removed;
	}
	
	public BoardComparator()
	{
		super(new BorderLayout());
		board1 = new BoardViewPane();
		board2 = new BoardViewPane();
		
		boardPanel = new JPanel();
		boardPanel.add(board1);
		boardPanel.add(board2);

		add(boardPanel, BorderLayout.CENTER);
	}
	
	public void refreshBoards()
	{
		board1.board.refresh();
		board2.board.refresh();
	}
	
	public void chooseBoard1()
	{
		board1.chooseBoard();
	}
	
	public void chooseBoard2()
	{
		board2.chooseBoard();
	}
	
	/*
	 * Currently not used
	 */
	public void setBoard1(Board newBoard)
	{
		board1.setChosenBoard(newBoard);
	}
	
	/*
	 * Currently not used
	 */
	public void setBoard2(Board newBoard)
	{
		board2.setChosenBoard(newBoard);
	}
	
	public void tabWasClosed(SudokuTab tab)
	{
		board1.tabWasClosed(tab);
		board2.tabWasClosed(tab);
	}
	
	public void updateChosenBoardInfos()
	{
		Application.boardComparatorFrame.setChosenInformation(board1, board1.getChosenBoardInfo());
		Application.boardComparatorFrame.setChosenInformation(board2, board2.getChosenBoardInfo());
	}
}