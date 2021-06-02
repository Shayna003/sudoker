package com.github.shayna003.sudoker.prefs.components;

import java.util.prefs.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * @since 2-27-2021
 * This class is useful because java's ButtonGroup class does not deselect other buttons
 * when you explicitly call a button's setSelected() method.
 * It also takes care of loading and saving data when there only needs to be one preference for the whole
 * group of buttons.
 * I still need ButtonGroup because I can't find a way to "veto" the to-be-deselected button that is already selected
 */
@SuppressWarnings("CanBeFinal")
public class PrefsButtonGroup extends ButtonGroup implements PrefsComponent
{
	public int selectedButton; // index of selectedButton
	String settingName;
	ChangeListener listener; // added to all buttons in the button group
	int defaultInitialSelection;
	
	public AbstractButton getButton(int index)
	{
		return buttons.get(index);
	}
	
	@Override
	public void resetToDefault()
	{
		selectedButton = defaultInitialSelection;
		buttons.get(selectedButton).setSelected(true);
	}
	
	public void saveSettings(Preferences node)
	{
		node.putInt(settingName, selectedButton);
	}
	
	public void loadSettings(Preferences node)
	{
		selectedButton = node.getInt(settingName, defaultInitialSelection);
		buttons.get(selectedButton).setSelected(true);
	}
	
	@Override
	public void add(AbstractButton b)
	{
		super.add(b);
		b.addChangeListener(event ->
		{
			if (b.isSelected() && buttons.get(selectedButton) != b)
			{
				for (int i = 0; i < buttons.size(); i++)
				{
					if (buttons.get(i) == b) 
					{
						selectedButton = i;
						break;
					}
				}
				if (listener != null) listener.stateChanged(event);
			}
		});
	}
	
	/**
	 * Adds listener to all of  {@code buttons}.
	 * If a button's deselection should trigger disabling some components, add a ChangeListener
	 * outside this class
	 */
	public PrefsButtonGroup(ChangeListener listener, String settingName, int defaultInitialSelection, AbstractButton... buttons)
	{
		this.settingName = settingName;
		this.listener = listener;
		this.defaultInitialSelection = defaultInitialSelection;
		
		for (AbstractButton b : buttons)
		{
			add(b);
		}
	}
}