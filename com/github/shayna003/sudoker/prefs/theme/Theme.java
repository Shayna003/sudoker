package com.github.shayna003.sudoker.prefs.theme;

import com.github.shayna003.sudoker.prefs.*;

import java.util.prefs.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * The code about SettingsFile and related classes can be automated.
 * @version 0.00 1-7-2021
 * @since 12-14-2021
 */
@SuppressWarnings("CanBeFinal")
public class Theme extends SettingsFile
{	
	public static int default_boardInsets = 20;
	public static int default_bottomOuterBorderWidth = 7;
	public static Color default_bottomOuterBorderColor = new Color(-8345700, true);
	
	public static int default_topOuterBorderWidth = 3;
	public static Color default_topOuterBorderColor = new Color(-11964828, true);
	
	public static int default_boxInnerBorderWidth = 5;
	public static Color default_boxInnerBorderColor = new Color(-4206902, true);
	
	public static int default_cellToBoxBorderGap = 5;
	public static int default_cellSize = 55;
	
	public static int default_cellBorderOption = 3; // raised 3D Rect
	public static Color default_cellColor = new Color(-328966, true);
	public static Color default_selectedCellColor = new Color(222, 216, 172);
	
	public static Color default_cellBorderColor = new Color(-328966, true);
	public static Color default_selectedCellBorderColor = new Color(222, 216, 172);
	
	public static Color default_boxBackgroundColor = new Color(-4206902, true);

	public static String default_candidateFontName = "SansSerif";
	public static int default_candidateFontStyle = Font.BOLD;
	public static int default_candidateFontSize = 14;
	public static Color default_candidateFontColor = new Color(-4141370, true);
	
	public static String default_solvedCandidateFontName = "SansSerif";
	public static int default_solvedCandidateFontStyle = Font.BOLD;
	public static int default_solvedCandidateFontSize = 28;
	public static Color default_solvedCandidateFontColor = new Color(-6380150, true);
	
	public static Color default_selectedCellFontColor = new Color(207, 161, 118);
	public static Color default_lockedCellFontColor = new Color(133, 194, 218);
	public static Color default_selectedLockedCellFontColor = new Color(133, 194, 218);
	
	public static String default_pencilMarkFontName = "SansSerif";
	public static int default_pencilMarkFontStyle = Font.ITALIC;
	public static int default_pencilMarkFontSize = 14;
	public static Color default_pencilMarkFontColor = new Color(-4141370, true);
	
	public static String default_indexFontName = "SansSerif";
	public static int default_indexFontStyle = Font.BOLD;
	public static int default_indexFontSize = 16;
	public static Color default_indexFontColor = new Color(-1379348, true);
	
	public static String default_boxIndexFontName = "SansSerif";
	public static int default_boxIndexFontStyle = Font.BOLD;
	public static int default_boxIndexFontSize = 60;
	public static Color default_boxIndexFontColor = new Color(222, 216, 172, 125);
	
	public static Color default_panelBackgroundColor = new Color(-4206902, true);
	
	public static Color default_sameUnitHighlightColor = new Color(79, 139, 231);
	public static Color default_sameBoxUnitHighlightColor = new Color(137, 179, 233);
	public static Color default_anyHighlightColor = new Color(197, 237, 255);
	
	public static Color[] default_candidateHighlightColors = new Color[] 
	{ 
		new Color(230, 230, 230), new Color(255, 241, 104), new Color(208, 255, 231),
		new Color(227, 255, 143), new Color(255, 167, 162), new Color(255, 199, 219),
		new Color(144, 185, 255),  new Color(144, 252, 148), new Color(192, 167, 255) 
	};

	public static Color default_noCandidateColor = new Color(192, 167, 255); // same as highlight 9
	public static Color default_repeatedCandidateColor = new Color(255, 167, 162); // same as highlight 5
	public static  Color default_eliminatedCandidateColor = new Color(255, 167, 162); // same as highlight 5
	public static Color default_onlyCandidateColor = new Color(144, 252, 148); // same as highlight 8
	
	public static Color default_differentValueColor = new Color(255, 241, 104); // same as highlight 2
	public static Color default_hasUniqueValueColor = new Color(144, 252, 148); // same as highlight 8

	public void exportToTargetFile(File file, SettingsPanel settingsPanel) throws IOException, BackingStoreException
	{
		Theme newTheme = new Theme(file, settingsPanel);
		settingsPanel.saveSettings(newTheme, true);

		try (FileOutputStream out = new FileOutputStream(file))
		{
			newTheme.node.exportSubtree(out);
		}
	}
	
	/**
	 * This is only called by 'restore and reset all default themes'
	 */
	public Theme newDefaultFromExistingFile(File file) throws IOException, InvalidPreferencesFormatException
	{
		return new Theme(file, true, settingsPanel);
	}
	
	/**
	 * Awkwardly doing this requires setting the settingsPanel's values to the file's stored values
	 */
	public Theme newFromExistingFile(File file, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults, SettingsPanel settingsPanel) throws IOException, InvalidPreferencesFormatException
	{
		Theme tmp = new Theme(file, false, settingsPanel);
		settingsPanel.loadSettings(tmp);
		
		String newFileNameNoRepeats = SettingsFile.getNewFileName(PreferenceFrame.removeFileExtension(file), defaults, customs, application_defaults, true);
		
		File newFile = new File(ThemesPanel.customs_folder, newFileNameNoRepeats + ".xml");
		Theme newTheme = new Theme(newFile, settingsPanel);
		settingsPanel.saveSettings(newTheme, true);
		newTheme.save();
		return newTheme;
	}
	
	public Theme newFromDefault(String newFileName) 
	{
		return new Theme(new File(ThemesPanel.customs_folder, newFileName + ".xml"), settingsPanel);
	}
	
	public Theme(File file, SettingsPanel settingsPanel)
	{
		super(file, settingsPanel);
	}
	
	public Theme(File file, boolean isDefault, SettingsPanel settingsPanel) throws IOException, InvalidPreferencesFormatException
	{
		super(file, isDefault, settingsPanel);
	}
	
	public Theme duplicate(String newFileName, SettingsPanel settingsPanel, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults) throws IOException, InvalidPreferencesFormatException, BackingStoreException
	{
		String newFileNameNoRepeats = SettingsFile.getNewFileName(newFileName, defaults, customs, application_defaults, true);
		File newFile = new File(ThemesPanel.customs_folder, newFileNameNoRepeats + ".xml");

		Theme newTheme = new Theme(newFile, settingsPanel);
		settingsPanel.saveSettings(newTheme, true);
		newTheme.save();
		return newTheme;
	}
}