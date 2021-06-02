package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.DigitsAndIndexesPanel;
import com.github.shayna003.sudoker.util.*;
import static com.github.shayna003.sudoker.Application.digitsAndIndexesPanel;

import java.util.*;

/**
 * Solving techniques go here
 * @version 0.0.0
 * @since 2020-11-1
 * last modified: 5-24-2021
 */
public class Solver
{
	// used by solve and guess, their values not used in the Application yet
	public static int recursed = 0; // records how many times the guess() function was called
	public static int successfulGuess = 0; // records the number of times a successful guess was made

	public static final String messageOutputIndent = "    ";

	public static void resetCounters()
	{
		recursed = 0;
		successfulGuess = 0;
	}

	public static final int FIND_ONE_SOLUTION = 0;
	public static final int RETURN_IF_FINDS_SECOND_SOLUTION = 1;
	public static final int KEEP_ON_LOOKING_UNTIL_MAX_REACHED = 2;

	/**
	 * Called by SolverPanel.
	 * Solves a puzzle and find its number of solutions.
	 * Stops checking for solutions after it exceeds maximum count specified in SolverSettingsPanel.
	 * @param puzzleSolutions where the solutions found are assinged to, can be null
	 * @param mode one of {@code FIND_ONE_SOLUTION, RETURN_IF_FINDS_SECOND_SOLUTION, KEEP_ON_LOOKING_UNTIL_MAX_REACHED }
	 * @return number of total solutions found, < 1 = invalid puzzle
	 */
	public static int solve(Sudoku sudoku, ArrayList<int[][]> puzzleSolutions, int mode)
	{
		Checker.statusMatchGrid(sudoku, true, false);
		int[] eliminateResults = new int[3];
		Sudoku sudokuCopy = sudoku.clone();

		// try to solve puzzle through elimination
		Solver.eliminate(null, sudokuCopy, true, true, eliminateResults);
		if (eliminateResults[VALIDITY_INDEX] < 0)
		{
			return -1;
		}

		// check if puzzle is solved
		int[] isSolved = Checker.checkIfPuzzleSolved(sudokuCopy, false);
		if (isSolved[0] == 1) // puzzle solved and valid
		{
			if (puzzleSolutions != null) puzzleSolutions.add(GridUtil.copyOf(sudokuCopy.status));
			return 1;
		}

		// need to guess to be able to solve this puzzle
		return guess(sudokuCopy, puzzleSolutions, mode, 0);
	}

	/**
	 * Called by guess
	 * @param r the row index of the newly guessed cell
	 * @param c  the col index of the newly guessed cell
	 */
	private static int solve(Sudoku sudoku, ArrayList<int[][]> puzzleSolutions, int mode, int solutionsFound, int r, int c)
	{
		Checker.statusMatchGrid(sudoku, true, false);
		int[] eliminateResults = new int[3];

		// try to solve puzzle through elimination
		deleteInUnit(null, sudoku, r, c, true, true, eliminateResults);
		if (eliminateResults[VALIDITY_INDEX] < 0) // results in invalid puzzle
		{
			return -1;
		}

		// check if puzzle is solved
		int[] isSolved = Checker.checkIfPuzzleSolved(sudoku, false);
		if (isSolved[0] == 1) // puzzle solved and valid
		{
			if (puzzleSolutions != null) puzzleSolutions.add(GridUtil.copyOf(sudoku.status));
			return ++solutionsFound;
		}

		// need to guess to be able to solve this puzzle
		return guess(sudoku, puzzleSolutions, mode, solutionsFound);
	}

	/**
	 * Used by solve
	 * Stops checking for solutions after it exceeds maximum count specified in SolverSettingsPanel.
	 * The final step for solving if all other steps don't work
	 * Having the least priority in all solving methods
	 * Recursive function - guesses one possibility of a cell as its supposed number
	 * @param puzzleSolutions where the solutions found are assinged to, can be null
	 * @param mode one of {@code FIND_ONE_SOLUTION, RETURN_IF_FINDS_SECOND_SOLUTION, KEEP_ON_LOOKING_UNTIL_MAX_REACHED }
	 * @return number of total solutions found, < 1 = invalid puzzle
	 */
	public static int guess(Sudoku sudoku, ArrayList<int[][]> puzzleSolutions, int mode, int solutionsFound)
	{
		int solveResult = 0;
		Sudoku sudokuCopy = sudoku.clone();
		int[] cellCopy;

		for (int n = 2; n < 10; n++) // loop through numbers 2 ~ 9, locate a cell with the smallest number of possibilities
		{
			for (int r = 0; r < 9; r++)
			{
				for (int c = 0; c < 9; c++)
				{
					if (sudokuCopy.status[r][c] == -n) // if that cell has exactly n possibilities
					{
						cellCopy = GridUtil.copyOf(sudokuCopy.grid[r][c]);

						for (int x = 0; x < 9; x++) // loop through all the possibilities of this cell
						{
							if (cellCopy[x] > 0) // set one possibility as that cell's only possibility
							{
								sudokuCopy.setValueAt(r, c, x + 1);
								recursed++;
								solveResult = solve(sudokuCopy.clone(), puzzleSolutions, mode, solutionsFound, r, c);

								if (solveResult > 0)
								{
									successfulGuess++;
									solutionsFound = solveResult;

									if (mode == FIND_ONE_SOLUTION)
									{
										assert solutionsFound == 1 : solutionsFound;
										return solutionsFound;
									}
									else if (mode == RETURN_IF_FINDS_SECOND_SOLUTION && solutionsFound > 1)
									{
										assert solutionsFound == 2 : solutionsFound;
										return solutionsFound;
									}
									else if (solutionsFound > (Integer) Application.solverSettingsPanel.maxSolutionsForSolveAll.getValue())
									{
										assert mode == KEEP_ON_LOOKING_UNTIL_MAX_REACHED : mode;
										return solutionsFound;
									}
								}
							}
						}
						if (solutionsFound > 0) return solutionsFound;
						else return -1;
					}
				}
			}
		}
		return -1;
	}

