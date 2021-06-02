package com.github.shayna003.sudoker.prefs.components;

import java.util.prefs.*;

/**
 * Classes of this interface are swing components that store data that can be changed by the user.
 * These data can be loaded from and saved to Preference nodes linked with physical files.
 * @since 2-27-2021
 */
public interface PrefsComponent
{
	void loadSettings(Preferences node);
	
	void saveSettings(Preferences node);
	
	void resetToDefault();
}