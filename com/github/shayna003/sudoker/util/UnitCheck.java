package com.github.shayna003.sudoker.util;

import com.github.shayna003.sudoker.*;
import java.util.*;

/**
 * This class provides functions to search for things in a unit, and see if cells are in the same unit
 * @version 0.0.0 
 * @since 2020-11-1
 * last modified: 5-25-2021
 */
public class UnitCheck
{
    /**
     * @return whether 2 indexes are in the same grid
     */
    public static boolean sameBox(int r, int c, int r2, int c2)
    {
        return Math.floorDiv(r, 3) == Math.floorDiv(r2, 3) && Math.floorDiv(c, 3) == Math.floorDiv(c2, 3);
    }

    /**
     * @return whether all the indexes of rx, cx are in the same grid
     */
    public static boolean sameBox(int[] rx, int[] cx)
    {
        int boxr = Math.floorDiv(rx[0], 3);
        int boxc = Math.floorDiv(cx[0], 3);

        for (int i = 1; i < rx.length; i++)
        {
            if (Math.floorDiv(rx[i], 3) != boxr || Math.floorDiv(cx[i], 3) != boxc) return false;
        }
        return true;
    }

    /**
     * @return whether all the indexes of rx, cx are in the same grid
     */
    public static boolean sameBox(ArrayList<Integer> rx, ArrayList<Integer> cx)
    {
        int boxr = Math.floorDiv(rx.get(0), 3);
        int boxc = Math.floorDiv(cx.get(0), 3);
        for (int i = 1; i < rx.size(); i++)
        {
            if (Math.floorDiv(rx.get(i), 3) != boxr|| Math.floorDiv(cx.get(i), 3) != boxc) return false;
        }
        return true;
    }

    /**
     * @return whether all the indexes [of indexes in selected] of rx, cx are in the same grid
     */
    public static boolean sameBox(int[] rx, int[] cx, int[] selected)
    {
        int boxr = Math.floorDiv(rx[selected[0]], 3);
        int boxc = Math.floorDiv(cx[selected[0]], 3);
        for (int i = 1; i < selected.length; i++)
        {
            if (Math.floorDiv(rx[selected[i]], 3) != boxr|| Math.floorDiv(cx[selected[i]], 3) != boxc) return false;
        }
        return true;
    }

    /**
     * @return whether all the indexes [of indexes in selected] of rx, cx are in the same grid
     */
    public static boolean sameBox(ArrayList<Integer> rx, ArrayList<Integer> cx, int[] selected)
    {
        int boxr = Math.floorDiv(rx.get(selected[0]), 3);
        int boxc = Math.floorDiv(cx.get(selected[0]), 3);
        for (int i = 1; i < selected.length; i++)
        {
            if (Math.floorDiv(rx.get(selected[i]), 3) != boxr|| Math.floorDiv(cx.get(selected[i]), 3) != boxc) return false;
        }
        return true;
    }

    // used to indicate how a group of cells should relate to each other, and also the meaning of each item returend by sameUnit()
    public static final int SAME_UNIT = 0;
    public static final int SAME_ROW = 1;
    public static final int SAME_COLUMN = 2;
    public static final int SAME_BOX = 3;

    /**
     * Used by Board to paint highlights
     * @return unit true/false: [SAME_UNIT] if in same unit, [SAME_ROW] if in same row, [SAME_COLUMN] if in same column, [SAME_BOX] if in same box
     */
    public static boolean[] sameUnit(int r, int c, int r2, int c2)
    {
        boolean[] relations = new boolean[4];
        if (r == r2)
        {
            relations[SAME_UNIT] = true;
            relations[SAME_ROW] = true;
        }
        if (c == c2)
        {
            relations[SAME_UNIT] = true;
            relations[SAME_COLUMN] = true;
        }
        if (sameBox(r, c, r2, c2))
        {
            relations[SAME_UNIT] = true;
            relations[SAME_BOX] = true;
        }
        return relations;
    }

    /**
     * Used by Board to paint highlights
     * @return whether 2 indexes are in the same box row (e.g. box 0,0 and box 0,2)
     */
    public static boolean sameBoxRow(int r, int r2)
    {
        return Math.floorDiv(r, 3) == Math.floorDiv(r2, 3);
    }

