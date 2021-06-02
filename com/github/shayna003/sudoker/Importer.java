package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.history.Edit;
import com.github.shayna003.sudoker.history.EditType;
import com.github.shayna003.sudoker.prefs.components.PrefsButtonGroup;
import com.github.shayna003.sudoker.prefs.components.PrefsCheckBox;
import com.github.shayna003.sudoker.prefs.components.PrefsComponent;
import com.github.shayna003.sudoker.util.*;
import com.github.shayna003.sudoker.prefs.*;
import com.github.shayna003.sudoker.swingComponents.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * @since 5-16-2021
 */
public class Importer implements SettingsPanel
{
	public InputDialog inputDialog;
	JFileChooser chooser;
	public SingleSettingsFile settingsFile;
	ArrayList<PrefsComponent> prefsComponents;
	
	void initChooser()
	{
		if (!Application.exportsFolder.exists()) Application.exportsFolder.mkdirs();
		chooser = new JFileChooser(Application.exportsFolder);

		SudokuFileFilter sudokuFileFilter = new SudokuFileFilter();
		FileIconView sudokuIconView = new FileIconView(sudokuFileFilter, SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/sudoku-file-icon.png")));
		chooser.setFileView(sudokuIconView);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(sudokuFileFilter);
		chooser.setDragEnabled(true);
		
		JScrollPane accessoryPane = new JScrollPane(new SudokuFilePreview(chooser));
		accessoryPane.setPreferredSize(new Dimension(300, 300));
		chooser.setAccessory(accessoryPane);
		
		GeneralSettingsPanel.registerComponentAndSetFontSize(chooser);
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

	@SuppressWarnings("CanBeFinal")
	static class SudokuFilePreview extends JComponent
	{
		boolean fileSelected = false;
		Sudoku selectedSudoku;
		String oneLine;
		String sudokuString;
		int text_size = 12;
		String message = "Invalid/No Sudoku";
		
		@Override
		public Dimension getPreferredSize()
		{
			Rectangle2D bounds = getFont().getStringBounds(sudokuString.length() == 0 ? message : oneLine, getFontMetrics(getFont()).getFontRenderContext());
			//somehow should use -bounds.getY() (the ascent) instead of using bounds.getHeight() for the scroll pane to fit tightly
			return new Dimension((int) (bounds.getWidth() + 2 * text_size), (int) (-bounds.getY() * (sudokuString.length() == 0 ? 1 : 19) + 2 * text_size));
		}
		
		public SudokuFilePreview(JFileChooser chooser)
		{
			setFont(new Font(Font.MONOSPACED, Font.PLAIN, text_size));
			//Max for height is 253 for Mac look and feel
			//too wide makes the file chooser sort of malfunction
			
			sudokuString = "";
			oneLine = "";
			selectedSudoku = null;
			
			chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, event ->
			{
				File f = (File) event.getNewValue();
				
				if (f == null || (f.isDirectory() && chooser.isTraversable(f)))
				{
					fileSelected = false;
					selectedSudoku = null;
					sudokuString = "";
					repaint();
					return;
				}
				
				if (chooser.getSelectedFile() == f && chooser.getFileFilter().accept(f))
				{
					selectedSudoku = IO.readSudokuFromFile(f);
					fileSelected = true;
					if (selectedSudoku == null) // shouldn't happen for valid inputs
					{
						sudokuString = ""; 
						repaint();
						return;
					}
					sudokuString = IO.getDefaultString(selectedSudoku, 0, true, false);
					repaint();
				}
			});
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			if (fileSelected)
			{
				Graphics2D g2 = (Graphics2D) g;
				
				if (sudokuString.length() == 0) // won't display this now
				{
					g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
					revalidate();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, g2.getFontRenderContext());
					g2.drawString(message, (int) ((getWidth() - bounds.getWidth()) / 2), (int) (-bounds.getY() + (getHeight() - bounds.getHeight()) / 2));
				}
				else 
				{
					String[] lines = sudokuString.split(System.lineSeparator());
					
					for (int l = 0; l < lines.length; l++)
					{
						g2.drawString(lines[l], text_size, l * text_size + text_size);
					}
				}
			}
		}
	}
	
	class SudokuFileFilter extends FileFilter
	{
		@Override
		public String getDescription()
		{
			return "Application data files with extension .dat";
		}
		
