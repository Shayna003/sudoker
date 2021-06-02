package com.github.shayna003.sudoker.prefs;

/**
 * Different from SettingsPanel, this class only uses one settingFile
 * All tabs in PreferenceFrame should implement this interface or MultipleSettingsPanel
 * @since 1-30-2021
 */
public interface SettingsPanel
{
	/**
	 * Called to apply setting changes
	 */
	void applyChanges();
	
	/**
	 * Save settings to a settings file
	 * Put settings stored in JComponents into the file's preference node
	 * @param saveToFile if true, then saves the node's data onto a physical file
	 */
	void saveSettings(SingleSettingsFile file, boolean saveToFile);
	
	/**
	 * Load settings from a preference node
	 */
	void loadSettings(SingleSettingsFile file);

	SingleSettingsFile getSettingsFile();
}