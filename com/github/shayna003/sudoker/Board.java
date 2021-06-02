package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.prefs.theme.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.solver.SolverPanel;
import com.github.shayna003.sudoker.util.*;
import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.prefs.*;
import java.util.logging.*;
import java.awt.datatransfer.*;
import java.io.*;

/**
 * This class displays and stores a Sudoku, and gives view and highlight options
 * @since 3-26-2021
 * original version: GridPanel since 11-1-2021
 * Reimplemented in terms of JComponents to better handle Swing events
 */
@SuppressWarnings("CanBeFinal")
public class Board extends JPanel implements Printable
{
	public Edit creationEvent; // creation edit of this board
	public Sudoku sudoku;
	public String[][] pencilMarks; // pencil marks of each cell
	public String[][] notes; // notes of each cell
	public boolean[][] cellLocked; // records whether a cell is locked, or for clue location settings if board is used inside Generator
	
	ThemesPanel themesPanel; // for shorter references
	
	public Cell[][] cells; // initialized by class Box
	public Box[] boxes; // a Box contains 3 * 3 Cells
	public Cell selectedCell; // can be null
	
	// used by BoardComparator two highlight differences between two Boards
	// are null unless this Board resides in a BoardComparator
	public int[][][] candidate_differences; 
	public int[][] note_differences; 
	public int[][] pencil_mark_differences; 
	public int[][] lock_differences;

	// used by Solver to highlight invalid cells
	public int[][] validity;

	// used by Solver to highlight only possible candidates & eliminated candidates
	public int[][][] solverHighlights;
	
	public static final int SAME = 0; // two cells have the same value for one category
	
	public static final int HAS_UNIQUE = 1; // cell has value that the other cell does not have
	/*
		If one cell has notes/pencil marks, and the other doesn't
		If one cell has a candidate enabled, and the other doesn't
		If one cell has a lock, and the other doesn't
	*/
	
	public static final int DIFFERENT = -1; // two cells have different values
	/*
		If notes/pencil marks of two cells are different, both != ""
		If a cell is solved, and the other board's cell isn't for pencil marks mode
		If two solved cells have different solved value
	*/
	
	public static final int DOES_NOT_HAVE = -2; // the other cell has unique value that this cell does not have
	
	
	Rectangle[] outerBorderH;
	Rectangle[] outerBorderV;
	Rectangle[] innerBorderH;
	Rectangle[] innerBorderV;
	double boardInsets; // distance from outermost border's outer edge to edge of panel
	
	GridLayout gridLayout;
	public CellEditor cellEditor;
	CellHighlighter cellHighlighter;
	public Counter counter;
	public ViewOptions viewOptions;
	MouseListener cellMouseHandler;
	MouseMotionListener cellDragHandler;
	MouseListener cellDeselector;
	
	public JPopupMenu cellPopup;
	public Cell popupInvokerCell; // records which cell triggered the popup
	public Action copyAction; // copy values of a cell
	public Action pasteAction; // paste values of a cell
	
	FocusListener cellFocusListener;
	CellValueTransferHandler cellValueTransferHandler;
	
	public SudokuTab boardOwner;
	
	public boolean initializing = true;
	
	@Override
	public int print(Graphics g, PageFormat pf, int page)
	{
		if (page >= 1)
		{
			return Printable.NO_SUCH_PAGE;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.translate(pf.getImageableX(), pf.getImageableY());
		
		paintAll(g);
		return Printable.PAGE_EXISTS;
	}
	
	void configureFonts()
	{
		checkDefaultFont(themesPanel.solvedCandidateFontChooser);
		checkDefaultFont(themesPanel.candidateFontChooser);
		checkDefaultFont(themesPanel.indexFontChooser);
		checkDefaultFont(themesPanel.boxIndexFontChooser);
		checkDefaultFont(themesPanel.pencilMarkFontChooser);
	}
	
	void checkDefaultFont(FontChooserPanel chooserPanel)
	{
		if (chooserPanel.chosenFont == null)
		{
			setFont(null);
			setForeground(null);
			chooserPanel.setFontIgnoreChanges(getFont(), getForeground());
		}
	}
	
	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}

	/**
	 * Paints Board
	 */
	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;

		// paint box inner borders
		if (themesPanel.boxInnerBorderWidth.getValue() > 0)
		{
			g2.setColor(themesPanel.boxInnerBorderColor.color);
			for (Rectangle r : innerBorderH)
			{
				g2.fill(r);
			}
			for (Rectangle r : innerBorderV)
			{
				g2.fill(r);
			}
		}

		// paint bottom outer borders
		if (themesPanel.bottomOuterBorderWidth.getValue() > 0)
		{
			g2.setStroke(new BasicStroke(themesPanel.bottomOuterBorderWidth.getValue()));
			g2.setColor(themesPanel.bottomOuterBorderColor.color);
			for (int i = 0; i < 3; i++)
			{
				g2.draw(outerBorderH[i]);
				g2.draw(outerBorderV[i]);
			}
		}

		// paint top outer borders
		if (themesPanel.topOuterBorderWidth.getValue() > 0)
		{
			g2.setStroke(new BasicStroke(themesPanel.topOuterBorderWidth.getValue()));
			g2.setColor(themesPanel.topOuterBorderColor.color);
			for (int i = 0; i < 3; i++)
			{
				g2.draw(outerBorderH[i]);
				g2.draw(outerBorderV[i]);
			}
		}

		// paint row and column indexes
		setFontAndColor(g2, themesPanel.indexFontChooser);

		int outerBorderWidth = themesPanel.bottomOuterBorderWidth.getValue();
		int boxSize = boxes[0].getPreferredSize().width;
		int cellSize = cells[0][0].getPreferredSize().width;//themesPanel.cellSize.getValue();
		int innerBorderWidth = themesPanel.boxInnerBorderWidth.getValue();
		int cellBoxGap = themesPanel.cellToBoxBorderGap.getValue();

		// row indexes
		if (viewOptions.showRowIndexes.isSelected())
		{
			for (int b = 0; b < 3; b++)
			{
				for (int c = 0; c < 3; c++)
				{
					String message = Application.digitsAndIndexesPanel.getRowIndex(b * 3 + c);
					FontRenderContext context = g2.getFontRenderContext();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
					double ascent = -bounds.getY();

					double y = boardInsets + (boxSize + outerBorderWidth) * b + c * (cellSize + innerBorderWidth) + cellBoxGap;

					if (Application.digitsAndIndexesPanel.rowIndexBoth.isSelected() || Application.digitsAndIndexesPanel.rowIndexLeft.isSelected())
					{
						g2.drawString(message, (float) ((boardInsets - outerBorderWidth - bounds.getWidth()) / 2), (float) (ascent + y + (cellSize - bounds.getHeight()) / 2));
					}
					if (Application.digitsAndIndexesPanel.rowIndexBoth.isSelected() || Application.digitsAndIndexesPanel.rowIndexRight.isSelected())
					{
						g2.drawString(message, (float) (getPreferredSize().width - (boardInsets - outerBorderWidth + bounds.getWidth()) / 2), (float) (ascent + y + (cellSize - bounds.getHeight()) / 2));
					}
				}
			}
		}

