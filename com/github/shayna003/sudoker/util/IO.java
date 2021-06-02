package com.github.shayna003.sudoker.util;

import static com.github.shayna003.sudoker.Application.digitsAndIndexesPanel;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.DigitsAndIndexesPanel;

import java.util.*;
import java.io.*;
import java.nio.charset.*;

/**
 * The jobs of I/O are split upon DigitsAndIndexesPanel, this class, Importer, and Exporter.
 * Physical printing is taken care of in ApplicationFrame's Print menu.
 * @version 0.0.0
 * @since 2020-11-1
 * last modified: 5-16-2021
 */
public class IO
{
    public static String getCompact81CandidatesString(int[][] status)
    {
        return getString(new Sudoku(status), 0, "", "", false, false, false, false, 0, 0, 0, 0, 0, 0 ).toString();
    }

    public static String getCompact81CandidatesString(Sudoku sudoku)
    {
        return getString(sudoku, 0, "", "", false, false, false, false, 0, 0, 0, 0, 0, 0 ).toString();
    }

    public static String getCompactAllCandidatesString(Sudoku sudoku)
    {
        return getString(sudoku, 0, "", "", false, false, true, false, 0, 0, 0, 0, 0, 0 ).toString();
    }

    /**
     * A convenience method to print a sudoku puzzle
     */
    public static String getDefaultString(Sudoku s, int indent, boolean digit0ShouldBeBlank, boolean print_all)
    {
        return getString(s, indent, System.lineSeparator(), " ", digit0ShouldBeBlank, true, print_all, true, 1, 0, 0, 1, 1, 1).toString();
    }

    /**
     * Note: Character.digit() will not throw exceptions if you supply illegal characters
     */
    public static BoardData readBoardFromBufferedReader(BufferedReader in) throws Exception//, Sudoku sudoku, String[][] pencilMarks, String[][] notes, boolean[][] locks) throws IOException
    {
        Sudoku sudoku = null;// = new Sudoku(); // an empty sudoku
        String[][] pencilMarks = null;//= BoardData.emptyNotesOrPencilMarks();
        String[][] notes = null;//= BoardData.emptyNotesOrPencilMarks();
        boolean[][] locks = null;// = BoardData.emptyLocks();
        
        String header = in.readLine();

        boolean hasSudoku = header.charAt(0) == '1';
        boolean hasCellSeparators = header.charAt(1) == '1';
        boolean newLineForEachRow = header.charAt(2) == '1';
        boolean hasAllCandidates = header.charAt(3) == '1';
        
        boolean hasPencilMarks = header.charAt(4) == '1';
        boolean hasNotes = header.charAt(5) == '1';
        boolean hasLocks = header.charAt(6) == '1';
        
        boolean hasViewOptions = header.charAt(7) == '1';
        boolean hasHighlightOptions = header.charAt(8) == '1';
        boolean hasStopwatchTime = header.charAt(9) == '1';
        
        if (hasSudoku)
        {
            sudoku = readSudokuFromBufferedReader(in, hasCellSeparators, newLineForEachRow, hasAllCandidates);
        }
        
        if (hasPencilMarks)
        {
            readPencilMarksOrNotes(in, pencilMarks = BoardData.emptyNotesOrPencilMarks());
        }

        if (hasNotes)
        {
            readPencilMarksOrNotes(in, notes = BoardData.emptyNotesOrPencilMarks());
        }

        if (hasLocks)
        {
            in.readLine();

            locks = BoardData.emptyLocks();
            for (int r = 0; r < 9; r++)
            {
                for (int c = 0; c < 9; c++)
                {
                    locks[r][c] = in.read() == '1';
                }
            }
            in.readLine(); // line break
        }

        BoardData data = new BoardData(sudoku, pencilMarks, notes, locks);

        if (hasViewOptions)
        {
            in.readLine();
            int tmp = Character.digit(in.read(), 10);
            int viewMode = Math.max(tmp, 0);
            
            boolean showIndexes = in.read() == '1';
            boolean showColIndexes = in.read() == '1';
            boolean showBoxIndexes = in.read() == '1';
            data.setViewModeData(viewMode, showIndexes, showColIndexes, showBoxIndexes);
            in.readLine(); // line break
        }
        
        if (hasHighlightOptions)
        {
            in.readLine();
            int tmp = Character.digit(in.read(), 10);
            int mouseOverHighlight = Math.max(tmp, 0);

            boolean[] permanentHighlight = new boolean[9];
            for (int i = 0; i < 9; i++)
            {
                permanentHighlight[i] = in.read() == '1';
            }

            data.setHighlightData(mouseOverHighlight, permanentHighlight);
            in.readLine(); // line break
        }

        if (hasStopwatchTime)
        {
            in.readLine();
            StringBuilder s = new StringBuilder();
            int hours;
            int minutes;
            int seconds;

            s.append((char) in.read());
            s.append((char) in.read());
            hours = getInt(s.toString());
            in.read(); //:

            s.delete(0, s.length());
            s.append((char) in.read());
            s.append((char) in.read());
            minutes = getInt(s.toString());
            in.read(); //:

            s.delete(0, s.length());
            s.append((char) in.read());
            s.append((char) in.read());
            seconds = getInt(s.toString());

            data.setStopwatchData(hours, minutes, seconds);
        }
        return data;
    }
    
