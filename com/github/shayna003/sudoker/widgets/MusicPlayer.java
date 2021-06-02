package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.components.*;
import com.github.shayna003.sudoker.prefs.keys.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.sound.sampled.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import java.util.function.Predicate;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.logging.*;


/**
 * This class is capable of playing short audio files in weird formats? .WAV and .AIFF or something
 * Later will change this a little by adding a few default songs will always appear in the playlist at application startup? hmm.. maybe the user dislikes them? How about making clip chooser open up a directory
 * that contains Default_songs every time the user imports something? an accessory button to jump to that folder? I will see...
 * There are limitations of this application, for example it takes a lot of CPU & memory despite optimizations, about 200mb memory use... and despite everything, there is a lag for setting volume (gain) and mute
 * Remember to add a shutdownHook that calls the function savePlaylistAndSettings() before program exits
 * An important change has been made: previously the program makes a copy of a new song added to the playlist to a Music folder associated with the program (if the song's file name is different from all file names in the music folder). This has the advantage of having the songs show up next time the application is opened even if the user moved/deleted them, but 1. it takes up extra space, 2. show file location command now shows files in the music folder which is kind of weird, and 3. my application only performs a simple name clash check...
 * Therefore I changed this program to not use a music folder or copy files there, instead keep an xml file to save the file locations of songs. The major drawback is that if the user changed file locations, then the song won't show up next time.
 * Important change 2: 12-30-2020 to protect from concurrency issues and make this less confusing, the user can't modify the playlist during import, remove, and clearAll actions. A progressMonitor will pop up for import actions that can track and cancel the import action.
 * Important change 3: for performance reasons now MusicPlayer only plays files with the correct extension.
 * @version 0.40 1-27-2021
 * @since 12-18-2020
 * Last modified: 3-22-2021
 */
@SuppressWarnings("CanBeFinal")
public class MusicPlayer extends JPanel implements LineListener
{
	JMenu options;

	Preferences node = Preferences.userNodeForPackage(com.github.shayna003.sudoker.prefs.PreferenceFrame.class).node("playlist");
	File dataFile = new File(Application.dataFolder, "music_player_playlist.xml");
	boolean canPlay = false;
	boolean musicPlaying = false; // will be false temporarily if you slide timeSlider

	JFileChooser clipChooser;
	FileFilter musicFileFilter;
	ProgressMonitor progressMonitor;
	boolean isImporting;
	int filesScanned;
	int fileCount;
	
	Deque<File> filesToImport = new ArrayDeque<>();
	ArrayList<Song> songs;
	Song selectedSong;
	ScrollingText songTitle;

	JPanel buttonPanel;
	ImageButton previousButton;
	ImageButton nextButton;
	PlayButton playButton;
	ImageButton playlistButton;

	JPanel optionButtonsPanel;
	RectangularButton add;
	RectangularButton remove;
	//HelpButton help;
	SettingsButton settingsButton;

	Playlist playlist;
	JScrollPane playlistScrollPane;
	JPanel playlistPanel; //contains playlistScrollPane and option buttons
	
	Action togglePlaylist;

	//updates the time strings every second if sound is playing
	Timer timer;

	JPanel sliderPanel;
	JSlider timeSlider;
	JLabel currentTime;
	JLabel totalTime;
	String noDuration = "--:--"; //displayed when no song is selected

	JPanel volumePanel;
	boolean muted = false;
	JPopupMenu volumePopup;
	VolumeButton volumeButton;
	JSlider volumeSlider;
	JLabel volumeLabel;
	Action volumeUp;
	Action volumeDown;
	Action mute;
	Action resetVolume; //set volume to default (50)

	Action play;
	Action backward;
	Action forward;
	Action previous;
	Action next;
	Action jumpToBeginningOfSong; //set selected song's positions to 0
	Action setAllSongsToBeginning; //set all song's position to 0

	ButtonGroup loopButtons;
	JRadioButtonMenuItem noLoop;
	JRadioButtonMenuItem loopSong;
	JRadioButtonMenuItem loopList;
	Action showFileLocation;
	Action shufflePlaylist;
	Action copyAction;
	Action pasteAction;
	Action clearPlaylist;
	
	Predicate<Song> removePredicate;
	
	public static float getClipDurationInSeconds(File file) throws UnsupportedAudioFileException, IOException
	{
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
		AudioFormat format = audioInputStream.getFormat();
		long audioFileLength = file.length();
		int frameSize = format.getFrameSize();
		float frameRate = format.getFrameRate();
		return (audioFileLength / (frameSize * frameRate));
	}
	
	/*
	 * I guess add a shutdown hook to the main program using this to call this before quitting program
	 */
	public void savePlaylistAndSettings()
	{
		long start = System.currentTimeMillis();
		try
		{
			node.clear();
		}
		catch (BackingStoreException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "savePlaylistAndSettings", "Error when trying to clear playlist node " + node, e);
		}
		
		node.putInt("volume", volumeSlider.getValue());
		node.putBoolean("muted", muted);
		node.putInt("selected_song", playlist.getSelectedRow());
		node.put("loop", noLoop.isSelected() ? "none" : loopSong.isSelected() ? "song" : "playlist");
		
		boolean saveSongPositions = Application.miscellaneousSettingsPanel.saveMusicPlayerSongPositions.isSelected();
		for (int i = 0; i < songs.size(); i++)
		{
			node.put(String.valueOf(i), songs.get(i).file.getPath());
			if (saveSongPositions) node.putLong("position_" + i, songs.get(i).position);
		}

		if (!Application.dataFolder.exists())
		{
			Application.dataFolder.mkdirs();
		}
		
