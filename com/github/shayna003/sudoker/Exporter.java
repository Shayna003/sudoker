package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.util.*;
import com.github.shayna003.sudoker.prefs.*;
import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.*;
import java.nio.charset.*;

/**
 * @since 5-15-2021
 */
@SuppressWarnings("CanBeFinal")
public class Exporter extends JDialog implements SettingsPanel
{	
	public void showDialog(Board selectedBoard)
	{
		this.selectedBoard = selectedBoard;
		prepareDataText();
		preparePrintSudokuPanel();
		setLocationRelativeTo(selectedBoard.boardOwner.owner);
		setVisible(true);
	}

	void prepareDataText()
    {
        if (exportSudoku.isSelected())
        {
            sudokuString = getSudokuDataString(selectedBoard).toString();
        }

        if (exportPencilMarks.isSelected())
        {
            pencilMarksString = getPencilMarksString(selectedBoard).toString();
        }

        if (exportNotes.isSelected())
        {
            notesString = getNotesString(selectedBoard).toString();
        }

        if (exportLocks.isSelected())
        {
            locksString = getLocksString(selectedBoard).toString();
        }

        if (exportViewOptions.isSelected())
        {
            viewOptionsString = getViewOptionsString(selectedBoard);
        }

        if (exportHighlightOptions.isSelected())
        {
            highlightOptionsString = getHighlighterString(selectedBoard);
        }

        if (exportStopwatch.isSelected())
        {
            stopwatchString = getStopwatchString(selectedBoard);
        }

        setDataText();
    }
	
	public Board selectedBoard;
	JTabbedPane tabbedPane; // contains two export modes
	
	JPanel printSudokuPanel; // "prints" the board in ascii style
	JButton copySudokuStringToClipboard;
	JTextArea sudokuConsole;
	
	PrefsCheckBox printBorders;
	PrefsCheckBox printAll;
	PrefsCheckBox centerSolved;
	PrefsCheckBox use3Lines;
	PrefsNumberSpinner lineBreaksInSmallRow;
	PrefsNumberSpinner indent;

    PrefsNumberSpinner linesBreaksBetweenRows;
    PrefsNumberSpinner lineBreaksBetweenBoxes;

    PrefsNumberSpinner spacesAfterEachDigit;
    PrefsNumberSpinner spacesAfterEachCell;
    PrefsNumberSpinner spacesAfterEachBox;

	JPanel dataExportPanel; // export data that can be loaded back in later
	JButton copySudokuDataStringToClipboard;
	JButton copyDataStringToClipboard;
	JTextArea dataString; // string representation of data, as opposed to binary format
	
	// displayed in the same order in textArea
	String sudokuString; // variable length
	String pencilMarksString; // variable length
	String notesString; // variable length
	String locksString; // fixed length
	String viewOptionsString; //fixed length
	String highlightOptionsString; //fixed length
	String stopwatchString; //variable length
	
	// whether or not to add the strings into textArea
	PrefsCheckBox exportViewOptions; // what to show in unsolved cells and show indexes
	PrefsCheckBox exportHighlightOptions; // mouseover and permanent highlight
	PrefsCheckBox exportStopwatch; // time of stopwatch

	PrefsCheckBox exportSudoku;
	PrefsCheckBox exportPencilMarks;
	PrefsCheckBox exportNotes;
	PrefsCheckBox exportLocks;

	PrefsCheckBox useCellSeparators;
	PrefsCheckBox newLineForEachRow;
	PrefsCheckBox printAllCandidatesForSudokuData;

	JFileChooser chooser;
	JButton exportToFile; // in text format, UTF-8;
	JButton closeButton;
	JButton closePrintSudokuButton;

	public SingleSettingsFile settingsFile;
	ArrayList<PrefsComponent> prefsComponents;
	
	JFileChooser getFileChooser()
	{
		if (chooser == null)
		{
			if (!Application.exportsFolder.exists()) Application.exportsFolder.mkdirs();
			chooser = new JFileChooser(Application.exportsFolder);
			GeneralSettingsPanel.registerComponentAndSetFontSize(chooser);
		}
		return chooser;
	}