	public static final int ELIMINATED_INDEX = 0;
	public static final int SOLVED_INDEX = 1;
	public static final int VALIDITY_INDEX = 2;

	/**
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * Eliminates candidate n from all unsolved cells in the row, column, and box of the given index r, c
	 */
	public static void deleteInUnit(int[][][] solverHighlights, Sudoku sudoku, int r, int c, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		assert sudoku.status[r][c] > 0 : sudoku.status[r][c];
		deleteInRow(solverHighlights, sudoku, r, sudoku.status[r][c], eliminateRecursively, returnIfInvalid, results);
		if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;

		deleteInColumn(solverHighlights, sudoku, c, sudoku.status[r][c], eliminateRecursively, returnIfInvalid, results);
		if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;

		deleteInBox(solverHighlights, sudoku, r, c, sudoku.status[r][c], eliminateRecursively, returnIfInvalid, results);
	}

	/**
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * Eliminates candidate n from all unsolved cells in the given row r
	 */
	public static void deleteInRow(int[][][] solverHighlights, Sudoku sudoku, int r, int n, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		for (int c = 0; c < 9; c++)
		{
			if (Util.cellIsUnsolvedAndHasCandidate(sudoku, r, c, n))
			{
				results[ELIMINATED_INDEX]++;
				sudoku.grid[r][c][n - 1] = 0;
				sudoku.status[r][c] = GridUtil.getStatusForCell(sudoku.grid[r][c], sudoku.status[r][c] == -2);
				if (solverHighlights != null) solverHighlights[r][c][n - 1] = SolverPanel.ELIMINATED_CANDIDATE;

				if (returnIfInvalid && !Checker.checkValid(sudoku, r, c))
				{
					results[VALIDITY_INDEX] = -1;
					return;
				}

				if (sudoku.status[r][c] > 0) // cell becomes solved
				{
					results[SOLVED_INDEX]++;
					if (eliminateRecursively)
					{
						deleteInUnit(solverHighlights, sudoku, r, c, eliminateRecursively, returnIfInvalid, results);
						if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
					}
				}
			}
		}
	}

	/**
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * Eliminates candidate n from all unsolved cells in the given column c
	 */
	public static void deleteInColumn(int[][][] solverHighlights, Sudoku sudoku, int c, int n, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		for (int r = 0; r < 9; r++)
		{
			if (Util.cellIsUnsolvedAndHasCandidate(sudoku, r, c, n))
			{
				results[ELIMINATED_INDEX]++;
				sudoku.grid[r][c][n - 1] = 0;
				sudoku.status[r][c] = GridUtil.getStatusForCell(sudoku.grid[r][c], sudoku.status[r][c] == -2);
				if (solverHighlights != null) solverHighlights[r][c][n - 1] = SolverPanel.ELIMINATED_CANDIDATE;

				if (returnIfInvalid && !Checker.checkValid(sudoku, r, c))
				{
					results[VALIDITY_INDEX] = -1;
					return;
				}

				if (sudoku.status[r][c] > 0) // cell becomes solved
				{
					results[SOLVED_INDEX]++;
					if (eliminateRecursively)
					{
						deleteInUnit(solverHighlights, sudoku, r, c, eliminateRecursively, returnIfInvalid, results);
						if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
					}
				}
			}
		}
	}

	/**
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * Eliminates candidate n from all unsolved cells in the the box of the given index r, c
	 */
	public static void deleteInBox(int[][][] solverHighlights, Sudoku sudoku, int r, int c, int n, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
		int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;

		int tmpRow;
		int tmpCol;

		for (int ri = 0; ri < 3; ri++)
		{
			tmpRow = boxTopLeftCellR + ri;
			for (int ci = 0; ci < 3; ci++)
			{
				tmpCol = boxTopLeftCellC + ci;
				if (Util.cellIsUnsolvedAndHasCandidate(sudoku, tmpRow, tmpCol, n))
				{
					results[ELIMINATED_INDEX]++;
					sudoku.grid[tmpRow][tmpCol][n - 1] = 0;
					sudoku.status[tmpRow][tmpCol] = GridUtil.getStatusForCell(sudoku.grid[tmpRow][tmpCol], sudoku.status[tmpRow][tmpCol] == -2);
					if (solverHighlights != null) solverHighlights[tmpRow][tmpCol][n - 1] = SolverPanel.ELIMINATED_CANDIDATE;

					if (returnIfInvalid && !Checker.checkValid(sudoku, tmpRow, tmpCol))
					{
						results[VALIDITY_INDEX] = -1;
						return;
					}

					if (sudoku.status[tmpRow][tmpCol] > 0) // cell becomes solved
					{
						results[SOLVED_INDEX]++;
						if (eliminateRecursively)
						{
							deleteInUnit(solverHighlights, sudoku, tmpRow, tmpCol, eliminateRecursively, returnIfInvalid, results);
							if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
						}
					}
				}
			}
		}
	}

