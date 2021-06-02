package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.generator.Generator;
import com.github.shayna003.sudoker.prefs.*;
import com.github.shayna003.sudoker.prefs.theme.*;
import com.github.shayna003.sudoker.prefs.keys.*;
import com.github.shayna003.sudoker.solver.AllSolutionsFrame;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.history.*;
import com.github.shayna003.sudoker.widgets.*;

import java.util.*;
import java.util.prefs.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class keeps track of which windows are open
 * And registers shutdown hooks as well as calls System.exit() at the appropriate time,
 * And stores objects that have only one instance per application such as PreferenceFrame
 * @since 1-19-2021
 */
@SuppressWarnings("CanBeFinal")
public class Application
{
	public static boolean settingsLoaded = false;
	
	public static String application_name = "Sudoker";
	public static final String application_version = "v0.4.0 alpha";
	
	public static Preferences applicationNode = Preferences.userNodeForPackage(Application.class);
	public static Preferences showDialogs;
	
	public static File applicationFilesFolder;
	public static File preferenceFolder;
	public static File dataFolder;
	public static File logsFolder;
	public static File exportsFolder; // the default location for exports and imports
	
	public static PreferenceFrame preferenceFrame;
	public static GeneralSettingsPanel generalSettingsPanel;
	public static SingleSettingsFile generalSettingsFile;
	public static ThemesPanel themesPanel;
	public static SolverSettingsPanel solverSettingsPanel;
	public static DigitsAndIndexesPanel digitsAndIndexesPanel;
	public static BoardSettingsPanel boardSettingsPanel;
	public static HistoryTreeSettingPanel historyTreeSettingPanel;
	public static KeyboardSettingsPanel keyboardSettingsPanel;
	public static MiscellaneousSettingsPanel miscellaneousSettingsPanel;
	
	public static Exporter exporter;
	public static Importer importer;
	public static Generator generator;
	public static HistoryTreeFrame historyTreeFrame;
	public static BoardComparatorFrame boardComparatorFrame;
	public static AboutDialog aboutDialog;
	public static JFrame memoryMonitorFrame;
	public static MusicPlayerFrame musicPlayerFrame;
	public static AllSolutionsFrame allSolutionsFrame;
	
	public static boolean prefs_initialized;//?

	public static UIManager.LookAndFeelInfo[] lookAndFeelInfos;
	
	public static OpenWindowsAndTabs openWindowsAndTabs; // only a visual representation of openWindows
	public static ArrayList<ApplicationFrame> openWindows;
	
	public static JFileChooser fileChooser;
	
	public static Action saveSettingsAction; // I guess a background thread would call this from time to time...
	public static Action exitProgram;
	
	public static ArrayList<Component> componentsToUpdateUI;

	public static AllSolutionsFrame getAllSolutionsFrame()
	{
		if (allSolutionsFrame == null)
		{
			allSolutionsFrame = new AllSolutionsFrame();
		}
		return allSolutionsFrame;
	}

	public static AboutDialog getAboutDialog()
	{
		if (aboutDialog == null)
		{
			aboutDialog = new AboutDialog();
		}
		return aboutDialog;
	}

	public static Exporter getExporter()
	{
		if (exporter == null)
		{
			exporter = new Exporter();
		}
		return exporter;
	}
	
	public static Importer getImporter()
	{
		if (importer == null)
		{
			importer = new Importer();
		}
		return importer;
	}

	public static Generator getGenerator()
	{
		if (generator == null)
		{
			generator = new Generator();
		}
		return generator;
	}
	
	public static MusicPlayerFrame getMusicPlayerFrame()
	{
		if (musicPlayerFrame == null)
		{
			musicPlayerFrame = new MusicPlayerFrame();
		}
		return musicPlayerFrame;
	}
	
	public static BoardComparatorFrame getBoardComparatorFrame()
	{
		if (boardComparatorFrame == null)
		{
			boardComparatorFrame = new BoardComparatorFrame();
		}
		return boardComparatorFrame;
	}
	
	/**
	 * Used for renaming a tab.
	 */
	public static String getNameForTab(Component parent, String initialValue)
	{
		return JOptionPane.showInputDialog(parent, "Enter new name for Tab: ", initialValue);
	}
	