    /**
     * Used directly by Importer
     */
    public static BoardData readBoardFromString(String s)
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))
        {
            return readBoardFromBufferedReader(in);
        }
        catch(Exception e)
        {
            return new BoardData(null, null, null, null);
        }
    }
    
    /**
     * Used directly by Importer
     */
    public static BoardData readBoardFromFile(File file)
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
        {
            return readBoardFromBufferedReader(in);
        }
        catch (Exception e)
        {
            return new BoardData(null, null, null, null);
        }
    }
    
    public static int getInt(String s)
    {
        try 
        {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
    
    static void readPencilMarksOrNotes(BufferedReader in, String[][] values) throws Exception
    {
        StringBuilder s = new StringBuilder();
        char tmp;
        int lengthForCell = 0;
        in.readLine();

        for (int r = 0; r < 9; r++)
        {
            for (int c = 0; c < 9; c++)
            {
                if (s.length() > 0) s.delete(0, s.length());
                while ((tmp = (char) in.read()) != '|')
                {
                    s.append(tmp);
                }
                lengthForCell = getInt(s.toString());

                if (s.length() > 0) s.delete(0, s.length());
                for (int i = 0; i < lengthForCell; i++)
                {
                    s.append((char) in.read());
                }
                values[r][c] = s.toString();
            }
        }
        in.readLine(); // line break
    }
    
    static Sudoku readSudokuFromBufferedReader(BufferedReader in, boolean hasCellSeparators, boolean newLineForEachRow, boolean hasAllCandidates) throws Exception//IOException, StringIndexOutOfBoundsException, NullPointerException
    {
        String[] lines;
        in.readLine();
        
        if (newLineForEachRow)
        {
            lines = new String[9];
            for (int r = 0; r < 9; r++)
            {
                lines[r] = in.readLine();
            }
        }
        else 
        {
            lines = new String[1];
            lines[0] = in.readLine();
        }
        return readSudokuFromString(lines, hasCellSeparators, newLineForEachRow, hasAllCandidates);
    }
    
    /**
     * Reads a sudoku from a data file
     * @return null if this is an invalid file
     */
    public static Sudoku readSudokuFromFile(File file)
    {
        if (file.isDirectory()) return null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
        {
            String header = in.readLine();
            if (header == null) return null;
            boolean hasSudoku = header.charAt(0) == '1';
            if (!hasSudoku) return null;
            
            boolean hasCellSeparators = header.charAt(1) == '1';
            boolean newLineForEachRow = header.charAt(2) == '1';
            boolean hasAllCandidates = header.charAt(3) == '1';
            
            return readSudokuFromBufferedReader(in, hasCellSeparators, newLineForEachRow, hasAllCandidates);
        }
        catch (Exception e)//IOException | NullPointerException | StringIndexOutOfBoundsException e)
        {
            return null;
        }
    }

    /**
     * Reads a Sudoku from 1 or 9 lines of String inputs.
     * Does not support Unicode code points that are above the range of u+FFFF, i.e. digits have to be 1 char in length
     * Except for digit 0, the String used to represent no such candidate, which can be either "" or a char of length 1.
     * Assume that if digit 0 == "", hasCellSeparators == true
     * Invalid inputs for candidates will be interpreted as digit 0,
     * and cells with no candidates will be set to all possibilities.
     * @param inputLines String input separated by line separator, either of length 1 or 9
     * @return a Sudoku, null if input is invalid
     */
    public static Sudoku readSudokuFromString(String[] inputLines, boolean hasCellSeparators, boolean newLineForEachRow, boolean hasAllCandidates)
    {
        try
        {
            if (newLineForEachRow)
            {
                if (inputLines.length != 9) return null;
            }
            
            String character; // for extracting individual characters of the input
            String cellSeparator = digitsAndIndexesPanel.getCellSeparator();
            
            int row = 0;
            int col = 0;
            int digitIndex = 0; // within a cell, used if hasAllCandidates == true
            int characterIndexOfInput = 0;
            
            int[][] status = new int[9][9];
            int[][][] grid = new int[9][9][9];
            Sudoku sudoku = new Sudoku(grid, status);
            
            while (row < 9)
            {
                character = inputLines[newLineForEachRow ? row : 0].substring(characterIndexOfInput, characterIndexOfInput + 1);
                
                if (!character.equals(cellSeparator))
                {
                    int digit = DigitsAndIndexesPanel.getIndex(character, digitsAndIndexesPanel.input_digits);
                    if (digit <= 0)
                    {
                        if (!hasAllCandidates)
                        {
                            sudoku.clearCell(row, col);
                        }
                    }
                    else
                    {
                        sudoku.grid[row][col][digit - 1] = digit;
                        if (!hasAllCandidates)
                        {
                           sudoku.status[row][col] = digit;
                        }
                    }
                }
                
                digitIndex++;
                characterIndexOfInput++;

                if ((digitIndex == 9 && !hasCellSeparators) || (hasCellSeparators && character.equals(cellSeparator)) || (!hasCellSeparators && !hasAllCandidates) || characterIndexOfInput == inputLines[newLineForEachRow? row : 0].length())
                {

                    if (hasAllCandidates || status[row][col] == 0)
                    {
                        sudoku.status[row][col] = GridUtil.getStatusForCell(grid[row][col], false);
                        if (sudoku.status[row][col] == -9) sudoku.clearCell(row, col);
                    }
                    
                    col++;
                    digitIndex = 0;
                    if (col == 9)
                    {
                        if (newLineForEachRow) characterIndexOfInput = 0;
                        col = 0;
                        row++;
                    }
                }
                
                if (row < inputLines.length && characterIndexOfInput == inputLines[row].length())
                {
                    if (inputLines.length == 1)
                    {
                        if (hasAllCandidates || status[row][col] == 0)
                        {
                            sudoku.status[row][col] = GridUtil.getStatusForCell(grid[row][col], false);
                            if (sudoku.status[row][col] == -9) sudoku.clearCell(row, col);
                        }
                        break;
                    }
                    else 
                    {
                        if (hasAllCandidates || status[row][col] == 0)
                        {
                            sudoku.status[row][col] = GridUtil.getStatusForCell(grid[row][col], false);
                            if (sudoku.status[row][col] == -9) sudoku.clearCell(row, col);
                        }

                        digitIndex = 0;
                        characterIndexOfInput = 0;
                        col = 0;
                        row++;
                    }
                }
            }
            // make status match grid if input is invalid and resulted in a mismatch
            Checker.statusMatchGrid(sudoku, true, true);
            int differences = Checker.statusMatchGrid(sudoku, false, false);
            assert  differences == 0 : differences;
            return sudoku;
        }
        catch (Exception e) // StringIndexOutOfBounds etc
        {
            return null;
        }
    }

    /**
     * override happens only when the variable with higher priority is > 0
     * @param indent adds indent spaces before the start of each line, <= 0 equals none
     * length of each string is considered to make each line take up the same amount of space
     * @param l the string used to print a line
     * Note that by default, an l is added even if lines is 0
     * because lines specifies the visible number of gap lines between each line instead of noting an addition of l
     * to avoid printing line separators, set parameter l to "". This does not apply to spaces
     * @param sp the string used to print a space
     * notice that some are bold/half bold according to their supposed position
     * if null, then print nothing;
     * if length != 20, then print the default: (else put your symbols in this order)
     * [0]┏, [1]┓, [2]┗, [3]┛
     * [4]│, [5]┃, [6]─, [7]━,
     * [8]┯, [9]┳, [10]┷, [11]┻, [12]┠, [13]┣, [14]┨, [15]┫,
     * [16]┼, [17]╂, [18]┿, [19]╋
     * @param print_borders if false don't print borders
     * borders don't look correct if you choose to add spaces instead of lines at certain points
     * @param digit0ShouldBeBlank if true, make sure digits 0 == " ", used by Importer's preview accessory for imports
     * @param print_all if true print all candidates of each cell, if false print digit[0] for empty cells, if false, nullifies split
     * if print_all is false, then use sd and does not use sc
     * if a string is null then it is not printed
     * @param center_solved if true then centers the only candidate in solved cells
     * ignored if print_all is false
     * @param split if >= 1 then use 3 rows to print 1 row of cells
     * if > 1 then print split - 1 lines between each small row
     * only works if print_all is true
     * 1 2 3
     * 4 5 6   vs  1 2 3 4 5 6 7 8 9
     * 7 8 9
     * if <= 0 then don't use 3 rows to print 1 row of cells
     * override happens only when the parameter with higher priority is > 0 when it's time to print them
     * @param lbr add lbr lines after each box row, overrides lr, sr, lgc, sbc, sc, sd, when (r + 1) % 3 == 0
     * @param lr add lr lines after each row, overrides sc, & sd when c == 8
     * @param sbc add sbc spaces after each box column, overrides sc & when c % 3 == 0, overrides sd when i == 8
     * @param sc add sc spaces after each cell, overrides sd when i == 8
     * @param sd add sd spaces after each digit (only used when print_all is true)
     * @return a String representation of a grid
     */
    public static StringBuilder getString(Sudoku sudoku, int indent,
    String l, String sp, 
    boolean digit0ShouldBeBlank, boolean print_borders, boolean print_all, boolean center_solved,
    int split,
    int lbr, int lr, int sbc, int sc, int sd)
    {
        int[][][] grid = sudoku.grid;
        int[][] status = sudoku.status;
        String[] digits = DigitsAndIndexesPanel.getStringArray(digitsAndIndexesPanel.output_digits);
        if (digit0ShouldBeBlank) digits[0] = " ";
        String[] borders = digitsAndIndexesPanel.getBorders();
        
        assert sd >= 0 && sc >= 0 && sbc >= 0 && lr >= 0 && lbr >= 0 && split >= 0 && indent >= 0 : "indent: " + indent + ", split: " + split + ", lbr: " + lbr + ", lr: " + lr + ", sbc: " + sbc + ", sc: " + sc + ", sd: " + sd;
        assert digits.length == 10 : Arrays.toString(digits);
        assert borders.length == 20 : Arrays.toString(borders);

        StringBuilder s = new StringBuilder();

        //print top border
        s = addCharacters(s, sp, indent);
        if (print_borders)
        {
            s = addLines(digits, borders, true, true, false, false, false, true, print_all, split, s, l, sp, 0, sbc, sc, sd, indent, lbr);
        }

        for (int r = 0; r < 9; r++)
        {
            if (split > 0 && print_all)
            {
                //use 3 rows to print each row of cells if printing all of a cell's candidates
                for (int r3 = 0; r3 < 3; r3++)
                {
                    for (int c = 0; c < 9; c++)
                    {
                        if (c == 0)
                        {
                            if (print_borders)
                            {
                                s.append(borders[5]);
                                s = addCharacters(s, sp, sbc);
                            }
                        }
                        for (int c3 = 0; c3 < 3; c3++)
                        {
                            if (center_solved && status[r][c] > 0)
                            {
                                if (r3 != 1 || c3 != 1)
                                {
                                    s.append(digits[0]);
                                }
                                else
                                {
                                    s.append(digits[status[r][c]]);
                                }
                            }
                            else
                            {
                                s.append(digits[grid[r][c][r3 * 3 + c3]]);
                            }

                            if ((c + 1) % 3 == 0 && c3 == 2)
                            {
                                if (print_borders)
                                {
                                    s = addCharacters(s, sp, sbc);
                                    s.append(borders[5]);
                                }
                                if (c != 8)
                                {
                                    s = addCharacters(s, sp, sbc);
                                }
                            }
                            else if (c3 == 2)
                            {
                                if (print_borders)
                                {
                                    s = addCharacters(s, sp, sc);
                                    s.append(borders[4]);
                                }
                                s = addCharacters(s, sp, sc);
                            }
                            else if (sd > 0)
                            {
                                s = addCharacters(s, sp, sd);
                            }
                        }
                    }

                    if (r == 8 && r3 == 2)
                    {
                        if (print_borders) //print bottom line of borders
                        {
                            s = addLines(digits, borders, true, false, true, false, true, false, true, split, s, l, sp, r, sbc, sc, sd, indent, lbr);
                        }
                    }
                    else if ((r + 1) % 3 == 0 && r3 == 2)
                    {
                        s = addLines(digits, borders, print_borders, false, false, true, true, print_borders, true, split, s, l, sp, r, sbc, sc, sd, indent, lbr);
                    }
                    else if (r3 == 2)
                    {
                        s = addLines(digits, borders, print_borders, false, false, true, true, print_borders, true, split, s, l, sp, r, sbc, sc, sd, indent, lr);
                    }
                    else
                    {
                        s = addLines(digits, borders, print_borders, false, false, false, true, print_borders, true, split, s, l, sp, r, sbc, sc, sd, indent, split - 1);
                    }
                }
            }
            else
            {
                for (int c = 0; c < 9; c++)
                {
                    if (c == 0)
                    {
                        if (print_borders)
                        {
                            s.append(borders[5]);
                            s = addCharacters(s, sp, sbc);
                        }
                    }

                    if (print_all) //print like 123456789 fashion
                    {
                        for (int n = 0; n < 9; n++)
                        {
                            if (center_solved && status[r][c] > 0)
                            {
                                if (n != 4)
                                {
                                    s.append(digits[0]);
                                }
                                else
                                {
                                    s.append(digits[status[r][c]]);
                                }
                            }
                            else
                            {
                                s.append(digits[grid[r][c][n]]);
                            }

                            if ((c + 1) % 3 == 0 && n == 8)
                            {
                                if (print_borders)
                                {
                                    s = addCharacters(s, sp, sbc);
                                    s.append(borders[5]);
                                }
                                if (c != 8)
                                {
                                    s = addCharacters(s, sp, sbc);
                                }
                            }
                            else if (n == 8)
                            {
                                if (print_borders)
                                {
                                    s = addCharacters(s, sp, sc);
                                    s.append(borders[4]);
                                }
                                s = addCharacters(s, sp, sc);
                            }
                            else if (sd > 0)
                            {
                                s = addCharacters(s, sp, sd);
                            }
                        }
                    }
                    else //print only digit[0] for unsolved cells
                    {
                        s .append(status[r][c] > 0 ? digits[status[r][c]] : digits[0]);

                        if ((c + 1) % 3 == 0)
                        {
                            if (print_borders)
                            {
                                s = addCharacters(s, sp, sbc);
                                s .append(borders[5]);
                            }
                            if (c != 8)
                            {
                                s = addCharacters(s, sp, sbc);
                            }
                        }
                        else if (sc > 0)
                        {
                            if (print_borders)
                            {
                                s = addCharacters(s, sp, sc);
                                s.append(borders[4]);
                            }
                            s = addCharacters(s, sp, sc);
                        }
                    }
                }
                if (l.equals("") && !sp.equals("")) s.append(sp); // add cell separator if making a single line of output
                if (r == 8)
                {
                    if (print_borders) //print bottom line of borders
                    {
                        s = addLines(digits, borders, true, false, true, false, true, false, print_all, split, s, l, sp, r, sbc, sc, sd, indent, lbr);
                    }
                }
                else if ((r + 1) % 3 == 0)
                {
                    s = addLines(digits, borders, print_borders, false, false, true, true, print_borders, print_all, split, s, l, sp, r, sbc, sc, sd, indent, lbr);
                }
                else
                {
                    s = addLines(digits, borders, print_borders, false, false, true, true, print_borders, print_all, split, s, l, sp, r, sbc, sc, sd, indent, lr);
                }
            }
        }
        return s;
    }

    /**
     * Used in getString(grid, status, indent, l, s, ...) function
     * can supply whatever for borders, r, and row if print_borders if false
     * @param digits the strings used to print cell candidates
     * @param borders the strings used to print borders
     * @param print_borders if true, then proceed to print the specified borders,
     * if false then ignore the other print_xx booleans and does not print border
     * @param split if >= 1 then use 3 rows to print 1 row of cells
     * @param print_top if true then print the top most line of border, then print lines number of lines
     * @param print_bottom if true then print the bottom most line of border, then print lines number of lines
     * @param print_horizontal if true then print a line of horizontal borders
     * @param print_indent print_indent if true, print a line separator and an indent before each line
     * has no effect if print_borders is false
     * @param print_extra_indent if true print an extra line with indent
     * @param print_all if true print all of a cell's candidates
     * @param s the string to add lines number of line character l to
     * @param l the string character for a line separator
     * Note that by default, an l is added even if lines is 0
     * because lines specifies the visible number of gap lines between each line instead of noting an addition of l
     * to avoid printing line separators, set parameter l to "". This does not apply to spaces
     * @param sp the string character for a single space
     * @param r the current row index within the grid
     * @param sbc the number of spaces between each box column
     * @param sc the number of spaces between each column
     * @param sd the number of spaces between each digit (only used when print_all is true)
     * @param indent number of sp characters added after adding l
     * @param lines number of lines to be printed
     */
    public static StringBuilder addLines(String[] digits, String[] borders, boolean print_borders, boolean print_top, boolean print_bottom, boolean print_horizontal, boolean print_indent, boolean print_extra_indent, boolean print_all, int split, StringBuilder s, String l, String sp, int r, int sbc, int sc, int sd, int indent, int lines)
    {
        if (print_borders)
        {
            if (print_horizontal)
            {
                s = addLines(digits, borders, true, false, false, false, print_indent, false, print_all, split, s, l, sp, r, sbc, sc, sd, indent, lines);
                if (print_all)
                {
                    if (split > 0)
                    {
                        s = addBorderLine(borders, false, false, true, (r + 1) % 3 == 0, true, s, l, sp, indent, digits[0].length() * 3 + sd * 2 + sc + sbc, digits[0].length() * 3 + sd * 2 + sc * 2, digits[0].length() * 3 + sd * 2 + sc + sbc);
                    }
                    else
                    {
                        s = addBorderLine(borders, false, false, true, (r + 1) % 3 == 0, true, s, l, sp, indent, digits[0].length() * 9 + sd * 8 + sc + sbc, digits[0].length() * 9 + sd * 8 + sc * 2, digits[0].length() * 9 + sd * 8 + sc + sbc);
                    }
                }
                else
                {
                    s = addBorderLine(borders, false, false, true, (r + 1) % 3 == 0, true, s, l, sp, indent, digits[0].length() + sc + sbc, digits[0].length() + sc * 2, digits[0].length() + sc + sbc);
                }
                s = addLines(digits, borders, true, false, false, false, true, false, print_all, split, s, l, sp, r, sbc, sc, sd, indent, lines);
            }
            else
            {
                for (int j = 0; j < lines + 1; j++)
                {
                    if (print_all)
                    {
                        if (split > 0)
                        {
                            if (j == 0 && print_top)
                            {
                                s = addBorderLine(borders, true, false, false, false, print_indent, s, l, sp, indent, digits[0].length() * 3 + sd * 2 + sc + sbc, digits[0].length() * 3 + sd * 2 + sc * 2, digits[0].length() * 3 + sd * 2 + sc + sbc);
                            }
                            else if (j == lines && print_bottom)
                            {
                                s = addBorderLine(borders, false, true, false, false, true, s, l, sp, indent, digits[0].length() * 3 + sd * 2 + sc + sbc, digits[0].length() * 3 + sd * 2 + sc * 2, digits[0].length() * 3 + sd * 2 + sc + sbc);
                            }
                            else if (print_top || j < lines)
                            {
                                s = addBorderLine(borders, false, false, false, false, true, s, l, sp, indent, digits[0].length() * 3 + sd * 2 + sc + sbc, digits[0].length() * 3 + sd * 2 + sc * 2, digits[0].length() * 3 + sd * 2 + sc + sbc);
                            }
                        }
                        else
                        {
                            if (j == 0 && print_top)
                            {
                                s = addBorderLine(borders, true, false, false, false, print_indent, s, l, sp, indent, digits[0].length() * 9 + sd * 8 + sc + sbc, digits[0].length() * 9 + sd * 8 + sc * 2, digits[0].length() * 9 + sd * 8 + sc + sbc);
                            }
                            else if (j == lines && print_bottom)
                            {
                                s = addBorderLine(borders, false, true, false, false, true, s, l, sp, indent, digits[0].length() * 9 + sd * 8 + sc + sbc, digits[0].length() * 9 + sd * 8 + sc * 2, digits[0].length() * 9 + sd * 8 + sc + sbc);
                            }
                            else if (print_top || j < lines)
                            {
                                s = addBorderLine(borders, false, false, false, false, true, s, l, sp, indent, digits[0].length() * 9 + sd * 8 + sc + sbc, digits[0].length() * 9 + sd * 8 + sc * 2, digits[0].length() * 9 + sd * 8 + sc + sbc);
                            }
                        }
                    }
                    else
                    {
                        if (j == 0 && print_top)
                        {
                            s = addBorderLine(borders, true, false, false, false, print_indent, s, l, sp, indent, digits[0].length() + sc + sbc, digits[0].length() + sc * 2, digits[0].length() + sc + sbc);
                        }
                        else if (j == lines && print_bottom)
                        {
                            s = addBorderLine(borders, false, true, false, false, true, s, l, sp, indent, digits[0].length() + sc + sbc, digits[0].length() + sc * 2, digits[0].length() + sc + sbc);
                        }
                        else if (print_top || j < lines)
                        {
                            s = addBorderLine(borders, false, false, false, false, true, s, l, sp, indent, digits[0].length() + sc + sbc, digits[0].length() + sc * 2, digits[0].length() + sc + sbc);
                        }
                    }
                }
            }
        }
        else
        {
            //no borders at all, just add line separators and indents
            for (int j = 0; j < lines + 1; j++)
            {
                s.append(l);
                s = addCharacters(s, sp, indent);
            }
        }

        //used if printing a box row that needs to be followed by a row of numbers
        if (print_extra_indent)
        {
            s.append(l);
            s = addCharacters(s, sp, indent);
        }
        return s;
    }

    /**
     * Used in addLines()
     * Adds a single line of borders
     * @param borders the strings used to print borders
     * @param print_top if true print the top row of border, overrides print_horizontal and print_bold
     * @param print_bottom if true print the bottom row of border, overrides print_horizontal and print_bold
     * @param print_horizontal if true then print horizontal borders instead of vertical borders
     * @param print_bold if true then print (horizontal line is bold)bold horizontal borders instead of normal horizontal borders
     * does not have any effect if print_horizontal if false
     * @param print_indent print_indent if true, print a line separator and an indent before each line
     * @param s the string to add strings to, then returned
     * @param l the string character for a line separator
     * @param sp the string character for a single space
     * @param indent number of sp characters added after adding l
     * @param g1 the number of spaces between left most vertical border and the second vertical border
     * @param g2 the number of spaces between the second vertical border and the third vertical border
     * @param g3 the number of spaces between the third vertical border and the fourth vertical border (end of first box)
     */
    public static StringBuilder addBorderLine(String[] borders, boolean print_top, boolean print_bottom, boolean print_horizontal, boolean print_bold, boolean print_indent,
    StringBuilder s, String l, String sp, int indent, int g1, int g2, int g3)
    {
        if (print_indent)
        {
            s.append(l);
            s = addCharacters(s, sp, indent);
        }
        s.append(print_top ? borders[0] : (print_bottom ? borders[2] : (print_horizontal ? (print_bold ? borders[13] : borders[12]) : borders[5])));

        for (int i = 0; i < 3; i++)
        {
            s = addCharacters(s, (print_top || print_bottom) ? borders[7] : (print_horizontal ? (print_bold ? borders[7] : borders[6]) : sp), g1);

            s.append(print_top ? borders[8] : (print_bottom ? borders[10] : (print_horizontal ? (print_bold ? borders[18] : borders[16]) : borders[4])));

            s = addCharacters(s, (print_top || print_bottom) ? borders[7] : (print_horizontal ? (print_bold ? borders[7] : borders[6]) : sp), g2);

            s.append(print_top ? borders[8] : (print_bottom ? borders[10] : (print_horizontal ? (print_bold ? borders[18] : borders[16]) : borders[4])));

            s = addCharacters(s, (print_top || print_bottom) ? borders[7] : (print_horizontal ? (print_bold ? borders[7] : borders[6]) : sp), g3);

            if (i == 2)
            {
                //right most border of a line
                s.append(print_top ? borders[1] : (print_bottom ? borders[3] : (print_horizontal ? (print_bold ? borders[15] : borders[14]) : borders[5])));
            }
            else
            {
                s.append(print_top ? borders[9] : (print_bottom ? borders[11] : (print_horizontal ? (print_bold ? borders[19] : borders[17]) : borders[5])));
            }
        }
        return s;
    }

    /**
     * Used in getString(grid, status, indent, l, s, ...) function
     * @param s the string to add n number of c characters
     */
    public static StringBuilder addCharacters(StringBuilder s, String c, int n)
    {
        for (int i = 0; i < n; i++) s.append(c);
        return s;
    }
}
