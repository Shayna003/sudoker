package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import java.util.function.*;
import java.util.*;

/**
 * @since 2-20-2021
 * there also should be ? icons rendered in the JTree that leads to a page explaining the solving technique
 */
@SuppressWarnings("CanBeFinal")
public class SolvingTechnique
{
	public String name;
	public Difficulty difficulty;
	
	public boolean isTechniqueGroup; // e.g. Naked Candidates includes naked pairs, triples, and quads
	public SolvingTechnique group; // null if this is not a member of a technique group
	public SolvingTechnique[] members; // null if this is not a technique group
	
	// Results.found: records how many cases found, 0 = none found. 
	// If Results.found > 0, results will be recorded in the board.
	// And if takeStep of the tree is invoked again, will set the Board's Sudoku to the sudoku recorded in the Results object.
	public BiFunction<Board, SolvingTechniqueNode, Results> takeStep;
	
	public static EnumMap<Difficulty, ArrayList<SolvingTechnique>> techniques_byDifficulty = new EnumMap<>(Difficulty.class);

	public static final SolvingTechnique ELIMINATE;
	public static final SolvingTechnique NAKED_CANDIDATES;
	public static final SolvingTechnique NAKED_PAIRS;
	public static final SolvingTechnique NAKED_TRIPLES;
	public static final SolvingTechnique NAKED_QUADS;

	public static final SolvingTechnique HIDDEN_CANDIDATES;
	public static final SolvingTechnique HIDDEN_SINGLES;
	public static final SolvingTechnique HIDDEN_PAIRS;
	public static final SolvingTechnique HIDDEN_TRIPLES;
	public static final SolvingTechnique HIDDEN_QUADS;

	public static final SolvingTechnique POINTING_CANDIDATES;
	public static final SolvingTechnique BOX_LINE_REDUCTION;

	public static final SolvingTechnique INTERSECTION_REMOVAL;

	static
	{
		ArrayList<SolvingTechnique> basic = new ArrayList<>();
		ArrayList<SolvingTechnique> easy = new ArrayList<>();
		ArrayList<SolvingTechnique> moderate = new ArrayList<>();
		ArrayList<SolvingTechnique> hard = new ArrayList<>();
		
		techniques_byDifficulty.put(Difficulty.BASIC, basic);
		techniques_byDifficulty.put(Difficulty.EASY, easy);
		techniques_byDifficulty.put(Difficulty.MODERATE, moderate);
		techniques_byDifficulty.put(Difficulty.HARD, hard);

		ELIMINATE = new SolvingTechnique("Find New Solved Cells and Eliminate", Difficulty.BASIC, (board, node) ->
		{
			return Solver.eliminate(board, false, false, true);
		});
		addTechnique(ELIMINATE);

		// easy techniques

		// naked candidates
		NAKED_CANDIDATES = new SolvingTechnique("Naked Candidates", Difficulty.EASY);

		NAKED_PAIRS = new SolvingTechnique("Naked Pairs", Difficulty.EASY, (board, node) ->
		{
			return Solver.nakedCandidates(board, 2, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, NAKED_CANDIDATES);

		NAKED_TRIPLES = new SolvingTechnique("Naked Triples", Difficulty.EASY, (board, node) ->
		{
			return Solver.nakedCandidates(board, 3, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, NAKED_CANDIDATES);

		NAKED_QUADS = new SolvingTechnique("Naked Quads", Difficulty.EASY,(board, node) ->
		{
			return Solver.nakedCandidates(board, 4, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, NAKED_CANDIDATES);

		NAKED_CANDIDATES.members = new SolvingTechnique[] { NAKED_PAIRS, NAKED_TRIPLES, NAKED_QUADS };
		addTechnique(NAKED_CANDIDATES);

		// hidden candidates
		HIDDEN_CANDIDATES = new SolvingTechnique("Hidden Candidates", Difficulty.EASY);
		
		HIDDEN_SINGLES = new SolvingTechnique("Hidden Singles", Difficulty.EASY, (board, node) ->
		{
			return Solver.hiddenSingles(board, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, HIDDEN_CANDIDATES);

		HIDDEN_PAIRS = new SolvingTechnique("Hidden Pairs", Difficulty.EASY, (board, node) ->
		{
			return Solver.hiddenCandidates(board, 2, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, HIDDEN_CANDIDATES);

		HIDDEN_TRIPLES = new SolvingTechnique("Hidden Triples", Difficulty.EASY, (board, node) ->
		{
			return Solver.hiddenCandidates(board, 3, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, HIDDEN_CANDIDATES);

		HIDDEN_QUADS = new SolvingTechnique("Hidden Quads", Difficulty.EASY, (board, node) ->
		{
			return Solver.hiddenCandidates(board, 4, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, HIDDEN_CANDIDATES);

		HIDDEN_CANDIDATES.members = new SolvingTechnique[] { HIDDEN_SINGLES, HIDDEN_PAIRS, HIDDEN_TRIPLES, HIDDEN_QUADS };
		addTechnique(HIDDEN_CANDIDATES);
		
		// intersection removal
		INTERSECTION_REMOVAL = new SolvingTechnique("Intersection Removal", Difficulty.EASY);
		
		POINTING_CANDIDATES = new SolvingTechnique("Pointing Candidates", Difficulty.EASY, (board, node) ->
		{
			return Solver.pointingCandidates(board, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, INTERSECTION_REMOVAL);

		BOX_LINE_REDUCTION = new SolvingTechnique("Box Line Reduction", Difficulty.EASY, (board, node) ->
		{
			return Solver.boxLineReduction(board, Application.solverSettingsPanel.returnFirstMatch.isSelected(), false, false, new int[3], true);
		}, INTERSECTION_REMOVAL);

		INTERSECTION_REMOVAL.members = new SolvingTechnique[] { POINTING_CANDIDATES, BOX_LINE_REDUCTION };
		addTechnique(INTERSECTION_REMOVAL);
	}
	
	static void addTechnique(SolvingTechnique st)
	{
		techniques_byDifficulty.get(st.difficulty).add(st);
	}
	
	public static ArrayList<SolvingTechnique> get(Difficulty difficulty)
	{
		return techniques_byDifficulty.get(difficulty);
	}

	/**
	 * For a technique group
	 */
	private SolvingTechnique(String name, Difficulty difficulty)
	{
		this(name, difficulty, (board, node) ->
		{
			return node.returnResultsOfAChildNode();
		});
		this.isTechniqueGroup = true;
	}

	/*
	 * For a child technique
	 */
	private SolvingTechnique(String name, Difficulty difficulty, BiFunction<Board, SolvingTechniqueNode, Results> takeStep, SolvingTechnique group)
	{
		this(name, difficulty, takeStep);
		this.group = group;
	}

	/**
	 * Called by single techniques that don't belong to a group/have child techniques
	 */
	private SolvingTechnique(String name, Difficulty difficulty, BiFunction<Board, SolvingTechniqueNode, Results> takeStep)
	{
		this.name = name;
		this.difficulty = difficulty;
		this.takeStep = takeStep;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj instanceof SolvingTechnique)
		{
			return ((SolvingTechnique) obj).name.equals(name);
		}
		else return false;
	}
}