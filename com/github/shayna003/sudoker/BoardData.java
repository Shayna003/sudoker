package com.github.shayna003.sudoker;

/**
 * This classes contains information about a board, namely,
 * a Sudoku object, pencil marks, notes, and locked states of a Board's cells
 * @since 5-13-2021
 */
@SuppressWarnings("CanBeFinal")
public class BoardData
{
	// null values for these 4 fields represent that there is no specified/valid values for these read from a String/file,
	// which means default/empty values will be used for these fields
	public Sudoku sudoku;
	public String[][] pencilMarks;
	public String[][] notes;
	public boolean[][] cellLocked;
	
	// these are only used by Importer
	public boolean hasViewModeData = false;
	public int viewMode;
	public boolean showRowIndexes;
	public boolean showColIndexes;
	public boolean showBoxIndexes;
	
	public boolean hasHighlightData = false;
	public int mouseOverHighlight;
	public boolean[] permanentHighlight;
	
	// for stopwatch
	public boolean hasStopwatchData = false;
	public int hours;
	public int minutes;
	public int seconds;
	
	public BoardData(Board existingBoard)
	{
		sudoku = existingBoard.sudoku.clone();
		pencilMarks = existingBoard.getPencilMarksCopy();
		notes = existingBoard.getNotesCopy();
		cellLocked = existingBoard.getLocksCopy();
	}
	
	public BoardData(Sudoku sudoku, String[][] pencilMarks, String[][] notes, boolean[][] cellLocked)
	{
		this.sudoku = sudoku;
		this.pencilMarks = pencilMarks;
		this.notes = notes;
		this.cellLocked = cellLocked;
	}
	
	public void setViewModeData(int viewMode, boolean showRowIndexes, boolean showColIndexes, boolean showBoxIndexes)
	{
		hasViewModeData = true;
		this.viewMode = viewMode;
		this.showRowIndexes = showRowIndexes;
		this.showColIndexes = showColIndexes;
		this.showBoxIndexes = showBoxIndexes;
	}
	
	public void setHighlightData(int mouseOverHighlight, boolean[] permanentHighlight)
	{
		hasHighlightData = true;
		this.mouseOverHighlight = mouseOverHighlight;
		this.permanentHighlight = permanentHighlight;
	}
	
	public void setStopwatchData(int hours, int minutes, int seconds)
	{
		hasStopwatchData = true;
		this.hours = hours;
		this.minutes = minutes;
		this.seconds = seconds;
	}
	
	public static String[][] emptyNotesOrPencilMarks()
	{
		String[][] s = new String[9][9];
		
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				s[r][c] = "";
			}
		}
		
		return s;
	}
	
	public static boolean[][] emptyLocks()
	{
		boolean[][] b = new boolean[9][9];
		
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				b[r][c] = false;
			}
		}
		
		return b;
	}
}