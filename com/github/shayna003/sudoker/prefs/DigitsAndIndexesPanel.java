package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.util.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.components.*;

import javax.swing.*;
import javax.swing.event.*;
import java.util.prefs.*;
import java.util.*;
import java.io.*;
import java.awt.*;

/**
 * Many of the related functions are in IO.
 * Does not support Unicode code points that are above the range of u+FFFF
 * Even if the program supports it, you need to use an appropriate font to be able to display it(the usual fonts do not)
 * This class stores the old Prefs.java settings, including box strings, digits, and indexes used for display
 * @since 3-2-2021
 */
@SuppressWarnings("CanBeFinal")
public class DigitsAndIndexesPanel extends JPanel implements SettingsPanel
{
	boolean initializing;
	PreferenceFrame preferenceFrame;
	
	SingleSettingsFile settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "digits_and_indexes.xml"));
	
	ArrayList<PrefsComponent> prefsComponents = new ArrayList<>();

	DocumentListener applyChangesListener;
	// displayed on sudoku boards, solver outputs
	// [0] represents an empty value, a cell does not have that number as a candidate
	public PrefsTextField[] digits = new PrefsTextField[10];
	
	// for reading in and outputting Sudokus
	public PrefsTextField[] input_digits = new PrefsTextField[10];
	public PrefsTextField[] output_digits = new PrefsTextField[10];
	public PrefsTextField[] borders = new PrefsTextField[20];
	public PrefsTextField cellSeparator; // used in inputting 
	
	public PrefsTextField[] row_indexes = new PrefsTextField[9];
	public PrefsTextField[] col_indexes = new PrefsTextField[9];
	public PrefsTextField[] box_indexes = new PrefsTextField[9];
	
	// used in printed solver messages
	public PrefsTextField candidateSeparator; // e.g. ", " in "5, 6, 7"
	public PrefsTextField lastCandidateSeparator; // e.g. " and " in "5, 6, 7 and 8"
	
	JRadioButton rowIndexFirst;
	JRadioButton colIndexFirst;
	
	// determines where row and column indexes are painted relative to the board
	public JRadioButton rowIndexLeft;
	public JRadioButton rowIndexRight;
	public JRadioButton rowIndexBoth;
	PrefsButtonGroup rowIndexPosition;
	
	public JRadioButton colIndexTop;
	public JRadioButton colIndexBottom;
	public JRadioButton colIndexBoth;
	PrefsButtonGroup colIndexPosition;

	public PrefsTextField rc_index_separator; // e.g. "|" in "A|5, B|6, and D|7"
	public PrefsTextField cellIndexSeparator; // e.g. ", " in "A|5, B|6, and D|7"
	public PrefsTextField lastCellIndexSeparator; // e.g. "and " in "A|5, B|6, and D|7"
	
	// length 20
	String[] defaultBorders = new String[]
	{
		"┏", "┓", "┗", "┛", "│", "┃", "─", "━", "┯", "┳", "┷", "┻", "┠", "┣", "┨", "┫", "┼", "╂", "┿", "╋"
	};

	/**
	 * @return -1 if s is not included in fields
	 */
	public static int getIndex(String s, PrefsTextField[] fields)
	{
		for (int i = 0; i < fields.length; i++)
		{
			if (fields[i].getText().equals(s))
			{
				return i;
			}
		}
		return -1;
	}
	
	public String getCellSeparator()
	{
		return cellSeparator.getText();
	}
	
	public String getLastCellIndexSeparator()
	{
		return lastCellIndexSeparator.getText();
	}

	public String getCellIndexSeparator()
	{
		return cellIndexSeparator.getText();
	}
	
	public String get_rc_IndexSeparator()
	{
		return rc_index_separator.getText();
	}
	
	public String getCandidateSeparator()
	{
		return candidateSeparator.getText();
	}
	
	public String getLastCandidateSeparator()
	{
		return lastCandidateSeparator.getText();
	}
	
	/*
	 * @param d range [0, 9]
	 */
	public String getDigit(int d)
	{
		return digits[d].getText();
	}
	
	@Override
	public void updateUI()
	{
		Application.prefsLogger.entering("DigitsAndIndexesPanel", "updateUI");
		super.updateUI();
	}
	
	/**
	 * For functions in IO.java
	 */
	public static String[] getStringArray(PrefsTextField[] target)
	{
		String[] d = new String[target.length];
		for (int i = 0; i < target.length; i++)
		{
			d[i] = target[i].getText();
		}
		return d;
	}
	
	/**
	 * For functions in IO.java
	 */
	public String[] getBorders()
	{
		String[] b = new String[20];
		for (int i = 0; i < 20; i++)
		{
			b[i] = borders[i].getText();
		}
		return b;
	}
	
	/**
	 * @param d range [0, 9]
	 */
	public String getInputDigit(int d)
	{
		return input_digits[d].getText();
	}
	
	/**
	 * @param d range [0, 9]
	 */
	public String getOutputDigit(int d)
	{
		return output_digits[d].getText();
	}
	
	/**
	 * @param b range [0, 19]
	 */
	public String getBorder(int b)
	{
		return borders[b].getText();
	}
	
	public String getRowIndex(int r)
	{
		return row_indexes[r].getText();
	}
	
	public String getColIndex(int c)
	{
		return col_indexes[c].getText();
	}
	
	/**
	 * @param b:
	 * 012
	 * 345
	 * 678
	 */
	public String getBoxIndex(int b)
	{
		return box_indexes[b].getText();
	}
	
	/**
	 * @return a number that represents a box
	 * 012
	 * 345
	 * 678
	 * @param boxr a number in the range of [0, 2]
	 * @param boxc a number in the range of [0, 2]
	 *
	 */
	public static int getBoxNumber(int boxr, int boxc)
	{
		return boxr * 3 + boxc;
	}

	public static int getBoxNumberFromRC(int r, int c)
	{
		return (Math.floorDiv(r, 3) * 3 + Math.floorDiv(c, 3));
	}
	
	public String getBoxIndex(int boxr, int boxc)
	{
		return getBoxIndex(getBoxNumber(boxr, boxc));
	}
	
	public String getBoxIndexFromRC(int r, int c)
	{
		return getBoxIndex(getBoxNumber(Math.floorDiv(r, 3), Math.floorDiv(c, 3)));
	}
	
	/**
	 * This function could be used for within box indexes of cells
	 * and also box indexes within the sudoku grid 
	 * @return turns a list of box row and column indexes from
	 * 00 01 02
	 * 10 11 12
	 * 20 21 22
	 * and format them to
	 * 0 1 2
	 * 3 4 5
	 * 6 7 8
	 * @return values range from [0, 8], represent position in a box
	*/
	public static int[] getWithinBoxIndexes(int[] row, int[] col)
	{
		assert row != null && col != null && row.length == col.length : "row: " + Arrays.toString(row) + ", col: " + Arrays.toString(col);
		int[] result = new int[row.length];
		int boxr;
		int boxc;
		for (int i = 0; i < row.length; i++)
		{
			boxr = Math.floorDiv(row[i], 3) * 3;
			boxc = Math.floorDiv(col[i], 3) * 3;
			result[i] = (row[i] - boxr) * 3 + col[i] - boxc;
		}
		return result;
	}
	
	/**
	* @param cands an array of candidates
	* @return a long string with the following format:
	* "1, 2, 3, 4" <- an abstract representation
	*/
	public String getStringCandidates(int[] cands)
	{
		assert cands != null && cands.length > 0 : Arrays.toString(cands);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < cands.length; i++)
		{
			if (i == cands.length - 1 && i > 0)
			{
				b.append(cands.length > 2 && getLastCandidateSeparator().startsWith(" ") ? getLastCandidateSeparator().substring(1) : getLastCandidateSeparator());
			}
			b.append(getDigit(cands[i]));
			if (i != cands.length - 1 && cands.length != 2) b.append(getCandidateSeparator());
		}
		return b.toString();
	}
	
	public String getStringIndex(int r, int c)
	{
		StringBuilder b = new StringBuilder();
		if (rowIndexFirst.isSelected())
		{
			b.append(getRowIndex(r));
			b.append(get_rc_IndexSeparator());
			b.append(getColIndex(c));
		}
		else 
		{
			b.append(getColIndex(c));
			b.append(get_rc_IndexSeparator());
			b.append(getRowIndex(r));
		}
		return b.toString();
	}
	
	/**
	* @param rx an array of row indexes
	* @param cx an array of column indexes
	* @return a long string with the following format:
	* "A1, B2, C3, D4, E5, F6" <- an abstract representation
	*/
	public String getStringIndexes(int[] rx, int[] cx)
	{
		assert rx != null && cx != null && cx.length > 0 && rx.length == cx.length : "rx: " + Arrays.toString(rx) + ", cx: " + Arrays.toString(cx);

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < rx.length; i++)
		{
			if (i == rx.length - 1 && i > 0)
			{
				b.append(rx.length > 2 && getLastCellIndexSeparator().startsWith(" ") ? getLastCellIndexSeparator().substring(1) : getLastCellIndexSeparator());
			}
			if (rowIndexFirst.isSelected())
			{
				b.append(getRowIndex(rx[i]));
				b.append(get_rc_IndexSeparator());
				b.append(getColIndex(cx[i]));
			}
			else 
			{
				b.append(getColIndex(cx[i]));
				b.append(get_rc_IndexSeparator());
				b.append(getRowIndex(rx[i]));
			}
			if (i < rx.length - 1 && rx.length != 2) b.append(getCellIndexSeparator());
		}
		return b.toString();
	}

	/**
	 * Used by import and export digits
	 */
	public void showDigitDefaults(PrefsTextField[] fields, /*String message,*/ String title)
	{
		@SuppressWarnings("unchecked")
		ArrayWrapper<String> result = (ArrayWrapper<String>) JOptionPane.showInputDialog(preferenceFrame, "Choose from a default set of values", title, JOptionPane.PLAIN_MESSAGE, null, new Object[] {
			new ArrayWrapper<String>(new String[] {" ", "1", "2", "3", "4", "5", "6", "7", "8", "9"}),
			new ArrayWrapper<String>(new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}),
			new ArrayWrapper<String>(new String[] {".", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"}),
			new ArrayWrapper<String>(new String[] {".", "⓵", "⓶", "⓷", "⓸", "⓹", "⓺", "⓻", "⓼", "⓽"}),
			new ArrayWrapper<String>(new String[] {".", "一", "二", "三", "四", "五", "六", "七", "八", "九"}),
			new ArrayWrapper<String>(new String[] {".", "㊀", "㊁", "㊂", "㊃", "㊄", "㊅", "㊆", "㊇", "㊈"})}, null);
		
		if (result != null)
		{
			for (int i = 0; i < result.elements.length; i++)
			{
				fields[i].setText(result.elements[i]);
			}
		}
	}
	
	public void showIndexDefaults(PrefsTextField[] fields, /*String message,*/ String title)
	{
		@SuppressWarnings("unchecked")
		ArrayWrapper<String> result = (ArrayWrapper<String>) JOptionPane.showInputDialog(preferenceFrame, "Choose from a default set of values", title, JOptionPane.PLAIN_MESSAGE, null, new Object[] {
			new ArrayWrapper<String>(new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8"}),
			new ArrayWrapper<String>(new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9"}),		
			new ArrayWrapper<String>(new String[] {"⓪", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧"}),
			new ArrayWrapper<String>(new String[] {"①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"}),
			new ArrayWrapper<String>(new String[] {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈"}),
			new ArrayWrapper<String>(new String[] {"₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"}),
			new ArrayWrapper<String>(new String[] {"➀", "➁", "➂", "➃", "➄", "➅", "➆", "➇", "➈"}),
			new ArrayWrapper<String>(new String[] {"➊", "➋", "➌", "➍", "➎", "➏", "➐", "➑", "➒"}),
			new ArrayWrapper<String>(new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I"}),
			new ArrayWrapper<String>(new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"}),
			new ArrayWrapper<String>(new String[] {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"})}, null);
		
		if (result != null)
		{
			for (int i = 0; i < result.elements.length; i++)
			{
				fields[i].setText(result.elements[i]);
			}
		}
	}
	
	public DigitsAndIndexesPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		this.preferenceFrame = preferenceFrame;
		
		applyChangesListener = new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event) 
			{
				applyChanges();
			}
				
			@Override
			public void removeUpdate(DocumentEvent event) 
			{
				applyChanges();
			}
				
			@Override
			public void changedUpdate(DocumentEvent event) 
			{
				applyChanges();
			}
		};
		
		// initialize components
		int cols = 3;
		String toolTip;
		
		for (int i = 0; i < 10; i++)
		{
			toolTip = "Digit " + i;
			digits[i] = new PrefsTextField("digit_" + i, i == 0 ? "" : String.valueOf(i), cols, applyChangesListener).setToolTip(toolTip);
			input_digits[i] = new PrefsTextField("input_digit_" + i, String.valueOf(i), cols).setToolTip(toolTip);
			output_digits[i] = new PrefsTextField("output_digit_" + i, String.valueOf(i), cols).setToolTip(toolTip);
			prefsComponents.add(digits[i]);
			prefsComponents.add(input_digits[i]);
			prefsComponents.add(output_digits[i]);
		}
		
		for (int i = 0; i < 9; i++)
		{
			row_indexes[i] = new PrefsTextField("row_index_" + i, String.valueOf(i + 1), cols, applyChangesListener).setToolTip("Index for row " + (i + 1));
			col_indexes[i] = new PrefsTextField("col_index_" + i, String.valueOf((char) (65 + i)), cols, applyChangesListener).setToolTip("Index for column " + (i + 1)); // A - I
			box_indexes[i] = new PrefsTextField("box_index_" + i, String.valueOf(i + 1), cols, applyChangesListener).setToolTip("Index for box " + (i + 1) + " (1 = top left, 3 = top right, 5 = center, 9 = bottom right)");
			
			prefsComponents.add(row_indexes[i]);
			prefsComponents.add(col_indexes[i]);
			prefsComponents.add(box_indexes[i]);
		}
		
		for (int i = 0; i < 20; i++)
		{
			borders[i] = new PrefsTextField("border_" + i, defaultBorders[i], cols);
			prefsComponents.add(borders[i]);
		}
		
		cellSeparator = new PrefsTextField("cellSeparator", " ", cols).setToolTip("Used to simplify inputting all candidates of a Sudoku");
		
		rc_index_separator = new PrefsTextField("rc_index_separator", "", cols).setToolTip("row index + this + column index denotes a unique cell on a board");
		candidateSeparator = new PrefsTextField("candidateSeparator", ", ", cols).setToolTip("Used by solver to generate solving messages");
		lastCandidateSeparator = new PrefsTextField("lastCandidateSeparator", " and ", cols).setToolTip("Used by solver to generate solving messages");
		cellIndexSeparator = new PrefsTextField("cellIndexSeparator", ", ", cols).setToolTip("Used to separate many indexes denoting different cells");;
		lastCellIndexSeparator = new PrefsTextField("lastCellIndexSeparator", " and ", cols).setToolTip("Last Index Used to separate many indexes denoting different cells");;
		
		prefsComponents.add(cellSeparator);
		prefsComponents.add(rc_index_separator);
		prefsComponents.add(candidateSeparator);
		prefsComponents.add(lastCandidateSeparator);
		prefsComponents.add(cellIndexSeparator);
		prefsComponents.add(lastCellIndexSeparator);
		
		// other settingsPanel need this panel's settings during initialization, for example, BoxColorChooserComponent
		Application.digitsAndIndexesPanel = this;

		JPanel forDisplay = new JPanel(new GridBagLayout());
		forDisplay.setBorder(BorderFactory.createTitledBorder("Display and Solver Console Messages"));
		
		JPanel forIO = new JPanel(new GridBagLayout());
		forIO.setBorder(BorderFactory.createTitledBorder("Imports and Exports"));

		JLabel digit0Info = SwingUtil.makeTranslucentLabel("Digit 0 represents \"no such candidate\" in \"All Candidates\" view mode.", 125);
		forDisplay.add(digit0Info, new GBC(0, 0, 13, 1).setAnchor(GBC.WEST));

		forDisplay.add(new JLabel("Digits: "), new GBC(0, 1).setAnchor(GBC.EAST));
		for (int i = 0; i < digits.length; i++)
		{
			forDisplay.add(digits[i], new GBC(i + 1, 1));
		}
		JButton chooseDigitsFromDefaults = new JButton("See Defaults");
		chooseDigitsFromDefaults.setToolTipText("Import from defaults for digits");
		chooseDigitsFromDefaults.addActionListener(event ->
		{
			@SuppressWarnings("unchecked")
			ArrayWrapper<String> result = (ArrayWrapper<String>) JOptionPane.showInputDialog(preferenceFrame, "Import from a default set of values", "Import Digits", JOptionPane.PLAIN_MESSAGE, null, new Object[] {
				new ArrayWrapper<String>(new String[] {" ", "1", "2", "3", "4", "5", "6", "7", "8", "9"}),
				new ArrayWrapper<String>(new String[] {" ", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"}),
				new ArrayWrapper<String>(new String[] {" ", "⓵", "⓶", "⓷", "⓸", "⓹", "⓺", "⓻", "⓼", "⓽"}),
				new ArrayWrapper<String>(new String[] {" ", "一", "二", "三", "四", "五", "六", "七", "八", "九"}),
				new ArrayWrapper<String>(new String[] {" ", "㊀", "㊁", "㊂", "㊃", "㊄", "㊅", "㊆", "㊇", "㊈"}),
				new ArrayWrapper<String>(new String[] {" ", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"})}, null);
			
			if (result != null)
			{
				for (int i = 0; i < result.elements.length; i++)
				{
					digits[i].setText(result.elements[i]);
				}
			}
		});
		forDisplay.add(chooseDigitsFromDefaults, new GBC(11, 1).setAnchor(GBC.WEST));

		forDisplay.add(new JLabel("Candidate Separator: "), new GBC(0, 2).setAnchor(GBC.EAST));
		forDisplay.add(candidateSeparator, new GBC(1, 2));

		forDisplay.add(new JLabel("Last Candidate Separator: "), new GBC(0, 3).setAnchor(GBC.EAST));
		forDisplay.add(lastCandidateSeparator, new GBC(1, 3));

		JLabel warningMessage1 = SwingUtil.makeTranslucentLabel("For imports and exports to work as expected: ", 125);
		JLabel warningMessage2 = SwingUtil.makeTranslucentLabel("Output digits must match input digits to read back a Sudoku.", 125);
		JLabel warningMessage3 = SwingUtil.makeTranslucentLabel("Imports and Exports only work with single character digits with codepoint <= U+FFFF.", 125);
		JLabel warningMessage4 = SwingUtil.makeTranslucentLabel("Cell separators can be used to shorten representation of a cell from 9 characters to 1-10 characters.", 125);
		JLabel warningMessage5 = SwingUtil.makeTranslucentLabel("The value for cell separator must be different than digit 0 values of input and output digits.", 125);

		//The value for cell separator must be different than digit 0 values of input and output digits.
		forIO.add(warningMessage1, new GBC(0, 0, 13, 1).setAnchor(GBC.WEST));
		forIO.add(warningMessage2, new GBC(0, 1, 13, 1).setAnchor(GBC.WEST));
		forIO.add(warningMessage3, new GBC(0, 2, 13, 1).setAnchor(GBC.WEST));
		forIO.add(warningMessage4, new GBC(0, 3, 13, 1).setAnchor(GBC.WEST));
		forIO.add(warningMessage5, new GBC(0, 4, 13, 1).setAnchor(GBC.WEST));

		forIO.add(new JLabel("Input Digits: "), new GBC(0, 5).setAnchor(GBC.EAST));
		for (int i = 0; i < input_digits.length; i++)
		{
			forIO.add(input_digits[i], new GBC(i + 1, 5).setFill(GBC.BOTH));
		}
		JButton chooseInputDigitsFromDefaults = new JButton("See Defaults");
		chooseInputDigitsFromDefaults.setToolTipText("Import from defaults for input digits");
		chooseInputDigitsFromDefaults.addActionListener(event ->
		{
			showDigitDefaults(input_digits, "Import Input Digits");
		});
		forIO.add(chooseInputDigitsFromDefaults, new GBC(11, 5).setAnchor(GBC.WEST));
		
		// output digits
		forIO.add(new JLabel("Output Digits: "), new GBC(0, 6).setAnchor(GBC.EAST));
		for (int i = 0; i < output_digits.length; i++)
		{
			forIO.add(output_digits[i], new GBC(i + 1, 6));
		}
		JButton chooseOutputDigitsFromDefaults = new JButton("See Defaults");
		chooseOutputDigitsFromDefaults.setToolTipText("Import from defaults for output digits");
		chooseOutputDigitsFromDefaults.addActionListener(event ->
		{
			showDigitDefaults(output_digits, "Import Output Digits");
		});
		forIO.add(chooseOutputDigitsFromDefaults, new GBC(11, 6).setAnchor(GBC.WEST));
		
		// borders
		forIO.add(new JLabel("Borders: "), new GBC(0, 7).setAnchor(GBC.EAST));
		for (int i = 0; i < 10; i++)
		{
			forIO.add(borders[i], new GBC(i + 1, 7));
		}
		JButton setBordersToDefault = new JButton("Reset");
		setBordersToDefault.setToolTipText("Reset borders to defaults");
		setBordersToDefault.addActionListener(event ->
		{
			for (int i = 0; i < borders.length; i++)
			{
				borders[i].setText(defaultBorders[i]);
			}
		});
		forIO.add(setBordersToDefault, new GBC(11, 7).setAnchor(GBC.WEST));
		for (int i = 10; i < 20; i++)
		{
			forIO.add(borders[i], new GBC(i - 9, 8));
		}
		
		// separators
		forIO.add(new JLabel("Cell Separator: "), new GBC(0, 9).setAnchor(GBC.EAST));
		forIO.add(cellSeparator, new GBC(1, 9));

		JPanel indexPanel = new JPanel(new GridBagLayout());
		
		// decides whether row or column index comes first when referring to a cell, default is row index first
		rowIndexFirst = new JRadioButton("Row Index First");
		colIndexFirst = new JRadioButton("Column Index First");
		PrefsButtonGroup indexButtonGroup = new PrefsButtonGroup(null, "IndexOrder", 0, rowIndexFirst, colIndexFirst);
		prefsComponents.add(indexButtonGroup);
		
		rowIndexLeft = new JRadioButton("Left");
		rowIndexRight = new JRadioButton("Right");
		rowIndexBoth = new JRadioButton("Both Left and Right");
		rowIndexPosition = new PrefsButtonGroup(event -> applyChanges(), "rowIndexPosition", 0, rowIndexLeft, rowIndexRight, rowIndexBoth);
		prefsComponents.add(rowIndexPosition);
		
		colIndexTop = new JRadioButton("Top");
		colIndexBottom = new JRadioButton("Bottom");
		colIndexBoth = new JRadioButton("Both Top and Bottom");
		colIndexPosition = new PrefsButtonGroup(event -> applyChanges(), "colIndexPosition", 0, colIndexTop, colIndexBottom, colIndexBoth);
		prefsComponents.add(colIndexPosition);
		
		
		JPanel indexRadioButtonPanel = new JPanel(new GridBagLayout());
		indexRadioButtonPanel.add(new JLabel("Index Order When Referring a Cell: "), new GBC(0, 0).setAnchor(GBC.EAST));
		indexRadioButtonPanel.add(rowIndexFirst, new GBC(1, 0, 2, 1).setAnchor(GBC.WEST));
		indexRadioButtonPanel.add(colIndexFirst, new GBC(3, 0, 2, 1).setAnchor(GBC.WEST));
		
		indexRadioButtonPanel.add(new JLabel("Location to Paint Row Indexes Relative to Board: "), new GBC(0, 1).setAnchor(GBC.EAST));
		indexRadioButtonPanel.add(rowIndexLeft, new GBC(1, 1).setAnchor(GBC.WEST));
		indexRadioButtonPanel.add(rowIndexRight, new GBC(2, 1).setAnchor(GBC.WEST));
		indexRadioButtonPanel.add(rowIndexBoth, new GBC(3, 1).setAnchor(GBC.WEST));
		
		indexRadioButtonPanel.add(new JLabel("Location to Paint Column Indexes Relative to Board: "), new GBC(0, 2).setAnchor(GBC.EAST));
		indexRadioButtonPanel.add(colIndexTop, new GBC(1, 2).setAnchor(GBC.WEST));
		indexRadioButtonPanel.add(colIndexBottom, new GBC(2, 2).setAnchor(GBC.WEST));
		indexRadioButtonPanel.add(colIndexBoth, new GBC(3, 2).setAnchor(GBC.WEST));
		
		indexPanel.add(indexRadioButtonPanel, new GBC(0, 0, 11, 1));
		
		// row indexes
		indexPanel.add(new JLabel("Row Indexes: "), new GBC(0, 1).setAnchor(GBC.EAST));
		for (int i = 0; i < row_indexes.length; i++)
		{
			indexPanel.add(row_indexes[i], new GBC(i + 1, 1));
		}
		JButton chooseRowIndexFromDefaults = new JButton("See Defaults");
		chooseRowIndexFromDefaults.setToolTipText("Import from defaults for row indexes");
		chooseRowIndexFromDefaults.addActionListener(event ->
		{
			showIndexDefaults(row_indexes, "Import row indexes");
		});
		indexPanel.add(chooseRowIndexFromDefaults, new GBC(10, 1).setAnchor(GBC.WEST));
		
		// column indexes
		indexPanel.add(new JLabel("Column Indexes: "), new GBC(0, 2).setAnchor(GBC.EAST));
		for (int i = 0; i < col_indexes.length; i++)
		{
			indexPanel.add(col_indexes[i], new GBC(i + 1, 2));
		}
		JButton chooseColIndexFromDefaults = new JButton("See Defaults");
		chooseColIndexFromDefaults.setToolTipText("Import from defaults for column indexes");
		chooseColIndexFromDefaults.addActionListener(event ->
		{
			showIndexDefaults(col_indexes, "Import column indexes");
		});
		indexPanel.add(chooseColIndexFromDefaults, new GBC(10, 2).setAnchor(GBC.WEST));
		
		// box indexes
		indexPanel.add(new JLabel("Box Indexes: "), new GBC(0, 3).setAnchor(GBC.EAST));
		for (int i = 0; i < box_indexes.length; i++)
		{
			indexPanel.add(box_indexes[i], new GBC(i + 1, 3));
		}
		JButton chooseBoxIndexFromDefaults = new JButton("See Defaults");
		chooseBoxIndexFromDefaults.setToolTipText("Import from defaults for box indexes");
		chooseBoxIndexFromDefaults.addActionListener(event ->
		{
			@SuppressWarnings("unchecked")
			ArrayWrapper<String> result = (ArrayWrapper<String>) JOptionPane.showInputDialog(preferenceFrame, "Import from a default set of values", "Import Box Indexes", JOptionPane.PLAIN_MESSAGE, null, new Object[] {
				new ArrayWrapper<String>(new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8"}), 
				new ArrayWrapper<String>(new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9"}),
				new ArrayWrapper<String>(new String[] {"⓪", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧"}), 
				new ArrayWrapper<String>(new String[] {"①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"}),
				new ArrayWrapper<String>(new String[] {"➀", "➁", "➂", "➃", "➄", "➅", "➆", "➇", "➈"}),
				new ArrayWrapper<String>(new String[] {"➊", "➋", "➌", "➍", "➎", "➏", "➐", "➑", "➒"}),
				new ArrayWrapper<String>(new String[] {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈"}), 
				new ArrayWrapper<String>(new String[] {"₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"}), 
				new ArrayWrapper<String>(new String[] {"0, 0", "0, 1", "0, 2", "1, 0", "1, 1", "1, 2", "2, 0", "2, 1", "2, 2"}),
				new ArrayWrapper<String>(new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I"}),
				new ArrayWrapper<String>(new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i"}),
				new ArrayWrapper<String>(new String[] {"J", "K", "L", "M", "N", "O", "P", "Q", "R"}), 
				new ArrayWrapper<String>(new String[] {"j", "k", "l", "m", "n", "o", "p", "q", "r"}), 
				new ArrayWrapper<String>(new String[] {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"})}, null);
			
			if (result != null)
			{
				for (int i = 0; i < result.elements.length; i++)
				{
					box_indexes[i].setText(result.elements[i]);
				}
			}
		});
		indexPanel.add(chooseBoxIndexFromDefaults, new GBC(10, 3).setAnchor(GBC.WEST));
		
		// index separators
		indexPanel.add(new JLabel("Row and Column Index Separator: "), new GBC(0, 4).setAnchor(GBC.EAST));
		indexPanel.add(rc_index_separator, new GBC(1, 4));

		indexPanel.add(new JLabel("Cell Index Separator: "), new GBC(0, 5).setAnchor(GBC.EAST));
		indexPanel.add(cellIndexSeparator, new GBC(1, 5));

		indexPanel.add(new JLabel("Last Cell Index Separator: "), new GBC(0, 6).setAnchor(GBC.EAST));
		indexPanel.add(lastCellIndexSeparator, new GBC(1, 6));
		
		// put things together
		indexPanel.setBorder(BorderFactory.createTitledBorder("Index Settings"));

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.add(forDisplay);
		contentPanel.add(forIO);
		contentPanel.add(indexPanel);

		JPanel resetAllPanel = new JPanel();
		JButton resetAll = new JButton("Reset All to Default");
		resetAll.addActionListener(event ->
		{
			initializing = true;
			for (PrefsComponent c : prefsComponents)
			{
				c.resetToDefault();
			}
			initializing = false;
			applyChanges();
		});
		resetAllPanel.add(resetAll);

		setLayout(new BorderLayout());
		add(new JScrollPane(contentPanel), BorderLayout.NORTH);
		add(resetAllPanel, BorderLayout.SOUTH);
		
		loadSettings(settingsFile);
		initializing = false;
	}
	
	public void saveSettings(SingleSettingsFile file, boolean saveToFile)
	{
		Preferences node = file.node;
		Application.prefsLogger.entering("DigitsAndIndexesPanel", "saveSettings", node);

		for (PrefsComponent c : prefsComponents)
		{
			c.saveSettings(node);
		}
		
		if (saveToFile) file.save();
	}
	
	public void loadSettings(SingleSettingsFile file)
	{
		Preferences node = file.node;
		Application.prefsLogger.entering("DigitsAndIndexesPanel", "loadSettings", node);
		for (PrefsComponent c : prefsComponents)
		{
			c.loadSettings(node);
		}
	}
	
	public void applyChanges()
	{
		if (!initializing)
		{
			for (ApplicationFrame f : Application.openWindows)
			{
				for (int t = 0; t < f.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) f.tabbedPane.getComponentAt(t)).board.repaint();
				}
			}
		}
	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}