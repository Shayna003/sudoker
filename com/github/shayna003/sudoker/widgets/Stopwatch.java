package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.logging.*;

/**
 * @version 0.00 
 * @since 1-4-2021
 */
@SuppressWarnings("CanBeFinal")
public class Stopwatch extends JPanel implements ActionListener
{
	public boolean flashTimeSeparators;
	boolean timeSeparatorsVisible;
	
	Font timerFont;
	public JLayeredPane digitsPane;
	public JLabel label;
	public JLabel ghostLabel;
	public Timer timer;
	
	public int hours;
	public int minutes;
	public int seconds;
	
	public JPanel stopwatchPanel;
	public JPanel digitsPanel;
	public JPanel buttonPanel;
	public JButton start;
	public JButton reset;
	public SettingsButton settingsButton;

	@Override
	public void updateUI()
	{
		super.updateUI();
		if (digitsPanel != null)
		{
			digitsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getBackground(), 10), BorderFactory.createBevelBorder(BevelBorder.LOWERED)));
		}
	}
	
	/**
	 * Fonts created from FontStruct.com aren't smart enough. Either they are monospaced, or have a global spacing
	 * automatically added after every character(including the last one) or have no spacing between them at all.
	 * And this messes up the getStringBounds function be returning a rectangle that includes the added space.
	 * Therefore I set the font to have no spaces between characters, and thus I need to manually add spaces to create spacing between the characters.
	 */
	public static String insertSpaces(String s)
	{
		StringBuilder b = new StringBuilder("   ");
		for (int i = 0; i < s.length(); i++)
		{
			b.append(s.charAt(i));
			b.append("    ");
		}
		return b.toString();
	}
	
	public void setText()
	{	
		if (!flashTimeSeparators || timeSeparatorsVisible)
			label.setText(insertSpaces(String.format("%02d:%02d:%02d", hours, minutes, seconds)));
		else label.setText(insertSpaces(String.format("%02d %02d %02d", hours, minutes, seconds)));
	}
	
	public void actionPerformed(ActionEvent event)
	{
		if (seconds == 59)
		{
			seconds = 0;
			if (minutes == 59)
			{
				minutes = 0;
				if (hours == 99)
				{
					hours = 0;
				}
				else
				{
					hours++;
				}
			}
			else
			{
				minutes++;
			}
		}
		else
		{
			seconds++;
		}

		if (flashTimeSeparators) timeSeparatorsVisible = !timeSeparatorsVisible;
		setText();
	}
	
	public void setTime(int hours, int minutes, int seconds)
	{
		boolean isRunning = timer.isRunning();
		if (isRunning) timer.stop();
		this.hours = hours;
		this.minutes = minutes;
		this.seconds = seconds;
		setText();
		if (isRunning) timer.start();
	}
	
	public Stopwatch()
	{
		flashTimeSeparators = Application.miscellaneousSettingsPanel.flashTimeSeparators.isSelected();
		timeSeparatorsVisible = true;
		try
		{
			timerFont = Font.createFont(Font.TRUETYPE_FONT, ApplicationLauncher.class.getResourceAsStream("resources/stopwatch-digits.ttf")).deriveFont(((Integer) Application.miscellaneousSettingsPanel.stopwatchFontSize.getValue()).floatValue());
		}
		catch (FontFormatException | IOException | NullPointerException e)
		{
			Application.prefsLogger.logp(Level.SEVERE, getClass().toString(), "init", "Error when loading stopwatch font", e);
			timerFont = new Font("Monospace", Font.BOLD, (Integer) Application.miscellaneousSettingsPanel.stopwatchFontSize.getValue());
		}
		
		label = new JLabel(insertSpaces("00:00:00"), SwingConstants.CENTER);
		ghostLabel = new JLabel(insertSpaces("88:88:88"), SwingConstants.CENTER);
		label.setFont(timerFont);
		label.setForeground(Application.miscellaneousSettingsPanel.stopwatchFontColor.color);
		ghostLabel.setFont(timerFont);
		ghostLabel.setForeground(Application.miscellaneousSettingsPanel.ghostLabelColor.color);
		digitsPane = new JLayeredPane();
		label.setSize(label.getPreferredSize());
		ghostLabel.setSize(ghostLabel.getPreferredSize());
		digitsPane.setPreferredSize(label.getPreferredSize());
		
		//using two JPanels to get the correct resize behaviour
		digitsPane.add(ghostLabel, JLayeredPane.FRAME_CONTENT_LAYER);
		digitsPane.add(label, JLayeredPane.DEFAULT_LAYER); 
		stopwatchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		stopwatchPanel.add(digitsPane);
		stopwatchPanel.setBackground(Application.miscellaneousSettingsPanel.stopwatchBackgroundColor.color);

		digitsPanel = new JPanel(new BorderLayout());
		digitsPanel.setBackground(Application.miscellaneousSettingsPanel.stopwatchBackgroundColor.color);
		digitsPanel.add(stopwatchPanel);

		digitsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getBackground(), 10), BorderFactory.createBevelBorder(BevelBorder.LOWERED)));
		
		timer = new Timer(1000, this);
		
		start = new JButton("Start");
		AbstractAction startAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent event)
			{
				if (timer.isRunning()) 
				{
					stop();
				}
				else 
				{
					start();
				}
			}
		};
		start.addActionListener(startAction);
		
		reset = new JButton("Reset");
		AbstractAction resetAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent event)
			{
				reset();
			}
		};
		reset.addActionListener(resetAction);
		
		settingsButton = new SettingsButton("Stopwatch Settings", Application.miscellaneousSettingsPanel);
		
		buttonPanel = new JPanel();
		buttonPanel.add(start);
		buttonPanel.add(reset);
		buttonPanel.add(settingsButton);
		
		setLayout(new BorderLayout());
		add(digitsPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("startStopwatch", KeyboardSettingsPanel.getMenuItemString("Stopwatch", "Start/Pause Stopwatch"), false, 0, 0, startAction, Stopwatch.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		Application.keyboardSettingsPanel.registerOtherShortcut("resetStopwatch", KeyboardSettingsPanel.getMenuItemString("Stopwatch", "Reset Stopwatch"), false, 0, 0, resetAction, Stopwatch.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public void restart()
	{
		reset();
		start();
	}

	public void reset()
	{
		hours = 0;
		minutes = 0;
		seconds = 0;
		setText();
	}

	public void start()
	{
		timer.start();
		start.setText("Pause");
	}
	
	public void stop()
	{
		timer.stop();
		if (flashTimeSeparators && !timeSeparatorsVisible)
		{
			timeSeparatorsVisible = true;
			setText();
		}
		start.setText("Start");
	}
}