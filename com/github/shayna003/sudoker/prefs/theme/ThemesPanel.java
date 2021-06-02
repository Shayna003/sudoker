package com.github.shayna003.sudoker.prefs.theme;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.widgets.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import java.util.prefs.*;
import java.io.*;

/**
 * This panel allows the user to change the settings concerned with the visuals of the grid.
 * For setting the text correspondent to digits and indexes, this panel does not contain these preferences.(?)
 * @since 0.00 11-11-2020
 */
@SuppressWarnings("CanBeFinal")
public class ThemesPanel extends JPanel implements MultipleSettingsPanel
{	
	PreferenceFrame preferenceFrame;

	public ChangeListener sliderListener;
	
	public TextSliderPanel boardInsets;
	
	public TextSliderPanel bottomOuterBorderWidth;
	public ColorComponent bottomOuterBorderColor;
	
	public TextSliderPanel topOuterBorderWidth;
	public ColorComponent topOuterBorderColor;
	
	public TextSliderPanel boxInnerBorderWidth;
	public ColorComponent boxInnerBorderColor;
	
	public JRadioButton cellColor_uniform_button;
	public JRadioButton cellColor_byBox_button;
	public ColorComponent cellColor;
	public BoxColorComponent cellColor_byBox;
	
	public JRadioButton selectedCellColor_uniform_button;
	public JRadioButton selectedCellColor_byBox_button;
	public ColorComponent selectedCellColor;
	public BoxColorComponent selectedCellColor_byBox;
	
	public TextSliderPanel cellToBoxBorderGap;
	public TextSliderPanel cellSize;
	
	public TextSliderPanel cellBorderOptions;
	public JRadioButton cellBorderColor_uniform_button;
	public JRadioButton cellBorderColor_byBox_button;
	public ColorComponent cellBorderColor;
	public BoxColorComponent cellBorderColor_byBox;
	
	public JRadioButton selectedCellBorderColor_uniform_button;
	public JRadioButton selectedCellBorderColor_byBox_button;
	public ColorComponent selectedCellBorderColor;
	public BoxColorComponent selectedCellBorderColor_byBox;
		
	public FontChooserPanel candidateFontChooser;
	public FontChooserPanel solvedCandidateFontChooser;

	public FontChooserPanel pencilMarkFontChooser;
	
	public ColorComponent selectedCellFontColor;
	public ColorComponent lockedCellFontColor;
	public ColorComponent selectedLockedCellFontColor; // if both selected and locked, default is same as locked

	public FontChooserPanel indexFontChooser;
	public FontChooserPanel boxIndexFontChooser;
	
	public JRadioButton boxBackgroundColor_uniform_button;
	public JRadioButton boxBackgroundColor_byBox_button;
	public ColorComponent boxBackgroundColor;
	public BoxColorComponent boxBackgroundColor_byBox;
	public PrefsCheckBox paintBoxBackground;
	
	public ColorComponent panelBackgroundColor;
	public PrefsCheckBox paintPanelBackground;
	
	JPanel highLightOptionsPanel;
	public ColorComponent sameUnit;
	public ColorComponent sameBoxUnit;
	public ColorComponent any;
	public BoxColorComponent candidateHighlight;

	JPanel solverVisualsPanel;
	public ColorComponent repeatedCandidateColor;
	public ColorComponent noCandidateColor;
	public ColorComponent eliminatedCandidateColor;
	public ColorComponent onlyCandidateColor;
	
	public JPanel boardComparatorColorsPanel;
	public ColorComponent hasUniqueValueColor;
	public ColorComponent differentValueColor;
	
	public JScrollPane scrollPane;

	// iterated for loading and saving data
	ArrayList<PrefsComponent> prefsComponents = new ArrayList<>();
	
	public SettingsFileChooserPanel themeChooserPanel;
	boolean initializing; //don't call applyChanges during initialization
	
	/**
	 * Stores node settings to files. Pass this call to themeChooserPanel.
	 */
	public void saveSettingsToFiles()
	{
		Application.prefsLogger.entering("ThemesPanel", "saveSettingsToFiles");
		saveSettings(themeChooserPanel.selectedSetting, true);
		themeChooserPanel.saveSettingsToFiles();
	}
	
	/**
	 * Save current settings to a Preference node.
	 */
	public void saveSettings(SingleSettingsFile settingsFile, boolean saveToFile)
	{
		Application.prefsLogger.log(Level.FINE, "ThemesPanel saving settings to " + settingsFile);
		long start = System.currentTimeMillis();
		Preferences node = settingsFile.node;

		for (PrefsComponent c : prefsComponents)
		{
			c.saveSettings(node);
		}
		Application.loadTimeLogger.log(Level.FINE, "time taken to save preferences for node " + node + ": " + (System.currentTimeMillis() - start));
		if (saveToFile) settingsFile.save();
	}

