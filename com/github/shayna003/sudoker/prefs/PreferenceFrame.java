package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.theme.*;
import com.github.shayna003.sudoker.prefs.keys.*;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

/**
 * This class allows the user to change user preferences.
 * Changes made will apply instantly for simplicity and the user has options to undo them
 * @version 0.00 1-10-2021
 * @since 2020-11-1
 */
@SuppressWarnings("CanBeFinal")
public class PreferenceFrame extends JFrame implements ColorChooserDialogOwner
{		
	public static String[] allFontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	
	//so the program remembers the place the user dragged the chooser to
	JColorChooser colorChooser;
	public JDialog colorChooserDialog;
	
	// for settings the sample text size in GeneralSettingsPanel back to current global application font size.
	Component previouslySelectedTab; 
	
	public JTabbedPane tabbedPane;
	public GeneralSettingsPanel generalSettingsPanel;
	public SolverSettingsPanel solverSettingsPanel;
	public ThemesPanel themesPanel;
	public DigitsAndIndexesPanel digitsAndIndexesPanel;
	public BoardSettingsPanel boardSettingsPanel;
	public HistoryTreeSettingPanel historyTreeSettingPanel;
	public KeyboardSettingsPanel keyboardSettingsPanel;
	public MiscellaneousSettingsPanel miscellaneousSettingsPanel;
	public Action saveChanges;
	Color colorChooserDialogColor;
	
	boolean location_and_size_configured = false;
	
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
	
	/**
	 * @return The file name without extension
	 */
	public static String removeFileExtension(File pathName)
	{
		String fileName = pathName.getName();
		int i = fileName.lastIndexOf('.');
		if (i < 0) // no extension
		{
			return fileName;
		}
		else if (i == 0) //no file name
		{
			return "";
		}
		return fileName.substring(0, i);		
	}
	
	/**
	 * @return the file extension if @param pathName is a file, not a directory
	 */
	public static String getFileExtension(File pathName)
	{
		String fileName = pathName.getName();
		if (pathName == null) return null;

		int i = fileName.lastIndexOf('.');
		if (i < 0) return "";

		if (i < fileName.length() - 1)
		{
			return fileName.substring(i + 1);
		}
		return null; // last char is '.'
	}
	
	/**
	 * @return whether @param pathName has extension of @param desiredExtension
	 */
	public static boolean fileExtensionEquals(File pathName, String desiredExtension)
	{
		String fileName = pathName.getName();
		int i = fileName.lastIndexOf('.');
		if	(i < fileName.length() - 1)
		{
			String extension = fileName.substring(i + 1);
			return extension.toLowerCase().equals(desiredExtension);
		}
		return false;
	}
	
	public static FileFilter xmlFilter = new FileFilter()
	{
		@Override
		public boolean accept(File pathName)
		{		
			return pathName.isFile() && fileExtensionEquals(pathName, "xml");
		}
	};
	
	public Color showColorChooserDialog(String title, Color initialColor)
	{
		if (colorChooser == null)
		{
			initColorChooser();
		}
		colorChooserDialogColor = initialColor;
		colorChooser.setColor(initialColor);
		assert colorChooser.getColor().equals(initialColor) : "initial: " + initialColor + ", colorChooser's color: " + colorChooser.getColor();
		colorChooserDialog.setTitle(title);
		colorChooserDialog.setVisible(true);
		return colorChooserDialogColor;
	}
	
	void initColorChooser()
	{
		colorChooser = new JColorChooser();
		
		AbstractAction copyColorAction = new AbstractAction("copy color")
		{
			public void actionPerformed(ActionEvent event)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				ColorSelection selection = new ColorSelection(colorChooser.getColor());
				clipboard.setContents(selection, selection);
			}
		};
		
