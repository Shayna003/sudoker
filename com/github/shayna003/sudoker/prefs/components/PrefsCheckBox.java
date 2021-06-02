package com.github.shayna003.sudoker.prefs.components;

import javax.swing.*;
import java.util.prefs.*;

/**
 * A JCheckBox that support saving and loading preferences.
 * @since 2-26-2021
 */
@SuppressWarnings("CanBeFinal")
public class PrefsCheckBox extends JCheckBox implements PrefsComponent
{
	String settingName;
	boolean defaultValue;
	
	public void resetToDefault()
	{
		setSelected(defaultValue);
	}
	
	public PrefsCheckBox(String settingName, boolean defaultValue)
	{
		super();
		this.settingName = settingName;
		this.defaultValue = defaultValue;
	}
	
	public PrefsCheckBox(String settingName, String text, boolean defaultValue)
	{
		super(text);
		this.settingName = settingName;
		this.defaultValue = defaultValue;
	}
	
	public PrefsCheckBox(String settingName, String text, boolean selected, boolean defaultValue)
	{
		super(text, selected);
		this.settingName = settingName;
		this.defaultValue = defaultValue;
	}
	
	public PrefsCheckBox(String settingName, Action action, boolean defaultValue)
	{
		super(action);
		this.settingName = settingName;
		this.defaultValue = defaultValue;
	}
	
	public void saveSettings(Preferences node)
	{
		node.putBoolean(settingName, isSelected());
	}

	public void loadSettings(Preferences node)
	{
		setSelected(node.getBoolean(settingName, defaultValue));
	}
}