	public void loadSettings(SingleSettingsFile settingsFile)
	{
		long start = System.currentTimeMillis();
		Preferences node = settingsFile.node;
		initializing = true;
		
		for (PrefsComponent c : prefsComponents)
		{
			c.loadSettings(node);
		}

		Application.loadTimeLogger.log(Level.FINE, "time taken to load preferences for node " + node + ": " + (System.currentTimeMillis() - start));
		initializing = false;
		repaint();
		applyChanges();
	}
	
	public ThemesPanel(PreferenceFrame preferenceFrame)
	{
		long start = System.currentTimeMillis();
		initializing = true;
		this.preferenceFrame = preferenceFrame;

		sliderListener = event -> 
		{
			if (!initializing) applyChanges();
		};
		
		ChangeListener applyChangesListener = event ->
		{
			applyChanges();
		};

		boardInsets = new TextSliderPanel("boardInsets", JSlider.HORIZONTAL, "Width", 0, 100, Theme.default_boardInsets, true, true, 20, 10, true, null, true, true, applyChangesListener);
		prefsComponents.add(boardInsets);
		
		bottomOuterBorderWidth = new TextSliderPanel("bottomOuterBorderWidth", JSlider.HORIZONTAL, "Width", 0, 30, Theme.default_bottomOuterBorderWidth, true, true, 10, 5, true, null, true, true, sliderListener);
		prefsComponents.add(bottomOuterBorderWidth);

		bottomOuterBorderColor = new ColorComponent("bottomOuterBorderColor", null, Theme.default_bottomOuterBorderColor, ThemesPanel.this, preferenceFrame, "Color for outer border");
		prefsComponents.add(bottomOuterBorderColor);
		
		topOuterBorderWidth = new TextSliderPanel("topOuterBorderWidth", JSlider.HORIZONTAL, "Width", 0, 30, Theme.default_topOuterBorderWidth, true, true, 10, 5, true, null, true, true, sliderListener);
		prefsComponents.add(topOuterBorderWidth);
		
		topOuterBorderColor = new ColorComponent("topOuterBorderColor", null, Theme.default_topOuterBorderColor, ThemesPanel.this, preferenceFrame, "Color for outer border inner line");
		prefsComponents.add(topOuterBorderColor);
		
		boxInnerBorderWidth = new TextSliderPanel("boxInnerBorderWidth", JSlider.HORIZONTAL, "Width", 0, 20, Theme.default_boxInnerBorderWidth, true, true, 10, 5, true, null, true, true, sliderListener);
		prefsComponents.add(boxInnerBorderWidth);

		boxInnerBorderColor = new ColorComponent("boxInnerBorderColor", null, Theme.default_boxInnerBorderColor, ThemesPanel.this, preferenceFrame, "Color for inner border");
		prefsComponents.add(boxInnerBorderColor);
		
		cellToBoxBorderGap = new TextSliderPanel("cellToBoxBorderGap", JSlider.HORIZONTAL, "Gap", 0, 20, Theme.default_cellToBoxBorderGap, true, true, 10, 5, true, null, true, true, sliderListener);
		prefsComponents.add(cellToBoxBorderGap);
		
		cellSize = new TextSliderPanel("cellSize", JSlider.HORIZONTAL, "Size", 0, 100, Theme.default_cellSize, true, true, 20, 5, true, null, true, true, sliderListener);
		prefsComponents.add(cellSize);
		
		var labelTable = new Hashtable<Integer, Component>();
		JLabel tmp = new JLabel("None");
		labelTable.put(0, tmp);
		tmp = new JLabel("Lowered");
		labelTable.put(1, tmp);
		tmp = new JLabel("Flat");
		labelTable.put(2, tmp);
		tmp = new JLabel("Raised");
		labelTable.put(3, tmp);
		cellBorderOptions = new TextSliderPanel("cellBorderOption", JSlider.HORIZONTAL, "Draw Options:", 0, 3, Theme.default_cellBorderOption, true, true, 1, 0, true, labelTable, true, false, sliderListener);
		cellBorderOptions.slider.setSnapToTicks(true);
		prefsComponents.add(cellBorderOptions);
		
		
		cellBorderColor_uniform_button = new JRadioButton("Uniform");
		cellBorderColor_byBox_button = new JRadioButton("By Boxes");
		
		PrefsButtonGroup cellBorderColorButtonGroup = new PrefsButtonGroup(applyChangesListener, "cellBorderColorOption", 0, cellBorderColor_uniform_button, cellBorderColor_byBox_button);
		prefsComponents.add(cellBorderColorButtonGroup);
		
		cellBorderColor = new ColorComponent("cellBorder", null, Theme.default_cellBorderColor, ThemesPanel.this, preferenceFrame, "Color for Cell Borders");
		prefsComponents.add(cellBorderColor);
		
		cellBorderColor_byBox = new BoxColorComponent("cellBorder", new Color[9], Theme.default_cellBorderColor, ThemesPanel.this, preferenceFrame, "Color for Cell Borders", true);
		prefsComponents.add(cellBorderColor_byBox);
	
		
		selectedCellBorderColor_uniform_button = new JRadioButton("Uniform");
		selectedCellBorderColor_byBox_button = new JRadioButton("By Boxes");
		
		PrefsButtonGroup selectedCellBorderColorButtonGroup = new PrefsButtonGroup(applyChangesListener, "selectedCellBorderColorOption", 0, selectedCellBorderColor_uniform_button, selectedCellBorderColor_byBox_button);
		prefsComponents.add(selectedCellBorderColorButtonGroup);
		
		selectedCellBorderColor = new ColorComponent("selectedCellBorder", null, Theme.default_selectedCellBorderColor, ThemesPanel.this, preferenceFrame, "Color for Selected Cell's Border");
		prefsComponents.add(selectedCellBorderColor);
		
		selectedCellBorderColor_byBox = new BoxColorComponent("selectedCellBorder", new Color[9], Theme.default_selectedCellBorderColor, ThemesPanel.this, preferenceFrame, "Color for Selected Cell's Border", true);
		prefsComponents.add(selectedCellBorderColor_byBox);
	
		cellColor_uniform_button = new JRadioButton("Uniform");
		cellColor_byBox_button = new JRadioButton("By Boxes");
		PrefsButtonGroup cellColorButtonGroup = new PrefsButtonGroup(applyChangesListener, "cellColorOption", 0, cellColor_uniform_button, cellColor_byBox_button);
		prefsComponents.add(cellColorButtonGroup);
		
		cellColor = new ColorComponent("cellColor", null, Theme.default_cellColor, ThemesPanel.this, preferenceFrame, "Cell Color");
		prefsComponents.add(cellColor);
		
		cellColor_byBox = new BoxColorComponent("cellColor", new Color[9], Theme.default_cellColor, ThemesPanel.this, preferenceFrame, "Cell Color", true);
		prefsComponents.add(cellColor_byBox);
		
		
		selectedCellColor_uniform_button = new JRadioButton("Uniform");
		selectedCellColor_byBox_button = new JRadioButton("By Boxes");
		PrefsButtonGroup selectedCellColorButtonGroup = new PrefsButtonGroup(applyChangesListener, "selectedCellColorOption", 0, selectedCellColor_uniform_button, selectedCellColor_byBox_button);
		prefsComponents.add(selectedCellColorButtonGroup);
		
		selectedCellColor = new ColorComponent("selectedCellColor", null, Theme.default_selectedCellColor, ThemesPanel.this, preferenceFrame, "Selected Cell Color");
		prefsComponents.add(selectedCellColor);
		
		selectedCellColor_byBox = new BoxColorComponent("selectedCellColor", new Color[9], Theme.default_selectedCellColor, ThemesPanel.this, preferenceFrame, "Selected Cell Color", true);
		prefsComponents.add(selectedCellColor_byBox);
		
		
		boxBackgroundColor_uniform_button = new JRadioButton("Uniform");
		
		boxBackgroundColor_byBox_button = new JRadioButton("By Boxes");

		PrefsButtonGroup bockBackgroundButtonGroup = new PrefsButtonGroup(applyChangesListener, "boxBackgroundOption", 0, boxBackgroundColor_uniform_button, boxBackgroundColor_byBox_button);
		prefsComponents.add(bockBackgroundButtonGroup);
		
		boxBackgroundColor = new ColorComponent("boxBackgroundColor", null, Theme.default_boxBackgroundColor, ThemesPanel.this, preferenceFrame, "Box Background Color");
		prefsComponents.add(boxBackgroundColor);
		
		boxBackgroundColor_byBox = new BoxColorComponent("boxBackgroundColor", new Color[9], Theme.default_boxBackgroundColor, ThemesPanel.this, preferenceFrame, "Box Background Color", true);
		prefsComponents.add(boxBackgroundColor_byBox);
		
		paintBoxBackground = new PrefsCheckBox("paintBoxBackgrounds", new AbstractAction("Paint Box Backgrounds")
		{
			public void actionPerformed(ActionEvent event)
			{
				applyChanges();
			}
		}, true);
		prefsComponents.add(paintBoxBackground);

		candidateFontChooser = new FontChooserPanel("Normal Candidate Font", "candidateFont", Theme.default_candidateFontName, Theme.default_candidateFontStyle, Theme.default_candidateFontSize, 0, 100, 20, 5, Theme.default_candidateFontColor, ThemesPanel.this, preferenceFrame);
		prefsComponents.add(candidateFontChooser);

		solvedCandidateFontChooser = new FontChooserPanel("Solved Cell Font", "solvedCandidateFont", Theme.default_solvedCandidateFontName, Theme.default_solvedCandidateFontStyle, Theme.default_solvedCandidateFontSize, 0, 100, 20, 5, Theme.default_solvedCandidateFontColor, ThemesPanel.this, preferenceFrame);
		prefsComponents.add(solvedCandidateFontChooser);
		
		pencilMarkFontChooser = new FontChooserPanel("Pencil Mark Font", "pencilMarkFont", Theme.default_pencilMarkFontName, Theme.default_pencilMarkFontStyle, Theme.default_pencilMarkFontSize, 0, 100, 20, 5, Theme.default_pencilMarkFontColor, ThemesPanel.this, preferenceFrame);
		prefsComponents.add(pencilMarkFontChooser);
		
		indexFontChooser = new FontChooserPanel("Row and Column Index Font", "rowColIndexFont", Theme.default_indexFontName, Theme.default_indexFontStyle, Theme.default_indexFontSize, 0, 100, 20, 5, Theme.default_indexFontColor, ThemesPanel.this, preferenceFrame);
		prefsComponents.add(indexFontChooser);
		
		boxIndexFontChooser = new FontChooserPanel("Box Index Font", "boxIndexFont", Theme.default_boxIndexFontName, Theme.default_boxIndexFontStyle, Theme.default_boxIndexFontSize, 0, 150, 30, 10, Theme.default_boxIndexFontColor, ThemesPanel.this, preferenceFrame);
		prefsComponents.add(boxIndexFontChooser);
		
		paintPanelBackground = new PrefsCheckBox("paintPanelBackground", new AbstractAction("Paint Panel Background")
		{
			public void actionPerformed(ActionEvent event)
			{
				applyChanges();
			}
		}, true);
		prefsComponents.add(paintPanelBackground);

		panelBackgroundColor = new ColorComponent("panelBackgroundColor", null, Theme.default_panelBackgroundColor, ThemesPanel.this, preferenceFrame, "Color for panel background");		
		prefsComponents.add(panelBackgroundColor);
		
		selectedCellFontColor = new ColorComponent("selectedCellFontColor", null, Theme.default_selectedCellFontColor, this, preferenceFrame, "Text Color for Selected Cell");
		prefsComponents.add(selectedCellFontColor);
		
		lockedCellFontColor = new ColorComponent("lockedCellFontColor", null, Theme.default_lockedCellFontColor, this, preferenceFrame, "Text Color for Locked Cell");
		prefsComponents.add(lockedCellFontColor);
		
		selectedLockedCellFontColor = new ColorComponent("selectedLockedCellFontColor", null, Theme.default_selectedLockedCellFontColor, this, preferenceFrame, "Text Color for Selected & Locked Cell");
		prefsComponents.add(selectedLockedCellFontColor);
		
		
		sameUnit = new ColorComponent("sameUnitHighlight", null, Theme.default_sameUnitHighlightColor, this, preferenceFrame, "Color for highlighting cells in the same row, column, and box as selected cell");
		prefsComponents.add(sameUnit);
		
		sameBoxUnit = new ColorComponent("sameBoxUnitHighlight", null, Theme.default_sameBoxUnitHighlightColor, this, preferenceFrame, "Color for highlighting cells in the same box, box row,and  box column, as selected cell");
		prefsComponents.add(sameBoxUnit);
		
		any = new ColorComponent("anyHighlight", null, Theme.default_anyHighlightColor, this, preferenceFrame, "Color for highlighting any cell");
		prefsComponents.add(any);
		
		candidateHighlight = new BoxColorComponent("candidateHighlightColor", new Color[9], Theme.default_candidateHighlightColors, this, preferenceFrame, "Color for Highlighting Candidate", false);
		prefsComponents.add(candidateHighlight);

		repeatedCandidateColor = new ColorComponent("repeatedCandidateColor", null, Theme.default_repeatedCandidateColor, this, preferenceFrame, "Color for highlighting cells with the same solved value in the same unit");
		prefsComponents.add(repeatedCandidateColor);

		noCandidateColor = new ColorComponent("noCandidateColor", null, Theme.default_noCandidateColor, this, preferenceFrame, "Color for highlighting cells with no possible candidates");
		prefsComponents.add(noCandidateColor);

		eliminatedCandidateColor = new ColorComponent("eliminatedCandidateColor", null, Theme.default_eliminatedCandidateColor, this, preferenceFrame, "Color for highlighting an eliminated candidate");
		prefsComponents.add(eliminatedCandidateColor);

		onlyCandidateColor = new ColorComponent("onlyCandidateColor", null, Theme.default_onlyCandidateColor, this, preferenceFrame, "Color for highlighting the only possible candidates of a cell");
		prefsComponents.add(onlyCandidateColor);

		hasUniqueValueColor = new ColorComponent("hasUniqueValueColor", null, Theme.default_hasUniqueValueColor, this, preferenceFrame, "Color for highlighting a unique value in a cell");
		prefsComponents.add(hasUniqueValueColor);
		
		differentValueColor = new ColorComponent("differentValueColor", null, Theme.default_differentValueColor, this, preferenceFrame, "Color for highlighting two cells with different values");
		prefsComponents.add(differentValueColor);
		
		Application.loadTimeLogger.log(Level.CONFIG, "Creation time for ThemesPanel components: " + (System.currentTimeMillis() - start));

		// after all components have been initialized, load user settings
		long s2 = System.currentTimeMillis();
		loadSettingsFiles();
		Application.loadTimeLogger.log(Level.CONFIG, "Load Settings time for ThemesPanel: " + (System.currentTimeMillis() - s2));

		long s3 = System.currentTimeMillis();
		JPanel panel = new JPanel(new GridBagLayout());

		panel.add(new JLabel("Board Border Width"), new GBC(0, 0).setAnchor(GBC.WEST));
		panel.add(boardInsets, new GBC(1, 0, 2, 1).setAnchor(GBC.EAST));
		
		panel.add(new JLabel("Bottom Outer Border"), new GBC(0, 1).setAnchor(GBC.WEST));
		panel.add(bottomOuterBorderWidth, new GBC(1, 1, 2, 1).setAnchor(GBC.EAST));
		panel.add(new JLabel("Color "), new GBC(3, 1).setAnchor(GBC.EAST));
		panel.add(bottomOuterBorderColor, new GBC(4, 1).setAnchor(GBC.WEST));
		
		panel.add(new JLabel("Top Outer Border"), new GBC(0, 2).setAnchor(GBC.WEST));
		panel.add(topOuterBorderWidth, new GBC(1, 2, 2, 1).setAnchor(GBC.EAST));
		panel.add(new JLabel("Color "), new GBC(3, 2).setAnchor(GBC.EAST));
		panel.add(topOuterBorderColor, new GBC(4, 2).setAnchor(GBC.WEST));
		
		panel.add(new JLabel("Box Inner Border"), new GBC(0, 3).setAnchor(GBC.WEST));
		panel.add(boxInnerBorderWidth, new GBC(1, 3, 2, 1).setAnchor(GBC.EAST));
		panel.add(new JLabel("Color "), new GBC(3, 3).setAnchor(GBC.EAST));
		panel.add(boxInnerBorderColor, new GBC(4, 3).setAnchor(GBC.WEST));
		
		panel.add(new JLabel("Cell to Box Gap"), new GBC(0, 4).setAnchor(GBC.WEST));
		panel.add(cellToBoxBorderGap, new GBC(1, 4, 2, 1).setAnchor(GBC.EAST));
		
		panel.add(new JLabel("Cell Size"), new GBC(0, 5).setAnchor(GBC.WEST));
		panel.add(cellSize, new GBC(1, 5, 2, 1).setAnchor(GBC.EAST));
		
		panel.add(new JLabel("Cell Borders"), new GBC(0, 6).setAnchor(GBC.WEST));
		panel.add(cellBorderOptions, new GBC(1, 6, 2, 1).setAnchor(GBC.EAST));
		
		
		JPanel panel2 = new JPanel(new GridBagLayout());
		
		panel2.add(new JLabel("Cell Color"), new GBC(0, 0).setAnchor(GBC.WEST));
		panel2.add(cellColor_uniform_button, new GBC(1, 0).setAnchor(GBC.EAST));
		panel2.add(cellColor, new GBC(2, 0).setAnchor(GBC.WEST));
		panel2.add(cellColor_byBox_button, new GBC(3, 0).setAnchor(GBC.EAST));
		panel2.add(cellColor_byBox, new GBC(4, 0).setAnchor(GBC.WEST).setInsets(0, 0, 10, 0));

		panel2.add(new JLabel("Cell Border Color"), new GBC(0, 1).setAnchor(GBC.WEST));
		panel2.add(cellBorderColor_uniform_button, new GBC(1, 1).setAnchor(GBC.EAST));
		panel2.add(cellBorderColor, new GBC(2, 1).setAnchor(GBC.WEST));
		panel2.add(cellBorderColor_byBox_button, new GBC(3, 1).setAnchor(GBC.EAST));
		panel2.add(cellBorderColor_byBox, new GBC(4, 1).setAnchor(GBC.WEST).setInsets(0, 0, 10, 0));
		
		panel2.add(new JLabel("Selected Cell Color"), new GBC(0, 2).setAnchor(GBC.WEST));
		panel2.add(selectedCellColor_uniform_button, new GBC(1, 2).setAnchor(GBC.EAST));
		panel2.add(selectedCellColor, new GBC(2, 2).setAnchor(GBC.WEST));
		panel2.add(selectedCellColor_byBox_button, new GBC(3, 2).setAnchor(GBC.EAST));
		panel2.add(selectedCellColor_byBox, new GBC(4, 2).setAnchor(GBC.WEST).setInsets(0, 0, 10, 0));
		
		panel2.add(new JLabel("Selected Cell Border Color"), new GBC(0, 3).setAnchor(GBC.WEST));
		panel2.add(selectedCellBorderColor_uniform_button, new GBC(1, 3).setAnchor(GBC.EAST));
		panel2.add(selectedCellBorderColor, new GBC(2, 3).setAnchor(GBC.WEST));
		panel2.add(selectedCellBorderColor_byBox_button, new GBC(3, 3).setAnchor(GBC.EAST));
		panel2.add(selectedCellBorderColor_byBox, new GBC(4, 3).setAnchor(GBC.WEST).setInsets(0, 0, 10, 0));
		
		panel2.add(new JLabel("Selected Cell Font Color"), new GBC(0, 4).setAnchor(GBC.WEST));
		panel2.add(selectedCellFontColor, new GBC(2, 4).setInsets(10, 0, 10, 0));
		
		panel2.add(new JLabel("Locked Cell Font Color"), new GBC(0, 5).setAnchor(GBC.WEST));
		panel2.add(lockedCellFontColor, new GBC(2, 5).setInsets(10, 0, 10, 0));
		
		panel2.add(new JLabel("Selected & Locked Cell Font Color"), new GBC(0, 6).setAnchor(GBC.WEST));
		panel2.add(selectedLockedCellFontColor, new GBC(2, 6).setInsets(10, 0, 10, 0));
		
		panel2.add(paintBoxBackground, new GBC(0, 7).setAnchor(GBC.WEST).setInsets(0, 0, 0, 20));
		panel2.add(boxBackgroundColor_uniform_button, new GBC(1, 7).setAnchor(GBC.EAST));
		panel2.add(boxBackgroundColor, new GBC(2, 7).setAnchor(GBC.WEST));
		panel2.add(boxBackgroundColor_byBox_button, new GBC(3, 7).setAnchor(GBC.EAST));
		panel2.add(boxBackgroundColor_byBox, new GBC(4, 7).setAnchor(GBC.WEST).setInsets(0, 0, 10, 0));
		
		panel2.add(paintPanelBackground, new GBC(0, 8).setAnchor(GBC.WEST));
		panel2.add(new JLabel("Color "), new GBC(1, 8).setAnchor(GBC.EAST));
		panel2.add(panelBackgroundColor, new GBC(2, 8).setAnchor(GBC.WEST));

		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
		panel3.add(solvedCandidateFontChooser);
		panel3.add(candidateFontChooser);
		panel3.add(pencilMarkFontChooser);
		panel3.add(indexFontChooser);
		panel3.add(boxIndexFontChooser);
		
		highLightOptionsPanel = new JPanel(new GridBagLayout());
		highLightOptionsPanel.setBorder(BorderFactory.createTitledBorder("Highlight Colors"));
		
		highLightOptionsPanel.add(new JLabel("Mouse Over Highlight for Same Unit as Selected Cell: "), new GBC(0, 0).setAnchor(GBC.WEST));
		highLightOptionsPanel.add(sameUnit, new GBC(1, 0).setInsets(10, 0, 10, 0));
		
		highLightOptionsPanel.add(new JLabel("Mouse Over Highlight for Same Box Unit as Selected Cell: "), new GBC(0, 1).setAnchor(GBC.WEST));
		highLightOptionsPanel.add(sameBoxUnit, new GBC(1, 1).setInsets(10, 0, 10, 0));
		
		highLightOptionsPanel.add(new JLabel("Mouse Over Highlight for Any Cell: "), new GBC(0, 2).setAnchor(GBC.WEST));
		highLightOptionsPanel.add(any, new GBC(1, 2).setInsets(10, 0, 10, 0));
		
		highLightOptionsPanel.add(new JLabel("Highlight Color for different Candidates: "), new GBC(0, 3).setAnchor(GBC.WEST));
		highLightOptionsPanel.add(candidateHighlight, new GBC(1, 3).setInsets(0, 10, 0, 10));
		
		solverVisualsPanel = new JPanel(new GridBagLayout());
		solverVisualsPanel.setBorder(BorderFactory.createTitledBorder("Solver Colors"));
		
		solverVisualsPanel.add(new JLabel("Highlight for cells with repeated solved values in the same unit: "), new GBC(0, 0).setAnchor(GBC.WEST));
		solverVisualsPanel.add(repeatedCandidateColor, new GBC(1, 0).setInsets(10, 0, 10, 0));

		solverVisualsPanel.add(new JLabel("Highlight cells with no possible candidates: "), new GBC(0, 1).setAnchor(GBC.WEST));
		solverVisualsPanel.add(noCandidateColor, new GBC(1, 1).setInsets(10, 0, 10, 0));
		
		solverVisualsPanel.add(new JLabel("Highlight for eliminated candidates: "), new GBC(0, 2).setAnchor(GBC.WEST));
		solverVisualsPanel.add(eliminatedCandidateColor, new GBC(1, 2).setInsets(10, 0, 10, 0));

		solverVisualsPanel.add(new JLabel("Highlight for a cell's only possible candidates: "), new GBC(0, 3).setAnchor(GBC.WEST));
		solverVisualsPanel.add(onlyCandidateColor, new GBC(1, 3).setInsets(10, 0, 10, 0));
		
		boardComparatorColorsPanel = new JPanel(new GridBagLayout());
		boardComparatorColorsPanel.setBorder(BorderFactory.createTitledBorder("Board Comparator Colors"));
		
		boardComparatorColorsPanel.add(new JLabel("Highlight for a cell's unique value: "), new GBC(0, 0).setAnchor(GBC.WEST));
		boardComparatorColorsPanel.add(hasUniqueValueColor, new GBC(1, 0).setInsets(10, 0, 10, 0));
		
		boardComparatorColorsPanel.add(new JLabel("Highlight 2 cells' different values: "), new GBC(0, 1).setAnchor(GBC.WEST));
		boardComparatorColorsPanel.add(differentValueColor, new GBC(1, 1).setInsets(10, 0, 10, 0));
		
		JPanel centralPanel = new JPanel();
		centralPanel.setLayout(new BoxLayout(centralPanel, BoxLayout.Y_AXIS));
		
		JPanel resetAllPanel = new JPanel(new BorderLayout());
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
		JPanel p = new JPanel();
		p.add(resetAll);
		resetAllPanel.add(p, BorderLayout.NORTH);
		JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
		resetAllPanel.add(separator, BorderLayout.CENTER);
		centralPanel.add(resetAllPanel);

		centralPanel.add(panel);
		centralPanel.add(panel2);
		centralPanel.add(panel3);
		centralPanel.add(highLightOptionsPanel);
		centralPanel.add(solverVisualsPanel);
		centralPanel.add(boardComparatorColorsPanel);
		
		setLayout(new BorderLayout());
		scrollPane = new JScrollPane(centralPanel);

		JSplitPane split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, themeChooserPanel, scrollPane);
		scrollPane.setPreferredSize(new Dimension(centralPanel.getPreferredSize().width + scrollPane.getMinimumSize().width + split_pane.getDividerSize(), centralPanel.getPreferredSize().height + scrollPane.getMinimumSize().height));

