package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.components.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import java.util.prefs.*;
import java.util.logging.*;
import java.io.*;
import java.util.*;

/**
 * @since about 11-11-2020
 */
@SuppressWarnings("CanBeFinal")
public class GeneralSettingsPanel extends JPanel implements SettingsPanel
{
	PreferenceFrame preferenceFrame;

	SingleSettingsFile settingsFile;

	JPanel prefsFolderPanel;
	JTextField directoryField;
	JButton directoryButton;

	JPanel lookAndFeelPanel;
	public PrefsComboBox<String> lookAndFeelCombo;
	public PrefsComboBox<String> metalThemeCombo; // themes for Metal Look and Feel
	PrefsCheckBox frameDefaultLookAndFeelDecorated;
	JLabel lafSupportsWindowDecorations;

	public int currentGlobalFontSize;
	FixedSizeLabel currentFontSizeLabel;
	JPanel fontSizePanel;
	JLabel sampleText;
	TextSliderPanel fontSizeSlider;
	JButton setFontSize;
	
	JButton resetAllHiddenDialogs;

	public PrefsCheckBox restorePreviousSession;
	//PrefsCheckBox rememberFramePosition;
	//PrefsCheckBox rememberFrameSize;
	
	boolean initializing;
	
	/**
	 * Sets all fonts of this application to the new size
	 * The idea is simple, but not all lafs use all of these defaults
	 * According to the Java API Documentation: 
	
	 * "The set of defaults a particular look and feel supports is defined
	 * and documented by that look and feel. In addition, each look and
	 * feel, or {@code ComponentUI} provided by a look and feel, may
	 * access the defaults at different times in their life cycle. Some
	 * look and feels may aggressively look up defaults, so that changing a
	 * default may not have an effect after installing the look and feel.
	 * Other look and feels may lazily access defaults so that a change to
	 * the defaults may effect an existing look and feel. Finally, other look
	 * and feels might not configure themselves from the defaults table in
	 * any way. None-the-less it is usually the case that a look and feel
	 * expects certain defaults, so that in general
	 * a {@code ComponentUI} provided by one look and feel will not
	 * work with another look and feel."
	
	 * Therefore this function is used in conjucntion with {@code setComponentFontSizes}
	 * This does not guarantee that all font sizes are set, e.g. calls to {@code JOptionPane#showDialog}
	 * Still uses the previous default look and feel font size for Nimbus look and feel
	 */
	public void setGlobalFontSize(float size)
	{
		Enumeration<Object> e = UIManager.getLookAndFeel().getDefaults().keys();//getLookAndFeelDefaults().keys();
		while (e.hasMoreElements())
		{
			Object key = e.nextElement();
			if (UIManager.get(key) instanceof Font)
			{
				Font f = (Font) UIManager.get(key);
				UIManager.put(key, new FontUIResource(f.deriveFont(size)));
			}
		}

		if (UIManager.getLookAndFeelDefaults().getFont("defaultFont") == null)
		{
			UIManager.getLookAndFeelDefaults().put("defaultFont", new Font("SansSerif", Font.PLAIN, (int) size));
		}
		setAllComponentFontSizes(size);
	}
	
	public void setAllComponentFontSizes(float size)
	{
		Application.prefsLogger.log(Level.FINE, "entering setAllComponentFontSizes (" + size + ")");
		for (Component c : Application.componentsToUpdateUI)
		{
			setComponentFontSizes(c, size);
		}
	}
	
	public void setComponentFontSizes(Component c)
	{
		setComponentFontSizes(c, currentGlobalFontSize);
	}
	
	public static void setComponentFontSizes(Component c, float size)
	{
		if (c == null) return;
		Font f = c.getFont();

		if (f != null)
		{
			c.setFont(f.deriveFont(size));
		}
		else 
		{
			Application.prefsLogger.log(Level.FINE, "set font to default font derived size, defaultFont: " + UIManager.getFont("defaultFont"));
			
			FontUIResource defaultFont;
			if (UIManager.getFont("defaultFont") == null)
			{
				defaultFont = new FontUIResource(new Font("SansSerif", Font.PLAIN, (int) size));
				UIManager.getLookAndFeelDefaults().put("defaultFont", defaultFont);
				c.setFont(defaultFont.deriveFont(size));
			}
			else 
			{
				c.setFont(UIManager.getFont("defaultFont").deriveFont(size));
			}
		}
		
		if (c instanceof Container)
		{
			for (Component component : ((Container) c).getComponents())
			{
				if (component != null)
				{
					setComponentFontSizes(component, size);
				}
			}
		}
		
		if (c instanceof JMenu)
		{
			JMenu m = (JMenu) c;
			for (int i = 0; i < m.getItemCount(); i++)
			{
				setComponentFontSizes(m.getItem(i), size);
			}
		}
	}
	
