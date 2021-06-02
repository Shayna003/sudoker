package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.logging.*;
import java.util.prefs.*;

/**
 * Might consider renaming this class though
 * Current classes using this class: Application, SettingsFileChooserPanel
 * last modified: 3-23-2021
 */
public class PreferenceDialogs
{
	/**
	 * Contains a messagePanel and a checkBox to support Don't show again
	 * Intended to be the message used in JOptionPane.show calls
	 */
	@SuppressWarnings("CanBeFinal")
	public static class SimpleConcealableMessage extends JPanel
	{
		JPanel messagePanel;
		String settingName;
		JCheckBox dontShowAgain;

		public SimpleConcealableMessage()
		{
			setLayout(new BorderLayout());
			messagePanel = new JPanel();
			dontShowAgain = new JCheckBox("Don't show this message again", false);
			add(messagePanel, BorderLayout.CENTER);
			add(dontShowAgain, BorderLayout.SOUTH);
			dontShowAgain.addActionListener(event ->
			{
				Application.showDialogs.putBoolean(settingName, !dontShowAgain.isSelected());
				Application.generalSettingsFile.save();
			});
		}
		
		/**
		 * @param message if message is a Component, will be added to messagePanel directly. Else, adds a JLabel with text
		 * set to message.toString() to messagePanel.
		 */
		public SimpleConcealableMessage updateInfo(String settingName, Object message)
		{
			dontShowAgain.setSelected(false);
			this.settingName = settingName;
			messagePanel.removeAll();
			if (message instanceof Component)
			{
				messagePanel.add((Component) message);
				Application.generalSettingsPanel.setComponentFontSizes((Component) message);
			}
			else
			{
				JLabel l = new JLabel(message.toString(), SwingConstants.CENTER);
				Application.generalSettingsPanel.setComponentFontSizes(l);
				messagePanel.add(l);
			}
			return this;
		}
	}
	
	public static DirectoryMessage directoryMessage;
	public static SimpleConcealableMessage simpleConcealableMessage;
	
	public static boolean shouldShowMessage(String settingName)
	{
		return Application.showDialogs.getBoolean(settingName, true);
	}

	/**
	 * @param message if message is a Component, will be added to messagePanel directly. Else, adds a JLabel with text
	 * set to message.toString() to messagePanel.
	 */
	public static SimpleConcealableMessage getSimpleConcealableMessage(String settingName, Object message)
	{
		if (simpleConcealableMessage == null)
		{
			simpleConcealableMessage = new SimpleConcealableMessage();
			GeneralSettingsPanel.registerComponentAndSetFontSize(simpleConcealableMessage);
		}
		return simpleConcealableMessage.updateInfo(settingName, message);
	}
	
	/**
	 * @param message if message is a Component, will be added to messagePanel directly. Else, adds a JLabel with text
	 * set to message.toString() to messagePanel.
	 */
	public static DirectoryMessage getDirectoryMessage(String settingName, Object message, File file)
	{
		if (directoryMessage == null)
		{
			directoryMessage = new DirectoryMessage();
			
			// for the directory message that pops up after application folder has been created/relocated
			if (Application.generalSettingsPanel != null)
			{
				GeneralSettingsPanel.registerComponentAndSetFontSize(directoryMessage);
			}
			else 
			{
				Application.componentsToUpdateUI.add(directoryMessage);
			}
		}
		return directoryMessage.updateInfo(settingName, message, file);
	}
	
	/**
	 * This class stores the message object to be passed in JOptionPane methods
	 * For a dialog that pops up that requires showing a directory
	 */
	@SuppressWarnings("CanBeFinal")
	public static class DirectoryMessage extends JPanel
	{
		File file;
		JTextField directory;
		JPanel messagePanel;
		
		String settingName;
		JCheckBox dontShowAgain;
		JPanel directoryPanel;
		JPanel checkBoxPanel;
		JButton copyButton;
		public JButton showLocation;
		
