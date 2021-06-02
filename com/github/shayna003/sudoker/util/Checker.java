package com.github.shayna003.sudoker.util;

import com.github.shayna003.sudoker.*;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * For checking validity of a grid, checking whether it is solved, compare 2 grids, and see if status matches grid
 * @version 0.0.0
 * @since 2020-11-1
 */
public class Checker
{
    /**
     * Checks if a grid is considered solved
     * @return results[0]: 1 if all conditions met
     * @return results[1]: number of solved cells
     * @return results[2]: 1 if checkValid(status) == true, else 0 (currently only checks for repeats of solved cells in row, column, and box)
     * @return results[3]: return value of statusMatchGrid(), should be 0 to be valid
     */
    public static int[] checkIfPuzzleSolved(Sudoku sudoku, boolean fullCheck)
    {
        int[] results = new int[4];
        results[1] = solvedCellCount(sudoku);
        results[2] = checkValid(sudoku) ? 1 : 0;
        results[3] = statusMatchGrid(sudoku, fullCheck, false);
        if (results[3] != 0)
        {
            throw new GridUtil.GridAndStatusDoNotMatchException("places that don't match: " + results[3]);
        }
        results[0] = results[1] < 81 ? 0 : (results[2] == 0 ? 0 : (results[3] > 0 ? 0 : 1));
        return results;
    }

    /**
     * Only used by checkIfPuzzleSolved and SolverPanel
     * @return number of solved cells in a status
     */
    public static int solvedCellCount(Sudoku sudoku)
    {
        int solved = 0;
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                if (sudoku.status[r][c] > 0) solved++;
            }
        }
        return solved;
    }

    /**
     * A smaller, faster version of checkValid, only for a new solved cell
     */
     public static boolean checkValid(Sudoku sudoku, int r, int c)
     {
         if (sudoku.status[r][c] == 0) { return false; }
         if (sudoku.status[r][c] < 0) return true;

         if (UnitCheck.occurrenceCountInRow(sudoku, r, c, true) > 1) { return false; }
         if (UnitCheck.occurrenceCountInCol(sudoku, r, c, true) > 1) { return false; }
         if (UnitCheck.occurrenceCountInBox(sudoku, r, c, true) > 1) { return false; }
         return true;
     }

    /**
     * Only called by checkIfPuzzleSolved
     * Checks the validity of the puzzle: if two of the same number in the same row, column, or box
     * This is based on status[][] alone
     */
    public static boolean checkValid(Sudoku sudoku)
    {
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                if (!checkValid(sudoku, r, c))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Only used by Checker and IO when importing a new Sudoku
     * checks if grid and status match each other
     * @param fixIncorrectStatus if true, make status match grid
     * @return number of places where they don't match (max 81)
     */
    public static int statusMatchGrid(Sudoku sudoku, boolean fullCheck, boolean fixIncorrectStatus)
    {
        int supposedStatus;
        int counter = 0;
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                supposedStatus = GridUtil.getStatusForCell(sudoku.grid[r][c], false);
                if (supposedStatus != sudoku.status[r][c])
                {
                    counter++;
                    if (fixIncorrectStatus) sudoku.status[r][c] = supposedStatus;
                    if (!fullCheck) return counter;
                }
            }
        }
        return counter;
    }
}
