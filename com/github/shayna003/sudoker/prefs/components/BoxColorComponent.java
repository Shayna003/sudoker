package com.github.shayna003.sudoker.prefs.components;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.prefs.*;

/**
 * @version 0.00 1-18-2021
 * @since 11-12-2020
 * A convenience class to bring up 9 color choosers to colors by boxes
 * Can be further improved if I support r rows and c columns in the constructor
 */
@SuppressWarnings("CanBeFinal")
public class BoxColorComponent extends JPanel implements PrefsComponent
{
	public void resetToDefault()
	{
		for (ColorComponent c : colorComponents)
		{
			c.resetToDefault();
		}
	}
	
	ColorChooserDialogOwner chooserDialogOwner;
	public ColorComponent[] colorComponents = new ColorComponent[9];
	SettingsPanel settingsPanel;
	String settingName;

	@Override
	public void loadSettings(Preferences node)
	{
		for (int i = 0; i < 9; i++)
		{
			colorComponents[i].loadSettings(node);
		}
	}
	
	@Override
	public void saveSettings(Preferences node)
	{
		for (int i = 0; i < 9; i++)
		{
			colorComponents[i].saveSettings(node);
		}
	}
	
	public BoxColorComponent(String settingName, Color[] colors, Color defaultColor, SettingsPanel settingsPanel, ColorChooserDialogOwner chooserDialogOwner, String toolTipText, boolean modifyToolTipText)
	{
		this.settingName = settingName;
		this.settingsPanel = settingsPanel;
		this.chooserDialogOwner = chooserDialogOwner;

		setLayout(new GridLayout(3, 3, 0, 0));
		for (int i = 0; i < 9; i++) 
		{
			colorComponents[i] = new ColorComponent(settingName + "_" + i, colors[i], defaultColor, settingsPanel, chooserDialogOwner, 20, 20, new Color(250, 250, 250), new Color(159, 159, 159), new Color(119, 119, 119), new Color(170, 170, 170), 8, 1, modifyToolTipText ? (toolTipText + " for box " + Application.digitsAndIndexesPanel.getBoxIndex(i)) : (toolTipText + " " + (i + 1))/*Prefs.getBoxString(i)*/, i);
			add(colorComponents[i]);
		}
	}
	
	public BoxColorComponent(String settingName, Color[] colors, Color[] defaultColors, SettingsPanel settingsPanel, ColorChooserDialogOwner chooserDialogOwner, String toolTipText, boolean modifyToolTipText)
	{
		this.settingName = settingName;
		this.settingsPanel = settingsPanel;
		this.chooserDialogOwner = chooserDialogOwner;

		setLayout(new GridLayout(3, 3, 0, 0));
		for (int i = 0; i < 9; i++) 
		{
			colorComponents[i] = new ColorComponent(settingName + "_" + i, colors[i], defaultColors[i], settingsPanel, chooserDialogOwner, 20, 20, new Color(250, 250, 250), new Color(159, 159, 159), new Color(119, 119, 119), new Color(170, 170, 170), 8, 1, modifyToolTipText ? (toolTipText + " for box " + Application.digitsAndIndexesPanel.getBoxIndex(i)) : (toolTipText + " " + (i + 1))/*Prefs.getBoxString(i)*/, i);
			add(colorComponents[i]);
		}
	}
		
	public Dimension getPreferredSize()
	{
		return new Dimension((int) colorComponents[0].getPreferredSize().getWidth() * 3, (int) colorComponents[0].getPreferredSize().getHeight() * 3);
	}
	
	public void setColor(Color c, int index)
	{
		colorComponents[index].setColor(c);
		repaint();
	}
}