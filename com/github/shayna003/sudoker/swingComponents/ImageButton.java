package com.github.shayna003.sudoker.swingComponents;

import com.github.shayna003.sudoker.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;

/**
 * A button that displays an image with no background
 * @version 0.00 12-18-2020
 */
@SuppressWarnings("CanBeFinal")
public class ImageButton extends JButton
{
	boolean isPressed;
	boolean showingPressed;
	ImageIcon normal;
	ImageIcon pressed;
	
	public Dimension getPreferredSize()
	{
		return new Dimension(normal.getIconWidth(), normal.getIconHeight());
	}
	
	public ImageButton(ImageIcon normal, ImageIcon pressed)
	{
		this.normal = normal;
		this.pressed = pressed;
		isPressed = false;
		showingPressed = false;
		
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent event)
			{
				if (isEnabled())
				{
					isPressed = true;
					repaint();
				}
			}
			
			public void mouseReleased(MouseEvent event)
			{
				if (isEnabled())
				{
					isPressed = false;
					repaint();
				}
			}
		});
	}
	
	public void paint(Graphics g)
	{
		paintComponent(g);
	}
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		if (!isEnabled())
		{
			g2.setComposite(SwingUtil.makeComposite(0.3f));
		}
		
		g2.drawImage(isPressed || showingPressed ? pressed.getImage() : normal.getImage(), 0, 0, null);
	}
	
	// used as visual clue for keyboard actions that trigger the function of this button
	public void showPress()
	{
		new Thread(() ->
		{
			showingPressed = true;
			repaint();
			
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e) 
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "showPress", "Error when showing button press", e);
			}

			showingPressed = false;
			repaint();
		}).start();
	}
}