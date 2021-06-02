package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.components.*;

import java.awt.*;
import javax.swing.*;
import java.util.prefs.*;
import java.io.*;
import java.util.*;

/**
 * @since 3-30-2021
 * Started working on 5-2-2021
 * Preferences for new boards view options & highlight options,
 * And what to copy and paste for cell value dnd
 */
@SuppressWarnings("CanBeFinal")
public class BoardSettingsPanel extends JPanel implements SettingsPanel
{
	SingleSettingsFile settingsFile;
	
	PreferenceFrame preferenceFrame;
	ArrayList<PrefsComponent> prefsComponents = new ArrayList<>();
	
	JPanel viewOptionsPanel;
	public PrefsButtonGroup viewOptionsButtonGroup;
	public JRadioButton showAllCandidates;
	public JRadioButton showPencilMarks;
	public JRadioButton showBlank;
	public PrefsCheckBox showBoxIndexes;
	public PrefsCheckBox showRowIndexes;
	public PrefsCheckBox showColIndexes;
	
	JPanel highlightOptionsPanel;
	public JRadioButton none;
	public JRadioButton sameUnit;
	public JRadioButton sameBoxUnit;
	public JRadioButton any;
	public PrefsButtonGroup defaultMouseOverHighlight;
	public PrefsCheckBox highlightSolvedCell;
	
	JPanel copyOptionsPanel;
	public PrefsCheckBox copyCandidates;
	public PrefsCheckBox copyPencilMarks;
	public PrefsCheckBox copyNotes;
	public PrefsCheckBox copyLocks;
	
	JPanel boardComparatorSettingsPanel;
	public PrefsButtonGroup compareOptionsGroup;
	public JRadioButton compareCandidates;
	public JRadioButton comparePencilMarks;
	public JRadioButton compareNotes;
	public JRadioButton compareLocks;
	public PrefsCheckBox comparatorShowBoxIndexes;
	public PrefsCheckBox comparatorShowRowIndexes;
	public PrefsCheckBox comparatorShowColIndexes;
	public PrefsCheckBox defaultLinkEnabled;
	
	boolean initializing = true;
		