	/**
	 * If name == null, i.e. user closed the dialog or clicked on "cancel", then new tab won't be created.
	 */
	public static String getNameForNewTab(Component parent, int currentTabCount)
	{
		return JOptionPane.showInputDialog(parent, "Enter name for new Tab: ", "Tab " + (currentTabCount + 1));
	}
	
	/**
	 * Used for renaming a window.
	 */
	public static String getNameForWindow(Component parent, String initialValue)
	{
		return JOptionPane.showInputDialog(parent, "Enter new name for Window: ", initialValue);
	}
	
	/**
	 * If name == null, i.e. user closed the dialog or clicked on "cancel", then new window won't be created.
	 */
	public static String getNameForNewWindow(Component parent)
	{
		return JOptionPane.showInputDialog(parent, "Enter name for new Window: ", "Window " + (openWindows.size() + 1));
	}
	
	/**
	 * Used by Cloning and such
	 */
	public static SudokuTab addTab(ApplicationFrame frame, Board b)
	{
		String tabTitle = getNameForNewTab(frame, frame.tabbedPane.getTabCount());
		if (tabTitle == null) return null;
		
		SudokuTab tab = new SudokuTab(frame, b);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}

	/**
	 * Used by Session Restore
	 */
	public static SudokuTab addTab(ApplicationFrame frame, BoardData data, int creationType, String title)
	{
		SudokuTab tab = new SudokuTab(frame, data, creationType);
		registerNewTab(frame, tab, title);
		return tab;
	}

	public static SudokuTab addTab(ApplicationFrame frame, BoardData data, int creationType)
	{
		String tabTitle = getNameForNewTab(frame, frame.tabbedPane.getTabCount());
		if (tabTitle == null) return null;

		SudokuTab tab = new SudokuTab(frame, data, creationType);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}

	/**
	 * Called by ApplicationLauncher
	 */
	public static SudokuTab addTab(ApplicationFrame frame, Sudoku sudoku, String creationEventDescription, String title)
	{
		SudokuTab tab = new SudokuTab(frame, sudoku, creationEventDescription);
		registerNewTab(frame, tab, title);
		return tab;
	}

	public static SudokuTab addTab(ApplicationFrame frame, Sudoku sudoku, String creationEventDescription)
	{
		String tabTitle = getNameForNewTab(frame, frame.tabbedPane.getTabCount());
		if (tabTitle == null) return null;

		SudokuTab tab = new SudokuTab(frame, sudoku, creationEventDescription);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}
	
	public static void closeTab(SudokuTab tab, boolean save)
	{
		if (tab.owner.tabbedPane.getTabCount() == 1)
		{
			closeWindow(tab.owner, false, save);
		}
		else 
		{
			closeTabOnly(tab, save, true);
		}
	}

	static String getUniqueFileNameForTab()
	{
		int suffix = 1;

		if (!dataFolder.exists()) dataFolder.mkdirs();
		File[] files = dataFolder.listFiles();
		if (files != null)
		{
			while (containsTabName(files, suffix))
			{
				suffix++;
			}
		}
		return "Tab" + suffix;
	}

	static boolean containsTabName(File[] files, int suffix)
	{
		for (File f : files) if (f.getName().equals("Tab" + suffix + ".dat")) return true;
		return false;
	}

