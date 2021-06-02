package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.util.*;

import java.util.Arrays;

@SuppressWarnings("CanBeFinal")
public class Sudoku implements Cloneable
{
	public int[][][] grid; // a 9 * 9 * 9 array: records states of the 9 candidates of each of the 81 cells
	public int[][] status; // a 9 * 9 array: more than one candidate: -number of candidates, no candidates: 0, one candidate: value of the candidate

	/**
	 * Constructs an empty sudoku grid
	 */
	public Sudoku()
	{
		this.grid = GridUtil.emptyGrid();
		this.status = GridUtil.emptyStatus();
	}

	/**
	 * Constructs a new sudoku with solved status array
	 */
	public Sudoku(int[][] status)
	{
		this.status = status;
		grid = new int[9][9][9];
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				grid[r][c][status[r][c] - 1] = status[r][c];
			}
		}
	}

	public Sudoku(int[][][] grid, int[][] status)
	{
		this.grid = grid;
		this.status = status;
	}

	@Override
	public String toString()
	{
		return IO.getDefaultString(this, 0, true,true);
	}
	
	/**
	 * @param r row index of cell
	 * @param c column index of cell
	 * @param value new value of cell
	 */
	public void setValueAt(int r, int c, int value)
	{
		Arrays.fill(grid[r][c], 0);
		grid[r][c][value - 1] = value;
		status[r][c] = value;
	}

	/**
	 * @param newStatus status array of a solved puzzle.
	 * @return self
	 */
	public Sudoku setStatus(int[][] newStatus)
	{
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				assert newStatus[r][c] > 0 : newStatus[r][c];
				setValueAt(r, c, newStatus[r][c]);
			}
		}
		return this;
	}
	
	/**
	 * Sets the cell to all possibilities.
	 */
	public void clearCell(int r, int c)
	{
		status[r][c] = -9;
		for (int n = 0; n < 9; n++)
		{
			grid[r][c][n] = n + 1;
		}
	}
	
	/**
	 * @param r row index of cell
	 * @param c column index of cell
	 * @param candidate the candidate to turn on of off
	 * @param turnOn if true, turn on candidate, else turn it off
	 */
	public void setCandidateAt(int r, int c, int candidate, boolean turnOn)
	{
		grid[r][c][candidate - 1] = turnOn ? candidate : 0;
		status[r][c] = GridUtil.getStatusForCell(grid[r][c], false);
	}
	
	@Override
	public Sudoku clone()
	{
		return new Sudoku(GridUtil.copyOf(grid), GridUtil.copyOf(status));
	}
	
	public static int ROTATE_CLOCKWISE = 0;
	public static int ROTATE_ANTI_CLOCKWISE = 1;
	public static int VERTICAL_FLIP = 0;
	public static int HORIZONTAL_FLIP = 1;
	
	/**
	* Rotates the grid and status by 90 degrees
	* @param direction one of {@code ROTATE_CLOCKWISE } or {@code ROTATE_ANTI_CLOCKWISE }
	*/
	public void rotate(int direction)
	{
		int[][][] gridCopy = GridUtil.copyOf(grid);
		int[][] statusCopy = GridUtil.copyOf(status);
		if (direction == ROTATE_CLOCKWISE)
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					status[c][8 - r] = statusCopy[r][c];
					grid[c][8 - r] = gridCopy[r][c];
				}
			}
		}
		else // counterclockwise
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					status[r][c] = statusCopy[c][8 - r];
					grid[r][c] = gridCopy[c][8 - r];
				}
			}
		}
	}
	
	/**
	* Flips the grid vertically or horizontally
	* @param direction one of {@code VERTICAL_FLIP } or {@code HORIZONTAL_FLIP }
	*/
	public void flip(int direction)
	{
		if (direction == VERTICAL_FLIP)
		{
			int[][] lineG;
			int[] lineS;
			for (int r = 0; r < 4; r++) //middle line does not need to change
			{
				lineG = grid[r]; // in this case both status and grid[row] has dimension 9 * 9
				grid[r] = grid[8 - r];
				grid[8 - r] = lineG;
				
				lineS = status[r]; // in this case both cell and status[row] has dimension 1 * 9
				status[r] = status[8 - r];
				status[8 - r] = lineS;
			}
		}
		else //horizontally
		{
			int[] cell;
			int tmp;
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					cell = grid[r][c];
					grid[r][c] = grid[r][8 - c];
					grid[r][8 - c] = cell;
					
					tmp = status[r][c];
					status[r][c] = status[r][8 - c];
					status[r][8 - c] = tmp;
				}
			}
		}
	}
}