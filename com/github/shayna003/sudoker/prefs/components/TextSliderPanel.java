package com.github.shayna003.sudoker.prefs.components;

import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.prefs.*;
import java.awt.*;

/**
 * A panel that contains a slider with a text that changes as slider value gets changed
 * The maximum size of the text label is computed to ensure that the panel does not expand upon changing
 * the slider's value.
 */
@SuppressWarnings("CanBeFinal")
public class TextSliderPanel extends JPanel implements PrefsComponent
{
	public void resetToDefault()
	{
		setValue(default_value);
	}
	
	public class TextSlider extends JSlider
	{
		public TextSlider(int orientation, int min, int max, int value)
		{
			super(orientation, min, max, value);
			addChangeListener(event ->
			{
				TextSliderPanel.this.updateText();
			});
		}
		
		public void updateText()
		{
			label.setText(text + (displayNumber ? ": " + slider.getValue() : ""));
		}
	}
	
	String text;
	boolean displayNumber;
	int default_value;
	FixedSizeLabel label;
	public TextSlider slider;
	public String settingName;

	public void updateText()
	{
		slider.updateText();
	}
	
	public void setValue(int value)
	{
		value = value > slider.getMaximum() ? slider.getMaximum() : Math.max(value, slider.getMinimum());
		if (value != slider.getValue())
		{
			slider.setValue(value);
			slider.updateText();
		}
	}
	
	public int getValue()
	{
		return slider.getValue();
	}
	
	@Override
	public void saveSettings(Preferences node)
	{
		node.putInt(settingName, slider.getValue());
	}
	
	@Override
	public void loadSettings(Preferences node)
	{
		setValue(node.getInt(settingName, default_value));
	}
	
	String maxLengthMessage;
	@Override
	public void setFont(Font font)
	{
		super.setFont(font);
		if (slider != null)

			slider.setFont(font);
		
		if (label != null)
		{
			label.setFont(font);
		}
		
		if (slider != null && slider.getLabelTable() != null)
		{
			Enumeration e = slider.getLabelTable().elements();
			while (e.hasMoreElements())
			{
				Object o = e.nextElement();
				if (o instanceof Component)
				{
					((Component) o).setFont(font);
				}
			}
		}
	}
	
	public TextSliderPanel(String settingName, int orientation, String text, int min, int max, int default_value, boolean paintTrack, boolean paintTicks, int majorTickSpacing, int minorTickSpacing, boolean paintLabels, Dictionary labelTable, boolean addTextBeforeSlider, boolean displayNumber, ChangeListener listener)
	{
		this.settingName = settingName;
		this.text = text;
		this.default_value = default_value;
		this.displayNumber = displayNumber;

		slider = new TextSlider(orientation, min, max, default_value);
		slider.addChangeListener(listener);
		slider.setPaintTicks(paintTicks);
		if (majorTickSpacing >= 0) slider.setMajorTickSpacing(majorTickSpacing);
		if (minorTickSpacing >= 0) slider.setMinorTickSpacing(minorTickSpacing);
		slider.setPaintLabels(paintLabels);
		
		if (labelTable != null) slider.setLabelTable(labelTable);
		maxLengthMessage = text + ": " + (labelTable == null ? String.valueOf(max) : "");
		label = new FixedSizeLabel(text + (displayNumber ? ": " + slider.getValue() : ""), maxLengthMessage);

		if (orientation == JSlider.VERTICAL)
		{
			//for alignment purposes
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
			JPanel p2 = new JPanel();
			p2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			p1.add(label);
			p2.add(slider);
			if (addTextBeforeSlider) add(p1, 0.5f);
			add(p2, 0.5f);
			if (!addTextBeforeSlider) add(p1, 0.5f);
		}
		else
		{
			if (addTextBeforeSlider) add(label);
			add(slider);
			if (!addTextBeforeSlider) add(label);
		}
	}
}