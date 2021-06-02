package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Contains one MusicPlayer. 
 * One application only has one instance of this class.
 * @since 5-4-2021
 */
@SuppressWarnings("CanBeFinal")
public class MusicPlayerFrame extends JFrame
{
	public MusicPlayer musicPlayer;
	
	public MusicPlayerFrame()
	{
		musicPlayer = new MusicPlayer(this);
		
		JPanel panel = new JPanel();
		panel.add(musicPlayer);
		add(panel);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(musicPlayer.options);
		setJMenuBar(menuBar);
		
		setTitle("\u266b Music Player");
		pack();
		GeneralSettingsPanel.registerComponentAndSetFontSize(this);
	}
	
	/**
	 * Things don't fit on the screen if you call JFrame.setDefaultLookAndFeelDecorated(true) 
	 * When the look and feel is Metal
	 */
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets i = getToolkit().getScreenInsets(getGraphicsConfiguration());
		int insetsWidth = i.left + i.right;
		int insetsHeight = i.top + i.bottom;
		return new Dimension(d.width + insetsWidth > screenSize.width ? screenSize.width - insetsWidth : d.width, d.height + insetsHeight > screenSize.height ? screenSize.height - insetsHeight : d.height);
	}
}