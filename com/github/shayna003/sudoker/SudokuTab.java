package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.Board.*;
import com.github.shayna003.sudoker.Board.CellEditor;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.widgets.*;
import com.github.shayna003.sudoker.solver.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;

/**
 * This class contains a Board, Solver, Cell Editor, and other sudoku board specific objects
 * StopWatch + SolverPanel
 * Instances of this class will be added to a CloseableDndTabbedPane of an ApplicationFrame
 * @since 4-16-2021

 TabDnd needs to change the index of a SudokuTab and its corresponding leaf in OpenWindowsAndTabs??
 TabDnd does need to update OpenWindowsAndTabs
 */
public class SudokuTab extends JPanel
{
	public ApplicationFrame owner;
	public Board board;
	public QuickSaves quickSaves;
	
	public DropDownPanel cellEditorDropDown;
	public CellEditor cellEditor;
	
	public DropDownPanel cellHighlighterDropDown;
	public CellHighlighter cellHighlighter;
	
	public DropDownPanel counterDropDown;
	public Counter counter;
	
	public DropDownPanel stopwatchDropDown;
	public Stopwatch stopwatch;

	public DropDownPanel solverDropDown;
	public SolverPanel solverPanel;

	public ViewOptions viewOptions;
	public JToolBar viewOptionsToolBar;
	
	JButton boardSettingsButton;
	JPopupMenu boardSettingsPopup;
	AbstractAction themesItem;
	AbstractAction boardSettingsItem;
	
	public HistoryTreePanel historyTreePanel; // the historyTreePanel that corresponds with this SudokuTab, assigned by Application

	@Override
	public String toString() { return getName(); }
	
	/**
	 * Used by clone and such
	 */
	public SudokuTab(ApplicationFrame owner, Board board)
	{
		initStopwatch();
		this.owner = owner;
		this.board = board;
		board.boardOwner = this;
		layoutComponents();
	}

	public SudokuTab(ApplicationFrame owner, BoardData data, int creationType)
	{
		initStopwatch();
		this.owner = owner;
		this.board = new Board(data, creationType, SudokuTab.this);
		layoutComponents();
	}
	
	public SudokuTab(ApplicationFrame owner, Sudoku sudoku, String creationEventDescription)
	{
		initStopwatch();
		Edit creationEvent = new Edit(creationEventDescription, EditType.BOARD_CREATION, board);
		this.owner = owner;
		board = new Board(sudoku, creationEvent);
		board.boardOwner = this;
		layoutComponents();
	}

	void initStopwatch()
	{
		stopwatch = new Stopwatch();
	}