		// column indexes
		if (viewOptions.showColIndexes.isSelected())
		{
			for (int b = 0; b < 3; b++)
			{
				for (int c = 0; c < 3; c++)
				{
					String message = Application.digitsAndIndexesPanel.getColIndex(b * 3 + c);
					FontRenderContext context = g2.getFontRenderContext();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
					double ascent = -bounds.getY();
					double x = boardInsets + (boxSize + outerBorderWidth) * b + c * (cellSize + innerBorderWidth) + cellBoxGap;

					if (Application.digitsAndIndexesPanel.colIndexBoth.isSelected() || Application.digitsAndIndexesPanel.colIndexTop.isSelected())
					{
						g2.drawString(message, (float) (x + (cellSize - bounds.getWidth()) / 2), (float) (ascent + (boardInsets - outerBorderWidth - bounds.getHeight()) / 2));
					}
					if (Application.digitsAndIndexesPanel.colIndexBoth.isSelected() || Application.digitsAndIndexesPanel.colIndexBottom.isSelected())
					{
						g2.drawString(message, (float) (x + (cellSize - bounds.getWidth()) / 2), (float) (ascent + getPreferredSize().height - (boardInsets - outerBorderWidth + bounds.getHeight()) / 2));
					}
				}
			}
		}
	}
	
	public void refresh()
	{
		configureFonts();
		
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				cells[r][c].refresh();
			}
		}
		
		for (Box b : boxes)
		{
			b.refresh();
		}
		
		int gap = themesPanel.bottomOuterBorderWidth.getValue();
		gridLayout.setVgap(gap);
		gridLayout.setHgap(gap);
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				cells[r][c].revalidate();
			}
		}
		configureBackground();
		configureBorders();
		repaint();
	}
	
	void configureBackground()
	{
		setOpaque(themesPanel.paintPanelBackground.isSelected());
		if (isOpaque())
		{
			setBackground(themesPanel.panelBackgroundColor.color);
		}	
	}
	
	void configureBorders()
	{
		double cellBoxGap = themesPanel.cellToBoxBorderGap.getValue();
		double outerBorderWidth = themesPanel.bottomOuterBorderWidth.getValue();
		
		boardInsets = themesPanel.boardInsets.getValue() + outerBorderWidth;
		setBorder(new EmptyBorder(new Insets((int) boardInsets, (int) boardInsets, (int) boardInsets, (int) boardInsets)));
		double cellSize = cells[0][0].getPreferredSize().width;
		int innerBorderWidth = themesPanel.boxInnerBorderWidth.getValue();
		int boxSize = boxes[0].getPreferredSize().width;
		double tmp;
		double tmp2 = boardInsets - (outerBorderWidth / 2d);

		for (int i = 0; i < 3; i++)
		{
			tmp = boardInsets - outerBorderWidth / 2d + i * (outerBorderWidth + boxSize);
			outerBorderH[i].setRect(tmp2, tmp, boxSize * 3 + outerBorderWidth * 3, boxSize + outerBorderWidth);
			outerBorderV[i].setRect(tmp, tmp2, boxSize + outerBorderWidth, boxSize * 3 + outerBorderWidth * 3);
		}

		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				tmp = boardInsets + (boxSize + outerBorderWidth) * i + cellBoxGap + cellSize + (j == 0 ? 0 : cellSize + innerBorderWidth);
				//noinspection SuspiciousNameCombination
				innerBorderH[i * 2 + j].setRect(boardInsets, tmp, boxSize * 3 + outerBorderWidth * 2, innerBorderWidth);
				innerBorderV[i * 2 + j].setRect(tmp, boardInsets, innerBorderWidth, boxSize * 3 + outerBorderWidth * 2);
			}
		}
	}
	
	/**
	 * Null checks for cell editor and cell highlighter are for BoardComparators.
	 */
	void setSelectedCell(Cell newSelectedCell)
	{	
		if (newSelectedCell != null && !newSelectedCell.hasFocus())
		{
			newSelectedCell.requestFocus(); // which will call this function again
		}
		else if (newSelectedCell == null)
		{
			Board.this.requestFocus();
		}
		
		if (newSelectedCell != selectedCell)
		{
			popupInvokerCell = null;
			
			if (cellEditor.initNonTextFieldComponents) cellEditor.endEdit(); // if there was a previous edit
			Cell previousSelected = selectedCell;
			selectedCell = newSelectedCell;
			if (cellEditor.initNonTextFieldComponents) cellEditor.startEdit(selectedCell);

			if (previousSelected != null)
			{
				previousSelected.repaint();
			}

			if (selectedCell != null)
			{
				cellEditor.setEnabled(true);
				selectedCell.repaint();
				if (cellHighlighter != null) cellHighlighter.setJRadioButtonsEnabled(true);
			}
			else
			{
				cellEditor.setEnabled(false);
				if (cellHighlighter != null) cellHighlighter.setJRadioButtonsEnabled(false);
			}
		}
	}
	
	/**
	 * Used for notes and pencilMarks
	 */
	public static String[][] emptyNotes()
	{
		String[][] notes = new String[9][9];
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				notes[r][c] = "";
			}
		}
		return notes;
	}
	
	public void setSudokuAndNotesAndLocks(Board board, boolean repaint)
	{
		this.sudoku = board.sudoku.clone();
		this.pencilMarks = GridUtil.copyOf(board.pencilMarks);
		this.notes = GridUtil.copyOf(board.notes);
		this.cellLocked = GridUtil.copyOf(board.cellLocked);
		cellEditor.setEnabled(selectedCell != null);
		counter.calculateCounts();
		if (repaint) repaint();
	}

	/**
	 * Called by quick solve in SolverPanel
	 */
	public void setSudoku(int[][] status)
	{
		cellEditor.endEdit();
		sudoku.setStatus(status);
		cellEditor.setEnabled(selectedCell != null);
		counter.calculateCounts();
		repaint();
	}

	/**
	 * Called by Importer
	 */
	public void setSudoku(Sudoku sudoku)
	{
		assert sudoku != null;
		cellEditor.endEdit();

		this.sudoku = sudoku;
		cellEditor.setEnabled(selectedCell != null);
		counter.calculateCounts();
		repaint();
	}
	
	/*
	 * Used by imports and session restores
	 * Sets sudoku, pencil marks, notes, and locks
	 * And possibly view options, highlight options, and stopwatch time
	 * Needs to be called after the stopwatch for the tab has been initialized.
	 */
	public String setBoardData(BoardData data)
	{
		cellEditor.endEdit();
		StringBuilder b = new StringBuilder();

		if (data.sudoku != null)
		{
			this.sudoku = data.sudoku;
			b.append("Sudoku, ");
		}
		
		if (data.pencilMarks != null)
		{
			this.pencilMarks = data.pencilMarks;
			b.append("Pencil Marks, ");
		}
		
		if (data.notes != null)
		{
			this.notes = data.notes;
			b.append("Notes, ");
		}
		
		if (data.cellLocked != null)
		{
			this.cellLocked = data.cellLocked;
			b.append("Locks, ");
		}
		
		if (data.hasViewModeData)
		{
			viewOptions.viewButtonGroup.getButton(data.viewMode).setSelected(true);
			viewOptions.showRowIndexes.setSelected(data.showRowIndexes);
			viewOptions.showColIndexes.setSelected(data.showColIndexes);
			viewOptions.showBoxIndexes.setSelected(data.showBoxIndexes);
			b.append("View Mode, ");
		}
		
		if (data.hasHighlightData)
		{
			cellHighlighter.radioButtonGroup.getButton(data.mouseOverHighlight).setSelected(true);
			for (int i = 0; i < data.permanentHighlight.length; i++)
			{
				cellHighlighter.checkBoxes[i].setSelected(data.permanentHighlight[i]);
			}
			b.append("Highlight Options, ");
		}
		
		if (data.hasStopwatchData)
		{
			boardOwner.stopwatch.setTime(data.hours, data.minutes, data.seconds);
			b.append("Stopwatch time, ");
		}
		cellEditor.setEnabled(selectedCell != null);
		counter.calculateCounts();
		repaint();

		if (b.length() > 0) b.delete(b.length() - 2, b.length());
		return b.toString();
	}
	
	/**
	 * Used by HistoryTree, SudokuTab's load quick save, or used inside a BoardComparator
	 */
	public void setSudokuAndNotesAndLocks(BoardData data, boolean repaint)
	{
		this.sudoku = data.sudoku.clone();
		this.pencilMarks = GridUtil.copyOf(data.pencilMarks);
		this.notes = GridUtil.copyOf(data.notes);
		this.cellLocked = GridUtil.copyOf(data.cellLocked);
		cellEditor.setEnabled(selectedCell != null);
		counter.calculateCounts();
		if (repaint) repaint();
	}
	
	public void initViewOptions(boolean addListenersForRotateAndFlip)
	{
		viewOptions = new ViewOptions(addListenersForRotateAndFlip);
	}
	
	public void initCellEditor(boolean initNonTextFieldComponents)
	{
		cellEditor = new CellEditor(initNonTextFieldComponents);
		if (initNonTextFieldComponents) cellEditor.setEnabled(false); // initially no cell is selected
	}
	
	void makeCellHighlighter()
	{
		cellHighlighter = new CellHighlighter();
	}
	
	public void makeCounter()
	{
		counter = new Counter();
	}
	
	Cell copiedCell;
	Cell pasteTargetCell;

	public static final int FOR_NORMAL_BOARD = 0;
	public static final int FOR_BOARD_COMPARATOR = 1;
	public static final int FOR_GENERATOR = 2;
	public int type;

	public int maxClues; // used inside Generator
	public int clueCount; // used inside Generator
	public JLabel clueCountLabel; // used inside Generator

	public void initBoard(int purpose)
	{
		type = purpose;
		themesPanel = Application.themesPanel;
		int gap = themesPanel.bottomOuterBorderWidth.getValue();
		gridLayout = new GridLayout(3, 3, gap, gap);
		setLayout(gridLayout);
		configureBackground();
		
		outerBorderH = new Rectangle[3];
		outerBorderV = new Rectangle[3];
		innerBorderH = new Rectangle[6];
		innerBorderV = new Rectangle[6];
		for (int i = 0; i < 3; i++) 
		{
			outerBorderH[i] = new Rectangle();
			outerBorderV[i] = new Rectangle();
		}
		
		for (int i = 0; i < 6; i++)
		{
			innerBorderH[i] = new Rectangle();
			innerBorderV[i] = new Rectangle();
		}
		
		boxes = new Box[9];
		cells = new Cell[9][9];

		cellMouseHandler = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (purpose == FOR_GENERATOR)
				{
					Cell cell = (Cell) event.getSource();
					if (cellLocked[cell.row][cell.col])
					{
						cellLocked[cell.row][cell.col] = false;
						clueCount--;
						clueCountLabel.setText("Selected: " + clueCount);
						cell.repaint();
					}
					else if (maxClues > clueCount)
					{
						cellLocked[cell.row][cell.col] = true;
						clueCount++;
						clueCountLabel.setText("Selected: " + clueCount);
						cell.repaint();
					}
				}
				else
				{
					setSelectedCell((Cell) event.getSource());
				}
			}
		};

		if (purpose != FOR_GENERATOR)
		{
			cellDeselector = new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					setSelectedCell(null);
					popupInvokerCell = null;
				}
			};
			addMouseListener(cellDeselector);

			cellFocusListener = new FocusListener()
			{
				public void focusGained(FocusEvent event)
				{
					setSelectedCell((Cell) event.getComponent());
				}

				public void focusLost(FocusEvent event)
				{
					// do noting
				}
			};
		}
		else
		{
			clueCountLabel = new JLabel("Selected: 0");
		}
		
		if (purpose == FOR_NORMAL_BOARD)
		{
			cellDragHandler = new MouseMotionAdapter() 
			{
				@Override
				public void mouseDragged(MouseEvent event)
				{
					assert event.getSource() != null : event.getSource();
					cellValueTransferHandler.exportAsDrag((Cell) event.getSource(), event, TransferHandler.COPY);
				}
			};
			
			cellPopup = new JPopupMenu()
			{
				@Override
				public void show(Component invoker, int x, int y)
				{
					assert invoker instanceof Cell : invoker;
					popupInvokerCell = (Cell) invoker;
					super.show(invoker, x, y);
				}
			};
			GeneralSettingsPanel.registerComponentAndSetFontSize(cellPopup);
			
			copyAction = new AbstractAction("Copy")
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					if (popupInvokerCell != null)
					{
						copiedCell = popupInvokerCell;
					}
					else 
					{
						if (selectedCell == null) return;
						copiedCell = selectedCell;
					}

					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					CellValueTransferHandler.CellValueTransferable transferable = cellValueTransferHandler.createTransferable(copiedCell);
					clipboard.setContents(transferable, transferable);
					setPasteActionsEnabled(true);
					popupInvokerCell = null;
				}
			};
			copyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
			
			pasteAction = new AbstractAction("Paste")
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					if (popupInvokerCell != null)
					{
						pasteTargetCell = popupInvokerCell;
					}
					else 
					{
						if (selectedCell == null) return;
						pasteTargetCell = selectedCell;
					}
					
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					cellValueTransferHandler.importData(new TransferHandler.TransferSupport(pasteTargetCell, clipboard.getContents(pasteTargetCell)));
					popupInvokerCell = null;
				}
			};
			pasteAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);

			// triggers ClassNotFoundException sometimes if you start with something in clipboard
			pasteAction.setEnabled(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(Board.this).isDataFlavorSupported(cellDataFlavor));

			cellPopup.add(copyAction);
			cellPopup.add(pasteAction);

			cellValueTransferHandler = new CellValueTransferHandler();
			Application.keyboardSettingsPanel.registerOtherShortcut("copyCellValue", KeyboardSettingsPanel.getMenuItemString("Board", "Copy Selected Cell Values"), true, KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), copyAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			Application.keyboardSettingsPanel.registerOtherShortcut("pasteCellValue", KeyboardSettingsPanel.getMenuItemString("Board", "Paste Selected Cell Values"), true, KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), pasteAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
		
		for (int boxr = 0; boxr < 3; boxr++)
		{
			for (int boxc = 0; boxc < 3; boxc++)
			{
				boxes[boxr * 3 + boxc] = new Box(boxr, boxc);
				add(boxes[boxr * 3 + boxc]);
			}
		}
		configureBorders();
		/*
		 There is an awkward situation that if you select a cell and edit it's pencil marks, 
		 and then click somewhere else in this panel to deselect the cell,
		 The pencilMarksEditor will pass its focus to the next focusable element, which 
		 is the first cell in this board, and that cell will get selected...
		 So this call prevents this issue.
		*/
		setFocusable(true);
	}
	
	public void initArrays()
	{
		pencilMarks = new String[9][9];
		notes = new String[9][9];
		cellLocked = new boolean[9][9];
		
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				pencilMarks[r][c] = "";
				notes[r][c] = "";
				cellLocked[r][c] = false;
			}
		}
	}
	
	/*
	 * for cloning and BoardComparator
	 */
	public Board()
	{
		super();
	}
	
	public Board clone(String creationEventDescription)
	{
		Board copy = new Board();
		copy.initBoard(FOR_NORMAL_BOARD);
		copy.initViewOptions(true);
		copy.viewOptions.showBoxIndexes.setSelected(this.viewOptions.showBoxIndexes.isSelected());
		copy.viewOptions.showRowIndexes.setSelected(this.viewOptions.showRowIndexes.isSelected());
		copy.viewOptions.showColIndexes.setSelected(this.viewOptions.showColIndexes.isSelected());
		copy.viewOptions.viewButtonGroup.getButton(viewOptions.viewButtonGroup.selectedButton).setSelected(true);
		
		copy.makeCellHighlighter();
		copy.cellHighlighter.radioButtonGroup.getButton(cellHighlighter.radioButtonGroup.selectedButton).setSelected(true);
		for (int i = 0; i < 9; i++)
		{
			copy.cellHighlighter.checkBoxes[i].setSelected(cellHighlighter.checkBoxes[i].isSelected());
		}
		
		copy.makeCounter();
		copy.counter.setSolvedCount(counter.totalSolvedCells);
		copy.counter.setLockedCount(counter.totalLockedCells);
		copy.counter.setCandidateCount(counter.totalCandidates);
		
		copy.initCellEditor(true);
		copy.creationEvent = new Edit(creationEventDescription, EditType.BOARD_CREATION, copy);
		
		copy.sudoku = this.sudoku.clone();
		copy.notes = new String[9][9];
		copy.pencilMarks = new String[9][9];
		copy.cellLocked = new boolean[9][9];
		
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				copy.pencilMarks[r][c] = this.pencilMarks[r][c];
				copy.notes[r][c] = this.notes[r][c];
				copy.cellLocked[r][c] = this.cellLocked[r][c];
			}
		}
		
		copy.initializing = false;
		return copy;
	}
	
	/**
	 * Creates a Board with a given sudoku puzzle.
	 */
	public Board(Sudoku s, Edit creationEvent)
	{
		super();
		creationEvent.board = this;
		this.creationEvent = creationEvent;
		initBoard(FOR_NORMAL_BOARD);
		initViewOptions(true);
		initCellEditor(true);
		makeCellHighlighter();
		makeCounter();
		initArrays();
		this.sudoku = s;

		setViewOptionsInitialSelections();
		setHighlighterInitialSelections();
		counter.calculateCounts();
		initializing = false;
	}

	void setViewOptionsInitialSelections()
	{
		viewOptions.viewButtonGroup.getButton(Application.boardSettingsPanel.viewOptionsButtonGroup.selectedButton).setSelected(true);
		viewOptions.showRowIndexes.setSelected(Application.boardSettingsPanel.showRowIndexes.isSelected());
		viewOptions.showColIndexes.setSelected(Application.boardSettingsPanel.showColIndexes.isSelected());
		viewOptions.showBoxIndexes.setSelected(Application.boardSettingsPanel.showBoxIndexes.isSelected());
	}

	void setHighlighterInitialSelections()
	{
		cellHighlighter.radioButtonGroup.getButton(Application.boardSettingsPanel.defaultMouseOverHighlight.selectedButton).setSelected(true);
	}

	public static final int RESTORE_FROM_FILE = 0;
	public static final int NEW_TAB_FROM_STRING = 1;
	public static final int NEW_TAB_FROM_FILE = 2;
	public static final int NEW_WINDOW_FROM_STRING = 3;
	public static final int NEW_WINDOW_FROM_FILE = 4;

	/**
	 * @param creationType one of the constants above.
	 */
	public Board(BoardData data, int creationType, SudokuTab owner)
	{
		super();
		initBoard(FOR_NORMAL_BOARD);
		initViewOptions(true);
		initCellEditor(true);
		makeCellHighlighter();
		makeCounter();
		this.boardOwner = owner;

		String dataObtained = setBoardData(data);

		if (data.pencilMarks == null) pencilMarks = BoardData.emptyNotesOrPencilMarks();
		if (data.notes == null) notes = BoardData.emptyNotesOrPencilMarks();
		if (data.cellLocked == null) cellLocked = BoardData.emptyLocks();
		if (data.sudoku == null) sudoku = new Sudoku(); // empty sudoku

		if (!data.hasViewModeData) setViewOptionsInitialSelections();
		if (!data.hasHighlightData) setHighlighterInitialSelections();

		String editDescription;
		switch (creationType)
		{
			case RESTORE_FROM_FILE:
				editDescription = "Restored Following Data from File: " + dataObtained;
				break;
			case NEW_TAB_FROM_STRING:
				editDescription = "New Tab From Text Data: " + dataObtained;
				break;
			case NEW_TAB_FROM_FILE:
				editDescription = "New Tab From File Data: " + dataObtained;
				break;
			case NEW_WINDOW_FROM_STRING:
				editDescription = "New Window From Text Data: " + dataObtained;
				break;
			default:
				editDescription = "New Window From File Data: " + dataObtained;
				break;
		}

		creationEvent = new Edit(editDescription, EditType.BOARD_CREATION, Board.this);
		initializing = false;
	}
	
	/**
	 * Flips sudoku, PencilMarks, Notes and CellLocked
	 * @param direction one of {@code Sudoku.VERTICAL_FLIP } or {@code Sudoku.HORIZONTAL_FLIP }
	 */
	public void flip(int direction)
	{
		if (direction == Sudoku.VERTICAL_FLIP)
		{
			sudoku.flip(direction);
			
			String[] tmp;
			boolean[] tmp2;

			for (int r = 0; r < 4; r++) // middle line does not need to change
			{
				tmp = pencilMarks[r];
				pencilMarks[r] = pencilMarks[8 - r];
				pencilMarks[8 - r] = tmp;
				
				tmp = notes[r];
				notes[r] = notes[8 - r];
				notes[8 - r] = tmp;
				
				tmp2 = cellLocked[r];
				cellLocked[r] = cellLocked[8 - r];
				cellLocked[8 - r] = tmp2;
			}
		}
		else // horizontal flip
		{
			sudoku.flip(direction);
			
			String tmp;
			boolean tmp2;
			
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					tmp = pencilMarks[r][c];
					pencilMarks[r][c] = pencilMarks[r][8 - c];
					pencilMarks[r][8 - c] = tmp;
					
					tmp = notes[r][c];
					notes[r][c] = notes[r][8 - c];
					notes[r][8 - c] = tmp;
					
					tmp2 = cellLocked[r][c];
					cellLocked[r][c] = cellLocked[r][8 - c];
					cellLocked[r][8 - c] = tmp2;
				}
			}
		}
	}
	
	/**
	 * Rotates sudoku, PencilMarks, Notes and CellLocked
	 * @param direction one of {@code Sudoku.ROTATE_CLOCKWISE } or {@code Sudoku.ROTATE_ANTI_CLOCKWISE }
	 */
	public void rotate(int direction)
	{
		String[][] pencilMarksCopy = GridUtil.copyOf(pencilMarks);
		String[][] notesCopy = GridUtil.copyOf(notes);
		boolean[][] cellLockedCopy = GridUtil.copyOf(cellLocked);
		
		if (direction == Sudoku.ROTATE_CLOCKWISE) // rotate clockwise
		{
			sudoku.rotate(direction);
			
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					pencilMarks[c][8 - r] = pencilMarksCopy[r][c];
					notes[c][8 - r] = notesCopy[r][c];
					cellLocked[c][8 - r] = cellLockedCopy[r][c];
				}
			}
		}
		else //rotate counterclockwise
		{
			sudoku.rotate(direction);
			
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					pencilMarks[r][c] = pencilMarksCopy[c][8 - r];
					notes[r][c] = notesCopy[c][8 - r];
					cellLocked[r][c] = cellLockedCopy[c][8 - r];
				}
			}
		}
	}
	
	/**
	 * This panel controls the view for unsolved cells
	 */
	@SuppressWarnings("CanBeFinal")
	public class ViewOptions implements PrefsComponent
	{
		ChangeListener repaintListener;
		
		public JButton flipHorizontal;
		public JButton flipVertical;
		public JButton rotateClockwise;
		public JButton rotateAntiClockwise;
		
		public AbstractAction flipHorizontalAction;
		public AbstractAction flipVerticalAction;
		public AbstractAction rotateClockwiseAction;
		public AbstractAction rotateAntiClockwiseAction;
		
		public PrefsCheckBox showBoxIndexes;
		public PrefsCheckBox showRowIndexes;
		public PrefsCheckBox showColIndexes;
	
		public AbstractAction showBoxIndexAction;
		public AbstractAction showRowIndexAction;
		public AbstractAction showColIndexAction;
		
		public PrefsButtonGroup viewButtonGroup;
		public JRadioButton showAllCandidates;
		public JRadioButton showPencilMarks;
		public JRadioButton showBlank;
		public JRadioButton showLockDifferences; // only used by BoardComparators
		
		public AbstractAction showAllCandidatesAction;
		public AbstractAction showPencilMarksAction;
		public AbstractAction showBlankAction;
		public AbstractAction showLockDifferencesAction;
		
		public void resetToDefault()
		{
			viewButtonGroup.resetToDefault();
		}
		
		public void saveSettings(Preferences node)
		{
			viewButtonGroup.saveSettings(node);
		}
		
		public void loadSettings(Preferences node)
		{
			viewButtonGroup.loadSettings(node);
		}
		
		/**
		 * @param addListenersForRotateAndFlip false if making a board for BoardComparator
		 */
		public ViewOptions(boolean addListenersForRotateAndFlip)
		{
			super();
			
			rotateClockwise = new JButton("\u21BB");//2B6E"); // doesn't show
			rotateClockwise.setToolTipText("Rotate Board 90 Degrees Clockwise");
			if (addListenersForRotateAndFlip)
			{
				rotateClockwiseAction = new AbstractAction("\u21BB")
				{
					@Override
					public void actionPerformed(ActionEvent event)
					{
						cellEditor.endEdit();
						rotate(Sudoku.ROTATE_CLOCKWISE);
						cellEditor.setEnabled(selectedCell != null);
						boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Rotated Board Clockwise by 90 degrees", EditType.ROTATE, Board.this));
						Board.this.repaint();
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("rotateBoardClockwise", KeyboardSettingsPanel.getMenuItemString("Board", "Rotate Board 90 Degrees Clockwise"), false, 0, 0, rotateClockwiseAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
				rotateClockwise.setAction(rotateClockwiseAction);
				
			}
			
			rotateAntiClockwise = new JButton("\u21BA");//2B6E"); // doesn't show
			rotateAntiClockwise.setToolTipText("Rotate Board 90 Degrees Anticlockwise");
			if (addListenersForRotateAndFlip)
			{
				rotateAntiClockwiseAction = new AbstractAction("\u21BA")
				{
					@Override
					public void actionPerformed(ActionEvent event)
					{
						cellEditor.endEdit();
						rotate(Sudoku.ROTATE_ANTI_CLOCKWISE);
						cellEditor.setEnabled(selectedCell != null);
						boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Rotated Board Anticlockwise by 90 degrees", EditType.ROTATE, Board.this));
						Board.this.repaint();
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("rotateBoardAnticlockwise", KeyboardSettingsPanel.getMenuItemString("Board", "Rotate Board 90 Degrees Anticlockwise"), false, 0, 0, rotateAntiClockwiseAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
				rotateAntiClockwise.setAction(rotateAntiClockwiseAction);
			}
			
			flipVertical = new JButton("\u21C5"); //\u2B82"); // doesn't show
			flipVertical.setToolTipText("Flip Board Vertically");
			if (addListenersForRotateAndFlip)
			{
				flipVerticalAction = new AbstractAction("\u21C5")
				{
					@Override
					public void actionPerformed(ActionEvent event)
					{
						cellEditor.endEdit();
						flip(Sudoku.VERTICAL_FLIP);
						cellEditor.setEnabled(selectedCell != null);
						boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Flipped Board Vertically", EditType.FLIP, Board.this));
						Board.this.repaint();
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("flipBoardVertically", KeyboardSettingsPanel.getMenuItemString("Board", "Flip Board Vertically"), false, 0, 0, flipVerticalAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
				flipVertical.setAction(flipVerticalAction);
			}
			
			flipHorizontal = new JButton("\u21C4");//2B81"); // doesn't show
			flipHorizontal.setToolTipText("Flip Board Horizontally");
			if (addListenersForRotateAndFlip)
			{
				flipHorizontalAction = new AbstractAction("\u21C4")
				{
					@Override
					public void actionPerformed(ActionEvent event)
					{
						cellEditor.endEdit();
						flip(Sudoku.HORIZONTAL_FLIP);
						cellEditor.setEnabled(selectedCell != null);
						boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Flipped Board Horizontally", EditType.FLIP, Board.this));
						Board.this.repaint();
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("flipBoardHorizontal", KeyboardSettingsPanel.getMenuItemString("Board", "Flip Board Horizontally"), false, 0, 0, flipHorizontalAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
				flipHorizontal.setAction(flipHorizontalAction);
			}
			
			showAllCandidates = new JRadioButton("All Candidates");
			showPencilMarks = new JRadioButton("Pencil Marks");
			showBlank = new JRadioButton("Blank");
			
			showAllCandidatesAction = new AbstractAction("All Candidates")
			{
				public void actionPerformed(ActionEvent event)
				{
					showAllCandidates.setSelected(true); // if invoked from accelerator
					if (counter != null) counter.setCandidateCount(counter.totalCandidates);
					if (selectedCell != null)
					{
						if (cellHighlighter != null) cellHighlighter.setEnabled(true);
						
						if (cellEditor.initNonTextFieldComponents)
						{
							for (int cb = 0; cb < 9; cb++)
							{
								if (!cellLocked[selectedCell.row][selectedCell.col]) cellEditor.checkBoxes[cb].setEnabled(true);
								cellEditor.checkBoxes[cb].setSelected(sudoku.grid[selectedCell.row][selectedCell.col][cb] > 0);
							}
						}
					}
					else if (cellHighlighter != null)   
					{
						cellHighlighter.setCheckBoxesEnabled(true);
					}
					Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				showAllCandidatesAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
				Application.keyboardSettingsPanel.registerOtherShortcut("showAllCandidates", KeyboardSettingsPanel.getMenuItemString("Board", "Show All Candidates for Unsolved Cells"), false, 0, 0, showAllCandidatesAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			showAllCandidates.addChangeListener(event ->
			{
				if (showAllCandidates.isSelected()) showAllCandidatesAction.actionPerformed(null);
			});
			//showAllCandidates.setAction(showAllCandidatesAction);
			
			showPencilMarksAction = new AbstractAction("Show Pencil Marks")
			{
				public void actionPerformed(ActionEvent event)
				{
					showPencilMarks.setSelected(true); // if invoked from accelerator
					if (counter != null) counter.setCandidateCount(counter.totalCandidates);
					if (selectedCell != null)
					{
						if (cellHighlighter != null) cellHighlighter.setEnabled(true);
						
						if (cellEditor.initNonTextFieldComponents)
						{
							for (int cb = 0; cb < 9; cb++)
							{
								cellEditor.checkBoxes[cb].setEnabled(false);
								cellEditor.checkBoxes[cb].setSelected(false);
							}
						}
					}
					else if (cellHighlighter != null)
					{
						cellHighlighter.setCheckBoxesEnabled(true);
					}
					Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				showPencilMarksAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M); // P is for Print
				Application.keyboardSettingsPanel.registerOtherShortcut("showPencilMarks", KeyboardSettingsPanel.getMenuItemString("Board", "Show Pencil Marks for Unsolved Cells"), false, 0, 0, showPencilMarksAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			showPencilMarks.addChangeListener(event ->
			{
				if (showPencilMarks.isSelected()) showPencilMarksAction.actionPerformed(null);
			});

			showBlankAction = new AbstractAction("Show Blank")
			{
				public void actionPerformed(ActionEvent event)
				{
					showBlank.setSelected(true); // if invoked from accelerator
					if (counter != null) counter.setCandidateCount(counter.totalCandidates);
					if (cellHighlighter != null) cellHighlighter.setEnabled(false);
					
					if (selectedCell != null && cellEditor.initNonTextFieldComponents)
					{
						for (int cb = 0; cb < 9; cb++)
						{
							cellEditor.checkBoxes[cb].setEnabled(false);
							cellEditor.checkBoxes[cb].setSelected(false);
						}
					}
					Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				showBlankAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
				Application.keyboardSettingsPanel.registerOtherShortcut("showBlank", KeyboardSettingsPanel.getMenuItemString("Board", "Show Blank for Unsolved Cells"), false, 0, 0, showBlankAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			showBlank.addChangeListener(event ->
			{
				if (showBlank.isSelected()) showBlankAction.actionPerformed(null);
			});
			//showBlank.setAction(showBlankAction);

			repaintListener = event ->
			{
				if (!initializing) Board.this.repaint();
			};
			//#needToWork setting name needs to be board/window specific
			viewButtonGroup = new PrefsButtonGroup(null, "boardViewOption", Application.boardSettingsPanel.viewOptionsButtonGroup.selectedButton, showAllCandidates, showPencilMarks, showBlank);
			
			ActionListener repaintActionListener = event -> 
			{
				if (!initializing) Board.this.repaint();
			};
			//#needToWork setting name needs to be board/window specific
			showBoxIndexes = new PrefsCheckBox("showBoxIndex", "Box", Application.boardSettingsPanel.showBoxIndexes.isSelected());
			showRowIndexes = new PrefsCheckBox("showRowIndex", "Row", Application.boardSettingsPanel.showRowIndexes.isSelected());
			showColIndexes = new PrefsCheckBox("showColIndex", "Column", Application.boardSettingsPanel.showColIndexes.isSelected());
			
			showBoxIndexes.addActionListener(repaintActionListener);
			showRowIndexes.addActionListener(repaintActionListener);
			showColIndexes.addActionListener(repaintActionListener);
			
			showRowIndexAction = new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					showRowIndexes.setSelected(!showRowIndexes.isSelected());
					if (!initializing) Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				Application.keyboardSettingsPanel.registerOtherShortcut("showRowIndexes", KeyboardSettingsPanel.getMenuItemString("Board", "Toggle Row Indexes"), false, 0, 0, showRowIndexAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			
			
			showColIndexAction = new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					showColIndexes.setSelected(!showColIndexes.isSelected());
					if (!initializing) Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				Application.keyboardSettingsPanel.registerOtherShortcut("showColIndexes", KeyboardSettingsPanel.getMenuItemString("Board", "Toggle Column Indexes"), false, 0, 0, showColIndexAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			
			showBoxIndexAction = new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					showBoxIndexes.setSelected(!showBoxIndexes.isSelected());
					if (!initializing) Board.this.repaint();
				}
			};
			if (addListenersForRotateAndFlip)
			{
				Application.keyboardSettingsPanel.registerOtherShortcut("showBoxIndexes", KeyboardSettingsPanel.getMenuItemString("Board", "Toggle Box Indexes"), false, 0, 0, showBoxIndexAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	public class Counter extends JPanel
	{
		public int totalCandidates;
		public int totalSolvedCells;
		public int totalLockedCells;
		
		JLabel solvedCellCount;
		JLabel lockedCellCount;
		JLabel candidateCount;
		
		public void calculateCounts()
		{
			totalCandidates = 0;
			totalSolvedCells = 0;
			totalLockedCells = 0;
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					if (cellLocked[r][c]) totalLockedCells++;
					
					if (sudoku.status[r][c] > 0)
					{
						totalSolvedCells++;
						totalCandidates++;
					}
					else if (sudoku.status[r][c] < 0)
					{
						totalCandidates += -sudoku.status[r][c];
					}
				}
			}
			
			setCandidateCount(totalCandidates);
			setSolvedCount(totalSolvedCells);
			setLockedCount(totalLockedCells);
		}
		
		public void setCandidateCount(int previousStatus, int newStatus)
		{
			if (previousStatus > 0) previousStatus = 1;
			else if (previousStatus < 0) previousStatus = -previousStatus;
			
			if (newStatus > 0) newStatus = 1;
			else if (newStatus < 0) newStatus = -newStatus;
			
			int difference = newStatus - previousStatus;
			if (difference != 0) setCandidateCount(counter.totalCandidates + difference);
		}
		
		public void setLockedCount(boolean previousLock, boolean newLock)
		{
			if (previousLock && !newLock)
			{
				setLockedCount(totalLockedCells - 1);
			}
			else if (!previousLock && newLock)
			{
				setLockedCount(totalLockedCells + 1);
			}
		}
		
		public void setSolvedCount(int previousStatus, int newStatus)
		{
			if (previousStatus <= 0 && newStatus > 0) setSolvedCount(totalSolvedCells + 1);
			else if (previousStatus > 0 && newStatus <= 0) setSolvedCount(totalSolvedCells - 1);
		}
		
		public void setSolvedCount(int count)
		{
			totalSolvedCells = count;
			solvedCellCount.setText("Solved Cells: " + count + " / 81");
		}
		
		public void setLockedCount(int count)
		{
			totalLockedCells = count;
			lockedCellCount.setText("Locked Cells: " + count + " / 81");
		}
		
		public void setCandidateCount(int count)
		{
			totalCandidates = count;
			if (viewOptions.showAllCandidates.isSelected())
			{
				candidateCount.setText("Total Candidates: " + count + "/ 729");
			}
			else 
			{
				candidateCount.setText("Total Candidates: --- / ---");
			}
		}
		
		public Counter()
		{
			setLayout(new GridBagLayout());
			
			solvedCellCount = new JLabel();
			lockedCellCount = new JLabel();
			candidateCount = new JLabel();
			
			add(solvedCellCount, new GBC(0, 0).setAnchor(GBC.WEST));
			add(lockedCellCount, new GBC(0, 1).setAnchor(GBC.WEST));
			add(candidateCount, new GBC(0, 2).setAnchor(GBC.WEST));
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	class Box extends JPanel
	{
		int boxr;
		int boxc;
		GridLayout layout;

		/**
		 * To paint on top of cells
		 */
		@Override
		public void paint(Graphics g)
		{
			super.paint(g);
			
			if (viewOptions.showBoxIndexes.isSelected())
			{
				Graphics2D g2 = (Graphics2D) g;
				
				//paint box index
				if (themesPanel.boxIndexFontChooser.chosenFont == null)
				{
					setFont(null);
					setForeground(null);
					themesPanel.boxIndexFontChooser.setFontIgnoreChanges(getFont(), getForeground());
				}
				else 
				{
					g2.setFont(themesPanel.boxIndexFontChooser.chosenFont);
					g2.setColor(themesPanel.boxIndexFontChooser.colorComponent.color);
				}
				
				String message = Application.digitsAndIndexesPanel.getBoxIndex(boxr * 3 + boxc);
				FontRenderContext context = g2.getFontRenderContext();
				Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
				double ascent = -bounds.getY();
				
				g2.drawString(message, (float) ((getWidth() - bounds.getWidth()) / 2), (float) (ascent + (getHeight() - bounds.getHeight()) / 2));
			}	
		}
		
		public void refresh()
		{
			layout.setHgap(themesPanel.boxInnerBorderWidth.getValue());
			layout.setVgap(themesPanel.boxInnerBorderWidth.getValue());
			configureBackground();
			configureBorder();
		}
		
		void configureBorder()
		{
			int gap = themesPanel.cellToBoxBorderGap.getValue();
			setBorder(new EmptyBorder(new Insets(gap, gap, gap, gap)));
		}
		
		void configureBackground()
		{
			setOpaque(themesPanel.paintBoxBackground.isSelected());
			if (isOpaque())
			{
				if (themesPanel.boxBackgroundColor_uniform_button.isSelected())
				{
					setBackground(themesPanel.boxBackgroundColor.color);
				}
				else 
				{
					setBackground(themesPanel.boxBackgroundColor_byBox.colorComponents[boxr * 3 + boxc].color);
				}
			}
		}
		
		public Box(int boxr, int boxc)
		{
			super();
			layout = new GridLayout(3, 3, themesPanel.boxInnerBorderWidth.getValue(), themesPanel.boxInnerBorderWidth.getValue());
			setLayout(layout);
			configureBackground();
			configureBorder();
			this.boxr = boxr;
			this.boxc = boxc;
			
			for (int r = 0; r < 3; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					Cell cell = new Cell(boxr * 3 + r, boxc * 3 + c, boxr, boxc);
					cells[boxr * 3 + r][boxc * 3 + c] = cell;
					if (cellMouseHandler != null) cell.addMouseListener(cellMouseHandler);
					if (cellDragHandler != null) cell.addMouseMotionListener(cellDragHandler);
					if (cellFocusListener != null) cell.addFocusListener(cellFocusListener);
					
					if (cellPopup != null) cell.setComponentPopupMenu(cellPopup);
					if (cellValueTransferHandler != null) cell.setTransferHandler(cellValueTransferHandler);
					Box.this.add(cell);
				}
			}
		}
	}
	@SuppressWarnings("CanBeFinal")
	class Cell extends JComponent
	{
		int row;
		int col;
		int boxr;
		int boxc;
		
		Rectangle rect = new Rectangle();
		
		@Override
		public String toString()
		{
			return "Cell " + Application.digitsAndIndexesPanel.getStringIndex(row, col);
		}
		
		public Cell(int row, int col, int boxr, int boxc)
		{
			super();
			setOpaque(false);	
			
			this.row = row;
			this.col = col;
			this.boxr = boxr;
			this.boxc = boxc;
			refresh();
			
			setFocusable(true);
		}
		
		public void refresh()
		{
			rect.setRect(0, 0, themesPanel.cellSize.getValue(), themesPanel.cellSize.getValue());
			revalidate();
			//repaint();
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			// add 1 if cell border is enabled (draw3DRect somehow only paints a rectangle with stroke width of 1)
			int side = themesPanel.cellBorderOptions.getValue() != 0 ? themesPanel.cellSize.getValue() + 1 : themesPanel.cellSize.getValue();
			return new Dimension(side, side);
		}
		
		/**
		 * Paint Cell
		 */
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			
			// fill cell background
			if (type == FOR_GENERATOR)
			{
				if (cellLocked[row][col])
				{
					if (themesPanel.selectedCellColor_uniform_button.isSelected())
					{
						g2.setColor(themesPanel.selectedCellColor.color);
					}
					else
					{
						g2.setColor(themesPanel.selectedCellColor_byBox.colorComponents[boxr * 3 + boxc].color);
					}
				}
				else if (themesPanel.cellColor_byBox_button.isSelected())
				{
					g2.setColor(themesPanel.cellColor_byBox.colorComponents[boxr * 3 + boxc].color);
				}
				else
				{
					g2.setColor(themesPanel.cellColor.color);
				}
			}
			else if (selectedCell == this)
			{
				if (themesPanel.selectedCellColor_uniform_button.isSelected())
				{
					g2.setColor(themesPanel.selectedCellColor.color);
				}
				else 
				{
					g2.setColor(themesPanel.selectedCellColor_byBox.colorComponents[boxr * 3 + boxc].color);
				}
			}
			else if (validity != null && validity[row][col] != SolverPanel.VALID)
			{
				g2.setColor(validity[row][col] == SolverPanel.NO_CANDIDATES ? themesPanel.noCandidateColor.color : themesPanel.repeatedCandidateColor.color);
			}
			else if (viewOptions.showLockDifferences != null && viewOptions.showLockDifferences.isSelected() && lock_differences[row][col] == HAS_UNIQUE)
			{
				g2.setColor(themesPanel.differentValueColor.color);
			}
			else if (viewOptions.showBlank.isSelected() && note_differences != null && (note_differences[row][col] == DIFFERENT || note_differences[row][col] == HAS_UNIQUE))
			{
				int kind = note_differences[row][col];
				g2.setColor(kind == DIFFERENT ? themesPanel.differentValueColor.color : themesPanel.hasUniqueValueColor.color);
			}
			else if (themesPanel.cellColor_byBox_button.isSelected())
			{
				g2.setColor(themesPanel.cellColor_byBox.colorComponents[boxr * 3 + boxc].color);
			}
			else 
			{
				g2.setColor(themesPanel.cellColor.color);
			}
			g2.fill(rect);
			
			// draw cell border
			if (themesPanel.cellBorderOptions.getValue() != 0)
			{
				if (selectedCell == this)
				{
					if (themesPanel.selectedCellBorderColor_uniform_button.isSelected())
					{
						g2.setColor(themesPanel.selectedCellBorderColor.color);
					}
					else 
					{
						g2.setColor(themesPanel.selectedCellBorderColor_byBox.colorComponents[boxr * 3 + boxc].color);
					}
				}
				else if (themesPanel.cellBorderColor_uniform_button.isSelected())
				{
					g2.setColor(themesPanel.cellBorderColor.color);
				}
				else 
				{
					g2.setColor(themesPanel.cellBorderColor_byBox.colorComponents[boxr * 3 + boxc].color);
				}
				
				if (themesPanel.cellBorderOptions.getValue() == 2) // flat
				{
					g2.setStroke(new BasicStroke(1));
					g2.drawRect(0, 0, (int) rect.getWidth() + 1, (int) rect.getHeight() + 1);
					g2.draw(rect);
				}
				else  // raised or lowered
				{
					g2.draw3DRect(0, 0, rect.width, rect.height, themesPanel.cellBorderOptions.getValue() == 3);
				}
			}
			
			// paint cell candidates / pencil marks
			if (sudoku.status[row][col] < 0)
			{
				if (viewOptions.showAllCandidates.isSelected()) // paint all candidates
				{	
					for (int r = 0; r < 3; r++)
					{
						for (int c = 0; c < 3; c++)
						{
							setFontAndColor(Cell.this, g2, themesPanel.candidateFontChooser);
							String message = Application.digitsAndIndexesPanel.getDigit(sudoku.grid[row][col][r * 3 + c]);
							FontRenderContext context = g2.getFontRenderContext();
							Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
							double ascent = -bounds.getY();
							double tmpx = (getWidth() - bounds.getWidth() * 3) / 4;
							double tmpy = (getHeight() - bounds.getHeight() * 3) / 4;

							if (solverHighlights != null && solverHighlights[row][col][r * 3 + c] != SolverPanel.NORMAL_CANDIDATE)
							{
								g2.setColor(solverHighlights[row][col][r * 3 + c] == SolverPanel.ELIMINATED_CANDIDATE ? themesPanel.eliminatedCandidateColor.color : themesPanel.onlyCandidateColor.color);
								g2.fillRect((int) (tmpx < 0 ? 0 : tmpx * (c + 1) + bounds.getWidth() * c), (int) (tmpy * (r + 1) + bounds.getHeight() * r), (int) bounds.getWidth(), (int) bounds.getHeight());
							}
							// compare candidates if it is in a BoardComparator
							else if (candidate_differences != null)
							{
								int kind = candidate_differences[row][col][r * 3 + c];
								if (kind == DIFFERENT || kind == HAS_UNIQUE)
								{
									g2.setColor(kind == DIFFERENT ? themesPanel.differentValueColor.color : themesPanel.hasUniqueValueColor.color);
									g2.fillRect((int) (tmpx < 0 ? 0 : tmpx * (c + 1) + bounds.getWidth() * c), (int) (tmpy * (r + 1) + bounds.getHeight() * r), (int) bounds.getWidth(), (int) bounds.getHeight());
								}
							}
							// fill rectangle for highlight
							else if (cellHighlighter != null)
							{
								int shouldHighlight = cellHighlighter.shouldHighlightCellCandidate(row, col, r * 3 + c);
								if (shouldHighlight > 0 && (this != selectedCell || shouldHighlight == 4))
								{
									switch (shouldHighlight) 
									{
										case 1: // same unit
											g2.setColor(themesPanel.sameUnit.color);
											break;
										case 2: // same box row or box column
											g2.setColor(themesPanel.sameBoxUnit.color);
											break;
										case 3: // any
											g2.setColor(themesPanel.any.color);
											break;
										case 4: // permanently highlight candidate
											g2.setColor(themesPanel.candidateHighlight.colorComponents[r * 3 + c].color);
									}
									g2.fillRect((int) (tmpx < 0 ? 0 : tmpx * (c + 1) + bounds.getWidth() * c), (int) (tmpy * (r + 1) + bounds.getHeight() * r), (int) bounds.getWidth(), (int) bounds.getHeight());
								}
							}
							
							setFontAndColor(Cell.this, g2, themesPanel.candidateFontChooser);
							g2.drawString(message, (float) (tmpx < 0 ? 0 : tmpx * (c + 1) + bounds.getWidth() * c), (float) (ascent + tmpy * (r + 1) + bounds.getHeight() * r));
						}
					}
				}
				else if (viewOptions.showPencilMarks.isSelected()) // paint pencil marks
				{
					setFontAndColor(Cell.this, g2, themesPanel.pencilMarkFontChooser);
					String message = pencilMarks[row][col];
					FontRenderContext context = g2.getFontRenderContext();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
					double ascent = -bounds.getY();
					
					// compare pencil marks if it is in a BoardComparator
					if (pencil_mark_differences != null)
					{
						int kind = pencil_mark_differences[row][col];
						if (kind == DIFFERENT || kind == HAS_UNIQUE)
						{
							g2.setColor(kind == DIFFERENT ? themesPanel.differentValueColor.color : themesPanel.hasUniqueValueColor.color);
							g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
						}
					}
					// fill rectangle for highlight
					else if (cellHighlighter != null)
					{
						int shouldHighlight = cellHighlighter.shouldHighlightCellPencilMark(row, col, pencilMarks[row][col]);
						if (shouldHighlight > 0 && (this != selectedCell || shouldHighlight == 4))
						{
							switch (shouldHighlight) 
							{
								case 1: // same unit
									g2.setColor(themesPanel.sameUnit.color);
									break;
								case 2: // same box row or box column
									g2.setColor(themesPanel.sameBoxUnit.color);
									break;
								case 3: // any
									g2.setColor(themesPanel.any.color);
									break;
								case 4: // permanently highlight candidate
									// it's kind of awkward that pencil marks can have line breaks that affect painting locations, which makes it tedious to know where to highlight, so I just use fixed color
									g2.setColor(themesPanel.candidateHighlight.colorComponents[0].color);
							}
							g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
						}
					}

					setFontAndColor(Cell.this, g2, themesPanel.pencilMarkFontChooser);
					g2.drawString(message, (float) ((getWidth() - bounds.getWidth()) / 2), (float) (ascent + (getHeight() - bounds.getHeight()) / 2));
				}
			}
			else // paint solved value
			{
				setFontAndColor(Cell.this, g2, themesPanel.solvedCandidateFontChooser);
				String message = Application.digitsAndIndexesPanel.getDigit(sudoku.status[row][col]);
				FontRenderContext context = g2.getFontRenderContext();
				Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
				double ascent = -bounds.getY();

				if (solverHighlights != null && solverHighlights[row][col][sudoku.status[row][col] - 1] != SolverPanel.NORMAL_CANDIDATE)
				{
					assert solverHighlights[row][col][sudoku.status[row][col] - 1] == SolverPanel.ONLY_CANDIDATE : solverHighlights[row][col][sudoku.status[row][col] - 1];
					g2.setColor(themesPanel.onlyCandidateColor.color);
					g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
				}
				else if (candidate_differences != null && viewOptions.showAllCandidates.isSelected()) // compare candidates/pencil marks if it is in a BoardComparator
				{
					int kind = candidate_differences[row][col][sudoku.status[row][col] - 1];
					if (kind == DIFFERENT || kind == HAS_UNIQUE)
					{
						g2.setColor(kind == DIFFERENT ? themesPanel.differentValueColor.color : themesPanel.hasUniqueValueColor.color);
						g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
					}
				}
				else if (pencil_mark_differences != null && viewOptions.showPencilMarks.isSelected())
				{
					int kind = pencil_mark_differences[row][col];
					if (kind == DIFFERENT || kind == HAS_UNIQUE)
					{
						g2.setColor(kind == DIFFERENT ? themesPanel.differentValueColor.color : themesPanel.hasUniqueValueColor.color);
						g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
					}
				}
				// fill rectangle for highlight
				else if (cellHighlighter != null && Application.boardSettingsPanel.highlightSolvedCell.isSelected())
				{
					int shouldHighlight = cellHighlighter.shouldHighlightCellCandidate(row, col, sudoku.status[row][col] - 1);
					if (shouldHighlight > 0 && (this != selectedCell || shouldHighlight == 4))
					{
						switch (shouldHighlight) 
						{
							case 1: // same unit
								g2.setColor(themesPanel.sameUnit.color);
								break;
							case 2: // same box row or box column
								g2.setColor(themesPanel.sameBoxUnit.color);
								break;
							case 3: // any
								g2.setColor(themesPanel.any.color);
								break;
							case 4: // permanently highlight candidate
								if (viewOptions.showPencilMarks.isSelected())
									g2.setColor(themesPanel.candidateHighlight.colorComponents[0].color);
								else 
									g2.setColor(themesPanel.candidateHighlight.colorComponents[sudoku.status[row][col] - 1].color);
						}
						g2.fillRect((int) ((getWidth() - bounds.getWidth()) / 2), (int) ((getHeight() - bounds.getHeight()) / 2), (int) bounds.getWidth(), (int) bounds.getHeight());
					}
				}
				
				setFontAndColor(Cell.this, g2, themesPanel.solvedCandidateFontChooser);
				g2.drawString(message, (float) ((getWidth() - bounds.getWidth()) / 2), (float) (ascent + (getHeight() - bounds.getHeight()) / 2));
			}
		}
	}
	
	static void setFontAndColor(Graphics2D g2, FontChooserPanel chooserPanel)
	{
		g2.setFont(chooserPanel.chosenFont);
		g2.setColor(chooserPanel.colorComponent.color);
	}
	
	void setFontAndColor(Cell targetCell, Graphics2D g2, FontChooserPanel chooserPanel)
	{
		g2.setFont(chooserPanel.chosenFont);
		
		if (selectedCell == targetCell && cellLocked[targetCell.row][targetCell.col])
		{
			g2.setColor(themesPanel.selectedLockedCellFontColor.color);
		}
		else if (selectedCell == targetCell)
		{
			g2.setColor(themesPanel.selectedCellFontColor.color);
		}
		else if (cellLocked[targetCell.row][targetCell.col])
		{
			g2.setColor(themesPanel.lockedCellFontColor.color);
		}
		else 
		{
			g2.setColor(chooserPanel.colorComponent.color);
		}
	}
	
	public boolean selectedCellLocked()
	{
		return selectedCell != null && cellLocked[selectedCell.row][selectedCell.col];
	}
	
	int highlightCandidate = 0; // 0 = none, 1 = candidate 1, 9 = candidate 9, etc.
	@SuppressWarnings("CanBeFinal")
	class CellHighlighter extends JPanel
	{
		JPanel radioButtonPanel;
		JRadioButton noHighlight;
		JRadioButton highlightSameUnit;
		JRadioButton highlightSameBoxUnit;
		JRadioButton highlightAll;
		PrefsButtonGroup radioButtonGroup;
		
		JPanel checkBoxPanel;
		ActionListener checkBoxListener;
		NumberedCheckBox[] checkBoxes;
		
		@SuppressWarnings("CanBeFinal")
		class NumberedCheckBox extends JCheckBox
		{
			int number; // 1-9
			NumberedCheckBox(String text, int number)
			{
				super(text);
				this.number = number;
				addActionListener(checkBoxListener);
				setMinimumSize(getPreferredSize());
				
				AbstractAction toggleCheckBox = new AbstractAction()
				{
					public void actionPerformed(ActionEvent event)
					{
						if (NumberedCheckBox.this.isEnabled())
						{
							setSelected(!isSelected());
							Board.this.repaint();
						}
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("toggleHighlightCheckBox" + (number - 1), KeyboardSettingsPanel.getMenuItemString("Board", "Toggle Permanent Highlight for Candidate " + number), false, 0, 0, toggleCheckBox, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
		}
		
		/**
		 * @return 1: same unit
		 * 2: same box row or box column
		 * 3: any
		 * 4: permanently highlight candidate
		 * 0: should not highlight
		 */
		public int shouldHighlightCellPencilMark(int r, int c, String pencilMark)
		{
			if (selectedCell != null && highlightCandidate > 0 && pencilMark.contains(Application.digitsAndIndexesPanel.getDigit(highlightCandidate)))
			{
				int selectedR = selectedCell.row;
				int selectedC = selectedCell.col;
				
				if (UnitCheck.sameUnit(r, c, selectedR, selectedC)[UnitCheck.SAME_UNIT] && (!noHighlight.isSelected()))
				{
					return 1;
				}
				else if ((UnitCheck.sameBoxRow(r, selectedR) || UnitCheck.sameBoxCol(c, selectedC)) && (highlightSameBoxUnit.isSelected() || highlightAll.isSelected()))
				{
					return 2;
				}
				else if (highlightAll.isSelected())
				{
					return 3;
				}
				else if (checkBoxes[highlightCandidate - 1].isSelected())
				{
					return 4;
				}
				else 
				{
					if (shouldPermanentHighlightPencilMark(pencilMark)) return 4;
					else return 0;
				}
			}
			else 
			{
				if (shouldPermanentHighlightPencilMark(pencilMark)) return 4;
				else return 0;
			}
		}
		
		boolean shouldPermanentHighlightCandidate(int candidateIndex)
		{
			return cellHighlighter.checkBoxes[candidateIndex].isSelected();
		}
		
		//int permanentHighlightCandidate = 0;
		boolean shouldPermanentHighlightPencilMark(String pencilMark)
		{
			for (int d = 1; d < 10; d++)
			{
				if (pencilMark.contains(Application.digitsAndIndexesPanel.getDigit(d)) && cellHighlighter.checkBoxes[d - 1].isSelected()) 
				{
					//permanentHighlightCandidate = d;
					return true;
				}
			}
			return false;
		}
		
		/**
		 * @return 1: same unit
		 * 2: same box row or box column
		 * 3: any
		 * 4: permanently highlight candidate
		 * 0: should not highlight
		 */
		public int shouldHighlightCellCandidate(int r, int c, int candidateIndex)
		{	
			if (candidateIndex < 0) return 0;
			if (selectedCell != null && highlightCandidate > 0 && sudoku.grid[r][c][candidateIndex] == highlightCandidate)
			{
				int selectedR = selectedCell.row;
				int selectedC = selectedCell.col;
				
				if (UnitCheck.sameUnit(r, c, selectedR, selectedC)[UnitCheck.SAME_UNIT] && (!noHighlight.isSelected()))
				{
					return 1;
				}
				else if ((UnitCheck.sameBoxRow(r, selectedR) || UnitCheck.sameBoxCol(c, selectedC)) && (highlightSameBoxUnit.isSelected() || highlightAll.isSelected()))
				{
					return 2;
				}
				else if (highlightAll.isSelected())
				{
					return 3;
				}
				else if (checkBoxes[candidateIndex].isSelected())
				{
					return 4;
				}
				else 
				{
					if (shouldPermanentHighlightCandidate(candidateIndex)) return 4;
					else return 0;
				}
			}
			else 
			{
				if (shouldPermanentHighlightCandidate(candidateIndex)) return 4;
				else return 0;
			}
		}
		
		void setCheckBoxesEnabled(boolean enabled)
		{
			for (NumberedCheckBox c : checkBoxes)
			{
				c.setEnabled(enabled);
			}
		}
		
		void setJRadioButtonsEnabled(boolean enabled)
		{
			noHighlight.setEnabled(enabled);
			highlightSameUnit.setEnabled(enabled);
			highlightSameBoxUnit.setEnabled(enabled);
			highlightAll.setEnabled(enabled);
		}
		
		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			setJRadioButtonsEnabled(enabled);
			setCheckBoxesEnabled(enabled);
		}
		
		public CellHighlighter()
		{
			// Highlights other cell's candidates when mouse hovers over a checkbox/toggle button in CellEditor
			noHighlight = new JRadioButton("None");
			noHighlight.setToolTipText("No highlight");
			
			AbstractAction noHighlightAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					noHighlight.setSelected(true);
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("noHighlight", KeyboardSettingsPanel.getMenuItemString("Board", "Mouseover Highlight Options", "No Highlight"), false, 0, 0, noHighlightAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			highlightSameUnit = new JRadioButton("Same Unit as selected cell");
			highlightSameUnit.setToolTipText("Same row, column, or box as selected cell");
			AbstractAction highlightSameUnitAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					highlightSameUnit.setSelected(true);
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("highlightSameUnit", KeyboardSettingsPanel.getMenuItemString("Board", "Mouseover Highlight Options", "Highlight Cells in the Same Row, Column, or bOX as Selected Cell"), false, 0, 0, highlightSameUnitAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			highlightSameBoxUnit = new JRadioButton("Same Larger Unit");
			highlightSameBoxUnit.setToolTipText("Same box, box row, or box column as selected cell");
			AbstractAction highlightSameBoxUnitAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					highlightSameBoxUnit.setSelected(true);
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("highlightSameBoxUnit", KeyboardSettingsPanel.getMenuItemString("Board", "Mouseover Highlight Options", "Highlight Cells in the Same Box Row or Box Column as Selected Cell"), false, 0, 0, highlightSameBoxUnitAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			highlightAll = new JRadioButton("All");
			highlightAll.setToolTipText("All cells");
			AbstractAction highlightAllAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					highlightAll.setSelected(true);
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("highlightAll", KeyboardSettingsPanel.getMenuItemString("Board", "Mouseover Highlight Options", "Highlight All Cells other than Selected Cell"), false, 0, 0, highlightAllAction, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			radioButtonGroup = new PrefsButtonGroup(event ->
			{
				Board.this.repaint();
			}, "highLightOption", Application.boardSettingsPanel.defaultMouseOverHighlight.selectedButton, noHighlight, highlightSameUnit, highlightSameBoxUnit, highlightAll);
			
			radioButtonPanel = new JPanel(new GridBagLayout());
			radioButtonPanel.add(noHighlight, new GBC(0, 0).setAnchor(GBC.WEST));
			radioButtonPanel.add(highlightSameUnit, new GBC(0, 1).setAnchor(GBC.WEST));
			radioButtonPanel.add(highlightSameBoxUnit, new GBC(0, 2).setAnchor(GBC.WEST));
			radioButtonPanel.add(highlightAll, new GBC(0, 3).setAnchor(GBC.WEST));
			radioButtonPanel.setBorder(BorderFactory.createTitledBorder("Mouseover"));
			radioButtonPanel.setToolTipText("When mouse is over a candidate button in Candidate section of Cell Editor, highlight that candidate in cells of selected option");
			
			// Highlights candidates/pencil marks of the selected button
			checkBoxListener = event ->
			{
				Board.this.repaint();
			};
			
			checkBoxPanel = new JPanel(new GridLayout(3, 3, 0, 0));
			checkBoxes = new NumberedCheckBox[9];
			for (int i = 0; i < 9; i++)
			{
				checkBoxes[i] = new NumberedCheckBox(String.valueOf(i + 1), i + 1);
				checkBoxPanel.add(checkBoxes[i]);
			}
			checkBoxPanel.setBorder(BorderFactory.createTitledBorder("Permanent"));
			checkBoxPanel.setToolTipText("Highlight selected candidates or pencil marks in all cells of the board");
			
			setLayout(new BorderLayout());
			add(radioButtonPanel, BorderLayout.CENTER);
			add(checkBoxPanel, BorderLayout.SOUTH);
			
			setJRadioButtonsEnabled(false);
		}
	}
	
	/**
	 * For mouse over listener
	 */
	interface NumberedButton
	{
		int getNumber();
	}
	
	@SuppressWarnings("CanBeFinal")
	public class CellEditor extends JPanel
	{
		JButton clearOptions; 
		JPopupMenu clearOptionsMenu;
		AbstractAction clearCell; // sets candidates to all selected, clears pencilMarks and notes
		AbstractAction clearCandidates;
		AbstractAction clearPencilMarks;
		AbstractAction clearNotes;
		
		JPanel optionsPanel; // lock and clear cell options
		public JCheckBox lockCell; 
		ButtonGroup toggleButtonGroup;
		NumberedToggleButton[] toggleButtons; // these work like radio buttons, sets the cell's value to the button selected
		NumberedCheckBox[] checkBoxes; // these enable/disable individual candidates
		JPanel buttonPanel;
		JPanel checkBoxPanel;
		public JTextArea pencilMarksEditor;
		public JTextArea notesEditor;
		
		ActionListener toggleButtonListener;
		ActionListener checkBoxListener;
		MouseListener checkBoxMouseOverListener;
		
		boolean initNonTextFieldComponents;
		
		/**
		 * Called after {@code selectedCell } changes
		 */
		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			
			if (clearNotes != null) clearNotes.setEnabled(enabled);
			if (initNonTextFieldComponents) 
			{
				lockCell.setEnabled(enabled);
				notesEditor.setEnabled(enabled);
			}
			
			if (enabled)
			{	
				lockCell.setSelected(cellLocked[selectedCell.row][selectedCell.col]);
				
				if (initNonTextFieldComponents)
				{
					clearNotes.setEnabled(true);
					clearCell.setEnabled(!lockCell.isSelected());
					clearCandidates.setEnabled(!lockCell.isSelected());
					clearPencilMarks.setEnabled(!lockCell.isSelected());

					for (int i = 0; i < 9; i++)
					{
						toggleButtons[i].setEnabled(!selectedCellLocked());
					}
					
					for (int i = 0; i < 9; i++)
					{
						checkBoxes[i].setEnabled(viewOptions.showAllCandidates.isSelected() && !selectedCellLocked());
					}
					
					int statusNumber = sudoku.status[selectedCell.row][selectedCell.col];

					if (viewOptions.showAllCandidates.isSelected())
					{
						for (int cb = 0; cb < 9; cb++)
						{
							checkBoxes[cb].setSelected(sudoku.grid[selectedCell.row][selectedCell.col][cb] > 0);
						}
					}

					if (statusNumber > 0)
					{
						toggleButtons[sudoku.status[selectedCell.row][selectedCell.col] - 1].setSelected(true);
					}
					else 
					{
						toggleButtonGroup.clearSelection();
					}
					pencilMarksEditor.setEnabled(!selectedCellLocked());
				}
				pencilMarksEditor.setText(pencilMarks[selectedCell.row][selectedCell.col]);
				notesEditor.setText(notes[selectedCell.row][selectedCell.col]);
			}
			else
			{	
				if (initNonTextFieldComponents)
				{
					clearCell.setEnabled(false);
					clearCandidates.setEnabled(false);
					clearPencilMarks.setEnabled(false);
					
					toggleButtonGroup.clearSelection();
					for (int i = 0; i < 9; i++)
					{
						toggleButtons[i].setEnabled(false);
						checkBoxes[i].setEnabled(false);
						checkBoxes[i].setSelected(false);
					}
					lockCell.setSelected(false);
					pencilMarksEditor.setEnabled(false);
				}
				pencilMarksEditor.setText("");
				notesEditor.setText("");
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class NumberedToggleButton extends JToggleButton implements NumberedButton
		{
			int number; // 1-9
			boolean oldValue;

			@Override
			public void setSelected(boolean b)
			{
				super.setSelected(b);
				if (!oldValue && b) oldValue = true;
			}

			NumberedToggleButton(String text, int number)
			{
				super(text);
				this.number = number;
				addActionListener(toggleButtonListener);
				addMouseListener(checkBoxMouseOverListener);
				setMinimumSize(getPreferredSize());

				addItemListener(event -> oldValue = event.getStateChange() == ItemEvent.DESELECTED);
				AbstractAction toggleButton = new AbstractAction()
				{
					public void actionPerformed(ActionEvent event)
					{
						if (selectedCell != null && NumberedToggleButton.this.isEnabled() && !pencilMarksEditor.hasFocus() && !notesEditor.hasFocus())
						{
							toggleButtonClicked(number, NumberedToggleButton.this.isSelected(), NumberedToggleButton.this);
						}
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("toggleButton" + (number - 1), KeyboardSettingsPanel.getMenuItemString("Board", "Set Selected Cell's Value to " + number), true, ApplicationFrame.getKeyCodeForDigit(number), 0, toggleButton, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			
			@Override
			public int getNumber() { return number; }
		}
		
		@SuppressWarnings("CanBeFinal")
		class NumberedCheckBox extends JCheckBox  implements NumberedButton
		{
			int number; // 1-9
			NumberedCheckBox(String text, int number)
			{
				super(text);
				this.number = number;
				addActionListener(checkBoxListener);
				addMouseListener(checkBoxMouseOverListener);
				setMinimumSize(getPreferredSize());
				
				
				AbstractAction toggleCheckBox = new AbstractAction()
				{
					public void actionPerformed(ActionEvent event)
					{
						if (NumberedCheckBox.this.isEnabled() && !pencilMarksEditor.hasFocus() && !notesEditor.hasFocus())
						{
							setSelected(!isSelected());
							checkBoxClicked(number, isSelected());
						}
					}
				};
				Application.keyboardSettingsPanel.registerOtherShortcut("toggleCheckBox" + (number - 1), KeyboardSettingsPanel.getMenuItemString("Board", "Toggle Selected Cell's Candidate " + number), true, ApplicationFrame.getKeyCodeForDigit(number), InputEvent.SHIFT_DOWN_MASK, toggleCheckBox, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
			
			@Override
			public int getNumber() { return number; }
		}
		
		// records the values of the cell before edit
		int[] cellCandidates;
		int cellStatus;
		String cellPencilMarks;
		String cellNotes;
		Cell cellInEdit;
		boolean cellWasLocked;
		
		public void startEditIfCellSelected()
		{
			if (selectedCell != null) startEdit(selectedCell);
		}
		
		/*
		 * This is called when a new cell is selected
		 */
		public void startEdit(Cell cell)
		{
			if (cell == null)
			{
				cellInEdit = null;
				return;
			}

			cellInEdit = cell;
			if (cellCandidates == null)
			{
				cellCandidates = new int[9];
			}
			
			cellWasLocked = cellLocked[cellInEdit.row][cellInEdit.col];
			cellStatus = sudoku.status[cellInEdit.row][cellInEdit.col];
			System.arraycopy(sudoku.grid[cellInEdit.row][cellInEdit.col], 0, cellCandidates, 0, 9);
			cellPencilMarks = pencilMarks[cellInEdit.row][cellInEdit.col];
			cellNotes = notes[cellInEdit.row][cellInEdit.col];
		}
		
		/*
		 * Called when cell selection changes, or some changes will occur to the board
		 */
		public void endEdit()
		{
			boolean makeNewEdit = false;
			boolean cellLockChanged = false;
			boolean candidatesChanged = false;
			boolean pencilMarksChanged = false;
			boolean notesChanged = false;
			int itemsChanged = 0;
			
			if (cellInEdit != null)
			{
				if (sudoku.status[cellInEdit.row][cellInEdit.col] != cellStatus)
				{
					counter.setCandidateCount(cellStatus, sudoku.status[cellInEdit.row][cellInEdit.col]);
					counter.setSolvedCount(cellStatus, sudoku.status[cellInEdit.row][cellInEdit.col]);
					makeNewEdit = true;
					candidatesChanged = true;
					itemsChanged++;
				}
				else if (!GridUtil.cellsEqual(sudoku.grid[cellInEdit.row][cellInEdit.col], cellCandidates))
				{
					counter.setCandidateCount(cellStatus, sudoku.status[cellInEdit.row][cellInEdit.col]);
					counter.setSolvedCount(cellStatus, sudoku.status[cellInEdit.row][cellInEdit.col]);
					makeNewEdit = true;
					candidatesChanged = true;
					itemsChanged++;
				}
				
				if (!cellNotes.equals(notes[cellInEdit.row][cellInEdit.col]))
				{
					makeNewEdit = true;
					notesChanged = true;
					itemsChanged++;
				}
				if (!cellPencilMarks.equals(pencilMarks[cellInEdit.row][cellInEdit.col]))
				{
					makeNewEdit = true;
					pencilMarksChanged = true;
					itemsChanged++;
				}
				if (cellLocked[cellInEdit.row][cellInEdit.col] != cellWasLocked)
				{
					counter.setLockedCount(cellWasLocked, cellLocked[cellInEdit.row][cellInEdit.col]);
					makeNewEdit = true;
					cellLockChanged = true;
					itemsChanged++;
				}

				if (makeNewEdit)
				{
					StringBuilder b = new StringBuilder("Changes: ");
					int counter = 0;
					
					if (candidatesChanged)
					{
						b.append(" Candidates");
						if (++counter < itemsChanged && itemsChanged > 2) b.append(",");
					}
					
					if (notesChanged)
					{
						if (++counter == itemsChanged && itemsChanged > 1) b.append(" and");
						b.append(" Notes");
						if (counter < itemsChanged && itemsChanged > 2) b.append(",");
					}
					
					if (pencilMarksChanged)
					{
						if (++counter == itemsChanged && itemsChanged > 1) b.append(" and");
						b.append(" Pencil Marks");
						if (counter < itemsChanged && itemsChanged > 2) b.append(",");
					}
					
					if (cellLockChanged)
					{
						if (itemsChanged > 1) b.append(" and");
						b.append(cellWasLocked ? " Unlocked Cell " : " Locked Cell");
					}
					
					b.append(" for Cell ");
					b.append(Application.digitsAndIndexesPanel.getStringIndex(cellInEdit.row, cellInEdit.col));
					
					boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit(b.toString(), EditType.EDIT_CELL, Board.this));
				}
				cellInEdit = null;
			}
		}
		
		public void clearAllPencilMarks()
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					pencilMarks[r][c] = "";
				}
			}
		}
		
		public void clearAllNotes()
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					notes[r][c] = "";
				}
			}
		}
		
		public void clearAllLocks()
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					cellLocked[r][c] = false;
				}
			}
		}
		
		public void clearCandidates(Cell targetCell)
		{
			if (toggleButtonGroup != null && targetCell == selectedCell)
			{
				toggleButtonGroup.clearSelection();
			}
			
			sudoku.clearCell(targetCell.row, targetCell.col);
			
			if (checkBoxes != null && targetCell == selectedCell)
			{
				for (int i = 0; i < 9; i++)
				{
					checkBoxes[i].setSelected(viewOptions.showAllCandidates.isSelected());
				}
			}
			
			targetCell.repaint();
		}
		
		public void clearPencilMarks(Cell targetCell)
		{
			if (targetCell == selectedCell)
			{
				pencilMarksEditor.setText("");
			}
			else 
			{
				pencilMarks[targetCell.row][targetCell.col] = "";
			}
			
			if (viewOptions.showPencilMarks.isSelected())
			{
				targetCell.repaint();
			}
		}
		
		public void clearNotes(Cell targetCell)
		{
			if (targetCell == selectedCell)
			{
				notesEditor.setText("");
			}
			else 
			{
				notes[targetCell.row][targetCell.col] = "";
			}
		}
		
		public void clearLocks(Cell targetCell)
		{
			cellLocked[targetCell.row][targetCell.col] = false;
		}
		
		void lockCell()
		{
			clearCell.setEnabled(!lockCell.isSelected());
			clearCandidates.setEnabled(!lockCell.isSelected());
			clearPencilMarks.setEnabled(!lockCell.isSelected());
			
			cellLocked[selectedCell.row][selectedCell.col] = lockCell.isSelected();
			if (lockCell.isSelected())
			{
				for (int i = 0; i < 9; i++)
				{
					toggleButtons[i].setEnabled(false);
					checkBoxes[i].setEnabled(false);
				}
				pencilMarksEditor.setEnabled(false);
			}
			else 
			{
				for (int i = 0; i < 9; i++)
				{
					if (viewOptions.showAllCandidates.isSelected())
					{
						checkBoxes[i].setEnabled(true);
					}
					toggleButtons[i].setEnabled(true);
				}
				pencilMarksEditor.setEnabled(true);
			}
			
			selectedCell.repaint();
		}
		
		/**
		 * @param number the NumberedToggleButton's number
		 */
		void toggleButtonClicked(int number, boolean initiallySelected, NumberedToggleButton button)
		{
			assert selectedCell != null;
			if (viewOptions.showAllCandidates.isSelected() || !initiallySelected)
			{
				button.setSelected(true);
				for (int cb = 0; cb < 9; cb++)
				{
					checkBoxes[cb].setSelected(false);
				}
				if (viewOptions.showAllCandidates.isSelected())
				{
					checkBoxes[number - 1].setSelected(true);
				}
				sudoku.setValueAt(selectedCell.row, selectedCell.col, number);
			}
			else // view mode is pencil marks or notes, and button is initially selected
			{
				toggleButtonGroup.clearSelection();
				sudoku.clearCell(selectedCell.row, selectedCell.col);
			}
			selectedCell.repaint();
		}
		
		/**
		 * @param number the NumberedToggleButton's number
		 */
		void checkBoxClicked(int number, boolean isSelected)
		{
			sudoku.setCandidateAt(selectedCell.row, selectedCell.col, number, isSelected);

			if (sudoku.status[selectedCell.row][selectedCell.col] > 0)
			{
				toggleButtons[sudoku.status[selectedCell.row][selectedCell.col] - 1].setSelected(true);
			}
			else
			{
				toggleButtonGroup.clearSelection();
			}
			selectedCell.repaint();
		}
		
		void initNonTextFieldComponents()
		{
			buttonPanel = new JPanel(new GridLayout(3, 3, 0, 0));
			checkBoxPanel = new JPanel(new GridLayout(3, 3, 1, 1));
			toggleButtons = new NumberedToggleButton[9];
			checkBoxes = new NumberedCheckBox[9];
			toggleButtonGroup = new ButtonGroup();
			
			clearCell = new AbstractAction("Clear Cell")
			{
				public void actionPerformed(ActionEvent event)
				{
					if (!pencilMarksEditor.hasFocus() && !notesEditor.hasFocus())
					{
						clearCandidates(selectedCell);
						clearPencilMarks(selectedCell);
						clearNotes(selectedCell);
						clearLocks(selectedCell);
					}
				}
			};
			clearCell.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear Candidates, Pencil Marks, Notes, and Lock for this Cell");
			clearCell.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_C);
			clearCell.putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, 6);
			Application.keyboardSettingsPanel.registerOtherShortcut("clearCell", KeyboardSettingsPanel.getMenuItemString("Board", "Clear Selected Cell"), true, KeyEvent.VK_BACK_SPACE, 0, clearCell, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			clearCandidates = new AbstractAction("Clear Candidates")
			{
				public void actionPerformed(ActionEvent event)
				{
					if (!pencilMarksEditor.hasFocus() && !notesEditor.hasFocus())
					{
						clearCandidates(selectedCell);
					}
				}
			};
			clearCandidates.putValue(AbstractAction.SHORT_DESCRIPTION, "Select All Candidates for this Cell");
			clearCandidates.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_C);
			clearCandidates.putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, 6);
			Application.keyboardSettingsPanel.registerOtherShortcut("clearCellCandidates", KeyboardSettingsPanel.getMenuItemString("Board", "Clear Selected Cell's Candidates"), true, KeyEvent.VK_0, 0, clearCandidates, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			clearPencilMarks = new AbstractAction("Clear Pencil Marks")
			{
				public void actionPerformed(ActionEvent event)
				{
					clearPencilMarks(selectedCell);
				}
			};
			clearPencilMarks.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear Pencil Marks for this Cell");
			clearPencilMarks.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_P);
			Application.keyboardSettingsPanel.registerOtherShortcut("clearCellPencilMarks", KeyboardSettingsPanel.getMenuItemString("Board", "Clear Selected Cell's Pencil Marks"), false, 0, 0, clearPencilMarks, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			clearNotes = new AbstractAction("Clear Notes")
			{
				public void actionPerformed(ActionEvent event)
				{
					clearNotes(selectedCell);
				}
			};
			clearNotes.putValue(AbstractAction.SHORT_DESCRIPTION, "Clear Notes for this Cell");
			clearNotes.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_N);
			Application.keyboardSettingsPanel.registerOtherShortcut("clearCellNotes", KeyboardSettingsPanel.getMenuItemString("Board", "Clear Selected Cell's Notes"), false, 0, 0, clearNotes,  Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			clearOptionsMenu = new JPopupMenu();
			GeneralSettingsPanel.registerComponentAndSetFontSize(clearOptionsMenu);
			clearOptionsMenu.add(clearCell);
			clearOptionsMenu.add(clearCandidates);
			clearOptionsMenu.add(clearPencilMarks);
			clearOptionsMenu.add(clearNotes);
			
			clearOptions = new JButton("\u2326 Clear...");
			clearOptions.setMnemonic('C');
			clearOptions.addActionListener(event ->
			{
				Point point = SwingUtilities.convertPoint(optionsPanel, clearOptions.getLocation(), this);
				clearOptionsMenu.show(this, point.x + clearOptions.getWidth(), point.y);
			});
			
			lockCell.setToolTipText("Make Cell Candidates and Pencil Marks Uneditable");
			lockCell.addActionListener(event ->
			{
				assert selectedCell != null;
				lockCell();
			});
			
			toggleButtonListener = event ->
			{
				NumberedToggleButton source = (NumberedToggleButton) event.getSource();
				toggleButtonClicked(source.number, source.oldValue, source);
			};
			
			checkBoxListener = event ->
			{
				NumberedCheckBox source = (NumberedCheckBox) event.getSource();
				
				assert selectedCell != null : null;
				checkBoxClicked(source.number, source.isSelected());
			};
			
			checkBoxMouseOverListener = new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent event)
				{
					highlightCandidate = ((NumberedButton) event.getSource()).getNumber();
					Board.this.repaint();
				}
				
				@Override
				public void mouseExited(MouseEvent event)
				{
					highlightCandidate = 0;
					Board.this.repaint();
				}
			};
			
			for (int i = 0; i < 9; i++)
			{
				toggleButtons[i] = new NumberedToggleButton(String.valueOf(i + 1), i + 1);
				toggleButtonGroup.add(toggleButtons[i]);
				buttonPanel.add(toggleButtons[i]);
			}
			
			for (int i = 0; i < 9; i++)
			{
				checkBoxes[i] = new NumberedCheckBox(String.valueOf(i + 1), i + 1);
				checkBoxPanel.add(checkBoxes[i]);
			}
			
			pencilMarksEditor.getDocument().addDocumentListener(new DocumentListener() 
			{
				@Override
				public void insertUpdate(DocumentEvent event) 
				{
					updatePencilMarks();
				}
				
				@Override
				public void removeUpdate(DocumentEvent event) 
				{
					updatePencilMarks();
				}
				
				@Override
				public void changedUpdate(DocumentEvent event) 
				{
					updatePencilMarks();
				}
			});
			
			AbstractDocument document = (AbstractDocument) pencilMarksEditor.getDocument();
			document.setDocumentFilter(new DocumentFilter()
			{
				@Override
				public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr)
				{
					for (int i = 0; i < 10; i++)
					{
						string = string.replaceAll(String.valueOf(i), Application.digitsAndIndexesPanel.getDigit(i));
					}
					try 
					{
						super.insertString(fb, offset, string, attr);
					} 
					catch (BadLocationException e) 
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "document filter's insertString", "Error when trying to insert string", e);
					}
				}
				
				@Override
				public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				{
					for (int i = 0; i < 10; i++)
					{
						text = text.replaceAll(String.valueOf(i), Application.digitsAndIndexesPanel.getDigit(i));
					}
					try 
					{
						super.replace(fb, offset, length, text, attrs);
					} 
					catch (BadLocationException e) 
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "document filter's replace", "Error when trying to replace text", e);
					}
				}
			});
			
			notesEditor.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent event) 
				{
					updateNotes();
				}
				
				@Override
				public void removeUpdate(DocumentEvent event) 
				{
					updateNotes();
				}
				
				@Override
				public void changedUpdate(DocumentEvent event) 
				{
					updateNotes();
				}
			});
			
			optionsPanel = new JPanel(new BorderLayout());
			optionsPanel.add(clearOptions, BorderLayout.NORTH);
			optionsPanel.add(lockCell, BorderLayout.CENTER);
			optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
		}
		
		/**
		 * @param initNonTextFieldComponents if false, only initialize things used for BoardComparator
		 */
		public CellEditor(boolean initNonTextFieldComponents)
		{
			this.initNonTextFieldComponents = initNonTextFieldComponents;
			lockCell = new JCheckBox(initNonTextFieldComponents ? "Lock Cell" : "Cell Locked");
			AbstractAction toggleLockCell = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					if (selectedCell != null) 
					{
						lockCell.setSelected(!lockCell.isSelected());
						lockCell();
					}
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("lockCell", KeyboardSettingsPanel.getMenuItemString("Board", "Lock/Unlock Selected Cell"), true, KeyEvent.VK_L, InputEvent.SHIFT_DOWN_MASK, toggleLockCell, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			pencilMarksEditor = new JTextArea(1, 10);
			AbstractAction startEditingPencilMarks = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					if (selectedCell != null && pencilMarksEditor.isEnabled()) pencilMarksEditor.requestFocus();
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("startEditingPencilMarks", KeyboardSettingsPanel.getMenuItemString("Board", "Edit Pencil Marks of Selected Cell"), false, 0, 0, startEditingPencilMarks, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			JScrollPane pencilMarksPane = new JScrollPane(pencilMarksEditor);
			JPanel pencilMarksPanel = new JPanel(new BorderLayout(0, 0));
			pencilMarksPanel.add(pencilMarksPane, BorderLayout.CENTER);
			pencilMarksPanel.setBorder(BorderFactory.createTitledBorder("\u270e Pencil Marks"));
			
			notesEditor = new JTextArea(3, 10);
			AbstractAction startEditingNotes = new AbstractAction()
			{
				public void actionPerformed(ActionEvent event)
				{
					if (selectedCell != null && notesEditor.isEnabled()) notesEditor.requestFocus();
				}
			};
			Application.keyboardSettingsPanel.registerOtherShortcut("startEditingNotes", KeyboardSettingsPanel.getMenuItemString("Board", "Edit Notes of Selected Cell"), false, 0, 0, startEditingNotes, Board.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			JScrollPane notesPane = new JScrollPane(notesEditor);
			JPanel notesPanel = new JPanel(new BorderLayout());
			notesPanel.add(notesPane, BorderLayout.CENTER);
			notesPanel.setBorder(BorderFactory.createTitledBorder("\u270d Notes"));
			
			if (initNonTextFieldComponents) initNonTextFieldComponents();
			setLayout(new BorderLayout());
			
			if (initNonTextFieldComponents)
			{
				JPanel candidatePanel = new JPanel(new BorderLayout());
				JPanel compositeNorthPanel = new JPanel(new BorderLayout());
				
				candidatePanel.add(buttonPanel, BorderLayout.CENTER);
				candidatePanel.add(checkBoxPanel, BorderLayout.SOUTH);
				candidatePanel.setBorder(BorderFactory.createTitledBorder("\u24fd Candidates"));
				
				compositeNorthPanel.add(optionsPanel, BorderLayout.NORTH);
				compositeNorthPanel.add(candidatePanel, BorderLayout.CENTER);
				
				JPanel northPanel = new JPanel(new BorderLayout());
				northPanel.add(compositeNorthPanel, BorderLayout.NORTH);
				northPanel.add(pencilMarksPanel, BorderLayout.CENTER);
				
				add(northPanel, BorderLayout.NORTH);
				add(notesPanel, BorderLayout.CENTER);
			}
		}
		
		void updatePencilMarks()
		{
			if (selectedCell != null && initNonTextFieldComponents)
			{
				pencilMarks[selectedCell.row][selectedCell.col] = pencilMarksEditor.getText();
				selectedCell.repaint();
			}
		}
		
		void updateNotes()
		{
			if (selectedCell != null && initNonTextFieldComponents)
			{
				notes[selectedCell.row][selectedCell.col] = notesEditor.getText();
			}
		}
	}
	
	public String[][] getPencilMarksCopy()
	{
		String[][] copy = new String[9][9];
		for (int r = 0; r < 9; r++)
		{
			// Strings are immutable
			System.arraycopy(pencilMarks[r], 0, copy[r], 0, 9);
		}
		return copy;
	}
	
	public String[][] getNotesCopy()
	{
		String[][] copy = new String[9][9];
		for (int r = 0; r < 9; r++)
		{
			// Strings are immutable
			System.arraycopy(notes[r], 0, copy[r], 0, 9);
		}
		return copy;
	}
	
	public boolean[][] getLocksCopy()
	{
		boolean[][] copy = new boolean[9][9];
		for (int r = 0; r < 9; r++)
		{
			System.arraycopy(cellLocked[r], 0, copy[r], 0, 9);
		}
		return copy;
	}
	
	static DataFlavor cellDataFlavor;
	static String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + Cell.class.getName();
	static 
	{
		try
		{
			cellDataFlavor = new DataFlavor(mimeType);
		} 
		catch (ClassNotFoundException e) 
		{
			Application.exceptionLogger.logp(Level.SEVERE, "Board", "init", "Error when creating DataFlavor from String " + mimeType, e);
		}
	}
	
	static void setPasteActionsEnabled(boolean enabled)
	{
		for (ApplicationFrame frame : Application.openWindows)
		{
			JTabbedPane pane = frame.tabbedPane;
			for (int t = 0; t < pane.getTabCount(); t++)
			{
				((SudokuTab) pane.getComponentAt(t)).board.pasteAction.setEnabled(enabled);
			}
		}
	}
	
	class CellValueTransferHandler extends TransferHandler
	{		
		@Override
		protected CellValueTransferable createTransferable(JComponent comp)
		{
			assert comp instanceof Cell;
			Cell source = (Cell) comp;
			int r = source.row;
			int c = source.col;
			return new CellValueTransferable(new CellData((Cell) comp, cellLocked[r][c], sudoku.status[r][c], sudoku.grid[r][c], pencilMarks[r][c], notes[r][c]));
		}
		
		@Override
		public boolean canImport(TransferHandler.TransferSupport info)
		{
			Transferable t = info.getTransferable();
			return t.isDataFlavorSupported(cellDataFlavor);
		}
		
		@Override
		public int getSourceActions(JComponent c) 
		{
			if (c instanceof Cell) return TransferHandler.COPY;
			else return TransferHandler.NONE;
		}
		
		@Override
		public boolean importData(TransferHandler.TransferSupport info)
		{
			Cell targetCell = (Cell) info.getComponent();
			if (targetCell != selectedCell)
			{
				cellEditor.endEdit();
				cellEditor.startEdit(targetCell);
			}
			
			int r = targetCell.row;
			int c = targetCell.col;
			Transferable t = info.getTransferable();
			try
			{
				if (!t.isDataFlavorSupported(cellDataFlavor))
				{
					setPasteActionsEnabled(false);
					return false;
				}
				
				CellData data = (CellData) t.getTransferData(cellDataFlavor);

				if (Application.boardSettingsPanel.copyCandidates.isSelected() && !cellLocked[r][c])
				{
					sudoku.status[r][c] = data.statusNumber;
					System.arraycopy(data.candidates, 0, sudoku.grid[r][c], 0, 9);
				}
				
				if (Application.boardSettingsPanel.copyPencilMarks.isSelected() && !cellLocked[r][c])
				{
					pencilMarks[r][c] = data.pencilMarks;
				}
				
				if (Application.boardSettingsPanel.copyNotes.isSelected())
				{
					notes[r][c] = data.notes;
				}
				
				if (Application.boardSettingsPanel.copyLocks.isSelected())
				{
					cellLocked[r][c] = data.locked;
				}
				
				if (targetCell == selectedCell)
				{
					cellEditor.setEnabled(true);
				}
				targetCell.repaint();
				
				if (targetCell != selectedCell)
				{
					cellEditor.endEdit();
				}

				return true;
			}
			catch (UnsupportedFlavorException | IOException | ClassCastException e)
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "importData", "Error when trying to import data from " + t, e);
				return false;
			}	
		}
		
		@Override
		protected void exportDone(JComponent c, Transferable t, int act)
		{
			setCursor(Cursor.getDefaultCursor());
		}
		
		@SuppressWarnings("CanBeFinal")
		class CellValueTransferable implements Transferable, ClipboardOwner
		{
			CellData data;
			
			public CellValueTransferable(CellData data)
			{
				this.data = data;
			}
			
			@Override
			public void lostOwnership(Clipboard c, Transferable transferable) 
			{
				boolean enabled = c.getContents(Board.this).isDataFlavorSupported(cellDataFlavor);
				setPasteActionsEnabled(enabled);
			}
			
			@Override
			public CellData getTransferData(DataFlavor flavor)
			{
				return data;
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[] { cellDataFlavor };
			}
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return flavor.getRepresentationClass().equals(Cell.class);
			}
		}
		
		/**
		* Contains data of a {@code Cell } for data transfer
		*/
		@SuppressWarnings("CanBeFinal")
		class CellData
		{
			int statusNumber;
			Cell source;
			boolean locked;
			int[] candidates;
			String pencilMarks;
			String notes;
			
			public CellData(Cell source, boolean locked, int statusNumber, int[] candidates, String pencilMarks, String notes)
			{
				this.source = source;
				this.locked = locked;
				this.statusNumber = statusNumber;
				this.candidates = new int[9];
				System.arraycopy(candidates, 0, this.candidates, 0, 9);
				this.pencilMarks = pencilMarks;
				this.notes = notes;
			}
		}
	}
}