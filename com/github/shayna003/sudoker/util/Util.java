package com.github.shayna003.sudoker.util;

import com.github.shayna003.sudoker.Sudoku;

import java.util.*;

/**
 * Convenience functions that don't belong to SolverUtil, GridUtil, UnitCheck, Checker, and IO end up in here
 * @version 0.0.0 
 * @since 2020-11-1
 */
public class Util
{
    /**
     * @return the candidates in {@code candidates} that are not included in {@code candidate_count}
     */
    public static int[] returnCandidatesNotIncluded(int[] candidate_count, int[] candidates)
    {
        ArrayList<Integer> found = new ArrayList<>();
        for (int i = 0; i < candidates.length; i++)
        {
            if (candidate_count[candidates[i] - 1] == 0) found.add(candidates[i]);
        }
        return getArray(found);
    }

    /**
     * I can replace this with return ArrayList.toArray(new int[l.size()]);
     * @return an int[] version of an {@code ArrayList<Integer> }
     */
    public static int[] getArray(ArrayList<Integer> l)
    {
        int[] copy = new int[l.size()];
        for (int i = 0; i < l.size(); i++) copy[i] = l.get(i);
        return copy;
    }

    /**
     * @return an int[] version of the selected indexes of an {@code ArrayList<Integer> }
     */
    public static int[] getArray(ArrayList<Integer> l, int[] selected)
    {
        int[] copy = new int[selected.length];
        for (int i = 0; i < selected.length; i++) copy[i] = l.get(selected[i]);
        return copy;
    }

    /**
     * @return the index of the first matching element
     * -1 if none found
     */
    public static int contains(int[] x, int n)
    {
        if (x == null) return -1;
        for (int i = 0; i < x.length; i++)
        {
            if (x[i] == n) return i;
        }
        return -1;
    }

    /**
     * @return whether all the indexes of x are the same
     */
    public static boolean sameIndex(int[] x)
    {
        for (int i = 1; i < x.length; i++)
        {
            if (x[i] != x[0]) return false;
        }
        return true;
    }

    /**
     * @return whether all the indexes [of indexes in selected] of x are the same
     */
    public static boolean sameIndex(ArrayList<Integer> x, int[] selected)
    {
        for (int i = 1; i < selected.length; i++)
        {
            if (x.get(selected[i]) != x.get(selected[0])) return false;
        }
        return true;
    }

    /**
     * @return the first!? index i such that ri[i] == r and ci == c
     * else return -1;
     */
    public static int findIndex(int[] ri, int[] ci, int r, int c)
    {
        for (int i = 0; i < ri.length; i++)
        {
            if (r == ri[i] && c == ci[i]) return i;
        }
        return -1;
    }

    /**
     * @return indexes from 0 ~ 9 not included in i
     * min the minimum possible index contained in the return array
     * max the max possible index contained in the return array
     */
    public static int[] getNotIncludedIndexes(int[] originalIndexes, int minIndex, int maxIndex)
    {
        if (originalIndexes.length == 0)
        {
            int[] result = new int[maxIndex + 1 - minIndex];
            for (int k = 0; k < maxIndex + 1; k++) result[k] = k;
            return result;
        }
        
        int[] result = new int[maxIndex + 1 - originalIndexes.length - minIndex];
        int counter = 0;
        int value = minIndex;
        for (int k = 0; k < result.length; k++)
        {
            while (counter < originalIndexes.length && originalIndexes[counter] == value)
            {
                value++;
                counter++;
            }
            result[k] = value;
            value++;
        }
        return result;
    }

    public static boolean cellIsUnsolvedAndHasCandidate(Sudoku sudoku, int r, int c, int n)
    {
        if (sudoku.status[r][c] >= 0) return false;
        return sudoku.grid[r][c][n - 1] > 0;
    }
}
