package com.github.shayna003.sudoker.prefs.components;

import java.util.prefs.*;
import javax.swing.*;
import java.util.function.*;

@SuppressWarnings("CanBeFinal")
public class PrefsComboBox<E> extends JComboBox<E> implements PrefsComponent
{
	public String settingName;
	Predicate<E> defaultSettingPredicate;
	
	public void resetToDefault()
	{
		if (defaultSettingPredicate != null)
		{
			for (int i = 0; i < getItemCount(); i++)
			{
				if (defaultSettingPredicate.test(getItemAt(i)))
				{
					setSelectedIndex(i);
					return;
				}
			}
		}
	}
	
	public PrefsComboBox(String settingName, Predicate<E> defaultSettingPredicate)
	{
		super();
		this.settingName = settingName;
		this.defaultSettingPredicate = defaultSettingPredicate;
	}
	
	public PrefsComboBox(String settingName, Predicate<E> defaultSettingPredicate, E[] items)
	{
		super(items);
		this.settingName = settingName;
		this.defaultSettingPredicate = defaultSettingPredicate;
	}
	
	public void loadSettings(Preferences node)
	{
		String s = node.get(settingName, null);

		if (s != null)
		{
			setSelectedItem(s);
		}
		else if (defaultSettingPredicate != null)
		{
			for (int i = 0; i < getItemCount(); i++)
			{
				if (defaultSettingPredicate.test(getItemAt(i)))
				{
					setSelectedIndex(i);
					return;
				}
			}
		}
	}
	
	public void saveSettings(Preferences node)
	{
		node.put(settingName, getItemAt(getSelectedIndex()).toString());
	}
}