		@Override
		public boolean accept(File f)
		{
			if (f.isDirectory() && !chooser.isTraversable(f)) return false;
			if (f.isDirectory()) return true;
			String extension = PreferenceFrame.getFileExtension(f);
			return extension != null && extension.equalsIgnoreCase("dat");
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	static class FileIconView extends FileView
	{
		FileFilter filter;
		Icon icon;
		
		/**
		* @param filter all files that this filter accepts will be shown with {@code icon }.
		*/
		public FileIconView(FileFilter filter, Icon icon)
		{
			this.filter = filter;
			this.icon = icon;
		}
		
		@Override
		public Icon getIcon(File f)
		{
			if (f.isFile() && IO.readSudokuFromFile(f) != null) return icon;
			else return null;
		}
	}

	public SudokuTab newBoardFromFile(ApplicationFrame parent, int creationType, boolean createNewWindow)
	{
		if (chooser == null) initChooser();
		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			BoardData data = IO.readBoardFromFile(chooser.getSelectedFile());
			if (createNewWindow) return Application.createNewWindowWithTab(parent, data, creationType);
			else return Application.addTab(parent, data, creationType);
		}
		else return null;
	}

	public void importFromFile(Board selectedBoard, ApplicationFrame parent)
	{
		if (chooser == null) initChooser();
		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			selectedBoard.cellEditor.endEdit();
			BoardData data = IO.readBoardFromFile(chooser.getSelectedFile());
			String changedParts = selectedBoard.setBoardData(data);
			if (changedParts.length() > 0)
			{
				selectedBoard.boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Imported Board Data from File, Changed: " + changedParts, EditType.IMPORT, selectedBoard));
				if (Application.miscellaneousSettingsPanel.restartTimerUponImport.isSelected()) selectedBoard.boardOwner.stopwatch.restart();
			}
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	class InputDialog extends JDialog
	{
		JTextArea textArea;
		JPanel contentPanel;

		JButton cancel;
		JButton ok;
		boolean okClicked = false;

		PrefsButtonGroup buttonGroup;
		JRadioButton importSudoku; // only enter the sudoku String
		JRadioButton importData; // enter Exporter's String as-is
		
		// sudoku options
		PrefsCheckBox useCellSeparators;
		PrefsCheckBox newLineForEachRow;
		PrefsCheckBox printAllCandidatesForSudokuData;

		JButton copy;
		JButton pasteFromClipboard;
		
		public InputDialog()
		{
			setModal(true);
			prefsComponents = new ArrayList<>();

			textArea = new JTextArea(10, 15);
			
			importSudoku = new JRadioButton("Import Sudoku From Sudoku Text");
			importSudoku.setToolTipText("Enter only the Sudoku Text obtained from Exporter");
			importSudoku.setSelected(true);
			importSudoku.addChangeListener(event ->
			{
				useCellSeparators.setEnabled(importSudoku.isSelected());
				newLineForEachRow.setEnabled(importSudoku.isSelected());
				printAllCandidatesForSudokuData.setEnabled(importSudoku.isSelected());
			});
			
			importData = new JRadioButton("Import Data From Data Text");
			importData.setToolTipText("Enter the String obtained from Exporter exactly as it is");
			
			buttonGroup = new PrefsButtonGroup(null, "importMode", 0, importSudoku, importData);
			prefsComponents.add(buttonGroup);
			
			useCellSeparators = new PrefsCheckBox("importUseCellSeparators", "Uses Cell Separators", false);
			newLineForEachRow = new PrefsCheckBox("importUseNewLineForEachRow", "Uses New Line for Each Row", true);
			printAllCandidatesForSudokuData = new PrefsCheckBox("importHasAllCandidates", "Has All Candidates", true);

			prefsComponents.add(useCellSeparators);
			prefsComponents.add(newLineForEachRow);
			prefsComponents.add(printAllCandidatesForSudokuData);

			copy = new JButton("Copy");
			copy.setMnemonic('C');
			copy.setToolTipText("Copy to System Clipboard");
			copy.addActionListener(event ->
			{
				StringSelection selection = new StringSelection(textArea.getText());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			});

			pasteFromClipboard = new JButton("Paste");
			pasteFromClipboard.setMnemonic('P');
			pasteFromClipboard.setToolTipText("Paste from System Clipboard");
			pasteFromClipboard.addActionListener(event ->
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				try
				{
					textArea.setText(clipboard.getData(DataFlavor.stringFlavor).toString());
				}
				catch (UnsupportedFlavorException | IOException e)
				{
					Application.exceptionLogger.log(Level.WARNING, "Error when pasting from clipboard.");
				}
			});

			JPanel checkBoxPanel = new JPanel(new GridBagLayout());
			checkBoxPanel.add(importData, new GBC(0, 0, 2, 1).setAnchor(GBC.WEST));
			checkBoxPanel.add(importSudoku, new GBC(0, 1, 2, 1).setAnchor(GBC.WEST));
			
			checkBoxPanel.add(new JLabel("Sudoku Options: "), new GBC(0, 2).setAnchor(GBC.WEST));
			checkBoxPanel.add(useCellSeparators, new GBC(1, 2).setAnchor(GBC.WEST));
			checkBoxPanel.add(newLineForEachRow, new GBC(2, 2).setAnchor(GBC.WEST));
			checkBoxPanel.add(printAllCandidatesForSudokuData, new GBC(3, 2).setAnchor(GBC.WEST));

			cancel = new JButton("Cancel");
			cancel.setMnemonic('C');
			cancel.addActionListener(event -> InputDialog.this.setVisible(false));

			ok = new JButton("Ok");
			ok.setMnemonic('O');
			ok.addActionListener(event ->
			{
				okClicked = true;
				InputDialog.this.setVisible(false);
			});

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			buttonPanel.add(copy);
			buttonPanel.add(pasteFromClipboard);
			buttonPanel.add(cancel);
			buttonPanel.add(ok);

			contentPanel = new JPanel(new BorderLayout());
			contentPanel.add(checkBoxPanel, BorderLayout.NORTH);
			contentPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
			contentPanel.add(buttonPanel, BorderLayout.SOUTH);

			int insets = 10;
			contentPanel.setBorder(BorderFactory.createEmptyBorder(insets, insets, insets, insets));

			setLayout(new BorderLayout());
			add(contentPanel, BorderLayout.CENTER);

			settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "import_settings.xml"));
			loadSettings(settingsFile);

