package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.PreferenceFrame;
import com.github.shayna003.sudoker.swingComponents.SwingUtil;
import com.github.shayna003.sudoker.util.IO;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.*;
import java.awt.*;

/**
 * Launches the application
 * Should be the main class in the manifest of the executable jar file
 * Shows a splash screen, loads stuff and brings up the home screen
 * Perhaps also give an option to take command line input or loads some preference or something
 * to directly go to a mode
 * use -ea:sudoku... to turn all assertions on

	Perhaps a draft of program flow:
	
	-Show SplashScreen while JVM loads? use Class.forName().load (see core java tip) and show what's being loaded
	-Locate the preference folder from BackingStore preference
	-Init things like look and feel, as well as JFrame and JDialog's setDefaultLookAndFeelDecorated()
	-perhaps set settingsFont
	-create PreferenceFrame
	
	(-create and show home frame)
	(-create Application frame)
	(-load previously open windows)
	
	
	
	When exiting program:
	
	(-show dialog to warn unsaved changes?)
	(-save preference frame's data if currently open)
	(-save music player's data if currently open)
	(-save open window data)
		(-save stuff like window size and location)
		(-save sudoku data)
	
 * @since 0.00 2020-11-1

 Commands: java -verbose:gc -Xlog:gc* -ea:sudoku... -cp "${compiler%:*}" "${compiler#*:}"

 -ea:sudoku... turns on assertion for all classes in sudoku package

 These two print Garbage Collection activity to the console
 -verbose:gc 
 -Xlog:gc*

 The original command in CodeRunner:
 java -cp "${compiler%:*}" "${compiler#*:}" 

 */
public class ApplicationLauncher 
{
	static SplashScreen splashScreen;

	static void paintSplashScreen(String text)
	{
		if (splashScreen == null) return;
		try
		{
			Graphics2D g2 = splashScreen.createGraphics();
			if (g2 == null)
			{
				return;
			}
			g2.setComposite(AlphaComposite.Clear);
			g2.fillRect(0,0,600,400);
			g2.setPaintMode();

			g2.setComposite(SwingUtil.makeComposite(0.7f));
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g2.setColor(Color.RED);
			g2.setFont(g2.getFont().deriveFont(48f));
			g2.drawString("Sudoker Is Loading...", 80, 180);

			g2.setFont(g2.getFont().deriveFont(Font.PLAIN).deriveFont(18f));
			g2.drawString(text + "...", 80, 240);
			splashScreen.update();
		}
		catch (IllegalStateException e) // might be triggered if Application shows popup to ask for location of Application Files Folder
		{
			return;
		}
	}

	public static void main(String[] args)
	{
		splashScreen = SplashScreen.getSplashScreen();
		paintSplashScreen("Loading");
		if (splashScreen == null)
		{
			JOptionPane.showMessageDialog(null, "Splash Screen is null");
		}

		EventQueue.invokeLater(() ->
		{	
			Application.loadSettings();

			Runtime.getRuntime().addShutdownHook(new Thread(() ->
			{
				Application.prefsLogger.log(Level.INFO, "Shut down hook triggered");
				if (Application.needToSaveWhenShutDown)
				{
					Application.prefsLogger.log(Level.INFO, "Shut down hook saving");
					Application.saveSettings();
					if (Application.generalSettingsPanel.restorePreviousSession.isSelected())
					{
						Application.saveAllTabData();
					}
				}
			}));
			
			long start = System.currentTimeMillis();

			ApplicationLauncher.paintSplashScreen("Creating Application Frame");
			ApplicationFrame frame = Application.createNewWindow("Window 1");

			if (Application.generalSettingsPanel.restorePreviousSession.isSelected())
			{
				if (!Application.dataFolder.exists()) Application.dataFolder.mkdirs();
				File[] files = Application.dataFolder.listFiles();

				if (files != null)
				{
					Arrays.sort(files, Comparator.comparingInt(f -> getTabNumber(f)));

					for (File f : files)
					{
						if (f.getName().startsWith("Tab"))
						{
							BoardData data = IO.readBoardFromFile(f);
							Application.addTab(frame, data, Board.RESTORE_FROM_FILE, getTabName(f));
							f.delete();
						}
					}
				}
			}

			if (frame.tabbedPane.getTabCount() == 0)
			{
				Application.addTab(frame, new Sudoku(), "New Tab with Empty Board", "Tab 1");
			}
			frame.pack();
			frame.setLocationRelativeTo(null);
			if (splashScreen != null)
			{
				try
				{
					splashScreen.close();
				}
				catch (IllegalStateException e) // might be triggered if Application shows popup to ask for location of Application Files Folder
				{
				}
			}
			frame.setVisible(true);
			Application.loadTimeLogger.log(Level.CONFIG, "time to make an ApplicationFrame: " + (System.currentTimeMillis() - start));
		});
	}

	static String getTabName(File file)
	{
		String name = "";
		try
		{
			name = PreferenceFrame.removeFileExtension(file);
			return name.substring(0, 3) + " " + name.substring(3);
		}
		catch (StringIndexOutOfBoundsException e) { return name; }
	}

	static int getTabNumber(File file)
	{
		if (file.getName().startsWith("Tab"))
		{
			try
			{
				return IO.getInt(PreferenceFrame.removeFileExtension(file).substring(3));
			}
			catch (StringIndexOutOfBoundsException e) { return -1; }
		}
		else
		{
			return -1;
		}
	}
}