    /**
     * Used by Board to paint highlights
     * @return whether 2 indexes are in the same box column (e.g. box 0,2 and box 2,2)
     */
    public static boolean sameBoxCol(int c, int c2)
    {
        return Math.floorDiv(c, 3) == Math.floorDiv(c2, 3);
    }

    /**
     * Use by BoxLineReduction.
     * @return [0] array of how many times each candidate (1 ~ 9) appeared in the row r
     * [1] int[][] of row indexes of each cell with a certain candidate, all = r
     * [2] int[][] of column indexes of each cell with a certain candidate
     * format:
     * {{ca1, ca2, ca3, ca4, ca5, ca6, ca7, ca8, ca9},
     * {[0]{r1, r2, r3}, [1]{r1, r2, r3} ... [8]{...}},
     * {[1]{c1, c2, c3}, [1]{c4, c5, c6} ... [8]{...}}
     * }
     */
    @SuppressWarnings("unchecked")
    public static Object[] rowCount(Sudoku sudoku, int r)
    {
        Object[] results = new Object[3];
        Object[] rx = new Object[9]; //records the row indexes of cells with a certain candidate, all = r
        Object[] cx = new Object[9]; //records the column indexes of cells with a certain candidate
        for (int i = 0; i < 9; i++)
        {
            rx[i] = new ArrayList<Integer>();
            cx[i] = new ArrayList<Integer>();
        }

        int[] record = new int[9]; //records the number of occurrences for each of the 9 candidates
        for (int c = 0; c < 9; c++)
        {
            for (int n = 0; n < 9; n++)
            {
                if (sudoku.grid[r][c][n] > 0)
                {
                    record[n]++;
                    ((ArrayList) (rx[n])).add(r);
                    ((ArrayList) (cx[n])).add(c);
                }
            }
        }

        int[][] rx_tmp = new int[9][];
        int[][] cx_tmp = new int[9][];
        for (int i = 0; i < 9; i++)
        {
            cx_tmp[i] = Util.getArray((ArrayList) cx[i]);
            rx_tmp[i] = Util.getArray((ArrayList) rx[i]);
        }

        results[0] = record;
        results[1] = rx_tmp;
        results[2] = cx_tmp;
        return results;
    }

    /**
     * Used by BoxLineReduction
     * @return [0] array of how many times each candidate (1 ~ 9) appeared in the column c
     * [1] int[][] of row indexes of each cell with a certain candidate
     * [2] int[][] of col indexes of each cell with a certain candidate, all = c
     * {{ca1, ca2, ca3, ca4, ca5, ca6, ca7, ca8, ca9},
     * {[0]{r1, r2, r3}, [1]{r1, r2, r3} ... [8]{...}},
     * {[1]{c1, c2, c3}, [1]{c4, c5, c6} ... [8]{...}}
     * }
     */
    @SuppressWarnings("unchecked")
    public static Object[] colCount(Sudoku sudoku, int c)
    {
        Object[] results = new Object[3];
        Object[] rx = new Object[9]; //records the row indexes of cells with a certain candidate
        Object[] cx = new Object[9]; //records the col indexes of cells with a certain candidate, all = c
        for (int i = 0; i < 9; i++)
        {
            rx[i] = new ArrayList<Integer>();
            cx[i] = new ArrayList<Integer>();
        }

        int[] record = new int[9]; //records the number of occurrences for each of the 9 candidates
        for (int r = 0; r < 9; r++)
        {
            for (int n = 0; n < 9; n++)
            {
                if (sudoku.grid[r][c][n] > 0)
                {
                    record[n]++;
                    ((ArrayList) (rx[n])).add(r);
                    ((ArrayList) (cx[n])).add(c);
                }
            }
        }

        int[][] rx_tmp = new int[9][];
        int[][] cx_tmp = new int[9][];
        for (int i = 0; i < 9; i++)
        {
            rx_tmp[i] = Util.getArray((ArrayList) rx[i]);
            cx_tmp[i] = Util.getArray((ArrayList) cx[i]);
        }

        results[0] = record;
        results[1] = rx_tmp;
        results[2] = cx_tmp;
        return results;
    }

