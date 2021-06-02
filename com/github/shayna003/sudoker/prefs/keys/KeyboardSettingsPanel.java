package com.github.shayna003.sudoker.prefs.keys;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.*;

import java.awt.*;
import javax.swing.*;
import java.util.prefs.*;
import java.io.*;
import java.util.*;

/**
 * Stores settings for Keyboard Shortcuts
 * Because the components that use these shortcuts need to be initialized first, 
 * KeyComponentPanels are created in the initialization code of those components.
 * I guess a feature? drawback? is that the key binding items will be lazily created, for example, for Music Player. 
 * This results in things not appearing in the most logical order.
 * @since 5-8-2021
 */
@SuppressWarnings("CanBeFinal")
public class KeyboardSettingsPanel extends JPanel implements SettingsPanel
{
	public SingleSettingsFile settingsFile;
	PreferenceFrame preferenceFrame;
	boolean initializing;
	
	ArrayList<KeyComponentPanel> menuItemShortcuts;
	KeyBindingsTable menuItemsTable;
	JScrollPane menuItemsScrollPane;
	JPanel menuItemsPanel;
	
	ArrayList<KeyComponentPanel> otherShortcuts;
	KeyBindingsTable othersTable;
	JScrollPane othersScrollPane;
	JPanel othersPanel;
	
	JPanel tablesPanel;
	public static String getMenuItemString(String... params)
	{
		StringBuilder b = new StringBuilder(params[0]);
		for (int i = 1; i < params.length; i++)
		{
			b.append(" > ");
			b.append(params[i]);
		}
		return b.toString();
	}
	
	/**
	 * There will only be one KeyComponentPanel for the same settingName.
	 * @param focusCondition one of JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, or JComponent.WHEN_IN_FOCUSED_WINDOW
	 */
	public void registerMenuShortcut(String settingName, String itemName, boolean hasDefaultKeyStroke, int defaultKeyCode, int defaultModifiers, Action action, JComponent focusOwner, int focusCondition)
	{
		for (KeyComponentPanel panel : menuItemShortcuts)
		{
			if (panel.settingName.equals(settingName))
			{
				panel.registerNewComponent(action, null, focusOwner);
				return;
			}
		}
		menuItemShortcuts.add(new KeyComponentPanel(settingName, itemName, hasDefaultKeyStroke, defaultKeyCode, defaultModifiers, action, null, focusOwner, focusCondition));
		menuItemsTable.revalidate();
		menuItemsTable.repaint();
	}
	
	/**
	 * Currently not used.
	 * There will only be one KeyComponentPanel for the same settingName.
	 * @param focusCondition one of JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, or JComponent.WHEN_IN_FOCUSED_WINDOW
	 */
	public void registerOtherShortcut(String settingName, String itemName, boolean hasDefaultKeyStroke, int defaultKeyCode, int defaultModifiers, Action action, JMenuItem menuItem, JComponent focusOwner, int focusCondition)
	{
		for (KeyComponentPanel panel : otherShortcuts)
		{
			if (panel.settingName.equals(settingName))
			{
				panel.registerNewComponent(action, menuItem, focusOwner);
				return;
			}
		}
		otherShortcuts.add(new KeyComponentPanel(settingName, itemName, hasDefaultKeyStroke, defaultKeyCode, defaultModifiers, action, menuItem, focusOwner, focusCondition));
		othersTable.revalidate();
		othersTable.repaint();
	}
	
	/**
	 * There will only be one KeyComponentPanel for the same settingName.
	 * @param focusCondition one of JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, or JComponent.WHEN_IN_FOCUSED_WINDOW
	 */
	public void registerOtherShortcut(String settingName, String itemName, boolean hasDefaultKeyStroke, int defaultKeyCode, int defaultModifiers, Action action, JComponent focusOwner, int focusCondition)
	{
		for (KeyComponentPanel panel : otherShortcuts)
		{
			if (panel.settingName.equals(settingName))
			{
				panel.registerNewComponent(action, null, focusOwner);
				return;
			}
		}
		otherShortcuts.add(new KeyComponentPanel(settingName, itemName, hasDefaultKeyStroke, defaultKeyCode, defaultModifiers, action, null, focusOwner, focusCondition));
		othersTable.revalidate();
		othersTable.repaint();
	}
	
	public KeyboardSettingsPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		this.preferenceFrame = preferenceFrame;
		
		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "keyboard_settings.xml"));
		
		JButton resetAll = new JButton("Reset All to Default");
		resetAll.addChangeListener(event ->
		{
			for (KeyComponentPanel k : menuItemShortcuts)
			{
				k.resetToDefault();
			}
			for (KeyComponentPanel k : otherShortcuts)
			{
				k.resetToDefault();
			}
			menuItemsTable.revalidate();
			menuItemsTable.repaint();
			
			othersTable.revalidate();
			othersTable.repaint();
		});
		JPanel resetAllButtonPanel = new JPanel();
		resetAllButtonPanel.add(resetAll);
		
		menuItemShortcuts = new ArrayList<>();
		menuItemsTable = new KeyBindingsTable(menuItemShortcuts);
		menuItemsScrollPane = new JScrollPane(menuItemsTable);
		menuItemsPanel = new JPanel(new BorderLayout());
		menuItemsPanel.add(menuItemsScrollPane, BorderLayout.CENTER);
		menuItemsPanel.setBorder(BorderFactory.createTitledBorder("Menu Item Shortcuts"));
		
		otherShortcuts = new ArrayList<>();
		othersTable = new KeyBindingsTable(otherShortcuts);
		othersScrollPane = new JScrollPane(othersTable);
		othersPanel = new JPanel(new BorderLayout());
		othersPanel.add(othersScrollPane, BorderLayout.CENTER);
		othersPanel.setBorder(BorderFactory.createTitledBorder("Other Shortcuts"));
		
		tablesPanel = new JPanel();
		tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
		tablesPanel.add(menuItemsPanel);
		tablesPanel.add(othersPanel);
		
		JPanel labelPanel = new JPanel();
		JLabel label = new JLabel("Click on a Shortcut to type a new keyboard combination for it, then deselect the button.");
		labelPanel.add(label);
		
		setLayout(new BorderLayout());
		add(labelPanel, BorderLayout.NORTH);
		add(tablesPanel, BorderLayout.CENTER);
		add(resetAllButtonPanel, BorderLayout.SOUTH);
		
		initializing = false;
	}
	
	public void loadSettings(SingleSettingsFile settingsFile)
	{
	}
	
	public void saveSettings(SingleSettingsFile settingsFile, boolean saveToFile)
	{
		Preferences node = settingsFile.node;
		for (KeyComponentPanel k : menuItemShortcuts)
		{
			k.saveSettings(node);
		}
		for (KeyComponentPanel k : otherShortcuts)
		{
			k.saveSettings(node);
		}
		if (saveToFile) settingsFile.save();
	}
	
	public void applyChanges()
	{
	}

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}