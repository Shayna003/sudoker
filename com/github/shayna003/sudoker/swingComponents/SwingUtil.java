package com.github.shayna003.sudoker.swingComponents;

import java.net.URL;
import java.util.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Don't know where to put these functions
 * @version 0.00 12-9-2020
 */
public class SwingUtil
{
	/**
	 * To prevent NullPointerExceptions
	 */
	public static ImageIcon getImageIcon(URL url)
	{
		if (url == null) return new ImageIcon();
		else return new ImageIcon(url);
	}

	/**
	 * @param alphaValue a value between 0 and 255
	 */
	public static JLabel makeTranslucentLabel(String text, int alphaValue)
	{
		return new JLabel(text)
		{
			int alpha = alphaValue;

			public void setAlpha(int newValue)
			{
				alpha = newValue;
				repaint();
			}

			@Override
			public void paintComponent(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g;

				// to be able to adapt to different look and feels
				Color foreground = getForeground();
				g2.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), alpha));
				FontRenderContext context = g2.getFontRenderContext();
				Rectangle2D bounds = g2.getFont().getStringBounds(getText(), context);
				g2.drawString(getText(), (float) ((getWidth() - bounds.getWidth()) / 2), (float) (-bounds.getY() +(getHeight() - bounds.getHeight()) / 2));
			}
		};
	}
	
	public static Color compoundDeriveColor(Color c, int addValue, float multiplier)
	{
		assert multiplier >= 0 : multiplier;
		return new Color(compoundDeriveColor(c.getRed(), addValue, multiplier), compoundDeriveColor(c.getGreen(), addValue, multiplier), compoundDeriveColor(c.getBlue(), addValue, multiplier), c.getAlpha());
	}
	
	public static Color deriveColor(Color c, float multiplier)
	{
		assert multiplier >= 0 : multiplier;
		return new Color(deriveColor(c.getRed(), multiplier), deriveColor(c.getGreen(), multiplier), deriveColor(c.getBlue(), multiplier), c.getAlpha());
	}
	
	public static int compoundDeriveColor(int value, int addValue, float multiplier)
	{
		return ((value + addValue) * multiplier > 255) ? 255 : (int) ((value + addValue) * multiplier);
	}
	
	public static int deriveColor(int value, float multiplier)
	{
		return (value * multiplier > 255) ? 255 : (int) (value * multiplier);
	}
	
	public static AlphaComposite makeComposite(float alpha) 
	{
		int type = AlphaComposite.SRC_OVER; //SRC_OVER
		return(AlphaComposite.getInstance(type, alpha));
	}
	
	public static Rectangle2D getStringBounds(String s, Font f, FontRenderContext context)
	{
		return f.getStringBounds(s, context);
	}
	
	public static Rectangle2D getStringBounds(JComponent c, String s, Font f)
	{
		return f.getStringBounds(s, c.getFontMetrics(f).getFontRenderContext());
	}
	
	public static ArrayList<Rectangle2D> getStringBounds(String[] texts, Font f, FontRenderContext context)
	{
		ArrayList<Rectangle2D> bounds = new ArrayList<>();
		for (int i = 0; i < texts.length; i++)
		{
			bounds.add(f.getStringBounds(texts[i], context));
		}
		return bounds;
	}
	
	public static Dimension getMaxDimension(ArrayList<Rectangle2D> bounds)
	{
		int max_width = (int) bounds.get(0).getWidth();
		int max_height = (int) bounds.get(0).getHeight();
		for (int i = 0; i < bounds.size(); i++)
		{
			if ((int) bounds.get(i).getWidth() > max_width) max_width = (int) bounds.get(i).getWidth();
			if ((int) bounds.get(i).getHeight() > max_height) max_height = (int) bounds.get(i).getHeight();
		}
		return new Dimension(max_width, max_height);
	}
}