    /**
     * Used by Solver.pointingCandidates
     * @return [0] array of how many times each candidate (1 ~ 9) appeared in the box of r, c
     * [1] int[][] of row indexes of each cell with a certain candidate
     * [2] int[][] of column indexes of each cell with a certain candidate
     * format:
     * {{ca1, ca2, ca3, ca4, ca5, ca6, ca7, ca8, ca9},
     * {[0]{r1, r2, r3}, [1]{r1, r2, r3} ... [8]{...}},
     * {[1]{c1, c2, c3}, [1]{c4, c5, c6} ... [8]{...}}
     * }
     */
    @SuppressWarnings("unchecked")
    public static Object[] boxCount(Sudoku sudoku, int r, int c)
    {
        Object[] results = new Object[3];
        Object[] rx = new Object[9]; //records the row indexes of cells with a certain candidate
        Object[] cx = new Object[9]; //records the column indexes of cells with a certain candidate
        for (int i = 0; i < 9; i++)
        {
            rx[i] = new ArrayList<Integer>();
            cx[i] = new ArrayList<Integer>();
        }

        int[] record = new int[9]; //records the number of occurrences for each of the 9 candidates
        int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
        int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;

        for (int ri = 0; ri < 3; ri++)
        {
            for (int ci = 0; ci < 3; ci++)
            {
                for (int n = 0; n < 9; n++)
                {
                    if (sudoku.grid[boxTopLeftCellR + ri][boxTopLeftCellC + ci][n] > 0)
                    {
                        record[n]++;
                        ((ArrayList) (rx[n])).add(boxTopLeftCellR + ri);
                        ((ArrayList) (cx[n])).add(boxTopLeftCellC + ci);
                    }
                }
            }
        }

        int[][] rx_tmp = new int[9][];
        int[][] cx_tmp = new int[9][];
        for (int i = 0; i < 9; i++)
        {
            rx_tmp[i] = Util.getArray((ArrayList) rx[i]);
            cx_tmp[i] = Util.getArray((ArrayList) cx[i]);
        }

        results[0] = record;
        results[1] = rx_tmp;
        results[2] = cx_tmp;
        return results;
    }

    public static final int OCCURS_EXACTLY_X_TIMES = 0;
    public static final int OCCURS_AT_LEAST_X_TIMES = 1;
    public static final int OCCURS_AT_MOST_X_TIMES = 2;

    /**
     * Used by Hidden Singles
     * @return a list of candidates that occurred
     * == x, >= x, or <= x times in row r depending on mode
     */
    public static int[] rowCountX(Sudoku sudoku, int r, int x, int mode)
    {
        ArrayList<Integer> results = new ArrayList<>();
        int[] record = new int[9]; // records the number of occurrences for each of the 9 candidates

        for (int c = 0; c < 9; c++)
        {
            if (sudoku.status[r][c] > 0)
            {
                record[sudoku.status[r][c] - 1]++;
            }
            else
            {
                for (int n = 0; n < 9; n++)
                {
                    if (sudoku.grid[r][c][n] > 0)
                    {
                        record[n]++;
                    }
                }
            }
        }

        for (int i = 0; i < 9; i++)
            if (record[i] == x || (record[i] > x && mode == OCCURS_AT_LEAST_X_TIMES) || (record[i] < x && mode == OCCURS_AT_MOST_X_TIMES))
                results.add(i + 1);
        return Util.getArray(results);
    }

