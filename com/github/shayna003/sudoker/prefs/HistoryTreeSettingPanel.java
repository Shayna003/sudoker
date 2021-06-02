package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.swingComponents.*;

import java.awt.*;
import javax.swing.*;
import java.util.prefs.*;
import java.io.*;
import java.util.*;

/**
 * @since 4-18-2021
 */
@SuppressWarnings("CanBeFinal")
public class HistoryTreeSettingPanel extends JPanel implements SettingsPanel
{
	SingleSettingsFile settingsFile;
	PreferenceFrame preferenceFrame;
	boolean initializing;
	
	ArrayList<PrefsComponent> prefsComponents;
	
	JPanel spinnerPanel;
	public PrefsNumberSpinner maxNodesSpinner;
	public PrefsNumberSpinner nodeHeightSpinner;
	public PrefsNumberSpinner nodeWidthSpinner;
	public PrefsNumberSpinner rowGapSpinner;
	public PrefsNumberSpinner nodeGapSpinner;
	
	JPanel specialColorsPanel;
	JPanel colorsPanel;
	public EnumMap<EditType, ColorComponent> editColors;
	ColorComponent boardCreation;
	ColorComponent importColor;
	ColorComponent generate;
	ColorComponent clone;
	ColorComponent loadQuickSave;
	ColorComponent clear;
	ColorComponent massLockChange;
	ColorComponent rotate;
	ColorComponent flip;
	ColorComponent editCell;
	ColorComponent takeStep;
	ColorComponent quickSolve;
	
	public ColorComponent currentNodeColor;
	public ColorComponent historyTreeBackgroundColor;
	
	static JButton makeSetToDefaultButton(PrefsComponent c)
	{
		final PrefsComponent pc = c;
		JButton b = new JButton("Set to Default");
		b.addActionListener(event ->
		{
			pc.resetToDefault();
		});
		return b;
	}
	
