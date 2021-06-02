package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.Sudoku;

import java.util.ArrayList;

/**
 * @since 5-24-2021
 */
public class Results
{
    public int found;
    public Sudoku sudoku;
    public ArrayList<String> messages;
    public SolvingTechnique solvingTechniqueUsed;
    public SolvingTechniqueNode techniqueNodeUsed;

    public Results(Sudoku sudoku, int found, ArrayList<String> messages, SolvingTechnique solvingTechniqueUsed)
    {
        this.sudoku = sudoku;
        this.found = found;
        this.messages = messages;
        this.solvingTechniqueUsed = solvingTechniqueUsed;
    }
}
