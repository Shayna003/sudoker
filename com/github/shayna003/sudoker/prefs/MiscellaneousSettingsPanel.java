package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.widgets.*;

import java.awt.*;
import javax.swing.*;
import java.util.prefs.*;
import java.io.*;
import java.util.*;

/**
 * Currently has Stopwatch & MusicPlayer settings
 * @since 4-28-2021
 */
@SuppressWarnings("CanBeFinal")
public class MiscellaneousSettingsPanel extends JPanel implements SettingsPanel
{
	SingleSettingsFile settingsFile;
	PreferenceFrame preferenceFrame;
	boolean initializing;
	
	ArrayList<PrefsComponent> prefsComponents;
	
	public PrefsCheckBox saveMusicPlayerSongPositions;
	
	JPanel stopwatchSettings;
	public PrefsCheckBox flashTimeSeparators;
	public ColorComponent stopwatchFontColor;
	public PrefsNumberSpinner stopwatchFontSize;
	public PrefsCheckBox paintGhostLabel;
	public ColorComponent ghostLabelColor;
	public ColorComponent stopwatchBackgroundColor;
	public PrefsCheckBox restartTimerUponImport;
	public PrefsCheckBox startTimerUponCreatingNewTab;

	
	void initStopwatchSettings()
	{		
		flashTimeSeparators = new PrefsCheckBox("stopwatchFlashTimeSeparators", true);
		flashTimeSeparators.addChangeListener(event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch.flashTimeSeparators = flashTimeSeparators.isSelected();
				}
			}
		});
		prefsComponents.add(flashTimeSeparators);

		stopwatchFontColor = new ColorComponent("stopwatchFontColor", null, new Color(24, 85, 239), this, preferenceFrame, "Font Color for Stopwatches");
		stopwatchFontColor.addColorChangeListener(event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch.label.setForeground(stopwatchFontColor.color);
				}
			}
		});
		prefsComponents.add(stopwatchFontColor);
		
		stopwatchFontSize = new PrefsNumberSpinner("stopwatchFontSize", 0, 72, 1, 30, event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					Stopwatch stopwatch = ((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch;
					stopwatch.label.setFont(stopwatch.label.getFont().deriveFont(((Integer) stopwatchFontSize.getValue()).floatValue()));
					stopwatch.ghostLabel.setFont(stopwatch.ghostLabel.getFont().deriveFont(((Integer) stopwatchFontSize.getValue()).floatValue()));
					stopwatch.label.setSize(stopwatch.label.getPreferredSize());
					stopwatch.ghostLabel.setSize(stopwatch.ghostLabel.getPreferredSize());
					stopwatch.digitsPane.setPreferredSize(stopwatch.label.getPreferredSize());
				}
			}
		}, 3);
		prefsComponents.add(stopwatchFontSize);
		
		paintGhostLabel = new PrefsCheckBox("stopwatchPaintGhostLabel", true);
		paintGhostLabel.addChangeListener(event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch.ghostLabel.setVisible(paintGhostLabel.isSelected());
				}
			}
		});
		prefsComponents.add(paintGhostLabel);
		
		ghostLabelColor = new ColorComponent("stopwatchGhostLabelColor", null, new Color(212, 233, 237), this, preferenceFrame, "Font Color for Underlying Text of Stopwatches");
		ghostLabelColor.addColorChangeListener(event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch.ghostLabel.setForeground((Color) event.getNewValue());
				}
			}
		});
		prefsComponents.add(ghostLabelColor);
		
		stopwatchBackgroundColor = new ColorComponent("stopwatchBackgroundColor", null, new Color(223, 237, 240), this, preferenceFrame, "Background Color for Stopwatches");
		stopwatchBackgroundColor.addColorChangeListener(event ->
		{
			if (initializing) return;
			// traverse all stopwatches and apply changes
			for (ApplicationFrame frame : Application.openWindows)
			{
				for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
				{
					Stopwatch stopwatch = ((SudokuTab) frame.tabbedPane.getComponentAt(t)).stopwatch;
					stopwatch.digitsPanel.setBackground(
						(Color) event.getNewValue());
					stopwatch.stopwatchPanel.setBackground((Color) event.getNewValue());
				}
			}
		});
		prefsComponents.add(stopwatchBackgroundColor);
		
		restartTimerUponImport = new PrefsCheckBox("restartTimerUponImport", false);
		prefsComponents.add(restartTimerUponImport);
		
		startTimerUponCreatingNewTab = new PrefsCheckBox("startTimerUponCreatingNewTab", false);
		prefsComponents.add(startTimerUponCreatingNewTab);
	}
	
	void layoutStopwatchSettings()
	{
		int insets = 5;
		stopwatchSettings = new JPanel(new GridBagLayout());
		stopwatchSettings.setBorder(BorderFactory.createTitledBorder("Stopwatch"));
		
		stopwatchSettings.add(new JLabel("Flash Time Separators"), new GBC(0, 0).setAnchor(GBC.WEST));
		stopwatchSettings.add(flashTimeSeparators, new GBC(1, 0).setAnchor(GBC.WEST));
		
		stopwatchSettings.add(new JLabel("Font Color"), new GBC(0, 1).setAnchor(GBC.WEST));
		stopwatchSettings.add(stopwatchFontColor, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(insets, 0, insets, 0));
		
		stopwatchSettings.add(new JLabel("Font Size"), new GBC(0, 2).setAnchor(GBC.WEST));
		stopwatchSettings.add(stopwatchFontSize, new GBC(1, 2).setAnchor(GBC.WEST));
		
		stopwatchSettings.add(new JLabel("Paint Ghost Label"), new GBC(0, 3).setAnchor(GBC.WEST));
		stopwatchSettings.add(paintGhostLabel, new GBC(1, 3).setAnchor(GBC.WEST));
		
		stopwatchSettings.add(new JLabel("Ghost Label Color"), new GBC(0, 4).setAnchor(GBC.WEST));
		stopwatchSettings.add(ghostLabelColor, new GBC(1, 4).setAnchor(GBC.WEST).setInsets(insets, 0, insets, 0));
		
		stopwatchSettings.add(new JLabel("Background Color"), new GBC(0, 5).setAnchor(GBC.WEST));
		stopwatchSettings.add(stopwatchBackgroundColor, new GBC(1, 5).setAnchor(GBC.WEST).setInsets(insets, 0, insets, 0));
		
		stopwatchSettings.add(new JLabel("Restart Stopwatch Upon Import (ignores Stopwatch time data)"), new GBC(0, 6, 3, 1).setAnchor(GBC.WEST));
		stopwatchSettings.add(restartTimerUponImport, new GBC(3, 6).setAnchor(GBC.WEST));
		
		stopwatchSettings.add(new JLabel("Start Stopwatch Upon Creating New Board"), new GBC(0, 7, 2, 1).setAnchor(GBC.WEST));
		stopwatchSettings.add(startTimerUponCreatingNewTab, new GBC(2, 7).setAnchor(GBC.WEST));
	}
	
	public MiscellaneousSettingsPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		this.preferenceFrame = preferenceFrame;
		
		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "miscellaneous_settings.xml"));
		prefsComponents = new ArrayList<>();

		initStopwatchSettings();
		layoutStopwatchSettings();
		
		
		saveMusicPlayerSongPositions = new PrefsCheckBox("saveSongPositions", "Save Song Positions for Music Player When Program Exits", true);
		prefsComponents.add(saveMusicPlayerSongPositions);
		
		
		JButton resetAll = new JButton("Reset All to Default");
		resetAll.addChangeListener(event ->
		{
			for (PrefsComponent c : prefsComponents)
			{
				c.resetToDefault();
			}
		});
		JPanel resetAllButtonPanel = new JPanel();
		resetAllButtonPanel.add(resetAll);
		
		setLayout(new BorderLayout());
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(saveMusicPlayerSongPositions, BorderLayout.NORTH);
		panel.add(stopwatchSettings, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);
		add(resetAllButtonPanel, BorderLayout.SOUTH);
		
		loadSettings(settingsFile);
		initializing = false;
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