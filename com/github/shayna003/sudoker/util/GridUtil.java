package com.github.shayna003.sudoker.util;

import com.github.shayna003.sudoker.*;
import java.util.*;

/**
 * Provides a short list of convenience functions that deals with grid and status
 * @version 0.0.0 2020-11-1
 */
public class GridUtil
{
    public static class GridAndStatusDoNotMatchException extends IllegalArgumentException
    {
        public GridAndStatusDoNotMatchException(){}
        public GridAndStatusDoNotMatchException(String gripe)
        {
            super(gripe);
        }
    }
    
    /**
     * @return a grid with all cells set to 123456789
     */
    public static int[][][] emptyGrid()
    {
        int[][][] grid = new int[9][9][9];
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                for (int n = 0; n < 9; n++)
                {
                    grid[r][c][n] = n + 1;
                }
            }
        }
        return grid;
    }

    /**
     * @return a status array with all values set to -9;
     */
    public static int[][] emptyStatus()
    {
        int[][] status = new int[9][9];
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                status[r][c] = -9;
            }
        }
        return status;
    }
    
    /**
     * Used by CellEditor to determine if a new EDIT_CELL edit needs to be made.
     * @return if two cells are equal
     */
    public static boolean cellsEqual(int[] cell1, int[] cell2)
    {
        for (int i = 0; i < cell1.length; i++)
        {
            if (cell1[i] != cell2[i]) return false;
        }
        return true;
    }

    /**
     * @return a copy of param array
     */
    public static int[] copyOf(int[] array)
    {
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }

    /**
     * @return a copy of param status
     */
    public static int[][] copyOf(int[][] status)
    {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++)
        {
            System.arraycopy(status[r], 0, copy[r], 0, 9);
        }
        return copy;
    }

    /**
     * @return a copy of param grid
     */
    public static int[][][] copyOf(int[][][] grid)
    {
        int[][][] copy = new int[9][9][9];
        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                System.arraycopy(grid[r][c], 0, copy[r][c], 0, 9);
            }
        }
        return copy;
    }
    
    /**
     * @return a copy of param array
     */
    public static String[][] copyOf(String[][] array)
    {
        String[][] copy = new String[9][9];
        for (int r = 0; r < 9; r++)
        {
            System.arraycopy(array[r], 0, copy[r], 0, 9);
        }
        return copy;
    }
    
    /**
     * @return a copy of param array
     */
    public static boolean[][] copyOf(boolean[][] array)
    {
        boolean[][] copy = new boolean[9][9];
        for (int r = 0; r < 9; r++)
        {
            System.arraycopy(array[r], 0, copy[r], 0, 9);
        }
        return copy;
    }

    /**
     * @return a cell's status number
     * @param returnFirst if true, return the cell's first candidate
     */
    public static int getStatusForCell(int[] cell, boolean returnFirst)
    {
        int sum = 0;
        int candidate = 0;
        for (int i = 0; i < 9; i++)
        {
            if (cell[i] > 0)
            {
                candidate = i + 1;
                if (returnFirst) return candidate;
                sum++;
            }
        }
        return sum > 1 ? sum * -1 : candidate;
    }
}
