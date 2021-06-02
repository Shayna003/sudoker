package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;

import java.util.prefs.*;
import java.io.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;

/**
 * This abstract super class makes it easier(?) for implementing multiple panels that use SettingsFileChooserPanel
 * For panels in PreferenceFrame that use a single settings file, refer to a class like SingleSettingsFile
 * instead.
 * @version 0.00 1-7-2021
 * @since 12-14-2020
 */
@SuppressWarnings("CanBeFinal")
public abstract class SettingsFile extends SingleSettingsFile
{
	public static NameComparator nameComparator = new NameComparator();
	public boolean deleted = false;
	public boolean isDefault; //defaults can be restored

	public boolean preferenceLoadedFromFile = false; // kind of a mess, what if user changes the file when application open? well whatever

	public String name; // file name without extension
	public SettingsPanel settingsPanel;
	
	public static class NameComparator implements Comparator<SettingsFile>
	{
		public int compare(SettingsFile f1, SettingsFile f2)
		{
			return Collator.getInstance().compare(f1.getName(), f2.getName());
		}
	}
	
	/**
	 * This method is used by ThemeChooserPanel
	 * When a new file with @param originalFileName needs to be created (originalFileName does not include file extension, e.g. "A Setting")
	 * @return the name that should be used for the new file, so that it does not
	 * repeat with any file in @param presets or @param custom_themes
	 */
	public static String getNewFileName(String originalFileName, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults, boolean removeSuffixOfOriginalName)
	{
		boolean originalNameFound = false;
		ArrayList<Integer> matches = new ArrayList<>();
		String tmp;
		int tmp2;
		if (removeSuffixOfOriginalName && originalFileName.matches(".* \\d+"))//a sequence of characters followed by a space and a sequence of digits   //c \\d+"))
		{
			originalFileName = originalFileName.replaceAll(" \\d+$", "");
		}
		
		for (int i = 0; i < defaults.size(); i++)
		{
			tmp = defaults.get(i).getName();
			if (tmp.startsWith(originalFileName))
			{
				tmp = tmp.substring(originalFileName.length());
				if (tmp.length() == 0) //file name is exactly equal to originalFileName
				{
					originalNameFound = true;
				}
				else if (tmp.matches(" \\d+")) // space followed by a sequence of digits
				{
					tmp2 = Integer.parseInt(tmp.substring(1));
					if (tmp2 > 1) matches.add(tmp2);
				}	
			}
		}
		
		for (int i = 0; i < customs.size(); i++)
		{
			tmp = customs.get(i).getName();
			if (tmp.startsWith(originalFileName))
			{
				tmp = tmp.substring(originalFileName.length());
				if (tmp.length() == 0) //file name is exactly equal to originalFileName
				{
					originalNameFound = true;
				}
				else if (tmp.matches(" \\d+")) // space followed by a sequence of digits
				{
					tmp2 = Integer.parseInt(tmp.substring(1));
					if (tmp2 > 1) matches.add(tmp2);
				}	
			}
		}
		if (!originalNameFound && !application_defaults.containsKey(originalFileName)) return originalFileName;
		
		int suffix = 2;

		while(true)
		{
			if (!matches.contains(suffix))
			{
				tmp = originalFileName + " " + suffix;
				if (!application_defaults.containsKey(tmp)) return tmp;
			}
			suffix++;
		}
	}
	
	@Override
	public String toString()
	{
		return name;//file.getName();//toString();
	}

	public abstract SettingsFile newFromDefault(String newFileName);

	public abstract SettingsFile duplicate(String newFileName, SettingsPanel settingsPanel, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults) throws IOException, InvalidPreferencesFormatException, BackingStoreException;

