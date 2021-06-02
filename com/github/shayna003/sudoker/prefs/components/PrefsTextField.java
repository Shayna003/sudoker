package com.github.shayna003.sudoker.prefs.components;

import javax.swing.*;
import javax.swing.event.*;
import java.util.prefs.*;

/**
 * A JTextField with convenient functions to save and load its text.
 * @since 3-2-2021
 */
@SuppressWarnings("CanBeFinal")
public class PrefsTextField extends JTextField implements PrefsComponent
{
	String settingName;
	String defaultText;
	
	public void resetToDefault()
	{
		setText(defaultText);
	}

	/**
	 * @return self
	 */
	public PrefsTextField setToolTip(String text)
	{
		setToolTipText(text);
		return this;
	}

	public PrefsTextField(String settingName, String defaultText, int columns, DocumentListener listener)
	{
		this(settingName, defaultText, columns);
		getDocument().addDocumentListener(listener);
	}
	
	public PrefsTextField(String settingName, String defaultText, int columns)
	{
		super(columns);
		this.settingName = settingName;
		this.defaultText = defaultText;
		
		// for GridBagLayout
		setMinimumSize(getPreferredSize());
	}
	
	public void saveSettings(Preferences node)
	{
		node.put(settingName, getText());
	}
	
	public void loadSettings(Preferences node)
	{
		setText(node.get(settingName, defaultText));
	}
}