	public HistoryTreeSettingPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		this.preferenceFrame = preferenceFrame;
		
		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "history_tree_settings.xml"));

		prefsComponents = new ArrayList<>();
		initSpinners();
		
		editColors = new EnumMap<EditType, ColorComponent>(EditType.class);
		initColorComponents();
		layoutColorComponents();
		
		spinnerPanel = new JPanel(new GridBagLayout());
		spinnerPanel.add(new JLabel("Max Number of Nodes in a Tree: "), new GBC(0, 0).setAnchor(GBC.WEST));
		spinnerPanel.add(maxNodesSpinner, new GBC(1, 0).setAnchor(GBC.WEST));
		spinnerPanel.add(makeSetToDefaultButton(maxNodesSpinner), new GBC(2, 0));
		
		spinnerPanel.add(new JLabel("Width of A Node: "), new GBC(0, 1).setAnchor(GBC.WEST));
		spinnerPanel.add(nodeWidthSpinner, new GBC(1, 1).setAnchor(GBC.WEST));
		spinnerPanel.add(makeSetToDefaultButton(nodeWidthSpinner), new GBC(2, 1));
		
		spinnerPanel.add(new JLabel("Height of A Node: "), new GBC(0, 2).setAnchor(GBC.WEST));
		spinnerPanel.add(nodeHeightSpinner, new GBC(1, 2).setAnchor(GBC.WEST));
		spinnerPanel.add(makeSetToDefaultButton(nodeHeightSpinner), new GBC(2, 2));
		
		spinnerPanel.add(new JLabel("Gab between Nodes: "), new GBC(0, 3).setAnchor(GBC.WEST));
		spinnerPanel.add(nodeGapSpinner, new GBC(1, 3).setAnchor(GBC.WEST));
		spinnerPanel.add(makeSetToDefaultButton(nodeGapSpinner), new GBC(2, 3));
		
		spinnerPanel.add(new JLabel("Gab between Rows: "), new GBC(0, 4).setAnchor(GBC.WEST));
		spinnerPanel.add(rowGapSpinner, new GBC(1, 4).setAnchor(GBC.WEST));
		spinnerPanel.add(makeSetToDefaultButton(rowGapSpinner), new GBC(2, 4));
		
		JButton resetAll = new JButton("Reset All to Default");
		resetAll.addActionListener(event ->
		{
			for (PrefsComponent c : prefsComponents)
			{
				c.resetToDefault();
			}
		});
		JPanel resetAllButtonPanel = new JPanel();
		resetAllButtonPanel.add(resetAll);
		
		setLayout(new BorderLayout());
		add(spinnerPanel, BorderLayout.NORTH);
		
		JPanel panel = new JPanel(new BorderLayout());
		JPanel horizontalPanel = new JPanel();
		horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
		horizontalPanel.add(specialColorsPanel);
		horizontalPanel.add(colorsPanel);
		JScrollPane pane = new JScrollPane(horizontalPanel);
		panel.add(pane, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createTitledBorder("Colors for History Tree Nodes"));
		add(panel, BorderLayout.CENTER);
		add(resetAllButtonPanel, BorderLayout.SOUTH);
		
		loadSettings(settingsFile);
		initializing = false;
	}
	
	void initSpinners()
	{
		int columns = 4;
		maxNodesSpinner = new PrefsNumberSpinner("maxNodes", 2, 1000, 1, 100, event ->
		{
			if (!initializing)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					for (int t = 0; t < window.getTabCount(); t++)
					{
						((HistoryTreePanel) window.getComponentAt(t)).historyTree.setMaxNodes((int) maxNodesSpinner.getValue());
					}
				}
			}
		}, columns);
		prefsComponents.add(maxNodesSpinner);
		
		nodeHeightSpinner = new PrefsNumberSpinner("nodeHeight", 5, 100, 1, 25, event ->
		{
			if (!initializing)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					for (int t = 0; t < window.getTabCount(); t++)
					{
						((HistoryTreePanel) window.getComponentAt(t)).historyTree.setNodeHeight((int) nodeHeightSpinner.getValue());
					}
				}
			}
		}, columns);
		prefsComponents.add(nodeHeightSpinner);
		
		nodeWidthSpinner = new PrefsNumberSpinner("nodeWidth", 20, 300, 1, 125, event ->
		{
			if (!initializing)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					for (int t = 0; t < window.getTabCount(); t++)
					{
						((HistoryTreePanel) window.getComponentAt(t)).historyTree.setNodeWidth((int) nodeWidthSpinner.getValue());
					}
				}
			}
		}, columns);
		prefsComponents.add(nodeWidthSpinner);
		
		rowGapSpinner = new PrefsNumberSpinner("rowGap", 0, 100, 1, 20, event ->
		{
			if (!initializing)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					for (int t = 0; t < window.getTabCount(); t++)
					{
						((HistoryTreePanel) window.getComponentAt(t)).historyTree.setRowGap((int) rowGapSpinner.getValue());
					}
				}
			}
		}, columns);
		prefsComponents.add(rowGapSpinner);
		
		nodeGapSpinner = new PrefsNumberSpinner("nodeGap", 0, 100, 1, 20, event ->
		{
			if (!initializing)
			{
				for (int w = 0; w < Application.historyTreeFrame.windows.getTabCount(); w++)
				{
					JTabbedPane window = (JTabbedPane) Application.historyTreeFrame.windows.getComponentAt(w);
					for (int t = 0; t < window.getTabCount(); t++)
					{
						((HistoryTreePanel) window.getComponentAt(t)).historyTree.setNodeGap((int) nodeGapSpinner.getValue());
					}
				}
			}
		}, columns);
		prefsComponents.add(nodeGapSpinner);
	}
	
	void initColorComponents()
	{
		boardCreation = new ColorComponent("boardCreation", null, new Color(150, 215, 131), this, preferenceFrame, "Node Color for edits of type Board Creation");
		prefsComponents.add(boardCreation);
		editColors.put(EditType.BOARD_CREATION, boardCreation);
		
		importColor = new ColorComponent("import", null, new Color(168, 190, 168), this, preferenceFrame, "Node Color for edits of type Import");
		prefsComponents.add(importColor);
		editColors.put(EditType.IMPORT, importColor);
		
		generate = new ColorComponent("generate", null, new Color(225, 230, 87), this, preferenceFrame, "Node Color for edits of type Generate");
		prefsComponents.add(generate);
		editColors.put(EditType.GENERATE, generate);
		
		clone = new ColorComponent("clone", null, new Color(254, 194, 125), this, preferenceFrame, "Node Color for edits of type Clone");
		prefsComponents.add(clone);
		editColors.put(EditType.CLONE, clone);
		
		loadQuickSave = new ColorComponent("loadQuickSave", null, new Color(154, 187, 241), this, preferenceFrame, "Node Color for edits of type Load Quick Save");
		prefsComponents.add(loadQuickSave);
		editColors.put(EditType.LOAD_QUICK_SAVE, loadQuickSave);
		
		clear = new ColorComponent("clear", null, new Color(252, 97, 94), this, preferenceFrame, "Node Color for edits of type Clear");
		prefsComponents.add(clear);
		editColors.put(EditType.CLEAR, clear);
		
		massLockChange = new ColorComponent("massLockChanges", null, new Color(21, 95, 237), this, preferenceFrame, "Node Color for edits of type Mass Lock Change");
		prefsComponents.add(massLockChange);
		editColors.put(EditType.MASS_LOCK_CHANGE, massLockChange);
		
		rotate = new ColorComponent("rotate", null, new Color(103, 106, 230), this, preferenceFrame, "Node Color for edits of type Rotate");
		prefsComponents.add(rotate);
		editColors.put(EditType.ROTATE, rotate);
		
		flip = new ColorComponent("flip", null, new Color(103, 106, 230), this, preferenceFrame, "Node Color for edits of type Flip");
		prefsComponents.add(flip);
		editColors.put(EditType.FLIP, flip);
		
		editCell = new ColorComponent("editCell", null, new Color(147, 164, 171), this, preferenceFrame, "Node Color for edits of type Edit Cell");
		prefsComponents.add(editCell);
		editColors.put(EditType.EDIT_CELL, editCell);
		
		takeStep = new ColorComponent("takeStep", null, new Color(110, 187, 201), this, preferenceFrame, "Node Color for edits of type Take Step");
		prefsComponents.add(takeStep);
		editColors.put(EditType.TAKE_STEP, takeStep);

		quickSolve = new ColorComponent("quickSolve", null, new Color(64, 227, 211), this, preferenceFrame, "Node Color for edits of type Quick Solve");
		prefsComponents.add(quickSolve);
		editColors.put(EditType.QUICK_SOLVE, quickSolve);
		
		// these only concern selected node and current node
		currentNodeColor = new ColorComponent("currentNode", null, Color.WHITE, this, preferenceFrame, "Node Color for Current Node");
		prefsComponents.add(currentNodeColor);
		
		historyTreeBackgroundColor = new ColorComponent("bgColor", null, new Color(255, 255, 255, 0), this, preferenceFrame, "Background Color for History Tree");
		prefsComponents.add(historyTreeBackgroundColor);
	}
	
	void layoutColorComponents()
	{
		int insets = 5;
		
		specialColorsPanel = new JPanel(new GridBagLayout());
		specialColorsPanel.add(new JLabel("Current Node: "), new GBC(0, 0).setAnchor(GBC.WEST));
		specialColorsPanel.add(currentNodeColor, new GBC(1, 0).setInsets(insets));
		specialColorsPanel.add(makeSetToDefaultButton(currentNodeColor), new GBC(2, 0));
		
		specialColorsPanel.add(new JLabel("Tree Background: "), new GBC(0, 1).setAnchor(GBC.WEST));
		specialColorsPanel.add(historyTreeBackgroundColor, new GBC(1, 1).setInsets(insets));
		specialColorsPanel.add(makeSetToDefaultButton(historyTreeBackgroundColor), new GBC(2, 1));
		
		colorsPanel = new JPanel(new GridBagLayout());
		
		colorsPanel.add(new JLabel("Board Creation: "), new GBC(0, 1).setAnchor(GBC.WEST));
		colorsPanel.add(boardCreation, new GBC(1, 1).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(boardCreation), new GBC(2, 1));
		
		colorsPanel.add(new JLabel("Import: "), new GBC(0, 2).setAnchor(GBC.WEST));
		colorsPanel.add(importColor, new GBC(1, 2).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(importColor), new GBC(2, 2));
		
		colorsPanel.add(new JLabel("Generate: "), new GBC(0, 3).setAnchor(GBC.WEST));
		colorsPanel.add(generate, new GBC(1, 3).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(generate), new GBC(2, 3));
		
		colorsPanel.add(new JLabel("Clone: "), new GBC(0, 4).setAnchor(GBC.WEST));
		colorsPanel.add(clone, new GBC(1, 4).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(clone), new GBC(2, 4));
		
		colorsPanel.add(new JLabel("Load Quick Save: "), new GBC(0, 5).setAnchor(GBC.WEST));
		colorsPanel.add(loadQuickSave, new GBC(1, 5).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(loadQuickSave), new GBC(2, 5));
		
		colorsPanel.add(new JLabel("Clear: "), new GBC(0, 6).setAnchor(GBC.WEST));
		colorsPanel.add(clear, new GBC(1, 6).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(clear), new GBC(2, 6));
		
		colorsPanel.add(new JLabel("Mass Lock Change: "), new GBC(0, 7).setAnchor(GBC.WEST));
		colorsPanel.add(massLockChange, new GBC(1, 7).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(massLockChange), new GBC(2, 7));
		
		colorsPanel.add(new JLabel("Rotate: "), new GBC(0, 8).setAnchor(GBC.WEST));
		colorsPanel.add(rotate, new GBC(1, 8).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(rotate), new GBC(2, 8));
		
		colorsPanel.add(new JLabel("Flip: "), new GBC(0, 9).setAnchor(GBC.WEST));
		colorsPanel.add(flip, new GBC(1, 9).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(flip), new GBC(2, 9));
		
		colorsPanel.add(new JLabel("Edit Cell: "), new GBC(0, 10).setAnchor(GBC.WEST));
		colorsPanel.add(editCell, new GBC(1, 10).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(editCell), new GBC(2, 10));
		
		colorsPanel.add(new JLabel("Take Step: "), new GBC(0, 11).setAnchor(GBC.WEST));
		colorsPanel.add(takeStep, new GBC(1, 11).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(takeStep), new GBC(2, 11));

		colorsPanel.add(new JLabel("Quick Solve: "), new GBC(0, 12).setAnchor(GBC.WEST));
		colorsPanel.add(quickSolve, new GBC(1, 12).setInsets(insets));
		colorsPanel.add(makeSetToDefaultButton(quickSolve), new GBC(2, 12));
	}
	
	public void loadSettings(SingleSettingsFile settingsFile)
	{
		Preferences node = settingsFile.node;
		for (PrefsComponent c : prefsComponents)
		{
			c.loadSettings(node);
		}
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


	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}