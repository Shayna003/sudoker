package com.github.shayna003.sudoker.swingComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * This class provides an animation of a text scrolling to the left
 * When the width of the text is wider than this component's preferred size.
 * Note that a font must be set prior to the call to setText()
 * @version 0.00 12-23-2020
 */
@SuppressWarnings("CanBeFinal")
public class ScrollingText extends JComponent
{
	String text = "";
	Rectangle2D bounds;
	Timer animation;
	int position;
	boolean needScrolling;
	boolean isScrolling;
	int gap = 10;
	int width = 200;
	
	public Dimension getPreferredSize()
	{
		return new Dimension(width, bounds == null ? 0 :(int) (bounds.getHeight() * 1.2));
	}
	
	public void setText(String s)
	{
		animation.stop();
		text = s;
		bounds = getFont().getStringBounds(text, getFontMetrics(getFont()).getFontRenderContext());
		revalidate();
		needScrolling = 200 < bounds.getWidth();
		position = needScrolling ? gap : (int) ((width - bounds.getWidth()) / 2);
		repaint();
	}
	
	public void startAnimation()
	{
		if (needScrolling) 
		{
			animation.start();
			isScrolling = true;
		}
	}
	
	public void stopAnimation()
	{
		animation.stop();
		isScrolling = false;
	}
	
	public ScrollingText()
	{
		setFont(new Font("", Font.PLAIN, 14));
		
		//for giving a default height
		bounds = getFont().getStringBounds("PLACE HOLDER TEXT", getFontMetrics(getFont()).getFontRenderContext());
		animation = new Timer(20, event ->
		{	
			position = position <= -bounds.getWidth() - gap - (bounds.getWidth() - width + gap) ? width : position - 1;
			repaint();
		});
	}
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		g2.drawString(text, position, (int) (-bounds.getY() + (getHeight() - bounds.getHeight()) / 2));

		if (needScrolling)
		{
			g2.drawString(text, position <= 0 ? (int) (position + bounds.getWidth() + gap) : (int) (position - bounds.getWidth() - gap), (int) (-bounds.getY() + (getHeight() - bounds.getHeight()) / 2));
		}
	}
}