	public BoardSettingsPanel(PreferenceFrame preferenceFrame)
	{
		this.preferenceFrame = preferenceFrame;
		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "board_settings.xml"));
		
		initDefaultViewOptions();
		initHighlightOptions();
		initCopyOptions();
		initBoardComparatorOptions();
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(viewOptionsPanel);
		add(highlightOptionsPanel);
		add(copyOptionsPanel);
		add(boardComparatorSettingsPanel);
		
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
		});
		resetAllPanel.add(resetAll);
		add(resetAllPanel, BorderLayout.SOUTH);
		
		loadSettings(settingsFile);
		initializing = false;
	}
	
	void initHighlightOptions()
	{
		int insets = 5;
		highlightOptionsPanel = new JPanel(new GridBagLayout());
		highlightOptionsPanel.setBorder(BorderFactory.createTitledBorder("Highlight Options"));
		
		none = new JRadioButton("None");
		sameUnit = new JRadioButton("Same Unit");
		sameBoxUnit = new JRadioButton("Same Larger Unit");
		any = new JRadioButton("Any");
		defaultMouseOverHighlight = new PrefsButtonGroup(null, "defaultMouseOverHighlight", 1, none, sameUnit, sameBoxUnit, any);
		prefsComponents.add(defaultMouseOverHighlight);
		
		highlightOptionsPanel.add(new JLabel("Default Mouse Over Highlight Option for New Boards: "), new GBC(0, 0, 4, 1).setAnchor(GBC.WEST));
		highlightOptionsPanel.add(none, new GBC(0, 1).setInsets(insets, 0, insets, insets));
		highlightOptionsPanel.add(sameUnit, new GBC(1, 1).setInsets(insets, 0, insets, insets));
		highlightOptionsPanel.add(sameBoxUnit, new GBC(2, 1).setInsets(insets, 0, insets, insets));
		highlightOptionsPanel.add(any, new GBC(3, 1).setInsets(insets, 0, insets, insets));
		
		highlightSolvedCell = new PrefsCheckBox("highlightSolvedCell", "Highlight Solved Cells", true);

		prefsComponents.add(highlightSolvedCell);
		highlightOptionsPanel.add(highlightSolvedCell, new GBC(0, 2, 4, 1).setInsets(rowGap, 0, 0, 0));
	}
	
	static int rowGap = 10;
	
	void initCopyOptions()
	{
		copyOptionsPanel = new JPanel();
		copyOptionsPanel.setBorder(BorderFactory.createTitledBorder("Items to Copy & Paste for Cell Copy & Paste and Drag & Drop"));
		
		copyCandidates = new PrefsCheckBox("copyCandidates", "Candidates", true);
		copyPencilMarks = new PrefsCheckBox("copyPencilMarks", "Pencil Marks", true);
		copyNotes = new PrefsCheckBox("copyNotes", "Notes", true);
		copyLocks = new PrefsCheckBox("copyLocks", "Lock State", true);
		
		prefsComponents.add(copyCandidates);
		prefsComponents.add(copyPencilMarks);
		prefsComponents.add(copyNotes);
		prefsComponents.add(copyLocks);
		
		copyOptionsPanel.add(copyCandidates);
		copyOptionsPanel.add(copyPencilMarks);
		copyOptionsPanel.add(copyNotes);
		copyOptionsPanel.add(copyLocks);
	}
	
	void initDefaultViewOptions()
	{
		int insets = 5;
		viewOptionsPanel = new JPanel(new GridBagLayout());
		viewOptionsPanel.setBorder(BorderFactory.createTitledBorder("Default View Options for new Boards"));
		
		showAllCandidates = new JRadioButton("All Candidates");
		showPencilMarks = new JRadioButton("Pencil Marks");
		showBlank = new JRadioButton("Blank");
		viewOptionsButtonGroup = new PrefsButtonGroup(null, "defaultViewOption", 0, showAllCandidates, showPencilMarks, showBlank);
		prefsComponents.add(viewOptionsButtonGroup);
		
		showBoxIndexes = new PrefsCheckBox("showBoxIndex", "Box", false);
		showRowIndexes = new PrefsCheckBox("showRowIndex", "Row", true);
		showColIndexes = new PrefsCheckBox("showColIndex", "Column", true);
		prefsComponents.add(showBoxIndexes);
		prefsComponents.add(showRowIndexes);
		prefsComponents.add(showColIndexes);
		
		viewOptionsPanel.add(new JLabel("Show in Unsolved Cells: "), new GBC(0, 0).setAnchor(GBC.WEST).setInsets(0, 0, 0, 10));
		viewOptionsPanel.add(showAllCandidates, new GBC(1, 0).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		viewOptionsPanel.add(showPencilMarks, new GBC(2, 0).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		viewOptionsPanel.add(showBlank, new GBC(3, 0).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		
		viewOptionsPanel.add(new JLabel("Show Board Indexes: "), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(rowGap, 0, 0, 0));
		viewOptionsPanel.add(showBoxIndexes, new GBC(1, 1).setAnchor(GBC.WEST));
		viewOptionsPanel.add(showRowIndexes, new GBC(2, 1).setAnchor(GBC.WEST));
		viewOptionsPanel.add(showColIndexes, new GBC(3, 1).setAnchor(GBC.WEST));
	}
	
	void initBoardComparatorOptions()
	{
		int insets = 5;
		boardComparatorSettingsPanel = new JPanel(new GridBagLayout());
		boardComparatorSettingsPanel.setBorder(BorderFactory.createTitledBorder("Default Settings for New Board Comparators: "));
		
		compareCandidates = new JRadioButton("Candidates");
		comparePencilMarks = new JRadioButton("Pencil Marks");
		compareNotes = new JRadioButton("Notes");
		compareLocks = new JRadioButton("Locks");
		compareOptionsGroup = new PrefsButtonGroup(null, "compareOption", 0, compareCandidates, comparePencilMarks, compareNotes, compareLocks);
		prefsComponents.add(compareOptionsGroup);
		
		comparatorShowBoxIndexes = new PrefsCheckBox("comparatorShowBoxIndex", "Box", false);
		comparatorShowRowIndexes = new PrefsCheckBox("comparatorShowRowIndex", "Row", true);
		comparatorShowColIndexes = new PrefsCheckBox("comparatorShowColIndex", "Column", true);
		prefsComponents.add(comparatorShowBoxIndexes);
		prefsComponents.add(comparatorShowRowIndexes);
		prefsComponents.add(comparatorShowColIndexes);
		
		defaultLinkEnabled = new PrefsCheckBox("defaultLinkEnabled", "Link to Chosen Board's History Tree's Selected Node", true);
		prefsComponents.add(defaultLinkEnabled);
		
		boardComparatorSettingsPanel.add(new JLabel("Compare Differences in: "), new GBC(0, 0, 4, 1).setAnchor(GBC.WEST));
		boardComparatorSettingsPanel.add(compareCandidates, new GBC(0, 1).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		boardComparatorSettingsPanel.add(comparePencilMarks, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		boardComparatorSettingsPanel.add(compareNotes, new GBC(2, 1).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		boardComparatorSettingsPanel.add(compareLocks, new GBC(3, 1).setAnchor(GBC.WEST).setInsets(insets, 0, insets, insets));
		
		boardComparatorSettingsPanel.add(new JLabel("Show Board Indexes: "), new GBC(0, 2, 3, 1).setAnchor(GBC.WEST).setInsets(rowGap, 0, 0, 0));
		boardComparatorSettingsPanel.add(comparatorShowBoxIndexes, new GBC(0, 3).setAnchor(GBC.WEST));
		boardComparatorSettingsPanel.add(comparatorShowRowIndexes, new GBC(1, 3).setAnchor(GBC.WEST));
		boardComparatorSettingsPanel.add(comparatorShowColIndexes, new GBC(2, 3).setAnchor(GBC.WEST));
		
		boardComparatorSettingsPanel.add(defaultLinkEnabled, new GBC(0, 4, 4, 1).setInsets(rowGap, 0, 0, 0));
	}
	
	public void loadSettings(SingleSettingsFile settingsFile)
	{
		Preferences node = settingsFile.node;
		
		for (PrefsComponent c : prefsComponents)
		{
			c.loadSettings(node);
		}

	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}

	public void saveSettings(SingleSettingsFile settingsFile, boolean saveToFile)
	{
		Preferences node = settingsFile.node;
		
		for (PrefsComponent c : prefsComponents)
		{
			c.saveSettings(node);
		}
		
		if (saveToFile) settingsFile.save();
	}
	
	public void applyChanges()
	{

	}
}