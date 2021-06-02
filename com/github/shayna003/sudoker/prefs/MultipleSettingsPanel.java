package com.github.shayna003.sudoker.prefs;

import java.io.*;

/**
 * All tabs using a SettingFileChooserPanel in the PreferenceFrame should implement this interface
 */
public interface MultipleSettingsPanel extends SettingsPanel
{
	/**
	 * Called to load settings into Preferences nodes from files.
	 */
	void loadSettingsFiles();
	
	/*
	 * SettingsFileChooserPanel should handle this. Pass this call to the SettingsFileChooserPanel if is using one.
	 */
	void saveSettingsToFiles();

	/**
	 * @return the InputStream obtained by xx.class.getResourceAsStream(...)
	 */
	InputStream getDefaultSetting(String settingName);
}