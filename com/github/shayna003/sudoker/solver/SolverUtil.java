package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.DigitsAndIndexesPanel;
import com.github.shayna003.sudoker.util.*;
import static com.github.shayna003.sudoker.Application.digitsAndIndexesPanel;

import java.util.*;
import java.util.logging.*;

/**
 * This class provides functions used more directly by the Solver class
 * last worked on 3-11-2021
 * @version 0.0.0
 * @since 2020-11-1
 * last modified: 5-24-2021
 */
public class SolverUtil
{
    /**
     * @return an array of the candidates of status[r][c]
     */
     public static int[] getCandidates(Sudoku sudoku, int r, int c)
     {
         assert sudoku.status[r][c] != 0 : "status[" + r + "][" + c + "]: " + sudoku.status[r][c];
         if (sudoku.status[r][c] > 0)
         {
             return new int[] { sudoku.status[r][c] };
         }

        int[] candidates = new int[sudoku.status[r][c] * -1];

         int numberOfUniqueCandidates = 0;
         for (int n = 0; n < 9; n++)
         {
             if (sudoku.grid[r][c][n] > 0)
             {
                 candidates[numberOfUniqueCandidates] = sudoku.grid[r][c][n];
                 if (numberOfUniqueCandidates == candidates.length - 1)
                 {
                     return candidates;
                 }
                 numberOfUniqueCandidates++;
             }
         }
         Application.exceptionLogger.log(Level.WARNING, "status and grid don't match: " + "more info: status number is " + sudoku.status[r][c] + ", but grid cell has only " + numberOfUniqueCandidates + " possibilities.");
         return candidates;
     }