    /**
     * Used by Hidden Singles
     * @return a list of candidates that occurred
     * == x, >= x, or <= x times in the box of r, c depending on mode
     */
    public static int[] boxCountX(Sudoku sudoku, int r, int c, int x, int mode)
    {
        ArrayList<Integer> results = new ArrayList<>();
        int[] record = new int[9];

        int boxTopLeftCellR =  Math.floorDiv(r, 3) * 3;
        int boxTopLeftCellC =  Math.floorDiv(c, 3) * 3;
        int tmpRow;
        int tmpCol;

        for (int ri = 0; ri < 3; ri++)
        {
            tmpRow = boxTopLeftCellR + ri;
            for (int ci = 0; ci < 3; ci++)
            {
                tmpCol = boxTopLeftCellC + ci;
                if (sudoku.status[tmpRow][tmpCol] > 0)
                {
                    record[sudoku.status[tmpRow][tmpCol] - 1]++;
                }
                else
                {
                    for (int n = 0; n < 9; n++)
                    {
                        if (sudoku.grid[tmpRow][tmpCol][n] > 0)
                        {
                            record[n]++;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < 9; i++)
            if (record[i] == x || (record[i] > x && mode == OCCURS_AT_LEAST_X_TIMES) || (record[i] < x && mode == OCCURS_AT_MOST_X_TIMES))
                results.add(i + 1);
        return Util.getArray(results);
    }


    /**
     * Used by Hidden Singles
     * @return a list of candidates that occurred
     * == x, >= x, or <= x times in column c depending on mode
     */
    public static int[] colCountX(Sudoku sudoku, int c, int x, int mode)
    {
        ArrayList<Integer> results = new ArrayList<>();
        int[] record = new int[9];

        for (int r = 0; r < 9; r++)
        {
            if (sudoku.status[r][c] > 0)
            {
                record[sudoku.status[r][c] - 1]++;
            }
            else
            {
                for (int n = 0; n < 9; n++)
                {
                    if (sudoku.grid[r][c][n] > 0)
                    {
                        record[n]++;
                    }
                }
            }
        }

        for (int i = 0; i < 9; i++)
            if (record[i] == x || (record[i] > x && mode == OCCURS_AT_LEAST_X_TIMES) || (record[i] < x && mode == OCCURS_AT_MOST_X_TIMES))
        results.add(i + 1);
        return Util.getArray(results);
    }

    /**
     * for checkValid() in Checker
     * @param r row index to search for
     * @param returnAfterFinding2 if true return after the second occurrence is found
     * @return number of occurrences of the solved candidate sudoku.status[r][c] that are found in row r
     */
    public static int occurrenceCountInRow(Sudoku sudoku, int r, int c, boolean returnAfterFinding2)
    {
        int found = 0;
        for (int col = 0; col < 9; col++)
        {
            if (sudoku.status[r][col] == sudoku.status[r][c])
            {
                found++;
                if (returnAfterFinding2 && found == 2) return found;
            }
        }
        assert found < 2 || !returnAfterFinding2 : found;
        return found;
    }

    /**
     * for checkValid() in Checker
     * @param c column index to search for
     * @param returnAfterFinding2 if true return after the second occurrences is found
     * @return number of occurrences of the the solved candidate sudoku.status[r][c] that are found in column c
     */
    public static int occurrenceCountInCol(Sudoku sudoku, int r, int c, boolean returnAfterFinding2)
    {
        int found = 0;
        for (int row = 0; row < 9; row++)
        {
            if (sudoku.status[row][c] == sudoku.status[r][c])
            {
                found++;
                if (returnAfterFinding2 && found == 2) return found;
            }
        }
        assert found < 2 || !returnAfterFinding2 : found;
        return found;
    }

    /**
     * for checkValid() in Checker
     * @param r row index of a cell whose box to search for
     * @param c column index of a cell whose box to search for
     * @param returnAfterFinding2 if true return after the second occurrence of Sudoku.status[r][c] is found
     * @return number of occurrences of the solved candidate sudoku.status[r][c] that are found in the box of r, c
     */
    public static int occurrenceCountInBox(Sudoku sudoku, int r, int c, boolean returnAfterFinding2)
    {
        int found = 0;
        int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
        int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;

        for (int ri = 0; ri < 3; ri++)
        {
            for (int ci = 0; ci < 3; ci++)
            {
                if (sudoku.status[boxTopLeftCellR + ri][boxTopLeftCellC + ci] == sudoku.status[r][c])
                {
                    found++;
                    if (returnAfterFinding2 && found == 2) return found;
                }
            }
        }
        assert found < 2 || !returnAfterFinding2 : found;
        return found;
    }
}