	public void setMetalTheme(String theme)
	{
		switch (theme) 
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
			default:
				MetalLookAndFeel.setCurrentTheme(new OceanTheme());
				break;
		}
			
		if (UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
		{
			setLafToMetal();
		}
	}
	
	public void setLafToMetal()
	{
		try 
		{
			Application.prefsLogger.logp(Level.FINE, getClass().toString(), "setLafToMetal", "Setting look and feel to metal");
			UIManager.setLookAndFeel(new MetalLookAndFeel());
			updateComponentUIs();
		} 
		catch (UnsupportedLookAndFeelException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "setLafToMetal", "Error when setting look and feel to metal", e);
		}
	}
	
	void initLookAndFeelComponents()
	{
		// the predicate is not used
		lookAndFeelCombo = new PrefsComboBox<>("lookAndFeel", null);
		
		lookAndFeelCombo.addActionListener(event ->
		{
			// works fine without this statement too, but added to avoid confusion
			metalThemeCombo.setEnabled(lookAndFeelCombo.getSelectedItem() != null && lookAndFeelCombo.getSelectedItem().equals("Metal"));
			
			if (!initializing)
			{
				Application.prefsLogger.entering("GeneralSettingsPanel", "lookAndFeelCombo's actionPerformed");
				try
				{
					Application.prefsLogger.log(Level.FINE, "set look and feel to " + Application.lookAndFeelInfos[lookAndFeelCombo.getSelectedIndex()].getClassName());
					
					UIManager.setLookAndFeel(Application.lookAndFeelInfos[lookAndFeelCombo.getSelectedIndex()].getClassName());
					updateComponentUIs();
				}
				catch (UnsupportedLookAndFeelException e)
				{
					Application.exceptionLogger.logp(Level.WARNING , getClass().toString(), "lookAndFeelCombo's ActionListener actionPerformed", "Look and feel not supported when trying to set look and feel to" + Application.lookAndFeelInfos[lookAndFeelCombo.getSelectedIndex()], e);
					JOptionPane.showMessageDialog(preferenceFrame, "The selected look and feel is not supported.", "Error", JOptionPane.ERROR_MESSAGE, null);
				}
				catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
				{
					Application.exceptionLogger.logp(Level.WARNING , getClass().toString(), "lookAndFeelCombo's ActionListener actionPerformed", "Error when trying to set look and feel to" + Application.lookAndFeelInfos[lookAndFeelCombo.getSelectedIndex()], e);
					JOptionPane.showMessageDialog(preferenceFrame, "Some error occurred while trying to set the look and feel to \"" + lookAndFeelCombo.getSelectedItem() + "\".", "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
				lafSupportsWindowDecorations.setText(UIManager.getLookAndFeel().isNativeLookAndFeel() || UIManager.getLookAndFeel().getSupportsWindowDecorations() ? "Some changes require restarting the application to take place" : "This operation is not supported for the current look and feel");
		});
		
		metalThemeCombo = new PrefsComboBox<>("metalTheme", s -> s.equals("Ocean (default)"));
		metalThemeCombo.addItem("Ocean (default)");
		metalThemeCombo.addItem("DefaultMetal");
		metalThemeCombo.addItem("CustomMetal");
		metalThemeCombo.addItem("DarkMetal");
		metalThemeCombo.addActionListener(event ->
		{
			if (!initializing)
			{
				String s = metalThemeCombo.getSelectedItem().toString();
				if (s != null)
				{
					setMetalTheme(s);
				}
			}
		});
		
		lafSupportsWindowDecorations = SwingUtil.makeTranslucentLabel("", 125);

		frameDefaultLookAndFeelDecorated = new PrefsCheckBox("frameDefaultLookAndFeelDecorated", "Decorate frames and dialogs with selected Look and Feel", false);
		frameDefaultLookAndFeelDecorated.addActionListener(event ->
		{
			JFrame.setDefaultLookAndFeelDecorated(frameDefaultLookAndFeelDecorated.isSelected());
		});
		
		for (int i = 0; i < Application.lookAndFeelInfos.length; i++)
		{
			if (Application.lookAndFeelInfos[i].getClassName().equals(Application.defaultLookAndFeelClassName))
			{
				lookAndFeelCombo.addItem(Application.lookAndFeelInfos[i].getName() + " (default)");
			}
			else 
			{
				lookAndFeelCombo.addItem(Application.lookAndFeelInfos[i].getName());
			}
			
			if (Application.lookAndFeelInfos[i].getName().equals(UIManager.getLookAndFeel().getName()))
			{
				lookAndFeelCombo.setSelectedIndex(i);
			}
		}
		
		lookAndFeelPanel = new JPanel(new GridBagLayout());
		//HelpButton lafHelpButton = new HelpButton("Help on Look and Feel", event -> {});

		lookAndFeelPanel.add(new JLabel("Look and Feel: "), new GBC(0, 0).setAnchor(GBC.WEST));
		lookAndFeelPanel.add(lookAndFeelCombo, new GBC(1, 0));
		lookAndFeelPanel.add(new JLabel("Metal Look and Feel Themes: "), new GBC(0, 1).setAnchor(GBC.WEST));
		lookAndFeelPanel.add(metalThemeCombo, new GBC(1, 1).setAnchor(GBC.EAST));
		lookAndFeelPanel.add(frameDefaultLookAndFeelDecorated, new GBC(0, 2, 4, 1).setAnchor(GBC.WEST));//, BorderLayout.NORTH);
		lookAndFeelPanel.add(lafSupportsWindowDecorations, new GBC(0, 3, 4, 1).setAnchor(GBC.WEST));
		
		lookAndFeelPanel.setBorder(BorderFactory.createTitledBorder("Look and Feel Settings"));
	}
	
	void initPrefsFolderComponents()
	{
		directoryField = new JTextField(Application.applicationFilesFolder.getPath(), 30);
		directoryField.setMinimumSize(directoryField.getPreferredSize());
		directoryField.setEditable(false);
		JButton copyDirectoryButton = new JButton("Copy");
		copyDirectoryButton.setToolTipText("Copy directory to system clipboard");
		copyDirectoryButton.addActionListener(event ->
		{
			StringSelection selection = new StringSelection(directoryField.getText());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		});
		
		Icon i = UIManager.getIcon("FileView.directoryIcon");
		
		if (i != null)
		{
			directoryButton = new JButton(i);
		}
		else
		{
			directoryButton = new JButton("Show Location");
		}
		
		directoryButton.setToolTipText("Show folder location");
		directoryButton.addActionListener(event ->
		{
			try 
			{
				Desktop.getDesktop().open(Application.applicationFilesFolder);
			} 
			catch (IOException e) 
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "directoryButton's ActionListener actionPerformed", "Error when showing folder location", e);
				JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while trying to show folder location.", "Error", JOptionPane.ERROR_MESSAGE, null);
			}
		});
		
		prefsFolderPanel = new JPanel(new GridBagLayout());
		
		prefsFolderPanel.add(new JLabel("Application Files Folder Location: "), new GBC(0, 0).setAnchor(GBC.EAST));
		prefsFolderPanel.add(directoryField, new GBC(1, 0));
		prefsFolderPanel.add(copyDirectoryButton, new GBC(2, 0));
		prefsFolderPanel.add(directoryButton, new GBC(3, 0));
		
		prefsFolderPanel.add(SwingUtil.makeTranslucentLabel("This is where " + Application.application_name + " will store its preferences and logs.", 125), new GBC(0, 1, 5, 1).setAnchor(GBC.WEST));
		prefsFolderPanel.add(SwingUtil.makeTranslucentLabel("To change the folder location, quit the application then move/delete the folder as desired.", 125), new GBC(0, 2, 5, 1).setAnchor(GBC.WEST));
		prefsFolderPanel.add(SwingUtil.makeTranslucentLabel("Upon opening the application again, a dialog will pop up to locate/create a new folder.", 125), new GBC(0, 3, 5, 1).setAnchor(GBC.WEST));
		prefsFolderPanel.add(SwingUtil.makeTranslucentLabel("You can move files in the preferences folder to corresponding places in another preferences folder to load them.", 125), new GBC(0, 4, 5, 1));
	}
	
	/**
	 * Called upon creating a new component
	 */
	public static void registerComponentAndSetFontSize(Component c)
	{
		Application.componentsToUpdateUI.add(c);
		setComponentFontSizes(c, Application.generalSettingsPanel.currentGlobalFontSize);
	}
	
	/**
	 * Called after changing the look and feel or changing global font size
	 */
	public void updateComponentUIs()
	{
		Application.prefsLogger.entering("GeneralSettingsPanel", "updateComponentUIs");

		for (Component c : Application.componentsToUpdateUI)
		{
			if (c != null)
			{
				SwingUtilities.updateComponentTreeUI(c);
				c.setSize(c.getPreferredSize());

				if (c instanceof Window)
				{
					((Window) c).pack();
				}
			}	
		}
		
		// different UI's have different icons
		Icon i = UIManager.getIcon("FileView.directoryIcon");
		if (i != null)
		{
			directoryButton.setIcon(i);
			directoryButton.setText(null);
		}
		else
		{
			directoryButton.setIcon(null);
			directoryButton.setText("Show Location");
		}
	}	
	
	public GeneralSettingsPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		
		this.preferenceFrame = preferenceFrame;
		
		initLookAndFeelComponents();
		initPrefsFolderComponents();
		
		int maxFontSize = 36;
		sampleText = new JLabel("Sample Text");
		Rectangle2D r = SwingUtil.getStringBounds(sampleText, sampleText.getText(), sampleText.getFont().deriveFont((float) maxFontSize));
		sampleText.setPreferredSize(new Dimension(/*(int) r.getWidth()*/sampleText.getPreferredSize().width, (int) r.getHeight()));
		
		// min font size is 0, and default font size is 12
		fontSizeSlider = new TextSliderPanel("applicationFontSize", JSlider.HORIZONTAL, "Set To", 0, maxFontSize, 12, true, true, 10, 2, true, null, true, true, event ->
		{
			sampleText.setFont(sampleText.getFont().deriveFont((float) fontSizeSlider.getValue()));
			Rectangle2D r2 = SwingUtil.getStringBounds(sampleText, sampleText.getText(), sampleText.getFont());
			sampleText.setPreferredSize(new Dimension(/*(int) r.getWidth()*/(int) r2.getWidth(), sampleText.getPreferredSize().height));
			sampleText.setMinimumSize(sampleText.getPreferredSize());
		});
		setFontSize = new JButton("Set Font Size");
		setFontSize.addActionListener(event ->
		{
			currentGlobalFontSize = fontSizeSlider.getValue();
			currentFontSizeLabel.setText("Current Application Font Size: " + currentGlobalFontSize);
			setGlobalFontSize((float) fontSizeSlider.getValue());
			updateComponentUIs();
		});
		
		currentFontSizeLabel = new FixedSizeLabel("", "Current Application Font Size: " + maxFontSize);
		
		fontSizePanel = new JPanel(new GridBagLayout());
		fontSizePanel.add(currentFontSizeLabel, new GBC(0, 0).setAnchor(GBC.WEST));
		fontSizePanel.add(fontSizeSlider, new GBC(0, 1).setAnchor(GBC.WEST));
		fontSizePanel.add(setFontSize, new GBC(1, 1));
		fontSizePanel.add(sampleText, new GBC(0, 2, 3, 1));
		fontSizePanel.setBorder(BorderFactory.createTitledBorder("Application Font Size"));

		restorePreviousSession = new PrefsCheckBox("restorePreviousSession", "Restore Previous Session Next Time after Program Exits", true);
		resetAllHiddenDialogs = new JButton("Reset All Hidden Dialogs...");
		
		resetAllHiddenDialogs.setToolTipText("All dialogs hidden by selecting \"Don't show this message again\" option will reappear");
		resetAllHiddenDialogs.addActionListener(event ->
		{
			if (JOptionPane.showOptionDialog(preferenceFrame, "All dialogs with \"Don't show this message again\" option selected will now show when applicable.", "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0) //yes
			{
				try 
				{
					Application.showDialogs.clear();
				} 
				catch (BackingStoreException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "resetAllHiddenDialogs' ActionListener actionPerformed", "Error when setting all dialogs to show.", e);
				}
			}
		});
		
		
		JPanel checkBoxPanel = new JPanel(new GridBagLayout());
		checkBoxPanel.add(restorePreviousSession, new GBC(0, 0).setAnchor(GBC.WEST));
		checkBoxPanel.add(resetAllHiddenDialogs, new GBC(0, 1));

		setLayout(new BoxLayout(GeneralSettingsPanel.this, BoxLayout.Y_AXIS));
		add(prefsFolderPanel);
		add(lookAndFeelPanel);
		add(fontSizePanel);
		add(checkBoxPanel);
		
		settingsFile = Application.generalSettingsFile;
		loadSettings(settingsFile);
		initializing = false;
	}
	
	public void loadSettings(SingleSettingsFile file)
	{
		Preferences node = file.node;
		lookAndFeelCombo.loadSettings(node);
		metalThemeCombo.loadSettings(node);
		fontSizeSlider.loadSettings(node);
		currentGlobalFontSize = fontSizeSlider.getValue();
		currentFontSizeLabel.setText("Current Application Font Size: " + currentGlobalFontSize);
		frameDefaultLookAndFeelDecorated.loadSettings(node);
		restorePreviousSession.loadSettings(node);
	}
	
	public void saveSettings(SingleSettingsFile file, boolean saveToFile)
	{
		Preferences node = settingsFile.node;
		node.put(lookAndFeelCombo.settingName, Application.lookAndFeelInfos[lookAndFeelCombo.getSelectedIndex()].getClassName());
		metalThemeCombo.saveSettings(node);
		frameDefaultLookAndFeelDecorated.saveSettings(node);
		node.putInt(fontSizeSlider.settingName, currentGlobalFontSize);
		restorePreviousSession.saveSettings(node);
		
		if (saveToFile) file.save();
	}
	
	public void applyChanges() { }

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}