		try (FileOutputStream out = new FileOutputStream(dataFile))
		{
			node.exportSubtree(out);
		}
		catch (IOException | BackingStoreException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "savePlaylistAndSettings", "Error when exporting playlist node to file " + dataFile, e);
		}
		Application.prefsLogger.log(Level.CONFIG, "Save time for " + songs.size() + " songs: " + (System.currentTimeMillis() - start) + " milliseconds.");
	}
	
	public void loadPlaylistAndSettingsFromDataFile()
	{
		long start = System.currentTimeMillis();
		
		if (!Application.dataFolder.exists())
		{
			Application.dataFolder.mkdirs();
		}
		
		try (FileInputStream in = new FileInputStream(dataFile))
		{
			Preferences.importPreferences(in);
		}
		catch (IOException | InvalidPreferencesFormatException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "loadPlaylistAndSettingsFromDataFile", "Error when importing settings from file " + dataFile + " to playlist node.", e);
		}
		
		try
		{
			int tmp2 = node.getInt("volume", 50);
			if (tmp2 >= 1 && tmp2 <= 100) volumeSlider.setValue(tmp2);
			muted = node.getBoolean("muted", false);
			String tmpStr = node.get("loop", "none");
			if (tmpStr.equals("song")) loopSong.setSelected(true);
			else if (tmpStr.equals("playlist")) loopList.setSelected(true);
			else noLoop.setSelected(true);
			
			String[] keys = node.keys(); // might throw BackingStoreException
			File tmp;
			Song tmpSong;
			long tmpLong;
			int playlistPosition;
			for (String key : keys)
			{
				if (key.matches("\\d+"))
				{
					tmp = new File(node.get(key, ""));
					if (tmp.exists()) 
					{
						try
						{
							playlistPosition = Integer.parseInt(key);
						}
						catch (NumberFormatException e)
						{
							playlistPosition = -1;
						}
						
						try
						{
							tmpSong = new Song(tmp, playlistPosition);
							
							if (Application.miscellaneousSettingsPanel.saveMusicPlayerSongPositions.isSelected() && playlistPosition >= 0) 
							{
								tmpLong = node.getLong("position_" + playlistPosition, 0);
								if (tmpLong > 0)
								{
									tmpSong.position = tmpLong;
								}
							}
							songs.add(tmpSong);
							playlist.revalidate();
							playlist.repaint();
						}
						catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) 
						{
							Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "loadPlaylistAndSettingsFromDataFile", "Error when creating new Song from file " + tmp + ".", e);
						}
					}
				}
			}
			songs.sort(new PlaylistOrderComparator());
			
			if (songs.size() > 0)
			{
				tmp2 = node.getInt("selected_song", 0);
				if (tmp2 > songs.size() - 1) tmp2 = songs.size() - 1;
				else if (tmp2 < 0) tmp2 = 0;
				playlist.setRowSelectionInterval(tmp2, tmp2);
				playlist.scrollRectToVisible(playlist.getCellRect(playlist.getSelectedRow(), 0, true));
				if (canPlay) setPlayable(true);
			}
			else
			{
				songTitle.setText("");
			}
		}
		catch (BackingStoreException e)
		{
			Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "loadPlaylistAndSettingsFromDataFile", "Error when obtaining keys from playlist node.", e);
		}
		
		playlist.setEnabled(true);
		Application.loadTimeLogger.log(Level.CONFIG, "Music Player load time for settings and " + songs.size() + " songs: " + (System.currentTimeMillis() - start) + " milliseconds.");
	}
	
	class PlaylistOrderComparator implements Comparator<Song>
	{
		/**
		 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		 */
		public int compare(Song a, Song b)
		{
			return a.playlistPosition - b.playlistPosition;
		}
	}
	
	class Song
	{
		File file;
		Clip clip;
		String durationString;
		boolean toBeRemoved;
		long duration;
		long position;
		int framePosition;
		int frameLength;
		int playlistPosition; //used for assembling the playlist upon startup
		
		public void loadClip() throws UnsupportedAudioFileException, IOException, LineUnavailableException
		{
			Application.musicPlayerLogger.log(Level.FINE, "load clip for " + file);
			try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file))
			{
				AudioFormat fileFormat = audioStream.getFormat();
				DataLine.Info info = new DataLine.Info(Clip.class, fileFormat);
				clip = (Clip) AudioSystem.getLine(info);
				clip.open(audioStream);
				clip.setMicrosecondPosition(position);
				clip.addLineListener(MusicPlayer.this);
				long tmp = clip.getMicrosecondLength();
				if (duration != tmp)
				{
					duration = tmp;
					durationString = getTimeString(duration);
					playlist.revalidate();
					playlist.repaint();
					totalTime.setText(durationString);
				}
			}
		}

		/**
		 * For cloning
		 */
		private Song()
		{
		}
		
		public Song(File file, int playlistPosition) throws IOException, UnsupportedAudioFileException, LineUnavailableException
		{
			this(file);
			this.playlistPosition = playlistPosition;
		}
		
		public Song(File file) throws IOException, UnsupportedAudioFileException
		{
			this.file = file;
			position = 0;
			framePosition = 0;
			float durationInSeconds = getClipDurationInSeconds(file);
			durationString = getTimeString((int) durationInSeconds);
			toBeRemoved = false;
		}
		
		public void showFileLocation()
		{
			try
			{
				Desktop.getDesktop().open(file.getParentFile());
			}
			catch (IOException e) 
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "showFileLocation", "Error when showing location of file " + file.getParentFile(), e);
				JOptionPane.showMessageDialog(null, "An error occurred while performing the operation \"Show File Location.\"", "Error", JOptionPane.WARNING_MESSAGE, null);
			}
		}
		
		public long getMicrosecondLength()
		{
			if (canPlay) return clip.getMicrosecondLength();
			else return 0;
		}
		
		public long getMicrosecondPosition()
		{
			if (canPlay) return clip.getMicrosecondPosition();
			else return 0;
		}
		
		public int getFramePosition()
		{
			if (canPlay) return clip.getFramePosition();
			else return 0;
		}
		
		public int getFrameLength()
		{
			if (canPlay) return clip.getFrameLength();
			else return 0;
		}
		
		public void start()
		{
			if (canPlay) clip.start();
		}
		
		public void stop()
		{
			if (canPlay && clip != null) clip.stop();
		}
		
		public void setMicrosecondPosition(long microseconds)
		{
			Application.musicPlayerLogger.entering("MusicPlayer.Song", "setMicrosecondPosition", microseconds);
			if (canPlay) clip.setMicrosecondPosition(microseconds);
			position = microseconds;
		}

		public Control getControl(Control.Type control)
		{
			if (canPlay) return clip.getControl(control);
			else return null;
		}
		
		public String toString()
		{
			return file.getName();
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	class VolumeButton extends JButton
	{
		ImageIcon normal;
		ImageIcon pressed;
		ImageIcon mutedIcon;
		ImageIcon muted_pressed;
		boolean isPressed;
		boolean showingPressed;

		public void paint(Graphics g)
		{
			paintComponent(g);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(normal.getIconWidth(), normal.getIconHeight());
		}

		//used as visual clue for keyboard actions that trigger the function of this button
		public void showPress()
		{
			new Thread(() ->
			{
				showingPressed = true;
				repaint();
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "showPress", "Error when showing button press.", e);
				}
				showingPressed = false;
				repaint();
			}).start();
		}

		public VolumeButton(ImageIcon normal, ImageIcon pressed, ImageIcon mutedIcon, ImageIcon muted_pressed)
		{
			this.normal = normal;
			this.pressed = pressed;
			this.mutedIcon = mutedIcon;
			this.muted_pressed = muted_pressed;
			isPressed = false;
			showingPressed = false;

			addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent event)
				{
					isPressed = true;
					repaint();


					Point point = SwingUtilities.convertPoint(buttonPanel, volumeButton.getX(), volumeButton.getY(), MusicPlayer.this);
					volumePopup.show(MusicPlayer.this, (int) (point.x - (volumePopup.getPreferredSize().width / 2) + (volumeButton.getPreferredSize().getWidth() / 2)), point.y - volumePopup.getPreferredSize().height - 5);
				}

				public void mouseReleased(MouseEvent event)
				{
					isPressed = false;
					repaint();
				}
			});
		}

		public void paintComponent(Graphics g)
		{

			g.drawImage(muted ? (isPressed || showingPressed ? muted_pressed.getImage() : mutedIcon.getImage()) : (isPressed || showingPressed ? pressed.getImage() : normal.getImage()), 0, 0, null);
		}
	}

	@SuppressWarnings("CanBeFinal")
	class PlayButton extends JButton
	{
		ImageIcon start;
		ImageIcon paused;

		public void paint(Graphics g)
		{
			paintComponent(g);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(start.getIconWidth(), start.getIconHeight());
		}

		public PlayButton(ImageIcon start, ImageIcon paused)
		{
			this.start = start;
			this.paused = paused;
		}

		public void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			if (!isEnabled())
			{
				g2.setComposite(SwingUtil.makeComposite(0.3f));
			}
			g2.drawImage(musicPlaying? paused.getImage() : start.getImage(), 0, 0, null);
		}
	}
	@SuppressWarnings("CanBeFinal")
	private class SongImporter extends SwingWorker<Void, Integer>
	{
		File[] files;
		public SongImporter(File[] files)
		{
			this.files = files;
		}
		
		public void recursiveFileCount(File f)
		{
			if (f != null && !progressMonitor.isCanceled())
			{
				if (f.isDirectory())
				{
					File[] files = f.listFiles();
					if (files != null) 
					{
						publish(0); //to make the progressMonitor appear
						for (File file : files)
						{
							if (progressMonitor.isCanceled()) return;
							recursiveFileCount(file);
						}
					}
				}
				else
				{
					if (f.exists() && isFileTypeSupported(f)) 
					{
						fileCount++;
						filesToImport.add(f);
						publish(0); //to make the progressMonitor appear
					}
				}
			}
		}
		
		//for analysis
		long start;
		int originalSize;

		@Override
		public Void doInBackground()
		{
			originalSize = songs.size();
			start = System.currentTimeMillis();
			
			for (File f : files)
			{
				recursiveFileCount(f);
			}
			Application.timeLogger.log(Level.CONFIG, "File counting finished! Time taken: " + (System.currentTimeMillis() - start) + " milliseconds.");
			
			EventQueue.invokeLater(() ->
			{
				progressMonitor.setMaximum(fileCount);
				progressMonitor.setNote(null);
			});
			
			while (filesToImport.size() > 0 && !progressMonitor.isCanceled())
			{
				File f = filesToImport.removeFirst();
				try 
				{
					songs.add(new Song(f));
				} 
				catch (IOException | UnsupportedAudioFileException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "doInBackground", "Error when trying to create new song from file " + f, e);
				}
				filesScanned++;
				publish(filesScanned);
			}
			return null;
		}
		
		/**
		 * Executes in the event dispatch thread
		 */
		@Override
		public void process(List<Integer> data)
		{
			if (isCancelled()) return;
			progressMonitor.setProgress(data.get(data.size() - 1));
			playlist.revalidate();
			playlist.repaint();
		}
		
		/**
		 * Executes in the event dispatch thread
		 */
		@Override
		public void done()
		{
			Application.timeLogger.log(Level.CONFIG, "Importing finished! " + (System.currentTimeMillis() - start) + " milliseconds " + (songs.size() - originalSize) + " songs.");
			
			progressMonitor.close();
			filesToImport.clear();
			isImporting = false;
					
			add.setEnabled(true);
			add.isPressed = false;
			add.repaint();
					
			setAbleToChangePlaylist(true);
		}
	}
	
	public void makeOptionButtons()
	{
		Dimension max_dimension = SwingUtil.getMaxDimension(SwingUtil.getStringBounds(new String[] {"+", "-", "?"}, getFont(), getFontMetrics(getFont()).getFontRenderContext()));

		Dimension square_dimension = max_dimension.width > max_dimension.height ? new Dimension((int) (max_dimension.width * 1.5), (int) (max_dimension.width * 1.5)) : new Dimension((int) (max_dimension.height * 1.5), (int) (max_dimension.height * 1.5));

		add = new RectangularButton("+", square_dimension, false);
		add.setFont(getFont());
		add.setFocusable(false);
		add.setToolTipText("Add New Song To Playlist...");
		add.addActionListener(event ->
		{
			add.setEnabled(false);
			if (clipChooser.showOpenDialog(MusicPlayer.this) == JFileChooser.APPROVE_OPTION)
			{
				setAbleToChangePlaylist(false);
				filesScanned = 0;
				fileCount = 0;
				isImporting = true;
				
				if (progressMonitor == null)
				{
					progressMonitor = new ProgressMonitor(MusicPlayer.this, "Importing selected files...", "Calculating  progress...", filesScanned, (int) Double.POSITIVE_INFINITY);
				}
				else
				{
					progressMonitor.setMaximum((int) Double.POSITIVE_INFINITY);
					progressMonitor.setNote("Calculating progress...");
				}
				
				Application.musicPlayerLogger.log(Level.FINE, "start import");
				long start = System.currentTimeMillis();
		
				new SongImporter(clipChooser.getSelectedFiles()).execute();
			}
			else
			{
				add.setEnabled(true);
				add.isPressed = false;
				add.repaint();
			}
		});

		remove = new RectangularButton("-", square_dimension, false);
		remove.setFont(getFont());
		remove.setFocusable(false);
		remove.setToolTipText("Remove From Playlist");
		
		removePredicate = new Predicate<>()
		{
			public boolean test(Song s)
			{
				return s.toBeRemoved;
			}
		};
		
		remove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int[] selectedRows = playlist.getSelectedRows();
				if (canPlay && selectedSong != null && selectedSong.clip != null)
				{
					selectedSong.clip.flush();
					selectedSong.clip.close();
				}
				
				for (int i : selectedRows)
				{
					songs.get(i).toBeRemoved = true;
				}
				removeSongs();
				
				if (songs.size() > 0) 
				{
					if (playlist.getSelectedRow() > songs.size() - 1)
					{
						playlist.setRowSelectionInterval(songs.size() - 1, songs.size() - 1);
					}
					else //clears multiple line's of selection
					{
						playlist.setRowSelectionInterval(playlist.getSelectedRow(), playlist.getSelectedRow());
					}
				}
				
				songChanged();
				playlist.revalidate();
				playlist.repaint();
			}
			
		});

		
		showFileLocation = new AbstractAction("Show File Location")
		{
			public void actionPerformed(ActionEvent event)
			{
				selectedSong.showFileLocation();
			}
		};
		showFileLocation.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		
		shufflePlaylist = new AbstractAction("Shuffle Playlist")
		{
			public void actionPerformed(ActionEvent event)
			{
				Collections.shuffle(songs);
				int index = songs.indexOf(selectedSong);
				playlist.setRowSelectionInterval(index, index);
				playlist.revalidate();
				playlist.repaint();
			}
		};
		shufflePlaylist.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		
		copyAction = new AbstractAction("Copy Selection")
		{
			public void actionPerformed(ActionEvent event)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				Playlist.SongSelection selection = ((Playlist.SongSelectionTransferHandler) playlist.getTransferHandler()).createTransferable(playlist);
				clipboard.setContents(selection, selection);
				pasteAction.setEnabled(true);
			}
		};
		copyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		
		pasteAction = new AbstractAction("Paste Selection")
		{
			public void actionPerformed(ActionEvent event)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				playlist.getTransferHandler().importData(new TransferHandler.TransferSupport(playlist, clipboard.getContents(MusicPlayer.this)));
			}
		};
		pasteAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		pasteAction.setEnabled(false); //even if you copied a selection of songs, that data gets corrupted after the program exits

		clearPlaylist = new AbstractAction("Clear Playlist")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (canPlay && selectedSong != null && selectedSong.clip != null)
				{
					selectedSong.clip.flush();
					selectedSong.clip.close();
				}
				
				songs.clear();
				songChanged();
				playlist.revalidate();
				playlist.repaint();
			}
		};
		clearPlaylist.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);

		//help = new HelpButton("Help on Music Player", null);
		//help.setFocusable(false);
		settingsButton = new SettingsButton("Music Player Settings", Application.miscellaneousSettingsPanel);
		settingsButton.setFocusable(false);
	}

	/**
	 * when importing, cannot delete/dnd/copy/paste
	 * similarly, when deleting, cannot import/dnd/copy/paste
	 * Otherwise there might be concurrency issues or simply confusing
	 */
	public void setAbleToChangePlaylist(boolean b)
	{
		Application.musicPlayerLogger.log(Level.FINE, "Setting able to change playlist to " + b);
		if (musicPlaying)
		{
			setPlaying(false, false);
		}

		playlist.setEnabled(b);
		boolean songSelected = isSongSelected();
		if (!b)
		{
			playButton.setEnabled(false);
			play.setEnabled(false);
			jumpToBeginningOfSong.setEnabled(false);
			setAllSongsToBeginning.setEnabled(false);
			previous.setEnabled(false);
			previousButton.setEnabled(false);
			next.setEnabled(false);
			nextButton.setEnabled(false);
			forward.setEnabled(false);
			backward.setEnabled(false);
			remove.setEnabled(false);
			copyAction.setEnabled(false);
			pasteAction.setEnabled(false);
			clearPlaylist.setEnabled(false);
			showFileLocation.setEnabled(false);
			shufflePlaylist.setEnabled(false);
		}
		else 
		{
		pasteAction.setEnabled(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(MusicPlayer.this).isDataFlavorSupported(songSelectionFlavor));
			playButton.setEnabled(canPlay && songSelected);
			play.setEnabled(canPlay && songSelected);
			jumpToBeginningOfSong.setEnabled(canPlay && songSelected);
			setAllSongsToBeginning.setEnabled(songs.size() > 0);
			previous.setEnabled(songSelected);
			previousButton.setEnabled(songSelected);
			next.setEnabled(songSelected);
			nextButton.setEnabled(songSelected);
			forward.setEnabled(canPlay && songSelected);
			backward.setEnabled(canPlay && songSelected);
			remove.setEnabled(songSelected);
			copyAction.setEnabled(songSelected);
			clearPlaylist.setEnabled(songs.size() > 0);
			showFileLocation.setEnabled(songSelected);
			shufflePlaylist.setEnabled(songs.size() > 0);
			
		}
		togglePlaylist.setEnabled(b);
		add.setEnabled(b);
	}
	
	public boolean isSongSelected()
	{
		return songs.size() > 0 && playlist.getSelectedRow() >= 0 && playlist.getSelectedRow() <= songs.size() - 1;
	}
	
	/**
	 * If a song is selected from the playlist, then is playable. False otherwise.
	 */
	public void setPlayable(boolean b)
	{	
		if (!b)
		{
			if (musicPlaying)
			{
				setPlaying(false, false);
			}	
		}
		
		setAllSongsToBeginning.setEnabled(songs.size() > 0);
		
		boolean songSelected = isSongSelected();
		
		remove.setEnabled(songSelected);
		showFileLocation.setEnabled(songSelected);
		previous.setEnabled(songSelected);
		previousButton.setEnabled(songSelected);
		next.setEnabled(songSelected);
		nextButton.setEnabled(songSelected);
		copyAction.setEnabled(songSelected);
		
		canPlay = songSelected && b;
		play.setEnabled(canPlay);
		playButton.setEnabled(canPlay);
		timeSlider.setEnabled(canPlay);
		jumpToBeginningOfSong.setEnabled(canPlay);
		setTimeAndVolume();

		forward.setEnabled(canPlay);
		backward.setEnabled(canPlay);
		
		if (songs.size() == 0) 
		{
			clearPlaylist.setEnabled(false);
			shufflePlaylist.setEnabled(false);
		}
		else
		{
			clearPlaylist.setEnabled(true);
			shufflePlaylist.setEnabled(true);
		}
	}
	
	public boolean endOfSong()
	{
		if (!canPlay) return false;
		boolean isEnd = selectedSong.getMicrosecondPosition() == selectedSong.getMicrosecondLength() || selectedSong.getFramePosition() == selectedSong.getFrameLength() || timeSlider.getValue() == timeSlider.getMaximum();
		Application.musicPlayerLogger.log(Level.FINE, "end of song for " + selectedSong + "? " + isEnd);
		return isEnd;
	}
	
	public boolean startOfPlaylist()
	{
		return playlist.getSelectedRow() == 0;
	}
	
	public boolean endOfPlaylist()
	{
		return playlist.getSelectedRow() == songs.size() - 1;
	}
	
	public void restartSong()
	{
		if (canPlay)
		{
			selectedSong.setMicrosecondPosition(0);
			timeSlider.setValue(0);
		}
	}

	public void removeSongs()
	{
		songs.removeIf(removePredicate);
	}

	public void setPlaying(boolean playing, boolean temporary)
	{
		assert selectedSong != null;
		if (selectedSong != null && canPlay)
		{
			if (!temporary)
			{
				play.putValue(Action.NAME, playing ? "Pause" : "Play");
				musicPlaying = playing;
				playButton.repaint();
			}
			
			if (playing) //start
			{
				if (playlist.getSelectedRows().length > 0) playlist.setRowSelectionInterval(playlist.getSelectedRow(), playlist.getSelectedRow());
				Application.musicPlayerLogger.log(Level.FINER, "starting song");
				
				if (endOfSong())
				{
					Application.musicPlayerLogger.log(Level.FINER, "end of song when trying to play");
					if (loopSong.isSelected() || songs.size() == 1 || (endOfPlaylist() && !loopList.isSelected()))
					{
						restartSong();
						selectedSong.start();
						timer.start();
					}
					else
					{
						selectedSong.setMicrosecondPosition(0);
						next.actionPerformed(null);
					}
				}
				else
				{
					selectedSong.start();
					timer.start();
				}
			}
			else //pause
			{
				Application.musicPlayerLogger.log(Level.FINER, "song paused");
				selectedSong.stop();
				timer.stop();
			}
		}
	}

	public void setMuted(boolean muted)
	{
		if (selectedSong != null && canPlay)
		{
			BooleanControl muteControl = (BooleanControl) selectedSong.getControl(BooleanControl.Type.MUTE);
			Application.musicPlayerLogger.log(Level.FINER, "set muted to" + muted);
			if (muteControl != null) muteControl.setValue(muted);
		}
	}

	/**
	 * sadly you can't mute the sound entirely if it's set to 0...
	 */
	public void setGain() 
	{
		if (selectedSong != null && canPlay)
		{
			Application.musicPlayerLogger.log(Level.FINER, "setting gain for " + selectedSong);
			//from Java SE 10 api: linearScalar = Math.pow(10.0, gainDB/20.0);
			double value = volumeSlider.getValue() / 50.0;
			
			
			//value x in log(x) of statement below must be > 0 and <= 2
			//can't log 0
			float dB = (float) (Math.log10(value == 0 ? 0.0001 : value) * 20);
			FloatControl gainControl = (FloatControl) selectedSong.getControl(FloatControl.Type.MASTER_GAIN);
			if (gainControl != null) gainControl.setValue(dB);
			Application.musicPlayerLogger.exiting("MusicPlayer", "setGain");
		}
	}

	public Song makeCopyOfSong(Song s)
	{
		Song copy = new Song();
		copy.file = new File(s.file.getPath());
		copy.duration = s.duration;
		copy.position = s.position;
		copy.frameLength = s.frameLength;
		copy.framePosition = s.framePosition;
		copy.durationString = s.durationString;
		return copy;
	}
	
	public static String getTimeString(long microseconds)
	{
		return getTimeString((int) (microseconds / 1000000));
	}

	public static String getTimeString(int seconds)
	{
		int minutes = 0;
		while (seconds >= 60)
		{
			seconds -= 60;
			minutes++;
		}
		return String.format("%02d:%02d", minutes, seconds);
	}
	
	public void setTimeAndVolume()
	{
		currentTime.setText(canPlay ? getTimeString(selectedSong.position) : noDuration);
		totalTime.setText(canPlay ? selectedSong.durationString : noDuration);
		if (canPlay) 
		{
			timeSlider.setMaximum((int) selectedSong.getMicrosecondLength());
			setGain();
			setMuted(muted);
		}
		timeSlider.setValue(canPlay ? (int) selectedSong.position : 0);
		if (!canPlay) timer.stop();
	}
	
	public void songChanged()
	{
		Application.musicPlayerLogger.log(Level.FINE, "song changed: selected song: " + selectedSong);

		if (selectedSong != null && canPlay)
		{
			setPlaying(false, false);
			selectedSong.position = selectedSong.getMicrosecondPosition();
			selectedSong.clip.flush();
			selectedSong.clip.close();
		}
		
		if (songs.size() > 0)
		{
			Song tmp = playlist.getSelectedRow() >= 0 ? songs.get(playlist.getSelectedRow()) : null;
			
			if (tmp != null)
			{
				songTitle.setText(tmp.toString());
				songTitle.startAnimation();
				selectedSong = tmp;
				try
				{
					playlist.scrollRectToVisible(playlist.getCellRect(playlist.getSelectedRow(), 0, true));
					repaint();
					selectedSong.loadClip();
					playlist.repaint();

					if (!canPlay && !isImporting) 
					{
						setPlayable(true);
					}
					else
					{
						setTimeAndVolume();
					}
				}
				catch (Exception e)
				{
					setPlayable(false);

					String s = selectedSong.toString();
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "songChanged", "Error when trying to load clip from file" + selectedSong.file, e);

					if (e instanceof UnsupportedAudioFileException)
					{
						JOptionPane.showMessageDialog(MusicPlayer.this, "The file type of \"" + s + "\" is not supported.", "Warning", JOptionPane.WARNING_MESSAGE, null);
					}
					else if (e instanceof LineUnavailableException)
					{
						JOptionPane.showMessageDialog(MusicPlayer.this, "Audio line for playing back is unavailable for \"" + s + "\".", "Warning", JOptionPane.WARNING_MESSAGE, null);
					}
					else if (e instanceof FileNotFoundException)
					{
						JOptionPane.showMessageDialog(MusicPlayer.this, "Error loading the audio file \"" + s + "\". The file cannot be found.", "Warning", JOptionPane.WARNING_MESSAGE, null);
					}
					else 
					{
						JOptionPane.showMessageDialog(MusicPlayer.this, "Error loading the audio file \"" + s + "\".", "Warning", JOptionPane.WARNING_MESSAGE, null);
					}
				}
			}
			else
			{
				songTitle.setText("");
				setPlayable(false);
			}
		}
		else
		{
			selectedSong = null;
			songTitle.setText("");
			playlist.clearSelection();
			setPlayable(false);
		}
	}

	static DataFlavor songSelectionFlavor;
	@SuppressWarnings("CanBeFinal")
	class Playlist extends JTable
	{
		String[] headerNames = { "#", "Name", "Duration" };
		String mimeType = DataFlavor.javaJVMLocalObjectMimeType
				+ ";class=" + Playlist.class.getName();
		
		public Playlist()
		{
			setModel(new PlaylistModel());
			setFillsViewportHeight(true);

			try
			{
				songSelectionFlavor = new DataFlavor(mimeType);
			} 
			catch (ClassNotFoundException e) 
			{
				Application.exceptionLogger.logp(Level.SEVERE, getClass().toString(), "init", "Error when creating DataFlavor from String " + mimeType, e);
			}
			
			this.setDragEnabled(true);
			this.setDropMode(DropMode.INSERT_ROWS);
			this.setTransferHandler(new SongSelectionTransferHandler());
		}

		@Override
		public void valueChanged(ListSelectionEvent event)
		{
			Application.musicPlayerLogger.log(Level.FINER, "Playlist valueChanged");
			super.valueChanged(event);
			if ((getSelectedRow() >= 0 ? songs.get(getSelectedRow()) : null) != selectedSong)
			{
				songChanged();
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class SongSelectionData
		{
			ArrayList<Song> songList;
			ArrayList<Song> songSelection;
			int[] selectedIndexes;
			Playlist source;
			
			public SongSelectionData(Playlist source, ArrayList<Song> songs, int[] selectedIndexes)
			{
				this.source = source;
				this.songList = songs;
				this.selectedIndexes = selectedIndexes;
				songSelection = new ArrayList<>(selectedIndexes.length);
				for (int i = 0; i < selectedIndexes.length; i++)
				{
					songSelection.add(songs.get(selectedIndexes[i]));
				}
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class SongSelection implements Transferable, ClipboardOwner
		{
			SongSelectionData data;
			
			@Override
			public void lostOwnership(Clipboard c, Transferable t) 
			{
				Application.musicPlayerLogger.log(Level.FINER, "SongSelection lost clipboard ownership to" + c.getContents(MusicPlayer.this));
				pasteAction.setEnabled(c.getContents(MusicPlayer.this).isDataFlavorSupported(songSelectionFlavor));
			}
			
			public SongSelection(ArrayList<Song> songs, int[] selectedIndexes)
			{
				data = new SongSelectionData(Playlist.this, songs, selectedIndexes);
			}
			
			@Override
			public Object getTransferData(DataFlavor flavor)
			{
				return data;
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[] { songSelectionFlavor };
			}
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return flavor.getRepresentationClass().equals(MusicPlayer.Playlist.class);
			}
		}
		
		/**
		 * Enables table row drag and drop
		 */
		class SongSelectionTransferHandler extends TransferHandler 
		{
			@Override
			protected SongSelection createTransferable(JComponent c)
			{
				if (playlist.getSelectedRow() >= 0) return new SongSelection(songs, playlist.getSelectedRows());
				else return null;
			}

			@Override
			public boolean canImport(TransferHandler.TransferSupport info)
			{
				Transferable t = info.getTransferable();
				return t.isDataFlavorSupported(songSelectionFlavor);
			}
			
			@Override
			public int getSourceActions(JComponent c) 
			{
				if (c instanceof Playlist) return TransferHandler.COPY_OR_MOVE;
				else return TransferHandler.NONE;
			}
			
			@Override
			public boolean importData(TransferHandler.TransferSupport info)
			{
				Application.musicPlayerLogger.log(Level.FINE, "importData");
				Transferable t = info.getTransferable();
				try
				{
					if (!t.isDataFlavorSupported(songSelectionFlavor)) 
					{ 
						Application.musicPlayerLogger.log(Level.FINER, "dataFlavor not supported");
						pasteAction.setEnabled(false);
						return false; 
					}
					SongSelectionData data = (SongSelectionData) t.getTransferData(songSelectionFlavor);
					int row = info.isDrop() ? ((JTable.DropLocation) info.getDropLocation()).getRow() : data.source.getSelectedRow() + 1;
			
					//for !info.isDrop(): you can trigger this with cmd x/cmd c + cmd v
					if (!info.isDrop() || info.getDropAction() == TransferHandler.COPY)
					{
						ArrayList<Song> copies = new ArrayList<>(data.songSelection.size());
						for (Song s : data.songSelection)
						{
							copies.add(makeCopyOfSong(s));
						}
						songs.addAll(row, copies);
						setRowSelectionInterval(row, row + data.songSelection.size() - 1);
						songChanged();
					}
					else 
					{
						assert info.getDropAction() == TransferHandler.MOVE : info.getDropAction();
						int targetRow = row;
						for (int i = 0; i < data.songSelection.size(); i++)
						{
							if (data.source == playlist && data.selectedIndexes[i] < row) targetRow--;
							data.songList.remove(data.songSelection.get(i));
						}

						int counter = targetRow;
						for (Song s : data.songSelection)
						{
							songs.add(counter, makeCopyOfSong(s));
							counter++;
						}
						setRowSelectionInterval(targetRow, targetRow + data.songSelection.size() - 1);
						if (data.source != playlist)
						{
							data.source.clearSelection();
							data.source.revalidate();
							data.source.repaint();
						}
					}
					
					playlist.revalidate();
					repaint();
					
					setCursor(Cursor.getDefaultCursor());
					return true;
				}
				catch(UnsupportedFlavorException | IOException | ClassCastException e)
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "importData", "Error when trying to import data from " + t, e);
					return false;
				}	
			}
			
			@Override
			protected void exportDone(JComponent c, Transferable t, int act)
			{
				Application.musicPlayerLogger.log(Level.FINE, "export done");
				setCursor(Cursor.getDefaultCursor());
			}
		}

		class PlaylistModel extends AbstractTableModel
		{
			public int getColumnCount() { return headerNames.length; }
			public int getRowCount() { return songs.size(); }
			public Object getValueAt(int r, int c)
			{
				if (c == 0)
				{
					return r + 1;
				}
				else if (c == 1)
				{
					return songs.get(r).toString();
				}
				else
				{
					return songs.get(r).durationString;
				}
			}
			public String getColumnName(int c) { return headerNames[c]; }
		}
	}
	
	/*
	 * A rough check based on extension name
	 */
	public static boolean isFileTypeSupported(File f)
	{
		String s = f.getName().toLowerCase();
		return s.endsWith(".au") || s.endsWith(".mid") || s.endsWith(".wav") || s.endsWith(".aif");
	}

	public void playCompleted()
	{
		Application.musicPlayerLogger.log(Level.FINE, "end of song reached by natural playing");

		if (loopSong.isSelected() || loopList.isSelected() && songs.size() == 1)
		{
			restartSong();
			setPlaying(true, false);
		}
		else
		{
			if (endOfPlaylist() && !loopList.isSelected())
			{
				setPlaying(false, false);
			}
			else
			{
				next.actionPerformed(null);
			}
		}
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (playlist != null) 
		{
			playlist.updateUI();
			playlist.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
			playlist.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
			
		}	
	}

	@Override
	public void update(LineEvent event)
	{
		LineEvent.Type type = event.getType();

		if (type == LineEvent.Type.START)
		{
			Application.musicPlayerLogger.log(Level.FINER, "Playback started for " + selectedSong + ".");
		}
		else if (type == LineEvent.Type.STOP)
		{
			Application.musicPlayerLogger.log(Level.FINER, "Playback completed for " +  selectedSong + ".");
			if (endOfSong())
			{
				playCompleted();
			}
		}
		//not needed, not used
		else if (type == LineEvent.Type.OPEN)
		{
			Application.musicPlayerLogger.log(Level.FINEST, "LineEvent.Type == OPEN");
		}
		else if (type == LineEvent.Type.CLOSE)
		{
			Application.musicPlayerLogger.log(Level.FINER, "LineEvent.Type == CLOSE");
			event.getLine().close();
			event.getLine().removeLineListener(MusicPlayer.this);
		}
	}

	public void setCurrentTimeText()
	{
		currentTime.setText(getTimeString(timeSlider.getValue() / 1000000));
	}

	public MusicPlayer(JFrame owner)
	{
		long start = System.currentTimeMillis();
		
		sliderPanel = new JPanel();
		currentTime = new JLabel();
		totalTime = new JLabel();
		timeSlider = new JSlider();
		timeSlider.setValue(0);
		timeSlider.setFocusable(false);

		timeSlider.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent event)
			{
				if (timeSlider.isEnabled())
				{
					if (musicPlaying)
					{
						setPlaying(false, true);
					}
				}
			}

			public void mouseReleased(MouseEvent event)
			{
				if (timeSlider.isEnabled())
				{
					Application.musicPlayerLogger.log(Level.FINEST, "mouse released for timeSlider");
					setCurrentTimeText();
					selectedSong.setMicrosecondPosition(timeSlider.getValue());
					selectedSong.position = timeSlider.getValue();
					if (musicPlaying)
					{
						setPlaying(true, false);
					}
				}
			}
		});

		timeSlider.addMouseMotionListener(new MouseMotionAdapter()
		{
			public void mouseDragged(MouseEvent event)
			{
				if (timeSlider.isEnabled())
				{
					setCurrentTimeText();
				}	
			}
		});

		sliderPanel.add(currentTime);
		sliderPanel.add(timeSlider);
		sliderPanel.add(totalTime);
		
		musicFileFilter = new FileFilter()
		{
			public String getDescription()
			{
				return "Supported Music File Formats (.wav, .au, .aif, .mid)";
			}
			
			//for consistency I'm going to change it to a name check only
			public boolean accept(File f)
			{
				if (f.isDirectory()) return true;
				return isFileTypeSupported(f);
			}
		};
		
		clipChooser = new JFileChooser();
		clipChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		
		clipChooser.setMultiSelectionEnabled(true);
		clipChooser.setFileFilter(musicFileFilter);
		clipChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		clipChooser.setAcceptAllFileFilterUsed(false);
		songs = new ArrayList<>();

		playlist = new Playlist();
		playlist.setEnabled(false);
		playlist.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent event)
			{
				if (event.getClickCount() >= 2)
				{
					setPlaying(true, false);
				}
			}
		});

		playlistScrollPane = new JScrollPane(playlist);
		playlistScrollPane.setPreferredSize(new Dimension(250, 200));
		Rectangle2D bounds = playlist.getFont().getStringBounds("Duration", getFontMetrics(playlist.getFont()).getFontRenderContext());
		TableColumn c = playlist.getColumn("Name");
		int preferredWidthForDuration = (int) (bounds.getWidth() * 1.2);

		c.setPreferredWidth(250 - preferredWidthForDuration);
		
		makeOptionButtons();
		optionButtonsPanel = new JPanel();
		optionButtonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		optionButtonsPanel.add(add);
		optionButtonsPanel.add(remove);
		//optionButtonsPanel.add(help);
		optionButtonsPanel.add(settingsButton);
		
		playlistPanel = new JPanel(new BorderLayout());
		playlistPanel.add(playlistScrollPane, BorderLayout.CENTER);
		playlistPanel.add(optionButtonsPanel, BorderLayout.SOUTH);

		volumeButton = new VolumeButton(SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/music_player/volume.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/volume-pressed.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/mute.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/muted-pressed.png")));
		volumeSlider = new JSlider(JSlider.NORTH, 1, 100, 50); //because sound is not actually muted at 0, and if I set 0 to trigger mute, the gap between 1 and 0 is too big

		volumeSlider.addChangeListener(event ->
		{
			setGain();
		});
		volumeSlider.setOpaque(false);

		volumeLabel = new JLabel("50", SwingConstants.CENTER);
		volumeLabel.setOpaque(false);

		bounds = volumeLabel.getFont().getStringBounds("100", volumeLabel.getFontMetrics(volumeLabel.getFont()).getFontRenderContext());
		volumeLabel.setPreferredSize(new Dimension((int) (bounds.getWidth() * 1.5), (int) (bounds.getHeight()  * 1.5)));
		volumeSlider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent event)
			{
				volumeLabel.setText(String.valueOf(volumeSlider.getValue()));
			}
		});
		volumePanel = new JPanel();
		volumePanel.setLayout(new BorderLayout(0, 0));
		volumePanel.add(volumeLabel, BorderLayout.NORTH);
		volumePanel.add(volumeSlider, BorderLayout.CENTER);
		volumePanel.setOpaque(false);

		volumePopup = new JPopupMenu();
		volumePopup.add(volumePanel);
		previousButton = new ImageButton(SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/music_player/previous.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/previous-pressed.png")));
		previousButton.addActionListener(event ->
		{
			previous.actionPerformed(event);
		});
		previousButton.setFocusable(false);
		
		nextButton = new ImageButton(SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/music_player/next.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/next-pressed.png")));
		nextButton.addActionListener(event ->
		{
			next.actionPerformed(event);
		});
		nextButton.setFocusable(false);

		playButton = new PlayButton(SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/music_player/play.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/pause.png")));

		
		togglePlaylist = new AbstractAction("Hide Playlist and Buttons")
		{
			public void actionPerformed(ActionEvent event)
			{
				playlistPanel.setVisible(!playlistPanel.isVisible());
				owner.pack();
				togglePlaylist.putValue(Action.NAME, playlistPanel.isVisible() ? "Hide Playlist and Buttons" : "Show Playlist and Buttons");
				togglePlaylist.putValue(Action.SHORT_DESCRIPTION, togglePlaylist.getValue(Action.NAME));
			}
		};
		togglePlaylist.putValue(Action.SHORT_DESCRIPTION, togglePlaylist.getValue(Action.NAME));
		
		playlistButton = new ImageButton(SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/music_player/playlist.png")), SwingUtil.getImageIcon(MusicPlayer.class.getResource("../resources/images/music_player/playlist-pressed.png")));
		playlistButton.setAction(togglePlaylist);
		playlistButton.setFocusable(false);
		
		buttonPanel = new JPanel();
		buttonPanel.add(volumeButton);
		buttonPanel.add(previousButton);
		buttonPanel.add(playButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(playlistButton);

		JPanel compoundPanel = new JPanel(new BorderLayout());
		songTitle = new ScrollingText();
		songTitle.setText("Loading Playlist...");
		JPanel titlePanel = new JPanel();
		titlePanel.add(songTitle);
		compoundPanel.add(titlePanel, BorderLayout.NORTH);
		compoundPanel.add(sliderPanel, BorderLayout.CENTER);
		compoundPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 20));
		setLayout(new BorderLayout());
		add(playlistPanel, BorderLayout.CENTER);
		add(compoundPanel, BorderLayout.SOUTH);
		
		
		timer = new Timer(10, event ->
		{
			selectedSong.position = selectedSong.getMicrosecondPosition();
			currentTime.setText(getTimeString(selectedSong.getMicrosecondPosition()));
			timeSlider.setValue((int) (selectedSong.getMicrosecondPosition()));
		});

		play = new AbstractAction("Play")
		{
			public void actionPerformed(ActionEvent event)
			{
				setPlaying(!musicPlaying, false);
			}
		};
		playButton.setAction(play);
		play.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);

		backward = new AbstractAction("Backward 5s")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (musicPlaying) timer.stop();
				Application.musicPlayerLogger.log(Level.FINE, "backward 5s");
				timeSlider.setValue(Math.max(timeSlider.getValue() - 5000000, 0));
				selectedSong.setMicrosecondPosition(timeSlider.getValue());
				currentTime.setText(getTimeString(timeSlider.getValue() / 1000000));
				if (musicPlaying) timer.start();
			}
		};
		backward.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);

		forward = new AbstractAction("Forward 5s")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (musicPlaying) timer.stop();
				Application.musicPlayerLogger.log(Level.FINE, "forward 5s");
				timeSlider.setValue(timeSlider.getValue() + 5000000 > selectedSong.getMicrosecondLength() ? (int) selectedSong.getMicrosecondLength() : timeSlider.getValue() + 5000000);
				selectedSong.setMicrosecondPosition(timeSlider.getValue());
				currentTime.setText(getTimeString(timeSlider.getValue() / 1000000));
				if (musicPlaying) 
				{
					timer.start();
				}
			}
		};
		forward.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);

		previous = new AbstractAction("Previous Song")
		{
			public void actionPerformed(ActionEvent event)
			{
				Application.musicPlayerLogger.log(Level.FINE, "previous.actionPerformed");
				previousButton.showPress();
				
				if (songs.size() == 1 && loopList.isSelected() || loopSong.isSelected())
				{
					restartSong();
				}
				else
				{
					boolean startOfPlaylist = startOfPlaylist();
					
					
					if (!startOfPlaylist || loopList.isSelected())
					{
						boolean wasPlaying = musicPlaying;
						if (wasPlaying) setPlaying(false, true);
						if (endOfSong()) selectedSong.setMicrosecondPosition(0);
						int tmp = startOfPlaylist ? playlist.getRowCount() - 1 : playlist.getSelectedRow() - 1;
						playlist.setRowSelectionInterval(tmp, tmp);
						if (wasPlaying) setPlaying(true, false);
					}
				}
			}
		};
		previous.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);

		next = new AbstractAction("Next Song")
		{
			public void actionPerformed(ActionEvent event)
			{
				Application.musicPlayerLogger.log(Level.FINE, "next.actionPerformed");
				nextButton.showPress();
				if (songs.size() == 1 && loopList.isSelected() || loopSong.isSelected())
				{
					restartSong();
				}
				else
				{
					boolean endOfPlaylist = endOfPlaylist();
					if (!endOfPlaylist || loopList.isSelected())
					{
						boolean wasPlaying = musicPlaying;
						if (wasPlaying) 
						{
							setPlaying(false, true);
						}

						if (endOfSong()) 
						{
							selectedSong.setMicrosecondPosition(0);
						}
						int tmp = endOfPlaylist ? 0 : playlist.getSelectedRow() + 1;
						playlist.setRowSelectionInterval(tmp, tmp);
						if (wasPlaying) 
						{
							setPlaying(true, false);
						}
					}
				}
			}
		};
		next.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);

		volumeUp = new AbstractAction("Volume + 5%")
		{
			public void actionPerformed(ActionEvent event)
			{
				Application.musicPlayerLogger.log(Level.FINE, "volume + 5%");
				volumeSlider.setValue(Math.min(volumeSlider.getValue() + 5, 100));
				volumeButton.showPress();
			}
		};
		volumeUp.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);

		volumeDown = new AbstractAction("Volume - 5%")
		{
			public void actionPerformed(ActionEvent event)
			{
				Application.musicPlayerLogger.log(Level.FINE, "volume - 5%");
				volumeSlider.setValue(Math.max(volumeSlider.getValue() - 5, 1));
				volumeButton.showPress();
			}
		};
		volumeDown.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);

		mute = new AbstractAction("Mute")
		{
			public void actionPerformed(ActionEvent event)
			{
				muted = !muted;
				setMuted(muted);
				volumeButton.repaint();
				mute.putValue(Action.NAME, muted ? "Unmute" : "Mute");
			}
		};
		mute.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);
		
		resetVolume = new AbstractAction("Reset Volume")
		{
			public void actionPerformed(ActionEvent event)
			{
				volumeSlider.setValue(50);
				volumeButton.showPress();
			}
		};
		resetVolume.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		
		jumpToBeginningOfSong = new AbstractAction("Jump to Beginning of Song")
		{
			public void actionPerformed(ActionEvent event)
			{
				if (musicPlaying) setPlaying(false, true);
				timeSlider.setValue(0);
				selectedSong.setMicrosecondPosition(0);
				if (musicPlaying) setPlaying(true, true);
			}
		};
		jumpToBeginningOfSong.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_J);
		
		setAllSongsToBeginning = new AbstractAction("Set All Songs to Beginning") //wording is a little awkward
		{
			public void actionPerformed(ActionEvent event)
			{
				if (musicPlaying) setPlaying(false, true);
				timeSlider.setValue(0);
				selectedSong.setMicrosecondPosition(0);
				
				for (Song s : songs)
				{
					s.position = 0;
				}
				if (musicPlaying) setPlaying(true, true);
			}
		};
		setAllSongsToBeginning.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);
		
		options = new JMenu("Options");
		options.setMnemonic('O');
		options.add(play);
		options.add(forward);
		options.add(backward);
		options.add(jumpToBeginningOfSong);
		options.add(setAllSongsToBeginning);
		options.addSeparator();
		options.add(next);
		options.add(previous);
		options.addSeparator();
		options.add(volumeUp);
		options.add(volumeDown);
		options.add(resetVolume);
		options.add(mute);
		options.addSeparator();
		
		JMenu loopOptions = new JMenu("Loop Options");
		loopOptions.setMnemonic('L');
		loopButtons = new ButtonGroup();
		noLoop = new JRadioButtonMenuItem("No Loop", true);
		noLoop.setMnemonic('N');
		
		loopSong = new JRadioButtonMenuItem("Loop Song");
		loopSong.setMnemonic('S');
		
		loopList = new JRadioButtonMenuItem("Loop Playlist");
		loopList.setMnemonic('P');
		loopButtons.add(noLoop);
		loopButtons.add(loopSong);
		loopButtons.add(loopList);
		
		loopOptions.add(noLoop);
		loopOptions.add(loopSong);
		loopOptions.add(loopList);
		options.add(loopOptions);
		
		options.addSeparator();
		options.add(copyAction);
		options.add(pasteAction);
		
		options.addSeparator();
		options.add(showFileLocation);
		
		options.addSeparator();
		options.add(shufflePlaylist);
		
		options.addSeparator();
		options.add(clearPlaylist);

		// configure accelerator keys
		Application.keyboardSettingsPanel.registerOtherShortcut("play", KeyboardSettingsPanel.getMenuItemString("Music Player", "Play/Pause"), true, KeyEvent.VK_SPACE, 0, play, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("forward5s", KeyboardSettingsPanel.getMenuItemString("Music Player", "Forward 5s"), true, KeyEvent.VK_RIGHT, 0, forward, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("backward5s", KeyboardSettingsPanel.getMenuItemString("Music Player", "Backward 5s"), true, KeyEvent.VK_LEFT, 0, backward, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		Application.keyboardSettingsPanel.registerOtherShortcut("jumpToBeginningOfSong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Jump to Beginning of Song"), true, KeyEvent.VK_0, 0, jumpToBeginningOfSong, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("setAllSongsToBeginning", KeyboardSettingsPanel.getMenuItemString("Music Player", "Set All Songs to Beginning"), true, KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), setAllSongsToBeginning, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("nextSong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Next Song"), true, KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), next, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("previousSong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Previous Song"), true, KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), previous, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		
		Application.keyboardSettingsPanel.registerOtherShortcut("volumeUp", KeyboardSettingsPanel.getMenuItemString("Music Player", "Volume + 5%"), true, KeyEvent.VK_UP, 0, volumeUp, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("volumeDown", KeyboardSettingsPanel.getMenuItemString("Music Player", "Volume - 5%"), true, KeyEvent.VK_DOWN, 0, volumeDown, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("resetVolume", KeyboardSettingsPanel.getMenuItemString("Music Player", "Reset Volume"), false, 0, 0, resetVolume, null, 0);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("mute", KeyboardSettingsPanel.getMenuItemString("Music Player", "Mute/Unmute"), true, KeyEvent.VK_M, 0, mute, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		
		Application.keyboardSettingsPanel.registerOtherShortcut("noLoop", KeyboardSettingsPanel.getMenuItemString("Music Player", "Loop Options", "No Loop"), false, 0, 0, null, noLoop, 0);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("loopSong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Loop Options", "Loop Song"), false, 0, 0, null, loopSong, 0);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("loopList", KeyboardSettingsPanel.getMenuItemString("Music Player", "Loop Options", "Loop Playlist"), false, 0, 0, null, loopList, 0);
		
		
		
		Application.keyboardSettingsPanel.registerOtherShortcut("copySong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Copy Selection"), true, KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), copyAction, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("pasteSong", KeyboardSettingsPanel.getMenuItemString("Music Player", "Paste Selection"), true, KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), pasteAction, MusicPlayer.this, JComponent.WHEN_IN_FOCUSED_WINDOW);
 
		Application.keyboardSettingsPanel.registerOtherShortcut("showSongLocation", KeyboardSettingsPanel.getMenuItemString("Music Player", "Show File Location"), false, 0, 0, showFileLocation, null, 0);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("shufflePlaylist", KeyboardSettingsPanel.getMenuItemString("Music Player", "Shuffle Playlist"), false, 0, 0, shufflePlaylist, null, 0);
		
		Application.keyboardSettingsPanel.registerOtherShortcut("clearPlaylist", KeyboardSettingsPanel.getMenuItemString("Music Player", "Clear Playlist"), false, 0, 0, clearPlaylist, null, 0);
		
		setPlayable(false);
		loadPlaylistAndSettingsFromDataFile();

		//note: if I don't set to false, I can use some neat default cmd c, cmd x, and cmd v actions similar to 
		//drag and drop, but this clashes with the focus issue, so I have to implement my own version
		//(bc I found no ways to make playlist not respond to arrow keys while it is focusable...)
		playlist.setFocusable(false);

		volumeSlider.setFocusable(false);
		volumePopup.setFocusable(false);

		GeneralSettingsPanel.registerComponentAndSetFontSize(volumePopup);
		GeneralSettingsPanel.registerComponentAndSetFontSize(clipChooser);
		setChildComponentsFocusable(MusicPlayer.this, false);

		Application.loadTimeLogger.log(Level.FINE, "time to make a MusicPlayer: " + (System.currentTimeMillis() - start));
	}
	
	void setChildComponentsFocusable(Component c, boolean b)
	{
		if (c == null) return;
		
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				setComponentFocusable(child, b);
			}
		}
	}
	
	void setComponentFocusable(Component c, boolean b)
	{
		if (c == null) return;
		c.setFocusable(b);

		setChildComponentsFocusable(c, b);
	}
}
					