     /**
      * @return [0] array of how many times each candidate [1, 9] appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountRow(Sudoku sudoku, int r)
     {
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int c = 0; c < 9; c++)
         {
             for (int n = 0; n < 9; n++)
             {
                 if (sudoku.grid[r][c][n] > 0)
                 {
                     if (record[n] == 0) numberOfUniqueCandidates++;
                     record[n]++;
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * Used by findHiddenCandidates to generate precise messages
      * @param selected only count the selected column indexes
      * @return [0] array of how many times each candidate (1 ~ 9) appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountRow(Sudoku sudoku, int[] selected, int r)
     {
         assert selected != null  : "selected is null ";
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int i = 0; i < selected.length; i++)
         {
             for (int n = 0; n < 9; n++)
             {
                 if (sudoku.grid[r][selected[i]][n] > 0)
                 {
                     if (record[n] == 0) numberOfUniqueCandidates++;
                     record[n]++;
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @return [0] array of how many times each candidate (1 ~ 9) appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountCol(Sudoku sudoku, int c)
     {
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int r = 0; r < 9; r++)
         {
             for (int n = 0; n < 9; n++)
             {
                 if (sudoku.grid[r][c][n] > 0)
                 {
                     if (record[n] == 0) numberOfUniqueCandidates++;
                     record[n]++;
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @param selected only count the selected row indexes
      * @return [0] array of how many times each candidate (1 ~ 9) appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountCol(Sudoku sudoku, int[] selected, int c)
     {
         assert selected != null  : "selected is null";
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int i = 0; i < selected.length; i++)
         {
             for (int n = 0; n < 9; n++)
             {
                 if (sudoku.grid[selected[i]][c][n] > 0)
                 {
                     if (record[n] == 0) numberOfUniqueCandidates++;
                     record[n]++;
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @return [0] array of how many times each candidate (1 ~ 9) appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountBox(Sudoku sudoku, int r, int c)
     {
         int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
         int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;

         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int ri = 0; ri < 3; ri++)
         {
             for (int ci = 0; ci < 3; ci++)
             {
                 for (int n = 0; n < 9; n++)
                 {
                     if (sudoku.grid[boxTopLeftCellR + ri][boxTopLeftCellC + ci][n] > 0)
                     {
                         if (record[n] == 0) numberOfUniqueCandidates++;
                         record[n]++;
                     }
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @param selected only count the selected indexes
      * mapping:
      * 0 1 2
      * 3 4 5
      * 6 7 8
      * @return [0] array of how many times each candidate (1 ~ 9) appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCountBox(Sudoku sudoku, int[] selected, int r, int c)
     {
         assert selected != null;
         int boxTopLeftCellR = Math.floorDiv(r, 3) * 3;
         int boxTopLeftCellC = Math.floorDiv(c, 3) * 3;

         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int i = 0; i < selected.length; i++)
         {
             for (int n = 0; n < 9; n++)
             {
                 if (sudoku.grid[boxTopLeftCellR + Math.floorDiv(selected[i], 3)][boxTopLeftCellC + selected[i] % 3][n] > 0)
                 {
                     if (record[n] == 0) numberOfUniqueCandidates++;
                     record[n]++;
                 }
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @param candidates a list of candidates for a number of cells
      * @return [0] array of how many times each of 9 candidates 1 ~ 9 appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCount(int[][] candidates)
     {
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;
         for (int i = 0; i < candidates.length; i++)
         {
             for (int x = 0; x < candidates[i].length; x++)
             {
                 if (record[candidates[i][x] - 1] == 0) numberOfUniqueCandidates++;
                 record[candidates[i][x] - 1]++;
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @param candidates a list of candidates for a number of cells
      * @return [0] array of how many times each of 9 candidates 1 ~ 9 appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCount(ArrayList<int[]> candidates)
     {
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;

         for (int i = 0; i < candidates.size(); i++)
         {
             for (int x = 0; x < candidates.get(i).length; x++)
             {
                 if (record[candidates.get(i)[x] - 1] == 0) numberOfUniqueCandidates++;
                 record[candidates.get(i)[x] - 1]++;
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * @param candidates a list of candidates for a number of cells
      * @param selected records candidates[selected[i]] is to be used
      * @return [0] array of how many times each of 9 candidates 1 ~ 9 appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCount(int[][] candidates, int[] selected)
     {
         assert candidates != null && selected != null : "candidates: " + Arrays.deepToString(candidates) + ", selected: " + Arrays.toString(selected);
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;

         for (int i = 0; i < selected.length; i++)
         {
             for (int x = 0; x < candidates[selected[i]].length; x++)
             {
                 if (record[candidates[selected[i]][x] - 1] == 0) numberOfUniqueCandidates++;
                record[candidates[selected[i]][x] - 1]++;
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

     /**
      * Used by findNakedCandidates
      * @param candidates a list of candidates for a number of cells
      * @param selected records candidates[selected[i]] is to be used
      * @return [0] array of how many times each of 9 candidates 1 ~ 9 appeared
      * @return [1] an array of the different candidates of the cells
      */
     public static int[][] candidateCount(ArrayList<int[]> candidates, int[] selected)
     {
         assert candidates != null && selected != null : "candidates: " + candidates + ", selected: " + Arrays.toString(selected);
         int[] record = new int[9];
         int[][] results;
         int numberOfUniqueCandidates = 0;
         int index = 0;

         for (int i = 0; i < selected.length; i++)
         {
             for (int x = 0; x < candidates.get(selected[i]).length; x++)
             {
                 if (record[candidates.get(selected[i])[x] - 1] == 0) numberOfUniqueCandidates++;
                 record[candidates.get(selected[i])[x] - 1]++;
             }
         }
         results = new int[2][];
         results[0] = record;
         results[1] = new int[numberOfUniqueCandidates];
         for (int i = 0; i < record.length; i++)
         {
             if (record[i] > 0)
             {
                 results[1][index] = i + 1;
                 index++;
             }
         }
         return results;
     }