		AbstractAction pasteColorAction = new AbstractAction("paste color")
		{
			public void actionPerformed(ActionEvent event)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				for (DataFlavor df : clipboard.getAvailableDataFlavors())
				{
					if (df.getRepresentationClass().equals(Color.class))
					{
						try 
						{
							colorChooser.setColor((Color) clipboard.getData(df));
						} 
						catch (IOException | UnsupportedFlavorException | ClassCastException e) 
						{
							Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "pateColorAction's actionPerformed", "Error when getting data from clipboard with DataFlavor " + df, e);
						}
					}
				}
				ColorSelection selection = new ColorSelection(colorChooser.getColor());
				clipboard.setContents(selection, selection);
			}
		};

		InputMap colorChooserInputMap = colorChooser.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		colorChooserInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy_color");
		colorChooserInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "paste_color");
		ActionMap colorChooserActionMap = colorChooser.getActionMap();
		colorChooserActionMap.put("copy_color", copyColorAction);
		colorChooserActionMap.put("paste_color", pasteColorAction);
		
		colorChooser.setDragEnabled(true);
		colorChooserDialog = JColorChooser.createDialog(this, "", true, colorChooser, event ->
		{
			colorChooserDialogColor = colorChooser.getColor();
		}, null);
		
		Application.componentsToUpdateUI.add(colorChooserDialog);
		Application.generalSettingsPanel.setComponentFontSizes(colorChooserDialog);
	}

	/*boolean generalSettingsVisited;
	boolean digitSettingsVisited;
	boolean boardSettingsVisited;
	boolean themesVisited;
	boolean solverSettingsVisited;
	boolean historyTreeSettingsVisited;
	boolean keyboardSettingsVisited;
	boolean miscellaneousSettingsVisited;*/

	HashMap<SettingsPanel, Boolean> panelsVisited;

	public PreferenceFrame()
	{
		Application.prefsLogger.entering("PreferenceFrame", "PreferenceFrame()");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); //should save settings upon close

		panelsVisited = new HashMap<>();

		saveChanges = new AbstractAction("Save and Close")
		{
			public void actionPerformed(ActionEvent event)
			{
				Application.prefsLogger.log(Level.FINE, "Preference Frame Saving");
				panelsVisited.forEach((panel, visited) ->
				{
					if (visited)
					{
						if (panel instanceof MultipleSettingsPanel)
						{
							((MultipleSettingsPanel) panel).saveSettingsToFiles();
						}
						else
						{
							panel.saveSettings(panel.getSettingsFile(), true);
						}
					}
				});
			}
		};
			
		setTitle("Application Preferences");

		long g = System.currentTimeMillis();
		generalSettingsPanel = new GeneralSettingsPanel(this);
		Application.generalSettingsPanel = generalSettingsPanel;
		panelsVisited.put(generalSettingsPanel, false);
		Application.loadTimeLogger.log(Level.CONFIG, "init time for generalSettingsPanel: " + (System.currentTimeMillis() - g));
		
		keyboardSettingsPanel = new KeyboardSettingsPanel(this);
		Application.keyboardSettingsPanel = keyboardSettingsPanel;
		panelsVisited.put(keyboardSettingsPanel, false);
		
		long d = System.currentTimeMillis();
		Application.prefsLogger.log(Level.CONFIG, "init DigitsAndIndexesPanel");
		digitsAndIndexesPanel = new DigitsAndIndexesPanel(this);
		Application.digitsAndIndexesPanel = digitsAndIndexesPanel;
		panelsVisited.put(digitsAndIndexesPanel, false);
		Application.loadTimeLogger.log(Level.CONFIG, "init time for digitsAndIndexesPanel: " + (System.currentTimeMillis() - d));
		
		boardSettingsPanel = new BoardSettingsPanel(this);
		Application.boardSettingsPanel = boardSettingsPanel;
		panelsVisited.put(boardSettingsPanel, false);
		
		long s = System.currentTimeMillis();
		Application.prefsLogger.log(Level.CONFIG, "init SolverSettingsPanel");
		solverSettingsPanel = new SolverSettingsPanel(this);
		Application.solverSettingsPanel = solverSettingsPanel;
		panelsVisited.put(solverSettingsPanel, false);
		Application.loadTimeLogger.log(Level.CONFIG, "init time for SolverSettingsPanel: " + (System.currentTimeMillis( ) - s));
		
		long gf = System.currentTimeMillis();
		historyTreeSettingPanel = new HistoryTreeSettingPanel(this);
		panelsVisited.put(historyTreeSettingPanel, false);
		Application.historyTreeSettingPanel = historyTreeSettingPanel;
		Application.loadTimeLogger.log(Level.CONFIG, "init time for HistoryTreeSettingPanel: " + (System.currentTimeMillis( ) - gf));
		
		
		miscellaneousSettingsPanel = new MiscellaneousSettingsPanel(this);
		panelsVisited.put(miscellaneousSettingsPanel, false);
		Application.miscellaneousSettingsPanel = miscellaneousSettingsPanel;
		
		long t = System.currentTimeMillis();
		themesPanel = new ThemesPanel(this);
		Application.themesPanel = themesPanel;
		panelsVisited.put(themesPanel, false);
		Application.loadTimeLogger.log(Level.CONFIG, "init time for ThemesPanel: " + (System.currentTimeMillis( ) - t));

		long t2 = System.currentTimeMillis(); 
		tabbedPane = new JTabbedPane();

		tabbedPane.addTab("General", null, generalSettingsPanel, "General Settings");
		tabbedPane.addTab("Digits and Indexes", null, digitsAndIndexesPanel, "Change Sudoku digits and indexes");
		tabbedPane.addTab("Themes", null, themesPanel, "Board Visuals");
		tabbedPane.addTab("Board", null, boardSettingsPanel, "More Board Settings");
		tabbedPane.addTab("Solver", null, solverSettingsPanel, "Solver Settings");
		tabbedPane.addTab("History Tree", null, historyTreeSettingPanel, "Settings for History Trees");
		tabbedPane.addTab("Key Bindings", null, keyboardSettingsPanel, "Keyboard Shortcut Settings");
		tabbedPane.addTab("Miscellaneous", null, miscellaneousSettingsPanel, "Miscellaneous Settings");

		tabbedPane.addChangeListener(event ->
		{
			if (tabbedPane.getTabCount() == 1) return;
			panelsVisited.put((SettingsPanel) tabbedPane.getSelectedComponent(), true);
			if (tabbedPane.getSelectedComponent() != generalSettingsPanel && previouslySelectedTab == generalSettingsPanel)
			{
				generalSettingsPanel.fontSizeSlider.setValue(generalSettingsPanel.currentGlobalFontSize);
			}
			previouslySelectedTab = tabbedPane.getSelectedComponent();
		});
		
		tabbedPane.setSelectedIndex(0);
		previouslySelectedTab = tabbedPane.getComponentAt(0);
		
		add(tabbedPane);
		Application.loadTimeLogger.log(Level.FINEST, "Time to configure TabbedPane: " + (System.currentTimeMillis() - t2));
		
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				generalSettingsPanel.fontSizeSlider.setValue(generalSettingsPanel.currentGlobalFontSize);
				saveChanges.actionPerformed(null);

				Set<SettingsPanel> keys = panelsVisited.keySet();
				keys.forEach(panel -> panelsVisited.put(panel, false));
				setVisible(false);
			}
		});
	}

	void configureLocationAndSize()
	{
		if (!location_and_size_configured)
		{
			setLocationRelativeTo(null);
		}
	}

	void switchToTab(JComponent tab)
	{
		if (tab != null)
		{
			tabbedPane.setSelectedComponent(tab);
		}
		panelsVisited.put((SettingsPanel) tabbedPane.getSelectedComponent(), true);
	}

	public void showUp(JComponent switchToTab, JComponent scrollToVisible)
	{
		configureLocationAndSize();
		switchToTab(switchToTab);

		if (scrollToVisible != null)
		{
			scrollToVisible.scrollRectToVisible(new Rectangle(scrollToVisible.getSize()));
		}
		setVisible(true);
	}
	
	public void showUp(JComponent switchToTab)
	{
		configureLocationAndSize();
		switchToTab(switchToTab);
		setVisible(true);
	}
}