	void initPrintSudokuPanel()
	{
		sudokuConsole = new JTextArea(20, 20);
		Font f = sudokuConsole.getFont();
        sudokuConsole.setFont(new Font(Font.MONOSPACED, f.getStyle(), f.getSize()));
		sudokuConsole.setEditable(false);

		copySudokuStringToClipboard = new JButton("Copy");
		copySudokuStringToClipboard.setMnemonic('C');
        copySudokuStringToClipboard.setToolTipText("Copy to System Clipboard");
        copySudokuStringToClipboard.addActionListener(event ->
		{
			StringSelection selection = new StringSelection(sudokuConsole.getText());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		});

		printBorders = new PrefsCheckBox("printBorders", "Print Borders", true);
		printAll = new PrefsCheckBox("printSudokuPrintAllCandidates", "Print All Candidates", true);
		centerSolved = new PrefsCheckBox("centerSolvedCandidates", "Center Solved Candidates", true);
		use3Lines = new PrefsCheckBox("use3Lines", "Use 3 lines to print a single cell", true);

		prefsComponents.add(printBorders);
		prefsComponents.add(printAll);
		prefsComponents.add(centerSolved);
		prefsComponents.add(use3Lines);

		printBorders.addActionListener(event ->
        {
            if (printBorders.isSelected())
            {
                if ((Integer) spacesAfterEachCell.getValue() == 0) spacesAfterEachCell.setValue(1);
                if ((Integer) spacesAfterEachBox.getValue() == 0) spacesAfterEachBox.setValue(1);
            }
            updatePrintSudokuString();
        });
        printAll.addActionListener(event ->
        {
            centerSolved.setEnabled(printAll.isSelected());
            use3Lines.setEnabled(printAll.isSelected());
            lineBreaksInSmallRow.setEnabled(printAll.isSelected() && use3Lines.isSelected());
            spacesAfterEachDigit.setEnabled(printAll.isSelected());
            updatePrintSudokuString();
        });
        centerSolved.addActionListener(event -> updatePrintSudokuString());
        use3Lines.addActionListener(event ->
        {
            lineBreaksInSmallRow.setEnabled(use3Lines.isSelected());
            updatePrintSudokuString();
        });

		int columns = 2;
		lineBreaksInSmallRow = new PrefsNumberSpinner("lineBreaksInSmallRow", 0, 100, 1, 0, event ->
		{
            updatePrintSudokuString();
		}, columns);
		prefsComponents.add(lineBreaksInSmallRow);

		indent = new PrefsNumberSpinner("printSudokuIndent", 0, 100, 1, 0, event ->
		{
            updatePrintSudokuString();
		}, columns);
		prefsComponents.add(indent);
		
        linesBreaksBetweenRows = new PrefsNumberSpinner("lineBreaksBetweenRows", 0, 10, 1, 0, event ->
        {
            updatePrintSudokuString();
        }, columns);
		prefsComponents.add(linesBreaksBetweenRows);

        lineBreaksBetweenBoxes = new PrefsNumberSpinner("lineBreaksBetweenBoxes", 0, 10, 1, 0, event ->
        {
            updatePrintSudokuString();
        }, columns);
		prefsComponents.add(lineBreaksBetweenBoxes);

        spacesAfterEachDigit = new PrefsNumberSpinner("spacesAfterEachDigit", 0, 10, 1, 0, event ->
        {
            updatePrintSudokuString();
        }, columns);
		prefsComponents.add(spacesAfterEachDigit);

        spacesAfterEachCell = new PrefsNumberSpinner("spacesAfterEachCell", 0, 10, 1, 0, event ->
        {
            updatePrintSudokuString();
        }, columns);
		prefsComponents.add(spacesAfterEachCell);

        spacesAfterEachBox = new PrefsNumberSpinner("spacesAfterEachBox", 0, 10, 1, 0, event ->
        {
            updatePrintSudokuString();
        }, columns);
		prefsComponents.add(spacesAfterEachBox);

        JPanel checkBoxPanel = new JPanel(new GridBagLayout());
		checkBoxPanel.add(printBorders, new GBC(0, 0).setAnchor(GBC.WEST));
        checkBoxPanel.add(printAll, new GBC(1, 0).setAnchor(GBC.WEST));
        checkBoxPanel.add(centerSolved, new GBC(2, 0).setAnchor(GBC.WEST));
        checkBoxPanel.add(use3Lines, new GBC(3, 0).setAnchor(GBC.WEST));

        int insets = 5;
        JPanel spinnerPanel = new JPanel(new GridBagLayout());
        spinnerPanel.add(new JLabel("Spaces after each digit: "), new GBC(0, 0).setAnchor(GBC.WEST));
        spinnerPanel.add(spacesAfterEachDigit, new GBC(1, 0).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));
        spinnerPanel.add(new JLabel("Spaces after each cell: "), new GBC(2, 0).setAnchor(GBC.WEST));
        spinnerPanel.add(spacesAfterEachCell, new GBC(3, 0).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));
        spinnerPanel.add(new JLabel("Spaces after each box: "), new GBC(4, 0).setAnchor(GBC.WEST));
        spinnerPanel.add(spacesAfterEachBox, new GBC(5, 0).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));

        spinnerPanel.add(new JLabel("Lines after each small row: "), new GBC(0, 1).setAnchor(GBC.WEST));
        spinnerPanel.add(lineBreaksInSmallRow, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));
        spinnerPanel.add(new JLabel("Lines after each row: "), new GBC(2, 1).setAnchor(GBC.WEST));
        spinnerPanel.add(linesBreaksBetweenRows, new GBC(3, 1).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));
        spinnerPanel.add(new JLabel("Lines after each box: "), new GBC(4, 1).setAnchor(GBC.WEST));
        spinnerPanel.add(lineBreaksBetweenBoxes, new GBC(5, 1).setAnchor(GBC.WEST).setInsets(0, 0, 0, insets));

        spinnerPanel.add(new JLabel("Row indents: "), new GBC(0, 2).setAnchor(GBC.WEST));
        spinnerPanel.add(indent, new GBC(1, 2).setAnchor(GBC.WEST));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(checkBoxPanel);
        northPanel.add(spinnerPanel);

        printSudokuPanel = new JPanel(new BorderLayout());
        printSudokuPanel.add(northPanel, BorderLayout.NORTH);
		printSudokuPanel.add(new JScrollPane(sudokuConsole), BorderLayout.CENTER);

		closePrintSudokuButton = new JButton("Close");
		closePrintSudokuButton.addActionListener(event ->
		{
			Exporter.this.setVisible(false);
		});
		JPanel buttonPanel  = new JPanel();
        buttonPanel.add(copySudokuStringToClipboard);
        buttonPanel.add(closePrintSudokuButton);
		printSudokuPanel.add(buttonPanel, BorderLayout.SOUTH);
	}

	void preparePrintSudokuPanel()
    {
        centerSolved.setEnabled(printAll.isSelected());
        use3Lines.setEnabled(printAll.isSelected());
        lineBreaksInSmallRow.setEnabled(printAll.isSelected() && use3Lines.isSelected());
        spacesAfterEachDigit.setEnabled(printAll.isSelected());
        updatePrintSudokuString();
    }

	void updatePrintSudokuString()
    {
    	if (selectedBoard == null) return;
        int split = use3Lines.isSelected() ? 1 : 0;
        if (split > 0) split += (Integer) lineBreaksInSmallRow.getValue();
        sudokuConsole.setText(IO.getString(selectedBoard.sudoku, (Integer) indent.getValue(), System.lineSeparator(), " ", true, printBorders.isSelected(), printAll.isSelected(), centerSolved.isSelected(), split, (Integer) lineBreaksBetweenBoxes.getValue(), (Integer) linesBreaksBetweenRows.getValue(), (Integer) spacesAfterEachBox.getValue(), (Integer) spacesAfterEachCell.getValue(), (Integer) spacesAfterEachDigit.getValue()).toString());
    }

	void initDataPanel()
	{
		copySudokuDataStringToClipboard = new JButton("Copy Sudoku");
		copySudokuDataStringToClipboard.setMnemonic('C');
		copySudokuDataStringToClipboard.setToolTipText("Copy Sudoku Text to System Clipboard");
		copySudokuDataStringToClipboard.addActionListener(event ->
		{
			StringSelection selection = new StringSelection(sudokuString);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		});
		
		copyDataStringToClipboard = new JButton("Copy All");
		copyDataStringToClipboard.setMnemonic('A');
		copyDataStringToClipboard.setToolTipText("Copy All to System Clipboard");
		copyDataStringToClipboard.addActionListener(event ->
		{
			StringSelection selection = new StringSelection(dataString.getText());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		});
		
		dataString = new JTextArea(20, 20);
		dataString.setEditable(false);
		
		exportSudoku = new PrefsCheckBox("exportSudoku", "Export Sudoku", true);
		exportSudoku.addActionListener(event ->
		{
			useCellSeparators.setEnabled(exportSudoku.isSelected());
			newLineForEachRow.setEnabled(exportSudoku.isSelected());
			printAllCandidatesForSudokuData.setEnabled(exportSudoku.isSelected());
			copySudokuDataStringToClipboard.setEnabled(exportSudoku.isSelected());
			updateSudokuDataText();
		});
		prefsComponents.add(exportSudoku);
		
		exportPencilMarks = new PrefsCheckBox("exportPencilMarks", "Export Pencil Marks", false);
		exportPencilMarks.addActionListener(event ->
		{
			if (exportPencilMarks.isSelected())
			{
				pencilMarksString = getPencilMarksString(selectedBoard).toString();
			}
			setDataText();
		});
		prefsComponents.add(exportPencilMarks);

		
		exportNotes = new PrefsCheckBox("exportNotes", "Export Notes", false);
		exportNotes.addActionListener(event ->
		{
			if (exportNotes.isSelected())
			{
				notesString = getNotesString(selectedBoard).toString();
			}
			setDataText();
		});
		prefsComponents.add(exportNotes);
		
		exportLocks = new PrefsCheckBox("exportLocks", "Export Locks", false);
		exportLocks.addActionListener(event ->
		{
			if (exportLocks.isSelected())
			{
				locksString = getLocksString(selectedBoard).toString();
			}
			setDataText();
		});
		prefsComponents.add(exportLocks);
		
		exportViewOptions = new PrefsCheckBox("exportViewOptions", "Export View Options", false);
		exportViewOptions.addActionListener(event ->
		{
			if (exportViewOptions.isSelected())
			{
				viewOptionsString = getViewOptionsString(selectedBoard);
			}
			setDataText();
		});
		prefsComponents.add(exportViewOptions);
		
		exportHighlightOptions = new PrefsCheckBox("exportHighlightOptions", "Export Highlight Options", false);
		exportHighlightOptions.addActionListener(event ->
		{
			if (exportHighlightOptions.isSelected())
			{
				highlightOptionsString = getHighlighterString(selectedBoard);
			}
			setDataText();
		});
		prefsComponents.add(exportHighlightOptions);
		
		exportStopwatch = new PrefsCheckBox("exportStopwatch", "Export Stopwatch Time", false);
		exportStopwatch.addActionListener(event ->
		{
			if (exportStopwatch.isSelected())
			{
				stopwatchString = getStopwatchString(selectedBoard);
			}
			setDataText();
		});
		prefsComponents.add(exportStopwatch);
		
		useCellSeparators = new PrefsCheckBox("useCellSeparators", "Print Cell Separators", false);
		useCellSeparators.addActionListener(event ->
		{
			updateSudokuDataText();
		});
		prefsComponents.add(useCellSeparators);
		
		newLineForEachRow = new PrefsCheckBox("newLineForEachRow", "Print New Line for Each Row", true);
		newLineForEachRow.addActionListener(event ->
		{
			updateSudokuDataText();
		});
		prefsComponents.add(newLineForEachRow);
		
		printAllCandidatesForSudokuData = new PrefsCheckBox("dataPrintAllCandidates", "Print All Candidates", true);
		printAllCandidatesForSudokuData.addActionListener(event ->
		{
			updateSudokuDataText();
		});
		printAllCandidatesForSudokuData.setToolTipText("Print 81 Candidates if not selected");
		prefsComponents.add(printAllCandidatesForSudokuData);
		
		// sort of awkward as there is the visible "glitch" as modal file choosers and dialogs show up and hide the Exporter
		exportToFile = new JButton("Export All to File...");
		exportToFile.setMnemonic('F');
		exportToFile.addActionListener(event ->
		{
			getFileChooser().setSelectedFile(new File(getFileChooser().getCurrentDirectory(), selectedBoard.boardOwner.getName()));
			int result = chooser.showSaveDialog(selectedBoard.boardOwner.owner);
			if (result == JFileChooser.APPROVE_OPTION)
			{
				File targetFile = chooser.getSelectedFile();
				exportToFile(targetFile, dataString.getText(), selectedBoard.boardOwner.owner, true);
			}
		});
		
		JPanel checkBoxesPanel = new JPanel(new GridBagLayout());
		checkBoxesPanel.add(new JLabel("Board Data:"), new GBC(0, 0).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportSudoku, new GBC(1, 0).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportPencilMarks, new GBC(2, 0).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportNotes, new GBC(3, 0).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportLocks, new GBC(4, 0).setAnchor(GBC.WEST));
		
		checkBoxesPanel.add(new JLabel("Tab Data:"), new GBC(0, 1).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportViewOptions, new GBC(1, 1).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportHighlightOptions, new GBC(2, 1).setAnchor(GBC.WEST));
		checkBoxesPanel.add(exportStopwatch, new GBC(3, 1).setAnchor(GBC.WEST));
		
		checkBoxesPanel.add(new JLabel("Sudoku Options: "), new GBC(0, 2).setAnchor(GBC.WEST));
		checkBoxesPanel.add(useCellSeparators, new GBC(1, 2).setAnchor(GBC.WEST));
		checkBoxesPanel.add(newLineForEachRow, new GBC(2, 2).setAnchor(GBC.WEST));
		checkBoxesPanel.add(printAllCandidatesForSudokuData, new GBC(3, 2).setAnchor(GBC.WEST));

		closeButton = new JButton("Close");
		closeButton.addActionListener(event ->
		{
			Exporter.this.setVisible(false);
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(copySudokuDataStringToClipboard);
		buttonPanel.add(copyDataStringToClipboard);
		buttonPanel.add(exportToFile);
		buttonPanel.add(closeButton);
		
		dataExportPanel = new JPanel(new BorderLayout());
		dataExportPanel.add(checkBoxesPanel, BorderLayout.NORTH);
		dataExportPanel.add(new JScrollPane(dataString), BorderLayout.CENTER);
		dataExportPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		GeneralSettingsPanel.registerComponentAndSetFontSize(Exporter.this);
	}
	
	void updateSudokuDataText()
	{
		if (exportSudoku.isSelected())
		{
			sudokuString = getSudokuDataString(selectedBoard).toString();
		}
		setDataText();
	}

	/*
	 * Used by session restore and saving tab data when closing
	 */
	public static String getFullDataString(Board board)
	{
		StringBuilder builder = new StringBuilder();

		// header values to record what's in the file/string
		builder.append("1001"); // export sudoku, don't use cell separators, no new line for each row, print all candidates.
		builder.append("111111"); // export pencil marks, notes, locks, view options, highlight options, stopwatch time
		builder.append(System.lineSeparator());

		builder.append("Sudoku:");
		builder.append(System.lineSeparator());
		builder.append(IO.getCompactAllCandidatesString(board.sudoku));
		builder.append(System.lineSeparator());

		builder.append("Pencil Marks:");
		builder.append(System.lineSeparator());
		builder.append(getPencilMarksString(board));
		builder.append(System.lineSeparator());

		builder.append("Notes:");
		builder.append(System.lineSeparator());
		builder.append(getNotesString(board));
		builder.append(System.lineSeparator());

		builder.append("Locks:");
		builder.append(System.lineSeparator());
		builder.append(getLocksString(board));
		builder.append(System.lineSeparator());

		builder.append("View Options:");
		builder.append(System.lineSeparator());
		builder.append(getViewOptionsString(board));
		builder.append(System.lineSeparator());

		builder.append("Highlight Options:");
		builder.append(System.lineSeparator());
		builder.append(getHighlighterString(board));
		builder.append(System.lineSeparator());

		builder.append("Stopwatch Time:");
		builder.append(System.lineSeparator());
		builder.append(getStopwatchString(board));

		return builder.toString();
	}

	void setDataText()
	{
		StringBuilder builder = new StringBuilder();
		
		// header values to record what's in the file/string
		builder.append(exportSudoku.isSelected() ? "1" : "0");
		builder.append(useCellSeparators.isSelected() ? "1" : "0");
		builder.append(newLineForEachRow.isSelected() ? "1" : "0");
		builder.append(printAllCandidatesForSudokuData.isSelected() ? "1" : "0");
		
		builder.append(exportPencilMarks.isSelected() ? "1" : "0");
		builder.append(exportNotes.isSelected() ? "1" : "0");
		builder.append(exportLocks.isSelected() ? "1" : "0");
		
		builder.append(exportViewOptions.isSelected() ? "1" : "0");
		builder.append(exportHighlightOptions.isSelected() ? "1" : "0");
		builder.append(exportStopwatch.isSelected() ? "1" : "0");
		builder.append(System.lineSeparator());
		
		if (exportSudoku.isSelected())
		{
			builder.append("Sudoku:");
			builder.append(System.lineSeparator());
			builder.append(sudokuString);
			builder.append(System.lineSeparator());
		}

		if (exportPencilMarks.isSelected())
		{
			builder.append("Pencil Marks:");
			builder.append(System.lineSeparator());
			builder.append(pencilMarksString);
			builder.append(System.lineSeparator());
		}
		
		if (exportNotes.isSelected())
		{
			builder.append("Notes:");
			builder.append(System.lineSeparator());
			builder.append(notesString);
			builder.append(System.lineSeparator());
		}
		
		if (exportLocks.isSelected())
		{
			builder.append("Locks:");
			builder.append(System.lineSeparator());
			builder.append(locksString);
			builder.append(System.lineSeparator());
		}
		
		if (exportViewOptions.isSelected())
		{
			builder.append("View Options:");
			builder.append(System.lineSeparator());
			builder.append(viewOptionsString);
			builder.append(System.lineSeparator());
		}
		
		if (exportHighlightOptions.isSelected())
		{
			builder.append("Highlight Options:");
			builder.append(System.lineSeparator());
			builder.append(highlightOptionsString);
			builder.append(System.lineSeparator());
		}
		
		if (exportStopwatch.isSelected())
		{
			builder.append("Stopwatch Time:");
			builder.append(System.lineSeparator());
			builder.append(stopwatchString);
		}
		
		dataString.setText(builder.toString());
	}
	
	public Exporter()
	{
		setModal(true);
		setTitle("Export Options");

		prefsComponents = new ArrayList<>();
		initDataPanel();
		initPrintSudokuPanel();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Export Data", null, dataExportPanel, "Save Sudoku/Board/Tab data that can be read back or inputted into other applications");
		tabbedPane.addTab("Print Sudoku", null, printSudokuPanel, "Get a textual representation of a Sudoku's Candidates");
		add(tabbedPane, BorderLayout.CENTER);
		
		pack();
		setLocationByPlatform(true);

		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "export_settings.xml"));
		loadSettings(settingsFile);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveSettings(settingsFile, true);
			}
		});
	}
	
	public StringBuilder getSudokuDataString(Board board)
	{
		return IO.getString(board.sudoku, 0, newLineForEachRow.isSelected() ? System.lineSeparator() : "", useCellSeparators.isSelected() ? Application.digitsAndIndexesPanel.getCellSeparator() : "", false, false, printAllCandidatesForSudokuData.isSelected(), false, 0, 0, 0, useCellSeparators.isSelected() ? 1 : 0, useCellSeparators.isSelected() ? 1 : 0, 0);
	}
	
	public static StringBuilder getLocksString(Board board)
	{
		StringBuilder b = new StringBuilder();
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				b.append(board.cellLocked[r][c] ? "1" : "0"); // 1 for locked, 0 for not locked
			}
		}
		return b;
	}
	
	public static StringBuilder getPencilMarksString(Board board)
	{
		StringBuilder b = new StringBuilder();
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				b.append(board.pencilMarks[r][c].length());
				b.append("|");
				b.append(board.pencilMarks[r][c]);
			}
		}
		return b;
	}
	
	public static StringBuilder getNotesString(Board board)
	{
		StringBuilder b = new StringBuilder();
		for (int r = 0; r < 9; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				b.append(board.notes[r][c].length());
				b.append("|");
				b.append(board.notes[r][c]);
			}
		}
		return b;
	}

	public static boolean exportToFile(File targetFile, String data, ApplicationFrame parent, boolean shouldShowMessages)
	{
		String fileName = targetFile.getName();

		if (!SettingsFile.isValidFileName(fileName))
		{
			if (shouldShowMessages) JOptionPane.showMessageDialog(parent, "Please supply a valid file name! Try avoid using special characters.", "File Name is Invalid", JOptionPane.WARNING_MESSAGE);
			return false;
		}

		String extension = PreferenceFrame.getFileExtension(targetFile);
		String savedFileName = extension != null && extension.equalsIgnoreCase("dat") ? fileName : fileName + ".dat";

		File[] otherFiles = targetFile.getParentFile().listFiles();
		if (otherFiles != null)
		{
			for (File otherFile : otherFiles)
			{
				if (otherFile.getName().equals(savedFileName))
				{
					if (shouldShowMessages) JOptionPane.showMessageDialog(parent, "There is already a file with the name \"" + savedFileName + "\". Please supply a different name.", "File Exists", JOptionPane.INFORMATION_MESSAGE);
					return false;
				}
			}
		}

		try (PrintWriter writer = new PrintWriter(targetFile.getParent() + File.separator + savedFileName, StandardCharsets.UTF_8))
		{
			writer.write(data);

			if (shouldShowMessages && PreferenceDialogs.shouldShowMessage("exportToDataFileSuccess"))
			{
				JOptionPane.showMessageDialog(parent, PreferenceDialogs.getDirectoryMessage("exportToDataFileSuccess", "The data for has been exported to the file \"" + savedFileName + "\" in the directory", targetFile.getParentFile()), "Save Success", JOptionPane.INFORMATION_MESSAGE);
			}
			return true;
		}
		catch (IOException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, "Exporter", "exportToFile", "Error when exporting to file " + targetFile, e);
			if (shouldShowMessages) JOptionPane.showMessageDialog(parent, "An error occurred when exporting to the file \"" + targetFile.getName() + "\".", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public static String getViewOptionsString(Board board)
	{
		StringBuilder b = new StringBuilder();
		b.append(board.viewOptions.viewButtonGroup.selectedButton);
		b.append(board.viewOptions.showRowIndexes.isSelected() ? "1" : "0");
		b.append(board.viewOptions.showColIndexes.isSelected() ? "1" : "0");
		b.append(board.viewOptions.showBoxIndexes.isSelected() ? "1" : "0");
		return b.toString();
	}
	
	public static String getHighlighterString(Board board)
	{
		StringBuilder b = new StringBuilder();
		b.append(board.cellHighlighter.radioButtonGroup.selectedButton);
		for (int i = 0; i < 9; i++)
		{
			b.append(board.cellHighlighter.checkBoxes[i].isSelected() ? "1" : "0");
		}
		return b.toString();
	}
	
	public static String getStopwatchString(Board board)
	{
		StringBuilder b = new StringBuilder();
		b.append(String.format("%02d", board.boardOwner.stopwatch.hours));
		b.append(":");
		b.append(String.format("%02d", board.boardOwner.stopwatch.minutes));
		b.append(":");
		b.append(String.format("%02d", board.boardOwner.stopwatch.seconds));
		return b.toString();
	}

	@Override
	public void applyChanges()
	{
	}

	@Override
	public void saveSettings(SingleSettingsFile file, boolean saveToFile)
	{
		for (PrefsComponent c : prefsComponents)
		{
			c.saveSettings(file.node);
		}
		if (saveToFile) file.save();
	}

	@Override
	public void loadSettings(SingleSettingsFile file)
	{
		for (PrefsComponent c : prefsComponents)
		{
			c.loadSettings(file.node);
		}
	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}