	/**
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * Find Solved cells in the entired board and call deleteInUnit
	 */
	public static void eliminate(int[][][] solverHighlights, Sudoku sudoku, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		int[][] statusCopy = GridUtil.copyOf(sudoku.status);
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				if (statusCopy[r][c] > 0)
				{
					deleteInUnit(solverHighlights, sudoku, r, c, eliminateRecursively, returnIfInvalid, results);
					if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
				}
			}
		}
	}

	/**
	 * Used directly by the solving technique tree
	 */
	public static Results eliminate(Board board, boolean eliminateRecursively, boolean returnIfInvalid, boolean compileMessages)
	{
		int[][] statusCopy = GridUtil.copyOf(board.sudoku.status);
		Sudoku sudokuCopy = board.sudoku.clone();
		ArrayList<String> messages = new ArrayList<>();
		StringBuilder messageBuilder = new StringBuilder();
		Results solveResults = new Results(sudokuCopy, 0, messages, SolvingTechnique.ELIMINATE);

		int[] eliminateResults = new int[3];

		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				if (statusCopy[r][c] > 0)
				{
					deleteInUnit(board.solverHighlights, sudokuCopy, r, c, eliminateRecursively, returnIfInvalid, eliminateResults);
					if (eliminateResults[ELIMINATED_INDEX] > 0)
					{
						solveResults.found++;
						if (compileMessages)
						{
							messageBuilder.append("Solved cell ");
							messageBuilder.append(digitsAndIndexesPanel.getStringIndex(r, c));
							messageBuilder.append(" helps eliminate candidate ");
							messageBuilder.append(digitsAndIndexesPanel.getDigit(statusCopy[r][c]));
							messageBuilder.append(" from ");
							messageBuilder.append(eliminateResults[ELIMINATED_INDEX]);
							messageBuilder.append(eliminateResults[ELIMINATED_INDEX] > 1 ? " other cells" : " other cell");
							messageBuilder.append(" in its unit.");
							messages.add(messageBuilder.toString());
							messageBuilder.delete(0, messageBuilder.length());
						}
						if (board.solverHighlights != null) board.solverHighlights[r][c][sudokuCopy.status[r][c] - 1] = SolverPanel.ONLY_CANDIDATE;
					}
					if (returnIfInvalid && eliminateResults[VALIDITY_INDEX] < 0) return solveResults;
					Arrays.fill(eliminateResults, 0);
				}
			}
		}
		return solveResults;
	}

	/**
	 * Deletes all matches of candidates in list candidates of cells in the same row as r, c
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * @param excludedCols the excluded column indexes
	 * @param inclusive if true include column c as well, if false don't check for r, c else check it also
	 * @param eliminateRecursively if false do not perform deleteInUnit on new solved cells
	 */
	public static void deleteCandidatesInRow(int[][][] solverHighlights, Sudoku sudoku, int[] candidates, int[] excludedCols, int r, int c, boolean eliminateRecursively, boolean returnIfInvalid, boolean inclusive, int[] results)
	{
		for (int ci = 0; ci < 9; ci++)
		{
			if ((ci != c || inclusive) && sudoku.status[r][ci] < 0)
			{
				if (Util.contains(excludedCols, ci) < 0)
				{
					deleteCandidatesInCell(solverHighlights, sudoku, candidates, r, ci, eliminateRecursively, returnIfInvalid, results);
					if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
				}
			}
		}
	}

	/**
	 * Deletes all matches of candidates in list candidates of cells in the same column as r, c
	 * @param excludedRows the excluded row indexes
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * @param inclusive if false don't check for r, c else check it also
	 * @param eliminateRecursively if true do not perform deleteInUnit on new solved cells
	 */
	public static void deleteCandidatesInCol(int[][][] solverHighlights, Sudoku sudoku, int[] candidates, int[] excludedRows, int r, int c, boolean eliminateRecursively, boolean returnIfInvalid, boolean inclusive, int[] results)
	{
		for (int ri = 0; ri < 9; ri++)
		{
			if ((ri != r || inclusive) && sudoku.status[ri][c] < 0)
			{
				if (Util.contains(excludedRows, ri) < 0)
				{
					deleteCandidatesInCell(solverHighlights, sudoku, candidates, ri, c, eliminateRecursively, returnIfInvalid, results);
					if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
				}
			}
		}
	}

	/**
	 * Deletes all matches of candidates in list candidates of cells in the same box as r, c
	 * @param excludedRows the excluded row indexes
	 * @param excludedCols the excluded column indexes
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * @param eliminateRecursively if true do not perform deleteInUnit on new solved cells
	 * @param inclusive if false don't check for r, c else check it also
	 */
	public static void deleteCandidatesInBox(int[][][] solverHighlights, Sudoku sudoku, int[] candidates, int[] excludedRowIndexes, int[] excludedColIndexes, int r, int c, boolean eliminateRecursively, boolean returnIfInvalid, boolean inclusive, int[] results)
	{
		int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
		int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;
		int tmpRow;
		int tmpCol;

		for (int ri = 0; ri < 3; ri++)
		{
			tmpRow = boxTopLeftCellR + ri;
			for (int ci = 0; ci < 3; ci++)
			{
				tmpCol = boxTopLeftCellC + ci;
				if ((tmpRow != r || tmpCol != c || inclusive) && sudoku.status[tmpRow][tmpCol] < 0)
				{
					if (Util.findIndex(excludedRowIndexes, excludedColIndexes, tmpRow, tmpCol) < 0)
					{
						deleteCandidatesInCell(solverHighlights, sudoku, candidates, tmpRow, tmpCol, eliminateRecursively, inclusive, results);
						if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return;
					}
				}
			}
		}
	}

	/**
	 * Removes all matches of candidates in list candidates from sudoku.grid[r][c]
	 * @param solverHighlights if != null, set deleted places to SolverPanel.ELIMINATED_CANDIDATE
	 * @param eliminateRecursively if true do not perform deleteInUnit on new solved cells
	 */
	public static void deleteCandidatesInCell(int[][][] solverHighlights, Sudoku sudoku, int[] candidates, int r, int c, boolean eliminateRecursively, boolean returnIfInvalid, int[] results)
	{
		if (sudoku.status[r][c] >= 0) return;

		for (int i = 0; i < candidates.length; i++)
		{
			if (sudoku.grid[r][c][candidates[i] - 1] > 0)
			{
				results[ELIMINATED_INDEX]++;
				sudoku.grid[r][c][candidates[i] - 1] = 0;
				if (solverHighlights != null) solverHighlights[r][c][candidates[i] - 1] = SolverPanel.ELIMINATED_CANDIDATE;
			}
		}

		sudoku.status[r][c] = GridUtil.getStatusForCell(sudoku.grid[r][c], false);
		if (sudoku.status[r][c] > 0) results[SOLVED_INDEX]++;

		if (!Checker.checkValid(sudoku, r, c))
		{
			results[VALIDITY_INDEX] = -1;
			if (returnIfInvalid) return;
		}

		if (sudoku.status[r][c] > 0 && eliminateRecursively)
		{
			deleteInUnit(solverHighlights, sudoku, r, c, true, returnIfInvalid, results);
		}
	}

	/**
	 * Looks for the only cell with a certain candidate in its row, column, or box
	 * Makes that caandidate the only value of that cell
     * @param returnFirst returns the first match, else return all
	 * @param compileMessages if true, compute string descriptions, else don't
	 */
	public static Results hiddenSingles(Board board, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, int[] results, boolean compileMessages)
	{
		Sudoku sudokuCopy = board.sudoku.clone();
		ArrayList<String> messages = new ArrayList<>();
		StringBuilder messageBuilder = new StringBuilder();
		Results solveResults = new Results(sudokuCopy, 0, messages, SolvingTechnique.HIDDEN_SINGLES);

		int[] rowCountResults = null;
		int[][] colCountResults = new int[9][];
		int[][] boxCountResults = new int[9][];

		//to record why a cell is a hidden single
		boolean[] uniqueInUnit = new boolean[3];
		int unqueInUnitCount; // unique in how many small units (row, col, box)

		for (int r = 0; r < 9; r++)
		{
			rowCountResults = UnitCheck.rowCountX(sudokuCopy, r, 1, UnitCheck.OCCURS_EXACTLY_X_TIMES);
			for (int c = 0; c < 9; c++)
			{
				int box_index = DigitsAndIndexesPanel.getBoxNumberFromRC(r, c);
				if (r == 0) colCountResults[c] = UnitCheck.colCountX(sudokuCopy, c, 1, UnitCheck.OCCURS_EXACTLY_X_TIMES);
				if (r % 3 == 0 && c % 3 == 0) boxCountResults[box_index] = UnitCheck.boxCountX(sudokuCopy, r, c, 1, UnitCheck.OCCURS_EXACTLY_X_TIMES);

				if (sudokuCopy.status[r][c] < 0)
				{
					for (int x = 0; x < 9; x++)
					{
						if (sudokuCopy.grid[r][c][x] > 0)
						{
							uniqueInUnit[0] = Util.contains(rowCountResults, x + 1) > -1;
							uniqueInUnit[1] = Util.contains(colCountResults[c], x + 1) > -1;
							uniqueInUnit[2] = Util.contains(boxCountResults[box_index], x + 1) > -1;

							if (uniqueInUnit[0] || uniqueInUnit[1] || uniqueInUnit[2]) // found it
							{
								solveResults.found++;
								if (board.solverHighlights != null) board.solverHighlights[r][c][x] = SolverPanel.ONLY_CANDIDATE;

								if (compileMessages)
								{
									unqueInUnitCount = 0; for (int i = 0; i < 3; i++) if (uniqueInUnit[i]) unqueInUnitCount++;
									messageBuilder.append("Hidden single found:");
									messageBuilder.append(System.lineSeparator());
									messageBuilder.append(Solver.messageOutputIndent);

									messageBuilder.append("Since ");
									messageBuilder.append(digitsAndIndexesPanel.getStringIndex(r, c));
									messageBuilder.append(" is the only cell with a candidate of ");
									messageBuilder.append(digitsAndIndexesPanel.getDigit(x + 1));
									messageBuilder.append(" in its ");
									messageBuilder.append(uniqueInUnit[0] ? unqueInUnitCount == 2 ? "row and " : "row, " : "");
									messageBuilder.append(uniqueInUnit[1] ? unqueInUnitCount == 2 && !uniqueInUnit[0] ? "column and " : "column, " : "");
									messageBuilder.append(uniqueInUnit[2] ? unqueInUnitCount == 3 ? "and box, " : "box, " : "");
									messageBuilder.append("it must be a ");
									messageBuilder.append(digitsAndIndexesPanel.getDigit(x + 1));
									messageBuilder.append(".");
									messages.add(messageBuilder.toString());
									messageBuilder.delete(0, messageBuilder.length());
								}

								sudokuCopy.setValueAt(r, c, x + 1);
								if (returnIfInvalid && !Checker.checkValid(sudokuCopy, r, c)) return solveResults;

								if (eliminateRecursively)
								{
									deleteInUnit(board.solverHighlights, sudokuCopy, r, c, eliminateRecursively, returnIfInvalid, results);
								}

								if (returnFirst) return solveResults;
								break; // for loop
							}
						}
					}
				}
			}
		}
		return solveResults;
	}

	/**
     * Looks for the entire board for a group of n cells inthat are in the same unit
     * such that altogether they have n unique candidates in total
     * @param returnFirst returns the first match, else return all
     * @param compileMessages if true, compute string descriptions
     */
    public static Results nakedCandidates(Board board, int n, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, int[] results, boolean compileMessages)
    {
        // for searching a combo of n cells in the same unit that meet the requirements
        ArrayList<Integer> possibleRowIndexes = new ArrayList<>();
        ArrayList<Integer> possibleColIndexes = new ArrayList<>();
        ArrayList<int[]> possibleCandidates = new ArrayList<>();

		Sudoku sudokuCopy = board.sudoku.clone();

		SolvingTechnique technique;
		switch (n)
		{
			case 2: technique = SolvingTechnique.NAKED_PAIRS; break;
			case 3: technique = SolvingTechnique.NAKED_TRIPLES; break;
			case 4: technique = SolvingTechnique.NAKED_QUADS; break;
			default: technique = SolvingTechnique.NAKED_CANDIDATES;
		}
		Results solveResults = new Results(sudokuCopy, 0, new ArrayList<String>(), technique);

        for (int r = 0; r < 9; r++)
        {
            possibleRowIndexes.clear();
            possibleColIndexes.clear();
            possibleCandidates.clear();
            for (int c = 0; c < 9; c++)
            {
                if (board.sudoku.status[r][c] < 0 && board.sudoku.status[r][c] * -1 <= n)
                {
                    possibleRowIndexes.add(r);
                    possibleColIndexes.add(c);
                    possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, r, c));
                }
            }
            if (possibleRowIndexes.size() >= n)
            {
            	SolverUtil.findNakedCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_ROW, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
				if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
            	if (returnFirst && solveResults.found > 0) return solveResults;
            }
        }

        for (int c = 0; c < 9; c++)
        {
            possibleRowIndexes.clear();
            possibleColIndexes.clear();
            possibleCandidates.clear();
            for (int r = 0; r < 9; r++)
            {
                if (board.sudoku.status[r][c] < 0 && board.sudoku.status[r][c] * -1 <= n)
                {
                    possibleRowIndexes.add(r);
                    possibleColIndexes.add(c);
                    possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, r, c));
                }
            }
			if (possibleRowIndexes.size() >= n)
			{
				SolverUtil.findNakedCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_COLUMN, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
				if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
				if (returnFirst && solveResults.found > 0) return solveResults;
			}
        }

        for (int boxTopLeftCellR = 0; boxTopLeftCellR < 9; boxTopLeftCellR += 3)
        {
            for (int boxTopLeftCellC = 0; boxTopLeftCellC < 9; boxTopLeftCellC += 3)
            {
                possibleRowIndexes.clear();
                possibleColIndexes.clear();
                possibleCandidates.clear();
                for (int ri = 0; ri < 3; ri++)
                {
                    for (int ci = 0; ci < 3; ci++)
                    {
                        if (board.sudoku.status[boxTopLeftCellR + ri][boxTopLeftCellC + ci] < 0 && board.sudoku.status[boxTopLeftCellR + ri][boxTopLeftCellC + ci] * -1 <= n)
                        {
                            possibleRowIndexes.add(boxTopLeftCellR + ri);
                            possibleColIndexes.add(boxTopLeftCellC + ci);
                            possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, boxTopLeftCellR + ri, boxTopLeftCellC + ci));
                        }
                    }
                }
				if (possibleRowIndexes.size() >= n)
				{
					SolverUtil.findNakedCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_BOX, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
					if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
					if (returnFirst && solveResults.found > 0) return solveResults;
				}
            }
        }
        return solveResults;
    }

	/**
	 * This function is pretty much entirely the same as nakedCandidates
     * Looks for the entire board for a group of n cells inthat are in the same unit
     * such that altogether they have more than n different candidates (not a naked group)
     * and share n candidates unique to their unit while some of
     * Their other candidates then can be eliminated
     * @param returnFirst returns the first match, else return all
	 * @param compileMessages if true, compute string descriptions, else don't
     */
    public static Results hiddenCandidates(Board board, int n, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, int[] results, boolean compileMessages)
    {
		// for searching a combo of n cells in the same unit that meet the requirements
        ArrayList<Integer> possibleRowIndexes = new ArrayList<>();
        ArrayList<Integer> possibleColIndexes = new ArrayList<>();
        ArrayList<int[]> possibleCandidates = new ArrayList<>();

		Sudoku sudokuCopy = board.sudoku.clone();

		SolvingTechnique technique;
		switch (n)
		{
			case 2: technique = SolvingTechnique.HIDDEN_PAIRS; break;
			case 3: technique = SolvingTechnique.HIDDEN_TRIPLES; break;
			case 4: technique = SolvingTechnique.HIDDEN_QUADS; break;
			case 1: technique = SolvingTechnique.HIDDEN_SINGLES; break;
			default: technique = SolvingTechnique.HIDDEN_CANDIDATES;
		}
		Results solveResults = new Results(sudokuCopy, 0, new ArrayList<String>(), technique);

        for (int r = 0; r < 9; r++)
        {
			possibleRowIndexes.clear();
			possibleColIndexes.clear();
			possibleCandidates.clear();
			for (int c = 0; c < 9; c++)
			{
				if (board.sudoku.status[r][c] < 0)
				{
					possibleRowIndexes.add(r);
					possibleColIndexes.add(c);
					possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, r, c));
				}
			}
			if (possibleRowIndexes.size() >= n)
			{
				SolverUtil.findHiddenCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_ROW, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
				if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
				if (returnFirst && solveResults.found > 0) return solveResults;
			}
		}

        for (int c = 0; c < 9; c++)
        {
			possibleRowIndexes.clear();
			possibleColIndexes.clear();
			possibleCandidates.clear();
			for (int r = 0; r < 9; r++)
			{
				if (board.sudoku.status[r][c] < 0)
				{
					possibleRowIndexes.add(r);
					possibleColIndexes.add(c);
					possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, r, c));
				}
			}
			if (possibleRowIndexes.size() >= n)
			{
				SolverUtil.findHiddenCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_COLUMN, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
				if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
				if (returnFirst && solveResults.found > 0) return solveResults;
			}
        }

		for (int boxTopLeftCellR = 0; boxTopLeftCellR < 9; boxTopLeftCellR += 3)
		{
			for (int boxTopLeftCellC = 0; boxTopLeftCellC < 9; boxTopLeftCellC += 3)
			{
				possibleRowIndexes.clear();
				possibleColIndexes.clear();
				possibleCandidates.clear();
				for (int ri = 0; ri < 3; ri++)
				{
					for (int ci = 0; ci < 3; ci++)
					{
						if (board.sudoku.status[boxTopLeftCellR + ri][boxTopLeftCellC + ci] < 0)
						{
							possibleRowIndexes.add(boxTopLeftCellR + ri);
							possibleColIndexes.add(boxTopLeftCellC + ci);
							possibleCandidates.add(SolverUtil.getCandidates(sudokuCopy, boxTopLeftCellR + ri, boxTopLeftCellC + ci));
						}
					}
				}
				if (possibleRowIndexes.size() >= n)
				{
					SolverUtil.findHiddenCandidates(board, possibleRowIndexes, possibleColIndexes, possibleCandidates, n, UnitCheck.SAME_BOX, returnFirst, eliminateRecursively, returnIfInvalid, compileMessages, results, solveResults);
					if (returnIfInvalid && results[VALIDITY_INDEX] < 0) return  solveResults;
					if (returnFirst && solveResults.found > 0) return solveResults;
				}
			}
		}
		return solveResults;
    }

	/**
     * Searches each box in the whole board for instances
     * where all of a box's candidate x are located in the same row/col (only pointing pairs and triples)
     * then will remove all of candidate x from that row/column's other cells not in the box
     * @param returnFirst returns the first match, else return all
     * @param computeMessages if true, compute String descriptions, else don't
     */
    public static Results pointingCandidates(Board board, boolean returnFirst, boolean eliminateRecursively,  boolean returnIfInvalid, int[] results, boolean compileMessages)
    {
    	Sudoku sudokuCopy = board.sudoku.clone();
		StringBuilder messageBuilder = new StringBuilder();
		ArrayList<String> messages = new ArrayList<>();
        Results solveResults = new Results(sudokuCopy, 0, messages, SolvingTechnique.POINTING_CANDIDATES);

        boolean sameRow;
        boolean sameColumn;
        int[] foundRowIndexes;
        int[] foundColIndexes;
        int[] eliminateResults = new int[3];

        Object[] boxCountResults;
        for (int boxr = 0; boxr < 3; boxr++)
        {
            for (int boxc = 0; boxc < 3; boxc++)
            {
                boxCountResults = UnitCheck.boxCount(board.sudoku, boxr * 3, boxc * 3);
                if (boxCountResults != null)
                {
                    for (int n = 0; n < 9; n++)
                    {
                        if (((int[]) boxCountResults[0])[n] == 2 || ((int[]) boxCountResults[0])[n] == 3) // a box can only fit up to 3 candidates in a row/col
                        {
                            sameRow = Util.sameIndex(((int[][]) boxCountResults[1])[n]);
                            sameColumn = Util.sameIndex(((int[][]) boxCountResults[2])[n]);

                            if (sameRow || sameColumn)
                            {
                                foundRowIndexes = ((int[][]) boxCountResults[1])[n];
                                foundColIndexes = ((int[][]) boxCountResults[2])[n];

								// to see if this group helps eliminate
								Arrays.fill(eliminateResults, 0);
								if (sameRow) { Solver.deleteCandidatesInRow(board.solverHighlights, sudokuCopy, new int[] {n + 1}, foundColIndexes, foundRowIndexes[0], foundColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults); }
								else { Solver.deleteCandidatesInCol(board.solverHighlights, sudokuCopy, new int[] {n + 1}, foundRowIndexes, foundRowIndexes[0], foundColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults); }

								results[0] += eliminateResults[0];
								results[1] += eliminateResults[1];
								results[2] += eliminateResults[2];

                                if (eliminateResults[ELIMINATED_INDEX] > 0) // found it
                                {
                                	solveResults.found++;
									if (board.solverHighlights != null)
									{
										for (int i = 0; i < foundRowIndexes.length; i++)
										{
											board.solverHighlights[foundRowIndexes[i]][foundColIndexes[i]][n] = SolverPanel.ONLY_CANDIDATE;
										}
									}

                                    if (compileMessages)
                                    {
										messageBuilder.append("Pointing ");
										messageBuilder.append(((int[]) boxCountResults[0])[n] == 2 ? "Pair" : "Triple");
										messageBuilder.append(" found in box ");
										messageBuilder.append(digitsAndIndexesPanel.getBoxIndex(boxr, boxc));
										messageBuilder.append(":");
										messageBuilder.append(System.lineSeparator());
										messageBuilder.append(Solver.messageOutputIndent);

										messageBuilder.append("Since all of the digit ");
										messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
										messageBuilder.append("s of this box are in ");
										messageBuilder.append(sameRow ? "row " : "column ");
										messageBuilder.append(sameRow ? digitsAndIndexesPanel.getRowIndex(foundRowIndexes[0]) : digitsAndIndexesPanel.getColIndex(foundColIndexes[0]));
										messageBuilder.append(", digit ");
										messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
										messageBuilder.append(" in this ");
										messageBuilder.append(sameRow ? "row" : "column");
										messageBuilder.append(" has to be in box ");
										messageBuilder.append(digitsAndIndexesPanel.getBoxIndex(boxr, boxc));
										messageBuilder.append(".");
										messageBuilder.append(System.lineSeparator());
										messageBuilder.append(Solver.messageOutputIndent);

										messageBuilder.append("This eliminates the rest of the digit ");
										messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
										messageBuilder.append("s not in box ");
										messageBuilder.append(digitsAndIndexesPanel.getBoxIndex(boxr, boxc));
										messageBuilder.append(" from ");
										messageBuilder.append(sameRow ? "row " : "column ");
										messageBuilder.append(sameRow ? digitsAndIndexesPanel.getRowIndex(foundRowIndexes[0]) : digitsAndIndexesPanel.getColIndex(foundColIndexes[0]));
										messageBuilder.append(".");
										messages.add(messageBuilder.toString());
										messageBuilder.delete(0, messageBuilder.length());
                                    }
									if (returnFirst) return solveResults;
                                }
								if (returnIfInvalid && eliminateResults[VALIDITY_INDEX] < 0) return solveResults;
                            }
                        }
                    }
                }
            }
        }
        return solveResults;
    }

    /**
     * Searches each row and column of board for instances
     * where all of row/column's candidate x are located in one box (only pairs and triples),
     * then will remove all of candidate x in that box that are not in that row/column.
     * @param returnFirst returns the first match, else return all matches.
     * @param compileMessages if true, compute String messages, else don't
     */
    public static Results boxLineReduction(Board board, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, int[] results, boolean compileMessages)
    {
        int[] foundRowIndexes;
        int[] foundColIndexes;
		int[] eliminateResults = new int[3];
        Object[] unitCheckResults;

		Sudoku sudokuCopy = board.sudoku.clone();
		StringBuilder messageBuilder = new StringBuilder();
		ArrayList<String> messages = new ArrayList<>();
		Results solveResults = new Results(sudokuCopy, 0, messages, SolvingTechnique.BOX_LINE_REDUCTION);

        for (int r = 0; r < 9; r++) // see if all candidate n in row r are in the same box
        {
			unitCheckResults = UnitCheck.rowCount(board.sudoku, r);
            for (int n = 0; n < 9; n++)
            {
                if (((int[]) unitCheckResults[0])[n] == 2 || ((int[]) unitCheckResults[0])[n] == 3) // a box can only fit up to 3 candidates in a row
                {
                    if (UnitCheck.sameBox(((int[][]) unitCheckResults[1])[n], ((int[][]) unitCheckResults[2])[n]))
                    {
                        foundRowIndexes = ((int[][]) unitCheckResults[1])[n];
                        foundColIndexes = ((int[][]) unitCheckResults[2])[n];

                        Arrays.fill(eliminateResults, 0);
						deleteCandidatesInBox(board.solverHighlights, sudokuCopy, new int[] {n + 1}, foundRowIndexes, foundColIndexes, foundRowIndexes[0], foundColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults);
						results[0] += eliminateResults[0];
						results[1] += eliminateResults[1];
						results[2] += eliminateResults[2];

                        if (eliminateResults[ELIMINATED_INDEX] > 0) // found it
						{
							solveResults.found++;
							if (board.solverHighlights != null)
							{
								for (int i = 0; i < foundRowIndexes.length; i++)
								{
									board.solverHighlights[foundRowIndexes[i]][foundColIndexes[i]][n] = SolverPanel.ONLY_CANDIDATE;
								}
							}

							if (compileMessages)
							{
								messageBuilder.append("Box Line Reduction found:");
								messageBuilder.append(System.lineSeparator());
								messageBuilder.append(Solver.messageOutputIndent);

								messageBuilder.append("since all of the digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append("s of row ");
								messageBuilder.append(digitsAndIndexesPanel.getRowIndex(r));
								messageBuilder.append(" are in box ");
								messageBuilder.append(digitsAndIndexesPanel.getBoxIndexFromRC(foundRowIndexes[0], foundColIndexes[0]));
								messageBuilder.append(", digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append(" of that box has to be in row ");
								messageBuilder.append(digitsAndIndexesPanel.getRowIndex(r));
								messageBuilder.append(".");
								messageBuilder.append(System.lineSeparator());
								messageBuilder.append(Solver.messageOutputIndent);

								messageBuilder.append("This eliminates the rest of the digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append(" from box ");
								messageBuilder.append(digitsAndIndexesPanel.getBoxIndexFromRC(foundRowIndexes[0], foundColIndexes[0]));
								messageBuilder.append(" that are not in row ");
								messageBuilder.append(digitsAndIndexesPanel.getRowIndex(r));
								messageBuilder.append(".");
								messages.add(messageBuilder.toString());
								messageBuilder.delete(0, messageBuilder.length());
							}
							if (returnFirst) return solveResults;
						}
						if (returnIfInvalid && eliminateResults[VALIDITY_INDEX] < 0) return solveResults;
                    }
                }
            }
        }

        for (int c = 0; c < 9; c++) // see if all candidate n in column c are in the same box
        {
			unitCheckResults = UnitCheck.colCount(board.sudoku, c);
            for (int n = 0; n < 9; n++)
            {
                if (((int[]) unitCheckResults[0])[n] == 2 || ((int[]) unitCheckResults[0])[n] == 3) // a box can only fit up to 3 candidates in a column
                {
                    if (UnitCheck.sameBox(((int[][]) unitCheckResults[1])[n], ((int[][]) unitCheckResults[2])[n]))
                    {
                        foundRowIndexes = ((int[][]) unitCheckResults[1])[n];
                        foundColIndexes = ((int[][]) unitCheckResults[2])[n];

						Arrays.fill(eliminateResults, 0);
						deleteCandidatesInBox(board.solverHighlights, sudokuCopy, new int[] {n + 1}, foundRowIndexes, foundColIndexes, foundRowIndexes[0], foundColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults);
						results[0] += eliminateResults[0];
						results[1] += eliminateResults[1];
						results[2] += eliminateResults[2];

						if (eliminateResults[ELIMINATED_INDEX] > 0) // found it
                        {
                        	solveResults.found++;
							if (board.solverHighlights != null)
							{
								for (int i = 0; i < foundRowIndexes.length; i++)
								{
									board.solverHighlights[foundRowIndexes[i]][foundColIndexes[i]][n] = SolverPanel.ONLY_CANDIDATE;
								}
							}

                            if (compileMessages)
                            {
								messageBuilder.append("Box Line Reduction found:");
								messageBuilder.append(System.lineSeparator());
								messageBuilder.append(Solver.messageOutputIndent);

								messageBuilder.append("Since all of the digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append("s of column ");
								messageBuilder.append(digitsAndIndexesPanel.getColIndex(c));
								messageBuilder.append(" are in box ");
								messageBuilder.append(digitsAndIndexesPanel.getBoxIndexFromRC(foundRowIndexes[0], foundColIndexes[0]));
								messageBuilder.append(", digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append(" of that box has to be in column ");
								messageBuilder.append(digitsAndIndexesPanel.getColIndex(c));
								messageBuilder.append(".");
								messageBuilder.append(System.lineSeparator());
								messageBuilder.append(Solver.messageOutputIndent);

								messageBuilder.append("This eliminates the rest of the digit ");
								messageBuilder.append(digitsAndIndexesPanel.getDigit(n + 1));
								messageBuilder.append(" from box ");
								messageBuilder.append(digitsAndIndexesPanel.getBoxIndexFromRC(foundRowIndexes[0], foundColIndexes[0]));
								messageBuilder.append(" that are not in column ");
								messageBuilder.append(digitsAndIndexesPanel.getColIndex(c));
								messageBuilder.append(".");
								messages.add(messageBuilder.toString());
								messageBuilder.delete(0, messageBuilder.length());
                            }
							if (returnFirst) return solveResults;
                        }
						if (returnIfInvalid && eliminateResults[VALIDITY_INDEX] < 0) return solveResults;
                    }
                }
            }
        }
        return solveResults;
    }
}