			setTitle("Input From Text");
			pack();
		}
	}

	/**
	 * @return a SudokuTab, or null
	 */
	public SudokuTab newBoardFromString(ApplicationFrame parent, int creationType, boolean createNewWindow)
	{
		if (inputDialog == null) inputDialog = new InputDialog();
		inputDialog.okClicked = false;
		inputDialog.setLocationRelativeTo(parent);
		inputDialog.setVisible(true);
		if (inputDialog.okClicked)
		{
			saveSettings(settingsFile, true);
			if (inputDialog.importData.isSelected())
			{
				BoardData data = IO.readBoardFromString(inputDialog.textArea.getText());
				if (createNewWindow) return Application.createNewWindowWithTab(parent, data, creationType);
				else return Application.addTab(parent, data, creationType);
			}
			else
			{
				Sudoku s = IO.readSudokuFromString(inputDialog.textArea.getText().split(System.lineSeparator()), inputDialog.useCellSeparators.isSelected(), inputDialog.newLineForEachRow.isSelected(), inputDialog.printAllCandidatesForSudokuData.isSelected());
				if (s == null)
				{
					JOptionPane.showMessageDialog(parent, "The String does not read back into a valid sudoku puzzle.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
					return null;
				}
				else
				{
					if (createNewWindow) return Application.createNewWindowWithTab(parent, s, "New Window From Sudoku Text");
					else return Application.addTab(parent, s, "New Tab From Sudoku Text");
				}
			}
		}
		else
		{
			saveSettings(settingsFile, true);
			return null;
		}
	}

	/**
	 * 2 options: 
	 * 1 input sudoku text only, with checkboxes to show options
	 * 2 paste as-is the outputted text obtained from Exporter
	 */
	public void importFromString(Board selectedBoard, ApplicationFrame parent)
	{
		if (inputDialog == null) inputDialog = new InputDialog();
		inputDialog.okClicked = false;
		inputDialog.setLocationRelativeTo(parent);
		inputDialog.setVisible(true);
		if (inputDialog.okClicked)
		{
			saveSettings(settingsFile, true);
			if (inputDialog.importData.isSelected())
			{
				selectedBoard.cellEditor.endEdit();
				String changedParts = selectedBoard.setBoardData(IO.readBoardFromString(inputDialog.textArea.getText()));
				if (changedParts.length() > 0)
				{
					selectedBoard.boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Imported Board Data from Text, Changed: " + changedParts, EditType.IMPORT, selectedBoard));
				}
			}
			else 
			{
				Sudoku s = IO.readSudokuFromString(inputDialog.textArea.getText().split(System.lineSeparator()), inputDialog.useCellSeparators.isSelected(), inputDialog.newLineForEachRow.isSelected(), inputDialog.printAllCandidatesForSudokuData.isSelected());
				if (s == null)
				{
					JOptionPane.showMessageDialog(parent, "The String does not read back into a valid sudoku puzzle.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
				}
				else
				{
					selectedBoard.cellEditor.endEdit();
					selectedBoard.setSudoku(s);
					selectedBoard.boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Imported Sudoku from Text", EditType.IMPORT, selectedBoard));
					if (Application.miscellaneousSettingsPanel.restartTimerUponImport.isSelected()) selectedBoard.boardOwner.stopwatch.restart();
				}
			}
		}
		else
		{
			saveSettings(settingsFile, true);
		}
	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}