	static void closeTabOnly(SudokuTab tab, boolean save, boolean calledDirectly)
	{
		if (save)
		{
			boolean success = Exporter.exportToFile(new File(dataFolder, getUniqueFileNameForTab()), Exporter.getFullDataString(tab.board), tab.owner, calledDirectly);
			if (!success)
			{
				if (calledDirectly)
				{
					return;
				}
				else JOptionPane.showMessageDialog(tab.owner, "Data for " + tab.getName() + " could not be saved.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		tab.owner.tabbedPane.remove(tab);
		tab.owner.historyTrees.remove(tab.historyTreePanel);
		
		openWindowsAndTabs.windowChanged(tab.owner);
		
		if (boardComparatorFrame != null)
		{
			boardComparatorFrame.tabWasClosed(tab);
		}
	}

	/**
	 * @param save only used if {@code calledDirectly } is true
	 */
	public static void closeWindow(ApplicationFrame frame, boolean calledDirectly, boolean save)
	{
		if (frame.tabbedPane.getTabCount() > 0) // ClosesableDndTabbedPane calls this method when tab count reaches 0 after dnd
		{
			int option;
			if (calledDirectly)
			{
				option = JOptionPane.showOptionDialog(frame, "Do you want to save data for the tabs in this window before closing?", "Closing Window", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Yes", "No", "Cancel"}, "Cancel");
				if (option < 0 || option > 1) return; // canceled
			}
			else option = save ? 0 : 1;

			while (frame.tabbedPane.getTabCount() > 0)
			{
				closeTabOnly((SudokuTab) frame.tabbedPane.getComponentAt(0), option == 0, false);
			}
		}
		
		openWindows.remove(frame);
		historyTreeFrame.windows.remove(frame.historyTrees);
		openWindowsAndTabs.rootChanged();
		frame.dispose();

		if (openWindows.size() == 0)
		{
			System.exit(0);
		}
	}
	
	/**
	 * Called by ApplicationFrame's "Move this tab to new window"
	 */
	public static ApplicationFrame createNewWindow(ApplicationFrame currentFrame)
	{
		String title = getNameForNewWindow(currentFrame);
		if (title == null) return null;
		return createNewWindow(title);
	}
	
	/**
	 * Only used by Session Restore
	 */
	static ApplicationFrame createNewWindow(String title)
	{
		ApplicationFrame frame = new ApplicationFrame();
		frame.setTitle(title);
		frame.setName(title);
		openWindows.add(frame);
		openWindowsAndTabs.addWindow(frame); // refreshes the tree
		frame.historyTrees = historyTreeFrame.makeTabbedPaneForWindow(frame);
		return frame;
	}
	
	static void registerNewTab(ApplicationFrame frame, SudokuTab tab, String tabTitle)
	{
		tab.setName(tabTitle);
		tab.historyTreePanel = historyTreeFrame.makeTreeForTab(tab);
		frame.tabbedPane.add(tab);
		openWindowsAndTabs.addTab(frame, tab); // refreshes the tree
		frame.tabbedPane.setSelectedIndex(frame.tabbedPane.getTabCount() - 1);
	}
	
	/**
	 * Used by Cloning and such
	 */
	public static SudokuTab createNewWindowWithTab(ApplicationFrame currentFrame, Board b)
	{
		String frameTitle = getNameForNewWindow(currentFrame);
		if (frameTitle == null) return null;
		
		String tabTitle = getNameForNewTab(currentFrame, 0);
		if (tabTitle == null) return null;
		
		ApplicationFrame frame = createNewWindow(frameTitle);
		
		SudokuTab tab = new SudokuTab(frame, b);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}

	public static SudokuTab createNewWindowWithTab(ApplicationFrame currentFrame, BoardData data, int creationType)
	{
		String frameTitle = getNameForNewWindow(currentFrame);
		if (frameTitle == null) return null;

		String tabTitle = getNameForNewTab(currentFrame, 0);
		if (tabTitle == null) return null;

		ApplicationFrame frame = createNewWindow(frameTitle);

		SudokuTab tab = new SudokuTab(frame, data, creationType);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}
	
	public static SudokuTab createNewWindowWithTab(ApplicationFrame currentFrame, Sudoku sudoku, String creationEventDescription)
	{
		String frameTitle = getNameForNewWindow(currentFrame);
		if (frameTitle == null) return null;
		
		String tabTitle = getNameForNewTab(currentFrame, 0);
		if (tabTitle == null) return null;
		
		ApplicationFrame frame = createNewWindow(frameTitle);
		
		SudokuTab tab = new SudokuTab(frame, sudoku, creationEventDescription);
		registerNewTab(frame, tab, tabTitle);
		return tab;
	}
	
	// configure loggers
	public static Logger prefsLogger; // logs activities in classes of the prefs package, and saving activities and times
	public static Logger loadTimeLogger; // logs load times
	public static Logger timeLogger; // logs time taken to do time consuming operations
	public static Logger musicPlayerLogger; // logs activities within music player
	public static Logger solverTreeLogger; // logs the dnd solver tree, which is different form solving algorithms
	public static Logger exceptionLogger; // will log to files, perhaps rotating and such
	
	static
	{
		solverTreeLogger = Logger.getLogger("com.github.shayna003.sudoker.solverTreeLogger");
		solverTreeLogger.setLevel(Level.OFF);
		ConsoleHandler handler7 = new ConsoleHandler();
		handler7.setLevel(Level.OFF);
		solverTreeLogger.addHandler(handler7);
		solverTreeLogger.setUseParentHandlers(false);
		
		musicPlayerLogger = Logger.getLogger("com.github.shayna003.sudoker.musicPlayer");
		musicPlayerLogger.setLevel(Level.OFF);
		ConsoleHandler handler3 = new ConsoleHandler();
		handler3.setLevel(Level.OFF);
		musicPlayerLogger.addHandler(handler3);
		musicPlayerLogger.setUseParentHandlers(false);
		
		timeLogger = Logger.getLogger("com.github.shayna003.sudoker.time");
		timeLogger.setLevel(Level.OFF);
		ConsoleHandler handler0 = new ConsoleHandler();
		handler0.setLevel(Level.OFF);
		timeLogger.addHandler(handler0);
		timeLogger.setUseParentHandlers(false);
		
		loadTimeLogger = Logger.getLogger("com.github.shayna003.sudoker.loadTime");
		loadTimeLogger.setLevel(Level.OFF);
		ConsoleHandler handler1 = new ConsoleHandler();
		handler1.setLevel(Level.OFF);
		loadTimeLogger.addHandler(handler1);
		loadTimeLogger.setUseParentHandlers(false);

		prefsLogger = Logger.getLogger("com.github.shayna003.sudoker.prefs");
		prefsLogger.setLevel(Level.OFF);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.OFF);
		prefsLogger.addHandler(handler);
		prefsLogger.setUseParentHandlers(false);
	}
	
	static boolean fileChooserFontSet = false;
	public static JFileChooser getFileChooser()
	{
		if (fileChooser == null)
		{
			long start = System.currentTimeMillis();
			fileChooser = new JFileChooser();
			if (settingsLoaded)
			{
				generalSettingsPanel.setComponentFontSizes(fileChooser);
				fileChooserFontSet = true;
			}
		}
		else 
		{
			if (!fileChooserFontSet && settingsLoaded)
			{
				generalSettingsPanel.setComponentFontSizes(fileChooser);
				fileChooserFontSet = true;
			}
		}
		return fileChooser;
	}
	
	public static JFileChooser getFileChooser(File currentDirectory)
	{
		getFileChooser().setCurrentDirectory(currentDirectory);
		return fileChooser;
	}
	
	public static JFileChooser getFileChooser(File currentDirectory, int selectionMode)
	{
		getFileChooser().setCurrentDirectory(currentDirectory);
		fileChooser.setFileSelectionMode(selectionMode);
		return fileChooser;
	}
	
	public static JFileChooser getFileChooser(int selectionMode)
	{
		getFileChooser().setFileSelectionMode(selectionMode);
		return fileChooser;
	}
	
	static void initExceptionLogger()
	{
		exceptionLogger = Logger.getLogger("sudoku.exceptionLogger");
		exceptionLogger.setLevel(Level.ALL);
		exceptionLogger.setUseParentHandlers(false);
		ConsoleHandler handler6 = new ConsoleHandler();
		FileHandler warningHandler;
		FileHandler severeHandler;
		
		if (!logsFolder.exists()) logsFolder.mkdirs();
		try
		{
			warningHandler = new FileHandler(logsFolder.getPath() + File.separator + "warning_messages%g.log", 100000, 5, true);
			warningHandler.setFilter(record -> record.getLevel() != Level.SEVERE);
			exceptionLogger.addHandler(warningHandler);
			
			severeHandler = new FileHandler(logsFolder.getPath() + File.separator + "severe_messages%g.log", 100000, 5, true);
			severeHandler.setFilter(record -> record.getLevel() == Level.SEVERE);
			exceptionLogger.addHandler(severeHandler);
		}
		catch (IOException e) 
		{
			JLabel label = new JLabel("Some errors happened when configuring loggers for error messages.");
			JPanel labelPanel = new JPanel();
			labelPanel.add(label);
			
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			
			JTextArea textArea = new JTextArea(errors.toString(), 10, 10);
			textArea.setEditable(false);
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(labelPanel, BorderLayout.NORTH);
			panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
			
			JOptionPane.showMessageDialog(null, panel, "Error", JOptionPane.WARNING_MESSAGE, null);
		}

		handler6.setLevel(Level.ALL);
		exceptionLogger.addHandler(handler6);
		exceptionLogger.setUseParentHandlers(false);
	}
	
	public static void loadApplicationFilesFolder()
	{
		// the chooser will not have the look and feel and font size settings implemented
		String s = applicationNode.get("applicationFilesFolderLocation", null);
		boolean shouldShowSuccessMessage = false;
		
		if (s == null || !(applicationFilesFolder = new File(s)).isDirectory() || (!applicationFilesFolder.exists() && !applicationFilesFolder.mkdirs()))
		{
			boolean filesFolderSet = false;
			getFileChooser(JFileChooser.DIRECTORIES_ONLY);
			JPanel p = null;
			String targetName = application_name + " Files";
			while (!filesFolderSet)
			{
				if (p == null)
				{
					p = new JPanel(new GridBagLayout());
					p.add(new JLabel("Please select a directory in which the folder to store application preferences and logs will be created."), new GBC(0, 0).setAnchor(GBC.WEST));
					p.add(new JLabel("You can also locate an existing \"" + application_name + " Files\" folder created by this application or its parent folder."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));
				}
				
				JOptionPane.showMessageDialog(null, p, "Application Files Folder Not Found", JOptionPane.INFORMATION_MESSAGE);
				if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				{
					File f = fileChooser.getSelectedFile();
					if (f.isDirectory())
					{
						if (f.getName().equals(targetName))
						{
							applicationFilesFolder = f;
							filesFolderSet = true;
						}
						else 
						{
							applicationFilesFolder = new File(f, targetName);
							if (applicationFilesFolder.exists() || applicationFilesFolder.mkdirs()) filesFolderSet = true;
						}
					}
				}
			}
			applicationNode.put("applicationFilesFolderLocation", applicationFilesFolder.getPath());
			
			// this message will not have the correct look and feel and font settings
			if (showDialogs.getBoolean("applicationFilesFolderSet", true))
			{
				shouldShowSuccessMessage = true;
			}
		}
		
		preferenceFolder = new File(applicationFilesFolder, "preferences");
		dataFolder = new File(applicationFilesFolder, "saved data");
		logsFolder = new File(applicationFilesFolder, "logs");
		exportsFolder = new File(applicationFilesFolder, "exports");

		initExceptionLogger();
		generalSettingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "general_settings.xml"));

		if (shouldShowSuccessMessage)
		{
			JOptionPane.showMessageDialog(null, PreferenceDialogs.getDirectoryMessage("applicationFilesFolderSet", "Now " + application_name + " will save its preferences and logs to the folder", applicationFilesFolder), "For more information, see " + KeyboardSettingsPanel.getMenuItemString(Application.application_name, "\u2699 Preferences...", "General"), JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public static String defaultLookAndFeelClassName;
	public static void loadLookAndFeel()
	{
		// install custom look and feels here
		long start = System.currentTimeMillis();
		lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
		LookAndFeel currentLaf = UIManager.getLookAndFeel();
		if (currentLaf != null)
		{
			defaultLookAndFeelClassName = currentLaf.getClass().getName();
		}
		Application.loadTimeLogger.log(Level.CONFIG, "time to get installed laf infos: " + (System.currentTimeMillis() - start));

		long b = System.currentTimeMillis();
		String laf = generalSettingsFile.node.get("lookAndFeel", null);
		String metalTheme = generalSettingsFile.node.get("metalTheme", "");
		switch (metalTheme) 
		{
			case "DefaultMetal":
				MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
				break;
			case "CustomMetal":
				MetalLookAndFeel.setCurrentTheme(new CustomMetalTheme());
				break;
			case "DarkMetal":
				MetalLookAndFeel.setCurrentTheme(new DarkMetalTheme());
				break;
		}

		long c = System.currentTimeMillis();
		if (laf != null && !laf.equals(UIManager.getLookAndFeel().getClass().getName()))
		{
			try 
			{
				UIManager.setLookAndFeel(laf);
			} 
			catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) 
			{
				Application.exceptionLogger.logp(Level.WARNING , "Application", "loadLookAndFeel()", "Error when trying to set look and feel to " + laf, e);
			}
		}
		Application.loadTimeLogger.log(Level.CONFIG, "time to set laf: " + (System.currentTimeMillis() - c));
		
		boolean frameDefaultLafDecorated = generalSettingsFile.node.getBoolean("frameDefaultLookAndFeelDecorated", false);
		JFrame.setDefaultLookAndFeelDecorated(frameDefaultLafDecorated);
		JDialog.setDefaultLookAndFeelDecorated(frameDefaultLafDecorated);
	}
	
	public static void loadSettings()
	{
		long start = System.currentTimeMillis();
		
		openWindows = new ArrayList<>();
		componentsToUpdateUI = new ArrayList<>();
		
		showDialogs = Preferences.userNodeForPackage(SingleSettingsFile.class).node("general_settings").node("showDialogs");

		ApplicationLauncher.paintSplashScreen("Loading Application Files Folder");
		loadTimeLogger.log(Level.FINEST, "load Application Files Folder, current time spent: " + (System.currentTimeMillis() - start)); //~60ms
		long folderStart = System.currentTimeMillis();
		loadApplicationFilesFolder();
		loadTimeLogger.log(Level.FINEST, "done loading Application Files Folder, time spent for it: " + (System.currentTimeMillis() - folderStart));

		ApplicationLauncher.paintSplashScreen("Loading Look and Feel");
		long lafStart = System.currentTimeMillis();
		loadLookAndFeel();
		long end = System.currentTimeMillis();
		loadTimeLogger.log(Level.INFO, "done loading Look and Feel, time spent for it: " + (end - lafStart));

		long prefsStart = System.currentTimeMillis();
		ApplicationLauncher.paintSplashScreen("Creating Preferences Frame");
		preferenceFrame = new PreferenceFrame();
		loadTimeLogger.log(Level.INFO, "done creating preferenceFrame frame, time spent for it: " + (System.currentTimeMillis() - prefsStart));

		componentsToUpdateUI.add(preferenceFrame);
		long fontStart = System.currentTimeMillis();
		ApplicationLauncher.paintSplashScreen("Setting Application Font Size");
		generalSettingsPanel.setGlobalFontSize(generalSettingsPanel.currentGlobalFontSize); // takes about 85ms
		loadTimeLogger.log(Level.CONFIG, "done setting global font size, time spent for it: " + (System.currentTimeMillis() - fontStart));
		
		historyTreeFrame = new HistoryTreeFrame();
		openWindowsAndTabs = new OpenWindowsAndTabs();
		GeneralSettingsPanel.registerComponentAndSetFontSize(historyTreeFrame);
		GeneralSettingsPanel.registerComponentAndSetFontSize(openWindowsAndTabs);
		
		generalSettingsPanel.updateComponentUIs();

		saveSettingsAction = new AbstractAction("Save")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				saveSettings();
				saveAllTabData();
			}
		};
		saveSettingsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);

		exitProgram = new AbstractAction("\u238B Exit Program")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				int option = JOptionPane.showOptionDialog(null, "Do you want to save data for tabs before exiting the application?", "Exiting Application", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Yes", "No", "Cancel"}, "Cancel");

				if (option == 1 || option == 0)
				{
					saveSettings();
					if (option == 0) saveAllTabData();
					needToSaveWhenShutDown = false;
					System.exit(0);
				}

			}
		};
		
