package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;

import java.util.prefs.*;
import java.io.*;
import java.util.logging.*;

/**
 * Superclass of SettingsFile used by SettingsFileChooserPanel
 */
public class SingleSettingsFile
{
	public Preferences node;
	public File file;
	
	/**
	 * So that SettingsFile can define its own constructor
	 */
	protected SingleSettingsFile() { }
	
	/**
	 * SingleSettingsFile nodes are in the prefs folder, differentiated by their name. 
	 * That means one tab in PreferenceFrame corresponds to one SingleSettingsFile with a specific name
	 * unless that tab uses SettingsFiles and SettingsFileChooserPanel.
	 */
	public SingleSettingsFile(File file)
	{
		this.file = file;
		node = Preferences.userNodeForPackage(this.getClass()).node(PreferenceFrame.removeFileExtension(file));

		long start = System.currentTimeMillis();
		if (file.exists())
		{
			//this way the user can import preferences using a physical file
			try (FileInputStream in = new FileInputStream(file))
			{
				Preferences.importPreferences(in);
			}
			catch (IOException | InvalidPreferencesFormatException e)
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "init", "Error when importing preferences from file " + file, e);
			}
		}
		Application.loadTimeLogger.log(Level.FINE, "time to IMPORT settings from file to " + node + ": " + (System.currentTimeMillis() - start));
	}
	
	/**
	 * Flushes the preference node and exports its preferences to the corresponding file of this object
	 */
	public boolean save()
	{
		File parentFile = file.getParentFile();
		if (!parentFile.exists()) parentFile.mkdirs();
		
		long start = System.currentTimeMillis();
		try (FileOutputStream out = new FileOutputStream(file))
		{
			node.exportSubtree(out);
			Application.loadTimeLogger.log(Level.FINE, "Time taken to save settings to node " + node + ": " + (System.currentTimeMillis() - start));
			return true;
		}
		catch (IOException | BackingStoreException e) //FileNotFoundException | IOException
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "save", "Error when exporting preference node to file " + file, e);
			return false;
		}
	}
	
	@Override
	public String toString()
	{
		return file.toString();
	}
}