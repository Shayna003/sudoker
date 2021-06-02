package com.github.shayna003.sudoker.prefs.keys;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;
import java.util.*;

/**
 * This class contains a KeyComponent, and buttons to clear the key shortcut and reset to default state.
 * @since 5-9-2021
 */
@SuppressWarnings("CanBeFinal")
public class KeyComponentPanel extends JPanel implements PrefsComponent
{
	String settingName;
	public String itemName; // action name triggered by the shortcut 
	public KeyComponent keyComponent;
	JLabel optionsLabel;
	JButton clear;
	JButton reset;
	
	ArrayList<Action> actions;
	ArrayList<JMenuItem> menuItems; // not used often
	ArrayList<JComponent> focusOwners;
	
	int focusCondition;
	
	/**
	 * @param focusOwner can be a JMenuItem
	 * @param menuItem is for popup menus, where menuItem is the item in the popup, and focusOwner is the owner of focus
	 * @param focusCondition one of JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, or JComponent.WHEN_IN_FOCUSED_WINDOW
	 */
	public KeyComponentPanel(String settingName, String itemName, boolean hasDefaultKeyStroke, int defaultKeyCode, int defaultModifiers, Action action, JMenuItem menuItem, JComponent focusOwner, int focusCondition)
	{
		super(new FlowLayout(FlowLayout.LEFT, 5, 0));
		this.settingName = settingName;
		this.itemName = itemName;
		
		actions = new ArrayList<>();
		actions.add(action);
		
		menuItems = new ArrayList<>();
		menuItems.add(menuItem);
		
		focusOwners = new ArrayList<>();
		focusOwners.add(focusOwner);
		
		this.focusCondition = focusCondition;
		
		keyComponent = new KeyComponent(hasDefaultKeyStroke, defaultKeyCode, defaultModifiers);
		
		reset = new JButton("Reset");
		reset.addActionListener(event ->
		{
			resetToDefault();
		});
		clear = new JButton("Clear");
		clear.addActionListener(event ->
		{
			clearKeyStroke();
		});
		
		// otherwise when editing stops in a table, they still appear as they have focus
		keyComponent.setFocusPainted(false);
		reset.setFocusPainted(false);
		clear.setFocusPainted(false);
		
		optionsLabel = new JLabel("Options: ");
		
		add(keyComponent);
		add(optionsLabel);
		add(reset);
		add(clear);
		
		GeneralSettingsPanel.registerComponentAndSetFontSize(this);
		loadSettings(Application.keyboardSettingsPanel.settingsFile.node);
		setOpaque(false);
	}
	
	public void stopEditing()
	{
		if (keyComponent.isSelected())
		{
			keyComponent.setSelected(false);
			if (keyComponent.keyCode != 0)
			{
				keyComponent.setKeyStroke(keyComponent.keyCode, keyComponent.modifiers);
			}
		}
	}
	
	public void registerNewComponent(Action action, JMenuItem menuItem, JComponent focusOwner)
	{
		actions.add(action);
		menuItems.add(menuItem);
		focusOwners.add(focusOwner);
		applyChanges(focusOwners.size() - 1, null);
	}
	
	void applyChanges(int index, KeyStroke previousKeyStroke)
	{
		JComponent focusOwner = focusOwners.get(index);
		JMenuItem menuItem = menuItems.get(index);
		Action action = actions.get(index);
		
		if (focusOwner != null)
		{
			if (focusOwner instanceof JMenuItem)
			{
				((JMenuItem) focusOwner).setAccelerator(keyComponent.keyStroke);
			}
			else 
			{
				InputMap imap = focusOwner.getInputMap(focusCondition);
				ActionMap amap = focusOwner.getActionMap();
				
				imap.remove(previousKeyStroke);
				imap.put(keyComponent.keyStroke, settingName);
				
				amap.remove(settingName);
				amap.put(settingName, action);
			}
		}
		
		if (menuItem != null) menuItem.setAccelerator(keyComponent.keyStroke);
		
		if (action != null) action.putValue(Action.ACCELERATOR_KEY, keyComponent.keyStroke);
	}
	
	void applyChanges(KeyStroke previousKeyStroke)
	{
		for (int i = 0; i < focusOwners.size(); i++)
		{
			applyChanges(i, previousKeyStroke);
		}
	}
	
	public KeyStroke getKeyStroke()
	{
		return keyComponent.keyStroke;
	}
	
	@Override
	public void resetToDefault()
	{
		keyComponent.resetToDefault();
	}
	
	public void clearKeyStroke()
	{
		keyComponent.clearKeyStroke();
	}
	
	@Override
	public void saveSettings(Preferences node)
	{
		keyComponent.saveSettings(node);
	}
	
	@Override
	public void loadSettings(Preferences node)
	{
		keyComponent.loadSettings(node);
	}
	