		@Override
		public void updateUI()
		{
			super.updateUI();
			if (showLocation != null)
			{
				Icon c = UIManager.getIcon("FileView.directoryIcon");
				if (c != null)
				{
					showLocation.setText(null);
					showLocation.setIcon(c);
				}
				else 
				{
					showLocation.setIcon(null);
					showLocation.setText("Show Location");
				}
			}
		}
		
		public DirectoryMessage()
		{
			setLayout(new BorderLayout());
			directory = new JTextField(30);
			directory.setDragEnabled(true);
			directory.setEditable(false);
			directory.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			
			messagePanel = new JPanel();

			directoryPanel = new JPanel();
			directoryPanel.add(directory);
			copyButton = new JButton("Copy");
			copyButton.setToolTipText("Copy to System Clipboard");
			copyButton.addActionListener(event ->
			{
				StringSelection selection = new StringSelection(directory.getText());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			});
			directoryPanel.add(copyButton);
			
			Icon c = UIManager.getIcon("FileView.directoryIcon");
			if (c != null)
			{
				showLocation = new JButton(c);
			}
			else 
			{
				showLocation = new JButton("Show Location");
			}
			
			showLocation.setToolTipText("Show folder location");
			showLocation.addActionListener(event ->
			{
				try 
				{
					//this works because SettingsFileChooserPanel uses the exportChooser's current directory
					Desktop.getDesktop().open(file);//.getParentFile());
				}
				catch (IOException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "showLocation's ActionListener's actionPerformed ", "Error when opening the file " + file, e);
					JOptionPane.showMessageDialog(DirectoryMessage.this, "An error occurred when trying to show file location.", "Error", JOptionPane.ERROR_MESSAGE);
				}	
			});
			directoryPanel.add(showLocation);
			
			add(messagePanel, BorderLayout.NORTH);
			add(directoryPanel, BorderLayout.CENTER);
			
			dontShowAgain = new JCheckBox("Don't show this message again", false);
			dontShowAgain.addActionListener(event ->
			{
				Application.showDialogs.putBoolean(settingName, !dontShowAgain.isSelected());
			});
			checkBoxPanel = new JPanel();
			((FlowLayout)(checkBoxPanel.getLayout())).setAlignment(FlowLayout.RIGHT);
			checkBoxPanel.add(dontShowAgain);
			add(checkBoxPanel, BorderLayout.SOUTH);
		}
		
		/**
		 * @param message if message is a Component, will be added to messagePanel directly. Else, adds a JLabel with text
		 * set to message.toString() to messagePanel.
		 */
		public DirectoryMessage updateInfo(String settingName, Object message, File file)
		{
			dontShowAgain.setSelected(false);
			this.settingName = settingName;
			messagePanel.removeAll();
			if (message instanceof Component)
			{
				messagePanel.add((Component) message);
				
				// for the directory message that pops up after application folder has been created/relocated
				if (Application.generalSettingsPanel != null)
				{
					Application.generalSettingsPanel.setComponentFontSizes((Component) message);
				}
			}
			else
			{
				JLabel l = new JLabel(message.toString(), SwingConstants.CENTER);
				
				// for the directory message that pops up after application folder has been created/relocated
				if (Application.generalSettingsPanel != null)
				{
					Application.generalSettingsPanel.setComponentFontSizes(l);
				}
				messagePanel.add(l);
			}
			
			this.file = file;
			try 
			{
				directory.setText(file.getCanonicalPath());
			} 
			catch (IOException e) 
			{
				directory.setText(file.getPath());
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "updateInfo", "Error when calling getCanonicalPath for file " + file, e);
			}
			return this;
		}
	}
	
	/**
	 * Used by SettingsFileChooserPanel.
	 */
	public static boolean shouldShowDialog(PreferenceChangeEvent event)
	{
		if (event.getNewValue() == null) 
		{
			// node removed, default state for showing dialog is true
			// note: node.put(String key, (String) null) results in NullPointerException
			return true;
		}
		else 
		{
			// note: Boolean.parseBoolean(null) returns false
			return Boolean.parseBoolean(event.getNewValue());
		}
	}
}