    /**
     * Used by Solver.nakedCandidates
     * @param n select a group of n cells such that together they hav n unique candidates in total
     * @param unit one of {@code UnitCheck.SAME_ROW, UnitCheck.SAME_COLUMN, UnitCheck.SAME_BOX}
     * @param returnFirst returns the first match, else return all
     * @param compileMessages if true, compute messages for SolverPanel's console, else don't
     */
    public static void findNakedCandidates(Board board, ArrayList<Integer> possibleRowIndexes, ArrayList<Integer> possibleColIndexes, ArrayList<int[]> possibleCandidates, int n, int unit, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, boolean compileMessages, int[] results, Results solveResults)
    {
        boolean sameRow;
        boolean sameColumn;
        boolean sameBox;

        int[] selectedRowIndexes;
        int[] selectedColIndexes;
        int[] foundCandidates;

        int[][] candidateCountResults;
        int sameUnitCount;

        int[] eliminateResults = new int[3];
        StringBuilder messageBuilder = new StringBuilder();

        int[] indexCombo = new int[n];
        Arrays.fill(indexCombo, -1);
        int indexToIncrement = 0;

        while (true) // find all unique combos of the given possible indexes
        {
            if (indexCombo[indexToIncrement] + (n - indexToIncrement) > possibleCandidates.size() - 1)
            {
                if (indexToIncrement == 0) return; // all combos tested
                indexToIncrement--;
            }
            else
            {
                indexCombo[indexToIncrement]++;
                for (int k = indexToIncrement + 1; k < n; k++)
                {
                    indexCombo[k] = indexCombo[k - 1] + 1;
                }
                indexToIncrement = n - 1;

                selectedRowIndexes = Util.getArray(possibleRowIndexes, indexCombo);
                selectedColIndexes = Util.getArray(possibleColIndexes, indexCombo);

                sameRow = unit == UnitCheck.SAME_ROW ? true : (unit == UnitCheck.SAME_COLUMN ? false : Util.sameIndex(selectedRowIndexes));
                sameColumn = unit == UnitCheck.SAME_ROW  ? false : (unit == UnitCheck.SAME_COLUMN ? true : Util.sameIndex(selectedColIndexes));
                sameBox = unit == UnitCheck.SAME_BOX ? true : UnitCheck.sameBox(selectedRowIndexes, selectedColIndexes);

                // to prevent checking repeats: this function would be called with unit = SAME_ROW or SAME_COLUMN and find the same results
                if (unit != UnitCheck.SAME_BOX || (!sameRow && !sameColumn))
                {
                    // [0] how many times each candidate appeared
                    // [1] an array of the different candidates of the cells
                    candidateCountResults = candidateCount(possibleCandidates, indexCombo);

                    if (candidateCountResults[1].length == n) // found
                    {
                        foundCandidates = candidateCountResults[1];

                        // see if it helps eliminate candidates
                        Arrays.fill(eliminateResults, 0);
                        if (sameRow) Solver.deleteCandidatesInRow(board.solverHighlights, solveResults.sudoku, foundCandidates, selectedColIndexes, selectedRowIndexes[0], selectedColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults);
                        if (sameColumn) Solver.deleteCandidatesInCol(board.solverHighlights, solveResults.sudoku, foundCandidates, selectedRowIndexes, selectedRowIndexes[0], selectedColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults);
                        if (sameBox) Solver.deleteCandidatesInBox(board.solverHighlights, solveResults.sudoku, foundCandidates, selectedRowIndexes, selectedColIndexes, selectedRowIndexes[0], selectedColIndexes[0], eliminateRecursively, returnIfInvalid, false, eliminateResults);
                        results[0] += eliminateResults[0];
                        results[1] += eliminateResults[1];
                        results[2] += eliminateResults[2];

                        if (eliminateResults[Solver.ELIMINATED_INDEX] > 0) // found it
                        {
                            solveResults.found++;
                            if (board.solverHighlights != null) // mark highlights
                            {
                                for (int i = 0; i < selectedRowIndexes.length; i++)
                                {
                                    for (int x = 0; x < n; x++)
                                    {
                                        if (solveResults.sudoku.grid[selectedRowIndexes[i]][selectedColIndexes[i]][foundCandidates[x] - 1] > 0)
                                        {
                                            board.solverHighlights[selectedRowIndexes[i]][selectedColIndexes[i]][foundCandidates[x] - 1] = SolverPanel.ONLY_CANDIDATE;
                                        }
                                    }
                                }
                            }

                            if (compileMessages)
                            {
                                sameUnitCount = 0;
                                if (sameRow) sameUnitCount++;
                                if (sameColumn) sameUnitCount++;
                                if (sameBox) sameUnitCount++;

                                switch (n)
                                {
                                    case 2:
                                        messageBuilder.append("Naked Pair"); break;
                                    case 3:
                                        messageBuilder.append("Naked Triple"); break;
                                    case 4:
                                        messageBuilder.append("Naked Quadruple"); break;
                                    default:
                                        messageBuilder.append("Naked Candidates of a group of ");
                                        messageBuilder.append(n);
                                        break;
                                }
                                messageBuilder.append(" found:");
                                messageBuilder.append(System.lineSeparator());
                                messageBuilder.append(Solver.messageOutputIndent);

                                messageBuilder.append("Since the ");
                                messageBuilder.append(n);
                                messageBuilder.append(" cells ");
                                messageBuilder.append(digitsAndIndexesPanel.getStringIndexes(selectedRowIndexes, selectedColIndexes));
                                messageBuilder.append(" altogether have only ");
                                messageBuilder.append(n);
                                messageBuilder.append(" candidates and are in the same ");
                                messageBuilder.append(sameRow ? sameUnitCount > 1 ? "row and " : "row, " : "");
                                messageBuilder.append(sameColumn ? sameUnitCount > 1 ? "column and " : "column, " : "");
                                messageBuilder.append(sameBox ? "box, " : "");
                                messageBuilder.append("they each have to be one of the candidates of ");
                                messageBuilder.append(digitsAndIndexesPanel.getStringCandidates(foundCandidates));
                                messageBuilder.append(".");

                                messageBuilder.append(System.lineSeparator());
                                messageBuilder.append(Solver.messageOutputIndent);
                                messageBuilder.append("Therefore the other cells in their ");
                                messageBuilder.append(sameRow ? sameUnitCount > 1 ? "row and " : "row " : "");
                                messageBuilder.append(sameColumn ? sameUnitCount > 1 ? "column and " : "column " : "");
                                messageBuilder.append(sameBox ? "box " : "");
                                messageBuilder.append("cannot have ");
                                messageBuilder.append(digitsAndIndexesPanel.getStringCandidates(foundCandidates));
                                messageBuilder.append(" as their candidates.");
                                solveResults.messages.add(messageBuilder.toString());
                                messageBuilder.delete(0, messageBuilder.length());
                            }
                            if (returnFirst) return;
                        }
                        if (returnIfInvalid && eliminateResults[Solver.VALIDITY_INDEX] < 0) return;
                    }
                    else if (candidateCountResults[1].length < n)
                    {
                        Application.exceptionLogger.log(Level.WARNING, "Error in findNakedCandidates(): " + n + " cells end up with only " + candidateCountResults[1].length + " total different candidates!");
                        if (returnIfInvalid) return;
                    }
                }
            }
        }
    }

