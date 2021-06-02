package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.plaf.metal.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import java.util.prefs.*;
import java.io.*;
import java.nio.file.*;

/**
 * This class enables the user to choose a preset or a theme
 * @version 0.00 
 * @since 2020-12-8
 */
@SuppressWarnings("CanBeFinal")
public class SettingsFileChooserPanel extends JPanel
{
	String settingName;
	String settingNamePlural;
	String settingNameCap;
	String settingNameCapPlural;
	
	File settings_directory; //defaults and customs are located in this directory
	
	//visible to users
	File defaults_folder; 
	File customs_folder;
	
	public JSplitPane split_pane;
	
	//located in jar file
	HashMap<String,InputStream> application_defaults;
	
	ArrayList<SettingsFile> default_files;
	ArrayList<SettingsFile> custom_files;
	
	public SettingsFile selectedSetting;
	
	DefaultSettingsTable defaultsTable;
	CustomSettingsTable customsTable;
	SettingsPanel settingsPanel;
	PreferenceFrame preferenceFrame;
	
	Preferences selectedSettingPreference;
	File selectedSettingDataFile;
	
	/**
	 * A SettingsPanel should pass the call of saveSettingsToFiles()
	 * to this function.
	 * Saves node Preferences to their corresponding files.
	 */
	public void saveSettingsToFiles()
	{
		for (SettingsFile settingsFile : default_files)
		{
			settingsFile.save();
		}
		
		for (SettingsFile settingsFile : custom_files)
		{
			settingsFile.save();
		}
		
		selectedSettingPreference.put("setting_name", selectedSetting.getName());
		selectedSettingPreference.putBoolean("is_default", selectedSetting.isDefault);
		
		try 
		{
			selectedSettingPreference.flush();
		}
		catch (BackingStoreException e) 
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "saveSettingsToFiles", "Error when flushing selectedSettingPreference", e);
		}
		
		try (FileOutputStream out = new FileOutputStream(selectedSettingDataFile))
		{
			selectedSettingPreference.exportSubtree(out);
		}
		catch (IOException | BackingStoreException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "saveSettingsToFiles", "Error when exporting  selectedSettingPreference to file " + selectedSettingDataFile, e);
		}
	}
	
	class DefaultSettingsTable extends JTable
	{
		public DefaultSettingsTable(TableModel model)
		{
			super(model);
			getColumnModel().getColumn(0).setHeaderValue("Default " + settingNameCapPlural);
		}
		
		public void valueChanged(ListSelectionEvent event)
		{
			super.valueChanged(event);

			//somehow is called twice regardless of calling super.valueChanged(event) or not
			if (getSelectedRow() >= 0)
			{
				if (customsTable.getCellEditor() != null) // there is a cell editor only if you start the editing process
				{
					customsTable.getCellEditor().stopCellEditing();
				}
				
				customsTable.clearSelection();
				requestFocus();
				SettingsFile tmp = (SettingsFile) getValueAt(getSelectedRow(), 0);
				updateSelection(selectedSetting, tmp);
			}
		}
	}
	
	class CustomSettingsTable extends JTable
	{
		public CustomSettingsTable(TableModel model)
		{
			super(model);
			getColumnModel().getColumn(0).setHeaderValue("Custom " + settingNameCapPlural);
		}
		
		public void valueChanged(ListSelectionEvent event)
		{
			super.valueChanged(event);
			//somehow is called twice regardless of calling super.valueChanged(event) or not
			
			if (getSelectedRow() >= 0)
			{
				if (defaultsTable.getCellEditor() != null)
				{
					defaultsTable.getCellEditor().stopCellEditing();
				}
				
				defaultsTable.clearSelection();
				requestFocus();
				
				SettingsFile tmp = (SettingsFile) getValueAt(getSelectedRow(), 0);
				updateSelection(selectedSetting, tmp);
			}
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	class SettingsListModel extends AbstractTableModel
	{
		ArrayList<SettingsFile> settingsFiles;
		
		public SettingsListModel(ArrayList<SettingsFile> settingsFiles)
		{
			this.settingsFiles = settingsFiles;
		}
		
		public boolean isCellEditable(int r, int c)
		{
			return settingsFiles != default_files;
		}
		
		public int getRowCount()
		{
			return settingsFiles.size();
		}
		
		public int getColumnCount()
		{
			return 1;
		}
		
		public Object getValueAt(int row, int column)
		{
			return settingsFiles.get(row);
		}
		
		public void setValueAt(Object obj, int r, int c)
		{
			SettingsFile tmp = (SettingsFile) getValueAt(r, c);
			
			//can't change the name of a custom setting to a default setting's name
			if (application_defaults.containsKey(obj.toString())) // only custom settings can be renamed
			{
				JOptionPane.showMessageDialog(preferenceFrame, "Can't complete the rename command because there is a default " + settingName + " named \"" + obj + "\".", "Can't rename", JOptionPane.INFORMATION_MESSAGE, null);
				return;
			}
			
			if (((SettingsFile) getValueAt(r, c)).setName(obj.toString(), default_files, custom_files, application_defaults))
			{
				if (settingsFiles == custom_files)
				{
					sortCustoms();
					int index = custom_files.indexOf(tmp);
					customsTable.setRowSelectionInterval(index, index);
				}
				else 
				{
					sortDefaults();
					int index = default_files.indexOf(tmp);
					defaultsTable.setRowSelectionInterval(index, index);
				}
				this.fireTableCellUpdated(r, c);
			}
		}
	}
	
	TableModel defaults_model;
	TableModel customs_model;
	
	JPopupMenu popup;
	
	Action newSetting; // new setting with default values
	Action duplicate;
	Action rename;
	
	// at least one settings will remain
	Action delete;
	Action deleteAllDefaults;
	Action deleteAllCustoms;
	
	Action restore;
	Action reset; //only affects defaults, restore settings only restores to last saved settings, but reset resets the preset to its original state upon installing the application
	Action resetAll; //only affects defaults
	
	Action importSetting;
	Action exportSetting;
	
	Action showFileLocation;
	
	FileNameExtensionFilter xmlFilter;
	public JFileChooser xmlChooser;
	
	public JFileChooser exportChooser;
	
	boolean initializing;
	
	void setSelectedSetting(SettingsFile newSelectedSetting)
	{
		selectedSetting = newSelectedSetting;

		try
		{
			selectedSetting.loadSettingsFromFile();
		}
		catch (IOException | InvalidPreferencesFormatException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "setSelectedSetting", "Error when trying to load settings from file " + selectedSetting.file, e);
		}
		settingsPanel.loadSettings(selectedSetting);
	}
	
	/**
	 * Called when the user clicks on a different setting in the tables
	 */
	public void updateSelection(SettingsFile oldSetting, SettingsFile newSetting)
	{
		boolean canDeleteAll = custom_files.size() > 0 && default_files.size() > 0;
		if (deleteAllDefaults.isEnabled() != canDeleteAll)
		{
			deleteAllDefaults.setEnabled(canDeleteAll);
			deleteAllCustoms.setEnabled(canDeleteAll);
		}
		
		if (initializing)
		{
			reset.setEnabled(newSetting.isDefault);
			rename.setEnabled(!newSetting.isDefault);
			setSelectedSetting(newSetting);
		}
		else if (oldSetting != newSetting)
		{
			if (oldSetting.isDefault != newSetting.isDefault)
			{
				reset.setEnabled(newSetting.isDefault);
				rename.setEnabled(!newSetting.isDefault);
			}
			
			if (!oldSetting.deleted) settingsPanel.saveSettings(oldSetting, false);
			setSelectedSetting(newSetting);
		}
	}
	
	/**
	 * Called when the user deletes a single setting
	 */
	// note: currently this function is never called with savePreviousSelectedSetting set to true
	public void updateSelection(boolean savePreviousSelectedSetting)
	{
		if (savePreviousSelectedSetting) settingsPanel.saveSettings(selectedSetting, false);
		
		SettingsFile oldSetting = selectedSetting;
		
		if (defaultsTable.getSelectedRow() >= 0) 
		{
			setSelectedSetting(default_files.get(defaultsTable.getSelectedRow()));
		}
		else 
		{
			setSelectedSetting(custom_files.get(customsTable.getSelectedRow()));
		}
		
		if (oldSetting.isDefault != selectedSetting.isDefault)
		{
			reset.setEnabled(selectedSetting.isDefault);
			rename.setEnabled(!selectedSetting.isDefault);
		}
	}
	
	public void sortDefaults()
	{
		default_files.sort(SettingsFile.nameComparator);   
	}
	
	public void sortCustoms()
	{
		custom_files.sort(SettingsFile.nameComparator);
	}
	
	void stopCellEditing()
	{
		TableCellEditor e = defaultsTable.getCellEditor();
		if (e != null)
		{
			e.stopCellEditing();
		}
		
		e = customsTable.getCellEditor();
		if (e != null)
		{
			e.stopCellEditing();
		}
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (customsTable != null) 
		{
			customsTable.updateUI();
			defaultsTable.updateUI();
			customsTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
			customsTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
			defaultsTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
			defaultsTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
		}	
	}
	
	/**
	 * @param settingNameCapPlural should be the same as the tab title in PreferenceFrame
	 */
	public SettingsFileChooserPanel(File settings_directory, String settingName, String settingNamePlural, String settingNameCap, String settingNameCapPlural, MultipleSettingsPanel settingsPanel, PreferenceFrame preferenceFrame, File defaults_folder, File customs_folder, HashMap<String, InputStream> application_defaults, ArrayList<SettingsFile> default_files, ArrayList<SettingsFile> custom_files, File selectedSettingDataFile)
	{
		long start = System.currentTimeMillis();
		Application.prefsLogger.entering("SettingsFileChooserPanel", "init");
		initializing = true;
		this.default_files = default_files;
		this.custom_files = custom_files;
		this.settingsPanel = settingsPanel;
		this.preferenceFrame = preferenceFrame;
		if (!settings_directory.exists()) settings_directory.mkdirs();
		this.settings_directory = settings_directory;
		
		this.settingName = settingName;
		this.settingNamePlural = settingNamePlural;
		this.settingNameCap = settingNameCap;
		this.settingNameCapPlural = settingNameCapPlural;
		
		sortDefaults();
		sortCustoms();
		
		this.defaults_folder = defaults_folder;
		this.customs_folder = customs_folder;
		this.application_defaults = application_defaults;
		
		this.selectedSettingDataFile = selectedSettingDataFile;
		
		
		xmlFilter = new FileNameExtensionFilter("XML Files", "xml");
		xmlChooser = new JFileChooser();

		Application.prefsLogger.log(Level.FINE, "set file filter to xmlFilter");
		xmlChooser.setFileFilter(xmlFilter);
		xmlChooser.setAcceptAllFileFilterUsed(false);
		
		if (!defaults_folder.exists())
		{
			defaults_folder.mkdirs();
		}
		Application.prefsLogger.log(Level.FINE, "xmlChooser.setCurrentDirectory(defaults_folder); defaults_folder: " + defaults_folder);
		xmlChooser.setCurrentDirectory(defaults_folder);
		
		exportChooser = new JFileChooser();
		if (!customs_folder.exists())
		{
			customs_folder.mkdirs();
		}
		Application.prefsLogger.log(Level.FINE, "exportChooser.setCurrentDirectory(customs_folder); customs_folder: " + customs_folder);
		exportChooser.setCurrentDirectory(customs_folder);
		
		Application.prefsLogger.log(Level.FINE, "init tables");
		popup = new JPopupMenu();
				
		defaults_model = new SettingsListModel(default_files);
		defaultsTable = new DefaultSettingsTable(defaults_model);
		defaultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		defaultsTable.getColumnModel().getColumn(0).setResizable(false);
		defaultsTable.getTableHeader().setReorderingAllowed(false);
		defaultsTable.setName("defaults");
		defaultsTable.setFillsViewportHeight(true);
		
		customs_model = new SettingsListModel(custom_files);
		customsTable = new CustomSettingsTable(customs_model);
		TableColumn column = customsTable.getColumnModel().getColumn(0);
		JTextField editorTextField = new JTextField();
		DefaultCellEditor editor = new DefaultCellEditor(editorTextField)
		{
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
			{
				// to override the original border of "setBorder(BorderFactory.createLineBorder(Color.BLACK));" in the default GenericEditor for Object.class of JTable, so that the cell being edited will be visible in my DarkMetalTheme of MetalLookAndFeel.
				if (UIManager.getLookAndFeel() instanceof MetalLookAndFeel && MetalLookAndFeel.getCurrentTheme() instanceof DarkMetalTheme)
				{
					editorTextField.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.selectionForeground")));
				}
				else 
				{
					// the default behaviour in the source code
					editorTextField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				}
				editorTextField.setText(value.toString());
				return editorTextField;
			}
		};
		
		customsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		customsTable.getColumnModel().getColumn(0).setResizable(false);
		customsTable.getTableHeader().setReorderingAllowed(false);
		customsTable.setName("Custom");
		customsTable.setFillsViewportHeight(true);
		customsTable.setDefaultEditor(Object.class, editor);
		
		JScrollPane defaults_pane = new JScrollPane(defaultsTable);
		defaults_pane.setPreferredSize(new Dimension(100, 200));
		
		JScrollPane customs_pane = new JScrollPane(customsTable);
		customs_pane.setPreferredSize(new Dimension(100, 200));

		split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaults_pane, customs_pane);
		split_pane.setOneTouchExpandable(true);
		split_pane.setDividerLocation(0.5);
		split_pane.setResizeWeight(0.5);
		
		
		setLayout(new BorderLayout());
		
		add(split_pane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
	
		Dimension max_dimension = SwingUtil.getMaxDimension(SwingUtil.getStringBounds(new String[] {"+", "-", "?", "\u27F2", "..."}, getFont(), getFontMetrics(getFont()).getFontRenderContext()));
		
		Dimension square_dimension = max_dimension.width > max_dimension.height ? new Dimension((int) (max_dimension.width * 1.5), (int) (max_dimension.width * 1.5)) : new Dimension((int) (max_dimension.height * 1.5), (int) (max_dimension.height * 1.5));

		RectangularButton add = new RectangularButton("+", square_dimension, false);
		add.setFont(getFont());
		add.setToolTipText("Create a New Custom " + settingNameCap);
		add.addActionListener(event ->
		{
			newSetting.actionPerformed(event);
		});
		
		RectangularButton remove = new RectangularButton("-", square_dimension, false);
		remove.setFont(getFont());
		remove.setToolTipText("Delete the Selected " + settingNameCap);
		
		RectangularButton options = new RectangularButton("...", square_dimension, false);
		options.setFont(getFont());
		options.setToolTipText("More " + settingNameCap + " Options...");
		
		options.addActionListener(event ->
		{
			Point point = SwingUtilities.convertPoint(buttonPanel, options.getLocation(), this);
			popup.show(this, point.x + options.getWidth(), point.y);
		});
		
		newSetting = new AbstractAction("New")
		{
			public void actionPerformed(ActionEvent event)
			{	
				SettingsFile newSettingsFile = selectedSetting.newFromDefault(SettingsFile.getNewFileName("Untitled", default_files, custom_files, application_defaults, false));
				
				custom_files.add(newSettingsFile);
				sortCustoms();
				int i = custom_files.indexOf(newSettingsFile);
				customsTable.setRowSelectionInterval(i, i);
				customsTable.editCellAt(i, 0);
				
				customsTable.revalidate();
				customsTable.repaint();
				
				newSettingsFile.save();
			}
		};
		newSetting.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
		Application.keyboardSettingsPanel.registerOtherShortcut("new" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "New " + settingNameCap), true, KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), newSetting, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		rename = new AbstractAction("Rename")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (selectedSetting.isDefault)
				{
					defaultsTable.editCellAt(defaultsTable.getSelectedRow(), 0);
				}
				else 
				{
					customsTable.editCellAt(customsTable.getSelectedRow(), 0);
				}
			}
		};
		rename.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		Application.keyboardSettingsPanel.registerOtherShortcut("rename" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Rename " + settingNameCap), true, KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), rename, SettingsFileChooserPanel.this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		
		duplicate = new AbstractAction("Duplicate")
		{
			public void actionPerformed(ActionEvent event)
			{
				try 
				{
					SettingsFile newSettingsFile = selectedSetting.duplicate(selectedSetting.getName(), settingsPanel, default_files, custom_files, application_defaults);
					custom_files.add(newSettingsFile);
					sortCustoms();
					int i = custom_files.indexOf(newSettingsFile);
					customsTable.setRowSelectionInterval(i, i);
					customsTable.editCellAt(i, 0);
					customsTable.revalidate();
					customsTable.repaint();
				}
				catch (IOException | InvalidPreferencesFormatException | BackingStoreException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "duplicate's actionPerformed", "Error when duplicating the file " + selectedSetting.file + " for the " + settingName + " " + selectedSetting, e);
					JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while duplicating the " + settingName + " \"" + selectedSetting + "\".", "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
		};
		duplicate.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("duplicate" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Duplicate " + settingNameCap), true, KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), duplicate, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		final String deleteSettingName = "ConfirmDelete" + settingName;
		delete = new AbstractAction(PreferenceDialogs.shouldShowMessage(deleteSettingName) ? "Delete..." : "Delete")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (default_files.size() + custom_files.size() > 1)
				{
					boolean ok = true;
					
					if (PreferenceDialogs.shouldShowMessage(deleteSettingName))
					{
						JPanel p = new JPanel(new GridBagLayout());
						p.add(new JLabel("Are you sure you want to delete the " + (selectedSetting.isDefault ? "default " : "custom ") + settingName + " \"" + selectedSetting.getName() + "\"? The file will be moved to the trash."), new GBC(0, 0).setAnchor(GBC.WEST));
						p.add(new JLabel(selectedSetting.isDefault ? "Deleted default " + settingNamePlural + " can be restored using the \"Reset and Restore All Default " + settingNameCapPlural + "' command." : "This action cannot be undone."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));
						
						ok = JOptionPane.showOptionDialog(preferenceFrame, PreferenceDialogs.getSimpleConcealableMessage(deleteSettingName, p), "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Yes", "No"}, "No") == 0;
					}
					
					if (ok)
					{
						stopCellEditing();
						boolean success = selectedSetting.delete(false);
						if (selectedSetting.isDefault)
						{
							default_files.remove(selectedSetting);
							if (default_files.size() > 0)
							{
								if (defaultsTable.getSelectedRow() > default_files.size() - 1)
								{
									defaultsTable.setRowSelectionInterval(default_files.size() - 1, default_files.size() - 1);
								}
								else
								{
									updateSelection(false);
								}
							}
							else
							{
								customsTable.setRowSelectionInterval(0, 0);
							}
							defaultsTable.revalidate();
							defaultsTable.repaint();
						}
						else
						{
							custom_files.remove(selectedSetting);
							if (custom_files.size() > 0)
							{
								if (customsTable.getSelectedRow() > custom_files.size() - 1)
								{
									customsTable.setRowSelectionInterval(custom_files.size() - 1, custom_files.size() - 1);
								}
								else
								{
									updateSelection(false);
								}
							}
							else
							{
								defaultsTable.setRowSelectionInterval(0, 0);
							}
							customsTable.revalidate();
							customsTable.repaint();
						}
					}
				}
				else 
				{
					JOptionPane.showMessageDialog(preferenceFrame, "You cannot delete the only " + settingName + " left.", "Cannot Delete", JOptionPane.WARNING_MESSAGE);
				}
			}
		};
		delete.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("delete" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Delete " + settingNameCap), true, KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), delete, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		remove.addActionListener(event ->
		{
			delete.actionPerformed(event);
		});
		
		final String deleteAllDefaultsSettingName = "ConfirmDeleteAllDefault" + settingNamePlural;
		deleteAllDefaults = new AbstractAction(PreferenceDialogs.shouldShowMessage(deleteAllDefaultsSettingName) ? "Delete All Default " + settingNameCapPlural + "..." : "Delete All Default " + settingNameCapPlural)
		{
			public void actionPerformed(ActionEvent event)
			{	
				boolean ok = true;
				
				if (PreferenceDialogs.shouldShowMessage(deleteAllDefaultsSettingName))
				{
					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel("Are you sure you want to delete all default " + settingNamePlural + "? The files will be moved to the trash."), new GBC(0, 0).setAnchor(GBC.WEST));
					p.add(new JLabel("You can recover default " + settingNamePlural + " later with the command \"" + "Recover and Reset All Default " + settingNameCapPlural + "\"."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));
					ok = JOptionPane.showOptionDialog(preferenceFrame, PreferenceDialogs.getSimpleConcealableMessage(deleteAllDefaultsSettingName, p), "Are You Sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0;
				}
				
				if (ok)
				{
					stopCellEditing();
					for (SettingsFile settingsFile : default_files)
					{
						settingsFile.delete(false);
					}
					default_files.clear();
					
					if (selectedSetting.isDefault)
					{
						customsTable.setRowSelectionInterval(0, 0);
					}
					
					defaultsTable.revalidate();
					defaultsTable.repaint();
				}	
			}
		};
		deleteAllDefaults.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteAllDefault" + settingNameCapPlural, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Delete All Default " + settingNameCapPlural), true, KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, deleteAllDefaults, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		//can't when currently selecting one
		final String deleteAllCustomsSettingName = "ConfirmDeleteAllCustom" + settingNamePlural;
		deleteAllCustoms = new AbstractAction(PreferenceDialogs.shouldShowMessage(deleteAllCustomsSettingName) ? "Delete All Custom " + settingNameCapPlural + "..." : "Delete All Custom " + settingNameCapPlural)
		{
			public void actionPerformed(ActionEvent event)
			{
				boolean ok = true;
				
				if (PreferenceDialogs.shouldShowMessage(deleteAllCustomsSettingName))
				{
					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel("Are you sure you want to delete all custom " + settingNamePlural + "? The files will be moved to the trash."), new GBC(0, 0).setAnchor(GBC.WEST));
					p.add(new JLabel("This action cannot be undone."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));

					ok = JOptionPane.showOptionDialog(preferenceFrame, PreferenceDialogs.getSimpleConcealableMessage(deleteAllCustomsSettingName, p), "Are You Sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0;
				}
				
				if (ok)
				{
					stopCellEditing();
					for (SettingsFile settingsFile : custom_files)
					{
						settingsFile.delete(false);
					}
					custom_files.clear();
					
					if (!selectedSetting.isDefault)
					{
						defaultsTable.setRowSelectionInterval(0, 0);
					}
					
					customsTable.revalidate();
					customsTable.repaint();
				}
			}
		};
		deleteAllCustoms.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteAllCustom" + settingNameCapPlural, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Delete All Custom " + settingNameCapPlural), false, 0, 0, deleteAllCustoms, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		importSetting = new AbstractAction("Import...")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (xmlChooser.showOpenDialog(preferenceFrame) == JFileChooser.APPROVE_OPTION)
				{
					File selectedFile = xmlChooser.getSelectedFile();
					
					try 
					{
						SettingsFile importedSetting = selectedSetting.newFromExistingFile(selectedFile, default_files, custom_files, application_defaults, settingsPanel);
						custom_files.add(importedSetting);
						sortCustoms();
						int index = custom_files.indexOf(importedSetting);
						
						customsTable.setRowSelectionInterval(index, index);
						customsTable.editCellAt(index, 0);
						
					} 
					catch(InvalidPreferencesFormatException e)
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "importSetting's actionPerformed", "Error when importing the file " + selectedFile, e);
						JOptionPane.showMessageDialog(preferenceFrame, "The file \"" + selectedFile + "\" is not in the correct format.", "Error", JOptionPane.ERROR_MESSAGE);
					}
					catch (IOException e) 
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "importSetting's actionPerformed", "Error when importing the file " + selectedFile, e);
						JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while importing the file \"" + selectedFile + "\".", "Error", JOptionPane.ERROR_MESSAGE);
					}
				} 
			}
		};
		importSetting.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		Application.keyboardSettingsPanel.registerOtherShortcut("import" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Import " + settingNameCap), true, KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), importSetting, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		exportSetting = new AbstractAction("Export...")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (exportChooser.showSaveDialog(preferenceFrame) == JFileChooser.APPROVE_OPTION)
				{
					File targetFile = exportChooser.getSelectedFile();
					String fileName = targetFile.getName();
					
					if (!SettingsFile.isValidFileName(fileName))
					{
						JOptionPane.showMessageDialog(preferenceFrame, "Please supply a valid file name! Try avoid using special characters.", "File Name is Invalid", JOptionPane.WARNING_MESSAGE);
						actionPerformed(event);
						return;
					}
					
					String extension = PreferenceFrame.getFileExtension(targetFile);
					String savedFileName = extension != null && extension.equalsIgnoreCase("xml") ? fileName : fileName + ".xml";
					
					File[] otherFiles = exportChooser.getCurrentDirectory().listFiles(PreferenceFrame.xmlFilter);
					if (otherFiles != null)
					{
						for (int i = 0; i < otherFiles.length; i++)
						{
							if (otherFiles[i].getName().equals(savedFileName))
							{
								JOptionPane.showMessageDialog(preferenceFrame, "There is already a file with the name \"" + savedFileName + "\". Please supply a different name.", "File Exists", JOptionPane.INFORMATION_MESSAGE);
								actionPerformed(event);
								return;
							}
						}
					}

					try 
					{
						selectedSetting.exportToTargetFile(new File(targetFile.getParent() + File.separator + savedFileName), settingsPanel);
						if (PreferenceDialogs.shouldShowMessage(settingName + "ExportSuccess"))
						{
							JOptionPane.showMessageDialog(preferenceFrame, PreferenceDialogs.getDirectoryMessage(settingName + "ExportSuccess", "The " + settingName + " \"" + selectedSetting.getName() + "\" " + " has been exported to the file \"" + savedFileName + "\" in the directory", exportChooser.getCurrentDirectory()), "Save Success", JOptionPane.INFORMATION_MESSAGE);
						}
					} 
					catch (IOException | BackingStoreException e) 
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "exportSetting's actionPerformed", "Error when exporting the " + settingName + " \"" + selectedSetting.getName() + "\" to file " + targetFile, e);
						JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while exporting the " + settingName + " \"" + selectedSetting.getName() + "\".", "Error", JOptionPane.ERROR_MESSAGE, null);
					}
				}
			}
		};
		exportSetting.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		Application.keyboardSettingsPanel.registerOtherShortcut("export" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Export " + settingNameCap), true, KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), exportSetting, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		final String restoreSettingName = "Restore" + settingName + "ToPreviousSave";
		restore = new AbstractAction(PreferenceDialogs.shouldShowMessage(restoreSettingName) ? "Restore to Previous Save..." : "Restore to Previous Save")
		{
			public void actionPerformed(ActionEvent event)
			{
				boolean ok = true;
				if (PreferenceDialogs.shouldShowMessage(restoreSettingName))
				{
					ok = JOptionPane.showOptionDialog(preferenceFrame, 					PreferenceDialogs.getSimpleConcealableMessage(restoreSettingName, "Are you sure you want to restore the " + settingName + " \"" + selectedSetting + "\" to its previous save?"), "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0;
				}
				if (ok)
				{
					try 
					{
						selectedSetting.loadSettingsFromFile();
						settingsPanel.loadSettings(selectedSetting);
					} 
					catch (IOException | InvalidPreferencesFormatException e) 
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "restore's actionPerformed", "Error when loading settings from file " + selectedSetting.file, e);
						JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while restoring the " + settingName + "\"" + selectedSetting.getName() + "\" to its previous save.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		};
		restore.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		Application.keyboardSettingsPanel.registerOtherShortcut("restore" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Restore selected " + settingNameCap + " to Previous Save"), true, KeyEvent.VK_R, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, restore, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		RectangularButton restoreButton = new RectangularButton("\u27F2", square_dimension, false);
		restoreButton.addActionListener(event ->
		{
			restore.actionPerformed(event);
		});
		restoreButton.setFont(getFont());
		restoreButton.setToolTipText("Restore to Previous Save...");
		
		buttonPanel.add(add);
		buttonPanel.add(remove);
		buttonPanel.add(restoreButton);
		buttonPanel.add(options);
		//HelpButton help = new HelpButton("Help on " + settingNameCapPlural, null);
		//buttonPanel.add(help);
		add(buttonPanel, BorderLayout.SOUTH);
		
		
		final String resetSettingName = "resetDefault" + settingName + "Confirm";
		reset = new AbstractAction(PreferenceDialogs.shouldShowMessage(resetSettingName) ? "Reset " + settingNameCap + "..." : "Reset " + settingNameCap) 
		// can only work on defaults, searches for a file with the same name in application defaults
		{
			public void actionPerformed(ActionEvent event)
			{
				if (application_defaults.containsKey(selectedSetting.getName()))
				{
					boolean ok = true;
					
					if (PreferenceDialogs.shouldShowMessage(resetSettingName))
					{
						ok = JOptionPane.showOptionDialog(preferenceFrame, PreferenceDialogs.getSimpleConcealableMessage(resetSettingName, "Are you sure you want to reset the default " + settingName + "\"" + selectedSetting.getName() + "\"? This action cannot be undone."), "Are You Sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0;
					}
						
					if (ok)
					{
						try (InputStream in = application_defaults.get(selectedSetting.getName()))
						{
							Files.copy(in, selectedSetting.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
							selectedSetting.loadSettingsFromFile();
							settingsPanel.loadSettings(selectedSetting);
						} 
						catch (IOException | InvalidPreferencesFormatException e) 
						{
							Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "reset's actionPerformed", "Error when resetting the file " + selectedSetting.file + " for the default " + settingName + " " + selectedSetting, e);
							JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while resetting the " + settingName + "\"" + selectedSetting.getName() + "\" to default.", "Error", JOptionPane.ERROR_MESSAGE, null);
						}
						finally //InputStreams can't be used twice
						{
							application_defaults.put(selectedSetting.getName(), settingsPanel.getDefaultSetting(selectedSetting.getName()));
						}
					}
				}
				else 
				{
					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel("The " + settingName + " is not a default " + settingName + " or the original file could not be determined."), new GBC(0, 0).setAnchor(GBC.WEST));
					p.add(new JLabel("You can try resetting using the command \"Restore and Reset All Default" + settingNameCapPlural + "\"."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));
					
					JOptionPane.showMessageDialog(preferenceFrame, p, "The " + settingName + " \"" + selectedSetting.getName() + "\" cannot be reset.", JOptionPane.INFORMATION_MESSAGE, null);
				}
			}
		};
		reset.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		Application.keyboardSettingsPanel.registerOtherShortcut("resetDefault" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Reset Default " + settingNameCap), true, KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK, reset, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		final String resetAllSettingName = "recoverAndResetAllDefault" + settingNamePlural;
		resetAll = new AbstractAction(PreferenceDialogs.shouldShowMessage(resetAllSettingName) ? "Recover and Reset All Default " + settingNameCapPlural + "..." : "Recover and Reset All Default " + settingNameCapPlural)
		{
			public void actionPerformed(ActionEvent event)
			{
				boolean ok = true;
				if (PreferenceDialogs.shouldShowMessage(resetAllSettingName))
				{
					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel("Are you sure you want to recover and reset all default " + settingNamePlural + "?"), new GBC(0, 0).setAnchor(GBC.WEST));
					p.add(new JLabel("All current default " + settingNamePlural + " will be moved to the trash. This cannot be undone."), new GBC(0, 1).setAnchor(GBC.WEST).setInsets(10, 0, 0, 0));

					ok = JOptionPane.showOptionDialog(preferenceFrame, PreferenceDialogs.getSimpleConcealableMessage(resetAllSettingName, p), "Are You Sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"Yes", "No"}, "No") == 0;
				}

				if (ok)
				{
					stopCellEditing();
					for (SettingsFile settingsFile : default_files)
					{
						settingsFile.delete(false);
					}
					default_files.clear();
					
					for (String key : application_defaults.keySet())
					{
						try (InputStream in = application_defaults.get(key))
						{
							File f = new File(defaults_folder, key + ".xml");
							Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
							SettingsFile newSetting = selectedSetting.newDefaultFromExistingFile(f);
							default_files.add(newSetting);
						}
						catch (IOException | InvalidPreferencesFormatException e) 
						{
							Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "resetAll's actionPerformed", "Error when resetting the file " + new File(defaults_folder, key + ".xml") + " for the default " + settingName + " " + key, e);
							JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while restoring the " + settingName + "\"" + key + "\".", "Error", JOptionPane.ERROR_MESSAGE, null);
						}
						finally //InputStreams can't be used twice
						{
							application_defaults.put(key, settingsPanel.getDefaultSetting(key));
						}
					}
					
					if (selectedSetting.isDefault)
					{
						int initialRowSelection = defaultsTable.getSelectedRow();
						boolean found = false;
						for (int i = 0; i < default_files.size(); i++)
						{
							if (default_files.get(i).getName().equals(selectedSetting.getName()))
							{
								defaultsTable.setRowSelectionInterval(i, i);
								if (i == initialRowSelection) updateSelection(false);
								found = true;
								break;
							}
						}
						
						//shouldn't happen though
						if (!found) 
						{
							defaultsTable.setRowSelectionInterval(0, 0);
						}
						settingsPanel.loadSettings(selectedSetting);
					}
					
					defaultsTable.revalidate();
					defaultsTable.repaint();
				}
			}
		};
		resetAll.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		Application.keyboardSettingsPanel.registerOtherShortcut("resetAllDefault" + settingNamePlural, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Reset All Default " + settingNameCapPlural), false, 0, 0, resetAll, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		showFileLocation = new AbstractAction("Show File Location")
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					selectedSetting.showFileLocation();
				}
				catch (IOException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "showFileLocation's actionPerformed", "Error when trying to show location of the file " + selectedSetting.file.getParentFile(), e);
					JOptionPane.showMessageDialog(preferenceFrame, "An error occurred while trying to show file location.", "Error", JOptionPane.WARNING_MESSAGE, null);
				}
			}
		};
		showFileLocation.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		Application.keyboardSettingsPanel.registerOtherShortcut("showFileFor" + settingNameCap, KeyboardSettingsPanel.getMenuItemString("Application Preferences", settingNameCapPlural, "Show File Location for " + settingNameCap), true, KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), showFileLocation, SettingsFileChooserPanel.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		/*
		defaults cannot be renamed
		cannot delete/delete all a preset/custom theme if they are the last remaining one
		cannot reset a custom theme
		 */
		
		popup.add(newSetting);
		popup.add(rename); 
		popup.add(duplicate);
		
		popup.addSeparator();
		popup.add(delete);
		popup.add(deleteAllDefaults);
		popup.add(deleteAllCustoms);
		popup.addSeparator();
		
		popup.add(importSetting);
		popup.add(exportSetting);
		popup.addSeparator();
		popup.add(restore);
		popup.add(reset);
		popup.add(resetAll);
		
		popup.addSeparator();
		popup.add(showFileLocation);
		
		defaultsTable.setComponentPopupMenu(popup);
		customsTable.setComponentPopupMenu(popup);

		Application.componentsToUpdateUI.add(popup);
		Application.componentsToUpdateUI.add(xmlChooser);
		Application.componentsToUpdateUI.add(exportChooser);

		//choose a SettingsFile for initial selection
		SettingsFile tmp = default_files.size() > 0 ? default_files.get(0) : custom_files.get(0);

		selectedSettingPreference = Preferences.userNodeForPackage(tmp.getClass()).node("selected_setting");
		if (selectedSettingDataFile.exists())
		{
			try (FileInputStream in = new FileInputStream(selectedSettingDataFile))
			{
				Preferences.importPreferences(in);
			}
			catch (IOException | InvalidPreferencesFormatException e)
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "init", "Error when importing settings from selectedSettingDataFile(" + selectedSettingDataFile + ")", e);
			}
		}
		
		String selectedSettingName = selectedSettingPreference.get("setting_name", "Untitled");
		boolean isSelectedDefault = selectedSettingPreference.getBoolean("is_default", application_defaults.containsKey(selectedSettingName));
		boolean found = false;
		
		if (isSelectedDefault)
		{
			for (int i = 0; i < default_files.size(); i++)
			{
				if (default_files.get(i).getName().equals(selectedSettingName))
				{
					found = true;
					defaultsTable.setRowSelectionInterval(i, i);
					break;
				}
			}
		}
		else 
		{
			for (int i = 0; i < custom_files.size(); i++)
			{
				if (custom_files.get(i).getName().equals(selectedSettingName))
				{
					found = true;
					customsTable.setRowSelectionInterval(i, i);
					break;
				}
			}
		}
		
		if (!found)
		{
			if (default_files.size() > 0)
			{
				defaultsTable.setRowSelectionInterval(0, 0);
			}
			else 
			{
				customsTable.setRowSelectionInterval(0, 0);
			}
		}
		
		Application.showDialogs.addPreferenceChangeListener(event ->
		{
			if (event.getKey().equals(restoreSettingName))
			{
				restore.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Restore to Previous Save..." : "Restore to Previous Save");
			}
			else if (event.getKey().equals(deleteSettingName))
			{
				delete.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Delete..." : "Delete");
			}
			else if (event.getKey().equals(deleteAllDefaultsSettingName))
			{
				deleteAllDefaults.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Delete All Default " + settingNameCapPlural + "..." : "Delete All Default " + settingNameCapPlural);
			}
			else if (event.getKey().equals(deleteAllCustomsSettingName))
			{
				deleteAllCustoms.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Delete All Custom " + settingNameCapPlural + "..." : "Delete All Custom " + settingNameCapPlural);
			}
			else if (event.getKey().equals(resetSettingName))
			{
				reset.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Reset " + settingNameCap + "..." : "Reset " + settingNameCap);
			}
			else if (event.getKey().equals(resetAllSettingName))
			{
				resetAll.putValue(AbstractAction.NAME, PreferenceDialogs.shouldShowDialog(event) ? "Recover and Reset All Default " + settingNameCapPlural + "..." : "Recover and Reset All Default " + settingNameCapPlural);
			}
		});
		
		initializing = false;
		Application.loadTimeLogger.log(Level.CONFIG, "Time to create a SettingsFileChooserPanel: " + (System.currentTimeMillis() - start));
	}
}