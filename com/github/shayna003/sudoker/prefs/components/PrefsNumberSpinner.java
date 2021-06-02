package com.github.shayna003.sudoker.prefs.components;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.prefs.*;

/**
 * This JSpinner deals with numeric values
 * @since 4-18-2021
 */
@SuppressWarnings("CanBeFinal")
public class PrefsNumberSpinner extends JSpinner implements PrefsComponent
{
	String settingName;
	int defaultValue;
	
	public PrefsNumberSpinner(String settingName, int minValue, int maxValue, int stepSize, int defaultValue, ChangeListener listener, int columns)
	{
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(defaultValue, minValue, maxValue, stepSize);
		setModel(spinnerModel);
		JComponent editorComponent = getEditor();
		JFormattedTextField field = (JFormattedTextField) editorComponent.getComponent(0);
		field.setColumns(columns);
		DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
		formatter.setCommitsOnValidEdit(true);
		formatter.setAllowsInvalid(false);
		addChangeListener(listener);
		setToolTipText("Please enter a value between " + minValue + " and " + maxValue + ".");
		
		this.defaultValue = defaultValue;
		this.settingName = settingName;
	}
	
	public void loadSettings(Preferences node)
	{
		setValue(node.getInt(settingName, defaultValue));
	}
	
	public void saveSettings(Preferences node)
	{
		node.putInt(settingName, (Integer) getValue());
	}
	
	public void resetToDefault()
	{
		setValue(defaultValue);
	}
}