		prefs_initialized = true;
		loadTimeLogger.log(Level.INFO, "done loading settings, total time spent: " + (System.currentTimeMillis() - start));
		settingsLoaded = true;
	}

	static boolean needToSaveWhenShutDown = true;

	static void saveSettings()
	{
		if (preferenceFrame.isVisible())
		{
			preferenceFrame.saveChanges.actionPerformed(null);
		}

		if (musicPlayerFrame != null)
		{
			musicPlayerFrame.musicPlayer.savePlaylistAndSettings();
		}

		if (exporter != null)
		{
			exporter.saveSettings(exporter.settingsFile, true);
		}

		if (importer != null && importer.inputDialog != null)
		{
			importer.saveSettings(importer.settingsFile, true);
		}

		if (generator != null)
		{
			generator.saveSettings(generator.settingsFile, true);
		}
	}

	static void saveAllTabData()
	{
		for (ApplicationFrame frame : openWindows)
		{
			for (int t = 0; t < frame.tabbedPane.getTabCount(); t++)
			{
				SudokuTab tab = (SudokuTab) frame.tabbedPane.getComponentAt(t);
				boolean success = Exporter.exportToFile(new File(dataFolder, getUniqueFileNameForTab()), Exporter.getFullDataString(tab.board), tab.owner, false);
				if (!success) JOptionPane.showMessageDialog(null, "Data for " + tab.getName() + " could not be saved.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}