package com.github.shayna003.sudoker.prefs.components;

import com.github.shayna003.sudoker.Application;
import com.github.shayna003.sudoker.swingComponents.RoundButton;
import com.github.shayna003.sudoker.swingComponents.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * This class has slightly modified its visuals compared with RoundButton to make the gear emoji larger.
 * last modified: 5-6-2021
 */
public class SettingsButton extends RoundButton
{
	@Override 
	public void setFont(Font font)
	{
		super.setFont(font == null ? null : font.deriveFont(font.getSize() * 1.5f));
	}
	
	@Override
	public void setButtonSize()
	{
		Rectangle2D bounds = SwingUtil.getStringBounds(this, getText(), getFont());
		double diameter = Math.max(bounds.getWidth(), bounds.getHeight());
		button.setFrame(border_width, border_width, diameter, diameter);
		border.setFrame(0, 0, diameter + border_width * 2, diameter + border_width * 2);
	}
	
	public SettingsButton(String toolTipText, JComponent settingsPanel)//, Component settingsComponent)//, ActionListener listener)
	{
		//super("\u2692"); // hammer and pick
		super();
		setText("\u2699"); // a gear emoji that looks too small
		setButtonSize();
		
		addActionListener(event ->
		{
			Application.preferenceFrame.showUp(settingsPanel);
		});
		setToolTipText(toolTipText);
	}
	
	public SettingsButton(String toolTipText, JComponent settingsPanel, JComponent settingsComponent)
	{
		super();
		setText("\u2699");
		setButtonSize();
		
		addActionListener(event ->
		{
			// scroll to make settingsComponent visible
			Application.preferenceFrame.showUp(settingsPanel, settingsComponent);
		});
		setToolTipText(toolTipText);
	}
}