		add(split_pane, BorderLayout.CENTER);
		initializing = false;
		
		Application.loadTimeLogger.log(Level.CONFIG, "Time for ThemesPanel to put components together: " + (System.currentTimeMillis() - s3));
	}
	
	public static File themes_folder;
	public static File defaults_folder;
	public static File customs_folder;
	public static HashMap<String, InputStream> application_defaults;
	public static File selected_theme_data;

	public static boolean containsName(File[] files, String s)
	{
		for (File file : files)
		{
			if (file.getName().equals(s)) return true;
		}
		return false;
	}
	
	/**
	* To load the 
	*/
	public void loadSettingsFiles()
	{
		long start = System.currentTimeMillis();
		themes_folder = new File(Application.preferenceFolder, "themes");
		if (!themes_folder.exists())
		{
			themes_folder.mkdirs();
		}
		
		defaults_folder = new File(themes_folder, "default_themes");
		if (!defaults_folder.exists())
		{
			defaults_folder.mkdirs();
		}
		
		customs_folder = new File(themes_folder, "custom_themes");
		if (!customs_folder.exists())
		{
			customs_folder.mkdirs();
		}

		long start2 = System.currentTimeMillis();
		application_defaults = new HashMap<>();
		application_defaults.put("Snow", ApplicationLauncher.class.getResourceAsStream("resources/preferences/themes/Snow.xml"));
		application_defaults.put("Print Ready", ApplicationLauncher.class.getResourceAsStream("resources/preferences/themes/Print Ready.xml"));
		application_defaults.put("Sudoker", ApplicationLauncher.class.getResourceAsStream("resources/preferences/themes/Sudoker.xml"));
		application_defaults.put("Dark", ApplicationLauncher.class.getResourceAsStream("resources/preferences/themes/Dark.xml"));
		
		File[] default_xmls = defaults_folder.listFiles(PreferenceFrame.xmlFilter);
		Application.prefsLogger.log(Level.CONFIG, "default_xmls: " + Arrays.toString(default_xmls));
		File[] custom_xmls = customs_folder.listFiles(PreferenceFrame.xmlFilter);
		Application.prefsLogger.log(Level.CONFIG, "custom_xmls: " + Arrays.toString(default_xmls));
		ArrayList<SettingsFile> default_themes = new ArrayList<>();
		ArrayList<SettingsFile> custom_themes = new ArrayList<>();

		long start3 = System.currentTimeMillis();
		if (default_xmls != null)
		{
			for (File f : default_xmls)
			{
				try 
				{
					long s2 = System.currentTimeMillis();
					default_themes.add(new Theme(f, true, this));
					Application.loadTimeLogger.log(Level.FINE, "Creation time for one default Theme: " + (System.currentTimeMillis() - s2));
				} 
				catch (IOException | InvalidPreferencesFormatException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "loadSettingsFiles", "Error when creating default Theme from file " + f, e);
				}
			}
		}
		Application.loadTimeLogger.log(Level.FINE, "Load time for default_xmls: " + (System.currentTimeMillis() - start3));
		
		
		Application.prefsLogger.log(Level.FINE, "if custom_xmls != null");
		//name repeats can cause serious issues, like if you delete a repeat, and call save, the node is already removed and will trigger an error...
		long start4 = System.currentTimeMillis();
		if (custom_xmls != null)
		{
			for (File f : custom_xmls)
			{
				try 
				{	
					//The file cannot have the same name as one of the default themes
					if (default_xmls != null && !containsName(default_xmls, f.getName()) && !application_defaults.containsKey(PreferenceFrame.removeFileExtension(f)))
					{
						long s = System.currentTimeMillis();
						custom_themes.add(new Theme(f, false, this));
						Application.loadTimeLogger.log(Level.FINE, "Creation time for one custom Theme: " + (System.currentTimeMillis() - s));
					}			
				} 
				catch (IOException | InvalidPreferencesFormatException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "loadSettingsFiles", "Error when creating custom Theme from file " + f, e);
				}
			}
		}
		Application.loadTimeLogger.log(Level.FINE, "Load time for custom_xmls: " + (System.currentTimeMillis() - start4));
		
		long start5 = System.currentTimeMillis();
		Application.prefsLogger.log(Level.FINE, "configure selected Theme");
		boolean default_selected;
		int selection_index;
		if (default_themes.size() + custom_themes.size() == 0)
		{
			Theme newTheme = new Theme(new File(customs_folder, "Untitled.xml"), this);
			newTheme.save();
			custom_themes.add(newTheme);
		}

		selected_theme_data = new File(themes_folder, "selected_theme.xml");
		Application.loadTimeLogger.log(Level.FINEST, "Load time for selected Theme: " + (System.currentTimeMillis() - start5));
		
		long start6 = System.currentTimeMillis();
		themeChooserPanel = new SettingsFileChooserPanel(themes_folder, "theme", "themes", "Theme", "Themes", this, preferenceFrame, defaults_folder, customs_folder, application_defaults, default_themes, custom_themes, selected_theme_data);
		Application.loadTimeLogger.log(Level.FINE, "time to create ThemeChooserPanel: " + (System.currentTimeMillis() - start6));
		Application.prefsLogger.log(Level.FINE, "loadSettingsFiles complete");
	}	
	
	public InputStream getDefaultSetting(String settingName)
	{
		return ApplicationLauncher.class.getResourceAsStream("resources/preferences/themes/" + settingName + ".xml");
	}
	
	public void applyChanges()
	{
		if (!initializing && Application.prefs_initialized) 
		{
			long start = System.currentTimeMillis();
			Application.prefsLogger.entering("ThemesPanel", "applyChanges");
			for (ApplicationFrame applicationFrame : Application.openWindows)
			{
				for (int t = 0; t < applicationFrame.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) applicationFrame.tabbedPane.getComponentAt(t)).board.refresh();
				}
			}
			
			if (Application.boardComparatorFrame != null)
			{
				for (int t = 0; t < Application.boardComparatorFrame.tabbedPane.getTabCount(); t++)
				{
					((BoardComparator) Application.boardComparatorFrame.tabbedPane.getComponentAt(t)).refreshBoards();
				}
				Application.prefsLogger.log(Level.CONFIG, "Time taken to apply changes for ThemesPanel: " + (System.currentTimeMillis() - start));
			}
		}
	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return null;
	}
}