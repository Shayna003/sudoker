package com.github.shayna003.sudoker.swingComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

/**
 * This class is used when component layout should not change after 
 * The text of the label changes in a predictable way
 * For example, when used with a JSlider
 * @since 3-7-2021
 */
@SuppressWarnings("CanBeFinal")
public class FixedSizeLabel extends JLabel
{
	String maxLengthText;

	public Dimension getPreferredSize()
	{
		FontRenderContext context = getFontMetrics(getFont()).getFontRenderContext();
		Rectangle2D bounds = getFont().getStringBounds(maxLengthText, context);
		return new Dimension((int) (bounds.getWidth()), (int) super.getPreferredSize().getHeight());
	}
	
	public FixedSizeLabel(String text, String maxLengthText)
	{
		super(text);
		this.maxLengthText = maxLengthText;
	}
}