	/*
	 * @param permanent if true delete permanently, else move to trash
	 */
	public boolean delete(boolean permanent)
	{
		deleted = true;
		try 
		{
			long start = System.currentTimeMillis();
			node.removeNode();
			Application.prefsLogger.log(Level.FINE, "time to clear node " + node + ": in delete" + (System.currentTimeMillis() - start));
		} 
		catch (BackingStoreException e) 
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "delete", "Error when trying to clear and remove the preference node " + node, e);
		}
		
		if (file.exists())
		{
			if (permanent)
			{
				try 
				{
					return file.delete();
				} 
				catch (SecurityException e) //no need to check for this actually, not a checked exception
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "delete", "Error when trying to permanently delete the file " + file, e);
					return false;
				}
			}
			else
			{
				try
				{
					return Desktop.getDesktop().moveToTrash(file);
				} 
				catch (UnsupportedOperationException | SecurityException | NullPointerException | IllegalArgumentException e)
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "delete", "Error when trying to move the file " + file + " to trash.", e);
					return false;
				}
			}
		}
		else 
		{
			return true;
		}
	}
	
	public void loadSettingsFromFile() throws IOException, InvalidPreferencesFormatException
	{
		if (!preferenceLoadedFromFile && file.exists())
		{
			long start = System.currentTimeMillis();
			try (FileInputStream in = new FileInputStream(file))
			{
				Preferences.importPreferences(in);
			}
			Application.loadTimeLogger.log(Level.FINE, "time taken to import preferences for node " + node + ": " + (System.currentTimeMillis() - start));
			preferenceLoadedFromFile = true;
		}
	}
	
	public abstract SettingsFile newDefaultFromExistingFile(File file) throws IOException, InvalidPreferencesFormatException;
	
	/**
	 * Makes a call of new SettingsFile(file, false);
	 */
	public abstract SettingsFile newFromExistingFile(File file, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults, SettingsPanel settingsPanel) throws IOException, InvalidPreferencesFormatException;
	
	
	public abstract void exportToTargetFile(File file, SettingsPanel settingsPanel) throws IOException, BackingStoreException;
	
	
	/**
	 * Creates a custom setting without loading the preferences from a file.
	 * Called by the New... action
	 */
	public SettingsFile(File file, SettingsPanel settingsPanel)
	{
		this.settingsPanel = settingsPanel;
		this.file = file;
		this.isDefault = false;

		Preferences packageNode = Preferences.userNodeForPackage(this.getClass());
		
		/*
		 * Uncleared nodes can make things messy
		 * For example, user deleted "Untitled" that has settings different from defaults
		 * Directly outside this application, then the node still exists that makes new setting
		 * With name "Untitled" hold settings different from the default
		 */
		try 
		{
			if (!file.exists() && packageNode.nodeExists(file.getName()))
			{
				long start = System.currentTimeMillis();
				packageNode.node(file.getName()).removeNode();
				Application.loadTimeLogger.log(Level.FINE, "time taken to clear node " + packageNode.node(file.getName()) + ": " + (System.currentTimeMillis() - start));
			}
		} 
		catch (BackingStoreException e) 
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "init", "Error when trying to clear the preference node " + packageNode.node(file.getName()), e);
		}
		this.name = PreferenceFrame.removeFileExtension(file);
		node = packageNode.node(name);
	}
	
	/**
	 * @param isDefault refers to whether this settingsFile represents an application default that comes with the application when installed
	 */
	public SettingsFile(File file, boolean isDefault, SettingsPanel settingsPanel) throws IOException, InvalidPreferencesFormatException
	{
		this(file, settingsPanel);
		this.isDefault = isDefault;
	}
	
	public String getName()
	{
		return name;
	}
	
	public static final String[] ILLEGAL_FILE_NAME_CHARACTERS = { "/", "\n", "\r", "\t", "\0", "\f", "`", "?", "*", "\\", "<", ">", "|", "\"", ":" };
	
	public static boolean isValidFileName(String name)
	{
		if (name == null || name.length() == 0) return false;
		if (name.length() > Preferences.MAX_NAME_LENGTH) return false;
		
		for (int i = 0; i < ILLEGAL_FILE_NAME_CHARACTERS.length; i++)
		{
			if (name.contains(ILLEGAL_FILE_NAME_CHARACTERS[i])) return false;
		}
		
		try 
		{
			Paths.get(name);
			return true;
		} 
		catch (InvalidPathException e) 
		{
			return false;
		}
	}
	
	/**
	 * Renames this SettingsFile and its corresponding file.
	 * @param newName should not contain extensions, e.g. "aSettingFile"
	 */
	public boolean setName(String newName, ArrayList<SettingsFile> defaults, ArrayList<SettingsFile> customs, HashMap<String, InputStream> application_defaults) //throws IOException, InvalidPreferencesFormatException
	{
		if (newName.equals(name)) return false;
		if (isValidFileName(newName))
		{
			String newNameNoRepeats = getNewFileName(newName, defaults, customs, application_defaults, false);
			File newFile = new File(file.getParentFile(), newNameNoRepeats + ".xml");
			boolean success = file.renameTo(newFile);
			
			if (success) 
			{
				this.file = newFile;
				this.name = newNameNoRepeats;
				
				try 
				{
					long start = System.currentTimeMillis();
					//node.clear(); // removeNode does this job.
					node.removeNode();
					Application.prefsLogger.log(Level.FINE, "time to clear node " + node + " in set name: " + (System.currentTimeMillis() - start));
				} 
				catch (BackingStoreException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "setName", "Error when trying to clear the preference node " + node + " after renaming its corresponding SettingsFile to " + newNameNoRepeats, e);
				}
				
				node = Preferences.userNodeForPackage(this.getClass()).node(newNameNoRepeats);
				settingsPanel.saveSettings(this, true);
			}
			return success;
		}
		return false;
	}
	
	public void showFileLocation() throws IOException
	{
		Desktop.getDesktop().open(file.getParentFile());
	}
}