	void layoutComponents()
	{
		quickSaves = new QuickSaves();

		cellEditor = board.cellEditor;
		cellHighlighter = board.cellHighlighter;
		viewOptions = board.viewOptions;
		counter = board.counter;
		
		JPanel boardPanel = new JPanel(new BorderLayout());
		JPanel boardContainerPanel = new JPanel(); // for correct resize behaviours
		boardContainerPanel.add(board);
		boardPanel.add(boardContainerPanel, BorderLayout.CENTER);
		
		viewOptionsToolBar = new JToolBar();
		viewOptionsToolBar.add(new JLabel("Rotate and Flip Board: "));
		viewOptionsToolBar.add(viewOptions.rotateClockwise);
		viewOptionsToolBar.add(viewOptions.rotateAntiClockwise);
		viewOptionsToolBar.add(viewOptions.flipVertical);
		viewOptionsToolBar.add(viewOptions.flipHorizontal);
		
		viewOptionsToolBar.addSeparator();
		
		viewOptionsToolBar.add(new JLabel("Show in Unsolved Cells:"));
		viewOptionsToolBar.add(viewOptions.showAllCandidates);
		viewOptionsToolBar.add(viewOptions.showPencilMarks);
		viewOptionsToolBar.add(viewOptions.showBlank);
		
		viewOptionsToolBar.addSeparator();
		
		viewOptionsToolBar.add(new JLabel("Show Indexes:"));
		viewOptionsToolBar.add(viewOptions.showRowIndexes);
		viewOptionsToolBar.add(viewOptions.showColIndexes);
		viewOptionsToolBar.add(viewOptions.showBoxIndexes);
		
		viewOptionsToolBar.addSeparator();
		
		themesItem = new AbstractAction("Themes (Board Visuals)")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				Application.preferenceFrame.showUp(Application.themesPanel);
			}
		};
		themesItem.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
		Application.keyboardSettingsPanel.registerOtherShortcut("showThemeSettings", KeyboardSettingsPanel.getMenuItemString("Board", "Show Theme Settings"), false, 0, 0, themesItem, SudokuTab.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		boardSettingsItem = new AbstractAction("Board Settings")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				Application.preferenceFrame.showUp(Application.boardSettingsPanel);
			}
		};
		boardSettingsItem.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		Application.keyboardSettingsPanel.registerOtherShortcut("showBoardSettings", KeyboardSettingsPanel.getMenuItemString("Board", "Show Board Settings"), false, 0, 0, boardSettingsItem, SudokuTab.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		boardSettingsPopup = new JPopupMenu();
		boardSettingsPopup.add(themesItem);
		boardSettingsPopup.add(boardSettingsItem);
		GeneralSettingsPanel.registerComponentAndSetFontSize(boardSettingsPopup);
		
		boardSettingsButton = new JButton("\u2699");
		boardSettingsButton.setToolTipText("Board Settings");
		boardSettingsButton.addActionListener(event ->
		{
			Point point = SwingUtilities.convertPoint(viewOptionsToolBar, boardSettingsButton.getLocation(), SudokuTab.this);
			boardSettingsPopup.show(SudokuTab.this, point.x + boardSettingsButton.getWidth(), point.y);
		});
		viewOptionsToolBar.add(boardSettingsButton);
		viewOptionsToolBar.setToolTipText("View Options for this Board");
		
		JPanel westPanel = new JPanel();
		westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));
		cellEditorDropDown = new DropDownPanel("Cell Editor", board.cellEditor);
		westPanel.add(cellEditorDropDown);
		
		cellHighlighterDropDown = new DropDownPanel("Highlight Options", board.cellHighlighter);
		westPanel.add(cellHighlighterDropDown);
		
		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
		
		counterDropDown = new DropDownPanel("Counter", counter);
		eastPanel.add(counterDropDown);
		
		if (Application.miscellaneousSettingsPanel.startTimerUponCreatingNewTab.isSelected())
		{
			stopwatch.start();
		}
		stopwatchDropDown = new DropDownPanel("\u23F1 Stopwatch", stopwatch);
		eastPanel.add(stopwatchDropDown);

		solverPanel = new SolverPanel(this);
		solverDropDown = new DropDownPanel("Solver", solverPanel);
		eastPanel.add(solverDropDown);

		// for correct resizing behaviours
		JPanel westPanelContainer = new JPanel();
		westPanelContainer.add(westPanel);

		JPanel eastPanelContainer = new JPanel();
		eastPanelContainer.add(eastPanel);

		boardPanel.add(solverPanel.outputPanel, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(boardPanel, BorderLayout.CENTER);
		add(viewOptionsToolBar, BorderLayout.NORTH);
		add(westPanelContainer, BorderLayout.WEST);
		add(eastPanelContainer, BorderLayout.EAST);
	}
	
	/**
	* This class manages the quick saves of a Board located in a SudokuTab
	* @since 5-13-2021
	*/
	@SuppressWarnings("CanBeFinal")
    public class QuickSaves
	{
		@SuppressWarnings("CanBeFinal")
        public class QuickSaveSlot
		{
			int index;
			BoardData data;
			String name;
			boolean isEmpty = true;
			
			public QuickSaveSlot(int index, String name)
			{
				this.index = index;
				this.name = name;
			}
			
			public void setData(BoardData data)
			{
				isEmpty = false;
				this.data = data;
			}
			
			public void setName(String name)
			{
				this.name = name;
			}
			
			@Override
			public String toString()
			{
				return name;
			}
		}
		
		public static final int SAVE_SLOTS = 16;
		public QuickSaveSlot[] saves;
		
		public QuickSaves()
		{
			saves = new QuickSaveSlot[SAVE_SLOTS];
			for (int s = 0; s < SAVE_SLOTS; s++)
			{
				saves[s] = new QuickSaveSlot(s, "Slot " + (s + 1) + " (Empty)");
			}
		}
		
		public void saveToSlot(BoardData data, int slotIndex)
		{
			saveToSlot(data, saves[slotIndex]);
		}
		
		public void saveToSlot(BoardData data, QuickSaveSlot targetSlot)
		{
			LocalTime time = LocalTime.now();
			String name = JOptionPane.showInputDialog(owner, "Enter a name for the save: ", "Slot " + (targetSlot.index + 1) + " saved at " + String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond()));
			if (name == null) return;
			targetSlot.setName(name);
			targetSlot.setData(data);
		}
		
		public void loadFromSlot(int slotIndex)
		{
			loadFromSlot(saves[slotIndex]);
		}
		
		public void loadFromSlot(QuickSaveSlot targetSlot)
		{
			if (targetSlot.isEmpty) return;
			board.cellEditor.endEdit();
			board.setSudokuAndNotesAndLocks(targetSlot.data, true);
			
			historyTreePanel.historyTree.addNodeForEdit(new Edit("Loaded Quick Save From " + targetSlot, EditType.LOAD_QUICK_SAVE, board));
			board.cellEditor.startEditIfCellSelected();
		}
	}
}