    /**
     * Used by Solver.hiddenCandidates
     * Find n unsolved cells so that together they have > n candidates and no other cell in their row/col/box have n of their candidates
     * Only at least one cell needs to have an extra candidate, and a cell can even have less candidates than n
     * @param n select a group of n cells
     * @param unit one of {@code UnitCheck.SAME_ROW, UnitCheck.SAME_COLUMN, UnitCheck.SAME_BOX}
     * @param returnFirst returns the first match, else return all
     * @param compileMessages if true, compute messages for SolverPanel's console, else don't
     */
    public static void findHiddenCandidates(Board board, ArrayList<Integer> possibleRowIndexes, ArrayList<Integer> possibleColIndexes, ArrayList<int[]> possibleCandidates,
    int n, int unit, boolean returnFirst, boolean eliminateRecursively, boolean returnIfInvalid, boolean compileMessages, int[] results, Results solveResults)
    {
        boolean sameRow;
        boolean sameColumn;
        boolean sameBox;

        boolean uniqueInRow;
        boolean uniqueInCol;
        boolean uniqueInBox;
        int uniqueInUnitCount;

        int[] selectedRowIndexes;
        int[] selectedColIndexes;
        int[] foundCandidates;

        int[][] candidateCountResults;
        int[][] candidateCountResultsForOtherCells;
        int[] candidatesNotIncludedInOtherCells;
        int[] candidatesNotIncludedInOtherCellsForMessageGeneration;
        int sameUnitCount;

        int[] eliminateResults = new int[3];
        StringBuilder messageBuilder = new StringBuilder();

        int[] indexCombo = new int[n];
        int[] otherIndexesInSameUnit;

        Arrays.fill(indexCombo, -1);
        int indexToIncrement = 0;

        while (true) // find all unique combos of the given possible indexes
        {
            if (indexCombo[indexToIncrement] + (n - indexToIncrement) > possibleCandidates.size() - 1)
            {
                if (indexToIncrement == 0) return; // all combos tested
                indexToIncrement--;
            }
            else
            {
                indexCombo[indexToIncrement]++;
                for (int k = indexToIncrement + 1; k < n; k++)
                {
                    indexCombo[k] = indexCombo[k - 1] + 1;
                }
                indexToIncrement = n - 1;

                selectedRowIndexes = Util.getArray(possibleRowIndexes, indexCombo);
                selectedColIndexes = Util.getArray(possibleColIndexes, indexCombo);

                sameRow = unit == UnitCheck.SAME_ROW ? true : (unit == UnitCheck.SAME_COLUMN ? false : Util.sameIndex(selectedRowIndexes));
                sameColumn = unit == UnitCheck.SAME_ROW  ? false : (unit == UnitCheck.SAME_COLUMN ? true : Util.sameIndex(selectedColIndexes));
                sameBox = unit == UnitCheck.SAME_BOX ? true : UnitCheck.sameBox(selectedRowIndexes, selectedColIndexes);

                // [0] how many times each candidate appeared
                // [1] an array of the different candidates of the cells
                candidateCountResults = candidateCount(possibleCandidates, indexCombo);

                if (candidateCountResults[1].length > n ) // if total candidates == n, then it would be a naked group instead of hidden group
                {
                    // to see if no other cell in their row/col/box (according to param unit) have n of their candidates
                    otherIndexesInSameUnit = Util.getNotIncludedIndexes(indexCombo, 0, possibleCandidates.size() - 1);
                    candidateCountResultsForOtherCells = candidateCount(possibleCandidates, otherIndexesInSameUnit);

                    candidatesNotIncludedInOtherCells = Util.returnCandidatesNotIncluded(candidateCountResultsForOtherCells[0], candidateCountResults[1]);
                    if (candidatesNotIncludedInOtherCells.length == n) // found it
                    {
                        solveResults.found++;
                        if (compileMessages)
                        {
                            // for precise message generation
                            if (unit == UnitCheck.SAME_BOX && sameRow)
                            {
                                candidatesNotIncludedInOtherCellsForMessageGeneration = Util.returnCandidatesNotIncluded(candidateCountRow(solveResults.sudoku, Util.getNotIncludedIndexes(selectedColIndexes, 0, 8), possibleRowIndexes.get(indexCombo[0]))[0], candidateCountResults[1]);
                                uniqueInRow = candidatesNotIncludedInOtherCellsForMessageGeneration.length == n;
                            }
                            else
                            {
                                uniqueInRow = unit == UnitCheck.SAME_ROW;
                            }

                            if (unit == UnitCheck.SAME_BOX && sameColumn)
                            {
                                candidatesNotIncludedInOtherCellsForMessageGeneration = Util.returnCandidatesNotIncluded(candidateCountCol(solveResults.sudoku, Util.getNotIncludedIndexes(selectedRowIndexes, 0, 8), possibleColIndexes.get(indexCombo[0]))[0], candidateCountResults[1]);
                                uniqueInCol = candidatesNotIncludedInOtherCellsForMessageGeneration.length == n;
                            }
                            else
                            {
                                uniqueInCol = unit == UnitCheck.SAME_COLUMN;
                            }

                            if (unit != UnitCheck.SAME_BOX && sameBox)
                            {
                                candidatesNotIncludedInOtherCellsForMessageGeneration = Util.returnCandidatesNotIncluded(candidateCountBox(solveResults.sudoku, Util.getNotIncludedIndexes(DigitsAndIndexesPanel.getWithinBoxIndexes(selectedRowIndexes, selectedColIndexes), 0, 8), possibleRowIndexes.get(indexCombo[0]), possibleColIndexes.get(indexCombo[0]))[0], candidateCountResults[1]);
                                uniqueInBox = candidatesNotIncludedInOtherCellsForMessageGeneration.length == n;
                            }
                            else
                            {
                                uniqueInBox = unit == UnitCheck.SAME_BOX;
                            }

                            uniqueInUnitCount = 0;
                            if (uniqueInRow) uniqueInUnitCount++;
                            if (uniqueInCol) uniqueInUnitCount++;
                            if (uniqueInBox) uniqueInUnitCount++;

                            switch(n)
                            {
                                case 2:
                                    messageBuilder.append("Hidden Pair"); break;
                                case 3:
                                    messageBuilder.append("Hidden Triple"); break;
                                case 4:
                                    messageBuilder.append("Hidden Quadruple"); break;
                                case 1:
                                    messageBuilder.append("Hidden Single"); break;
                                default:
                                    messageBuilder.append("Hidden candidates of a group of ");
                                    messageBuilder.append(n);
                                    break;
                            }
                            messageBuilder.append(" found:");
                            messageBuilder.append(System.lineSeparator());
                            messageBuilder.append(Solver.messageOutputIndent);
                            
                            messageBuilder.append("Since the ");
                            messageBuilder.append(n);
                            messageBuilder.append(" cells ");
                            messageBuilder.append(digitsAndIndexesPanel.getStringIndexes(selectedRowIndexes, selectedColIndexes));
                            messageBuilder.append(" are the only cells in their ");
                            messageBuilder.append(uniqueInRow ? uniqueInUnitCount > 1 ? "row and " : "row " : "");
                            messageBuilder.append(uniqueInCol ? uniqueInUnitCount > 1 ? "column and " : "column " : "");
                            messageBuilder.append(uniqueInBox ? "box " : "");
                            messageBuilder.append("that contain the ");
                            messageBuilder.append(n);
                            messageBuilder.append(" candidates ");
                            messageBuilder.append(digitsAndIndexesPanel.getStringCandidates(candidatesNotIncludedInOtherCells));
                            messageBuilder.append(", these cells each have to be one of them.");
                            solveResults.messages.add(messageBuilder.toString());
                            messageBuilder.delete(0, messageBuilder.length());
                        }

                        // remove candidates other than the "hidden" ones in the hidden candidate cell group
                        Arrays.fill(eliminateResults, 0);
                        for (int k = 0; k < selectedRowIndexes.length; k++)
                        {
                            // mark only candidates
                            if (board.solverHighlights != null)
                            {
                                for (int x = 0; x < candidatesNotIncludedInOtherCells.length; x++)
                                {
                                    if (solveResults.sudoku.grid[selectedRowIndexes[k]][selectedColIndexes[k]][candidatesNotIncludedInOtherCells[x] - 1] > 0)
                                    {
                                        board.solverHighlights[selectedRowIndexes[k]][selectedColIndexes[k]][candidatesNotIncludedInOtherCells[x] - 1] = SolverPanel.ONLY_CANDIDATE;
                                    }
                                }
                            }
                            Solver.deleteCandidatesInCell(board.solverHighlights, solveResults.sudoku, Util.getNotIncludedIndexes(candidatesNotIncludedInOtherCells, 1, 9), selectedRowIndexes[k], selectedColIndexes[k], eliminateRecursively, returnIfInvalid, eliminateResults);
                        }
                        results[0] += eliminateResults[0];
                        results[1] += eliminateResults[1];
                        results[2] += eliminateResults[2];
                        if (returnFirst) return;
                    }
                    if (returnIfInvalid && eliminateResults[Solver.VALIDITY_INDEX] < 0) return;
                }
            }
        }
    }
}