	void setVisuals()
	{
		if (keyComponent.keyStroke == null)// ^ keyComponent.hasDefaultKeyStroke == false)
		{
			clear.setVisible(false);
			String keysStr = KeyEvent.getKeyText(keyComponent.defaultKeyCode);
			boolean defaultIsUnknown = keysStr.contains("Unknown");

			reset.setVisible(!defaultIsUnknown && keyComponent.hasDefaultKeyStroke);
		}
		else 
		{
			clear.setVisible(true);
			if (!keyComponent.hasDefaultKeyStroke)
			{
				reset.setVisible(true);
			}
			else 
			{
				String modifiersStr = KeyEvent.getModifiersExText(keyComponent.defaultModifiers);
				String keysStr = KeyEvent.getKeyText(keyComponent.defaultKeyCode);
				String keyText = modifiersStr + keysStr;

				reset.setVisible(!(modifiersStr + keysStr).equals(keyComponent.getText()));
			}
		}
		optionsLabel.setVisible(clear.isVisible() || reset.isVisible());
	}
	
	/**
	* This class enables detecting a series of keys for a keyboard shortcut setting.
	* It listens for key presses when the toggle button is selected, then 
	* makes a KeyStroke object representing the shortcut.
	* A KeyStroke in java can have multiple modifiers, but only one key value.
	* As this link https://github.com/TurboVNC/turbovnc/issues/194
	* Shows, some key combinations get interpreted to dead keys on Mac.
	* @since 5-8-2021
	*/ 
	@SuppressWarnings("CanBeFinal")
	public class KeyComponent extends JToggleButton implements PrefsComponent
	{
		int keyCode = 0;
		int modifiers = 0;
		
		boolean hasDefaultKeyStroke;
		int defaultKeyCode;
		int defaultModifiers;
		
		public KeyStroke keyStroke; // the keyStroke stored by this KeyComponent
		
		public KeyComponent(boolean hasDefaultKeyStroke, int defaultKeyCode, int defaultModifiers)
		{	
			this.hasDefaultKeyStroke = hasDefaultKeyStroke;
			this.defaultKeyCode = defaultKeyCode;
			this.defaultModifiers = defaultModifiers;
			
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent event)
				{
					if (isSelected())
					{
						int tmp = event.getKeyCode();
						if (tmp != KeyEvent.VK_UNDEFINED)
						{
							keyCode = tmp;
						}
						modifiers = modifiers | event.getModifiersEx();
					}
				}
				
				@Override
				public void keyReleased(KeyEvent event) {}
				
				@Override
				public void keyTyped(KeyEvent event) {}
			});
			
			addActionListener(event ->
			{
				if (!isSelected())
				{
					if (keyCode != 0)
					{
						setKeyStroke(keyCode, modifiers);
					}
				}
				else 
				{
					keyCode = 0;
					modifiers = 0;
				}
			});
		}
		
		void setKeyStroke(int keyCode, int modifiers)
		{
			String modifiersStr = KeyEvent.getModifiersExText(modifiers);
			String keysStr = KeyEvent.getKeyText(keyCode);
			if (!modifiersStr.contains(keysStr) && !keysStr.contains("Unknown")) // Unknown keyCode: 0x0
			{
				KeyStroke tmp = KeyStroke.getKeyStroke(keyCode, modifiers);
				
				if (tmp != null)
				{
					this.keyCode = keyCode;
					this.modifiers = modifiers;
					KeyStroke previousKeyStroke = keyStroke;
					keyStroke = tmp;
					setText(modifiersStr + keysStr);
					setVisuals();
					applyChanges(previousKeyStroke);
				}
			}
		}
		
		@Override
		public void resetToDefault()
		{
			if (hasDefaultKeyStroke)
			{
				String keysStr = KeyEvent.getKeyText(defaultKeyCode);
				boolean defaultIsUnknown = keysStr.contains("Unknown");
				
				if (defaultIsUnknown)
				{
					clearKeyStroke();
				}
				else 
				{
					setKeyStroke(defaultKeyCode, defaultModifiers);
				}
			}
			else 
			{
				clearKeyStroke();
			}
		}
		
		void clearKeyStroke()
		{
			KeyStroke previousKeyStroke = keyStroke;
			keyStroke = null;
			setText("None");
			setVisuals();
			applyChanges(previousKeyStroke);
		}
		
		@Override
		public void saveSettings(Preferences node)
		{
			node.putBoolean(settingName, keyStroke != null);
			if (keyStroke != null)
			{
				node.putInt(settingName + ".KeyCode", keyCode);
				node.putInt(settingName + ".Modifiers", modifiers);
			}	
		}
		
		@Override
		public void loadSettings(Preferences node)
		{
			boolean hasKeyStroke = node.getBoolean(settingName, hasDefaultKeyStroke);
			if (hasKeyStroke)
			{
				setKeyStroke(node.getInt(settingName + ".KeyCode", defaultKeyCode), node.getInt(settingName + ".Modifiers", defaultModifiers));
			}
			
			if (keyStroke == null && hasDefaultKeyStroke)
			{
				setKeyStroke(defaultKeyCode, defaultModifiers);
			}
			
			if (keyStroke == null)
			{
				setText("None");
				setVisuals();
			}
		}
	}
	
	/**
	 * For renderer display in KeyBindingsTable.
	 */
	@Override
	public String toString()
	{
		return keyComponent.getText();
	}
}