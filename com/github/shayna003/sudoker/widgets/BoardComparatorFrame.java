package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.widgets.BoardComparator.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This frame holds Board Comparators.
 * @since 4-30-2021
 * Started working on 5-5-2021
 */
@SuppressWarnings("CanBeFinal")
public class BoardComparatorFrame extends JFrame
{
	JMenuBar menuBar;
	JMenu options; // options
	JMenu boards; // show which board is which
	
	// I used JMenuItm because I have no room to put this elsewhere
	// both disabled
	JMenuItem board1Info;
	JMenuItem board2Info;
	
	public JTabbedPane tabbedPane;
	public JScrollPane scrollPane;
	
	boolean boardComparatorAdded = false;
	
	JMenuItem newItem;
	JMenuItem deleteItem;
	JMenuItem swapBoards;
	public JCheckBoxMenuItem linkBoard1ToTree;
	public JCheckBoxMenuItem linkBoard2ToTree;
	JMenuItem boardComparatorSettings;
	JMenuItem boardComparatorColorSettings;
	
	public BoardComparator makeNewComparator()
	{
		BoardComparator newComparator = new BoardComparator();
		tabbedPane.addTab("Comparator " + (tabbedPane.getTabCount() + 1), newComparator);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		deleteItem.setEnabled(true);
		swapBoards.setEnabled(true);
		
		if (!boardComparatorAdded) 
		{
			boardComparatorAdded = true;
			pack();
		}
		return newComparator;
	}
	
	public BoardComparator getSelectedComparator()
	{
		return (BoardComparator) tabbedPane.getSelectedComponent();
	}
	
	public BoardComparatorFrame()
	{
		setTitle("Board Comparators");
		
		tabbedPane = new JTabbedPane();
		
		options = new JMenu("Options");
		options.setMnemonic('O');
		
		newItem = new JMenuItem("New Board Comparator", 'N');
		deleteItem = new JMenuItem("Close this Board Comparator", 'C');
		swapBoards = new JMenuItem("\u21CC Swap Board Positions", 'S');

		linkBoard1ToTree = new JCheckBoxMenuItem("Link Left Board to its History Tree's Selected Node");
		linkBoard1ToTree.setMnemonic('L');
		linkBoard1ToTree.setDisplayedMnemonicIndex(5);
		
		linkBoard2ToTree = new JCheckBoxMenuItem("Link Right Board to its History Tree's Selected Node");
		linkBoard2ToTree.setMnemonic('R');
		
		boardComparatorSettings = new JMenuItem("\u2699 Board Comparator Settings", 'S');
		boardComparatorColorSettings = new JMenuItem("\u2699 Board Comparator Color Settings", 'S');
		
		boardComparatorSettings.addActionListener(event ->
		{
			Application.preferenceFrame.showUp(Application.boardSettingsPanel);
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("boardComparatorSettings", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "\u2699 Board Comparator Settings"), true, KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, boardComparatorSettings, 0);
		
		boardComparatorColorSettings.addActionListener(event ->
		{
			Application.preferenceFrame.showUp(Application.themesPanel, Application.themesPanel.differentValueColor);
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("boardComparatorColorSettings", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "\u2699 Board Comparator Color Settings"), true, KeyEvent.VK_COMMA, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, null, boardComparatorColorSettings, 0);
		
		tabbedPane.addChangeListener(event ->
		{
			BoardComparator newSelected = (BoardComparator) tabbedPane.getSelectedComponent();
			if (newSelected != null)
			{
				linkBoard1ToTree.setSelected(newSelected.board1.linkHistoryTreeSelectedNode);
				linkBoard2ToTree.setSelected(newSelected.board2.linkHistoryTreeSelectedNode);
				linkBoard1ToTree.setEnabled(newSelected.board1.chosenTab != null);
				linkBoard2ToTree.setEnabled(newSelected.board2.chosenTab != null);
				
				setChosenInformation(newSelected.board1, newSelected.board1.getChosenBoardInfo());
				setChosenInformation(newSelected.board2, newSelected.board2.getChosenBoardInfo());
				
				board1Info.setEnabled(true);
				board2Info.setEnabled(true);
			}
			else 
			{
				board1Info.setText("--");
				board2Info.setText("--");
				board1Info.setEnabled(false);
				board2Info.setEnabled(false);
				
				deleteItem.setEnabled(false);
				swapBoards.setEnabled(false);

				linkBoard1ToTree.setEnabled(false);
				linkBoard2ToTree.setEnabled(false);
				linkBoard1ToTree.setSelected(Application.boardSettingsPanel.defaultLinkEnabled.isSelected());
				linkBoard2ToTree.setSelected(Application.boardSettingsPanel.defaultLinkEnabled.isSelected());
			}
		});
		
		linkBoard1ToTree.addActionListener(event ->
		{
			BoardComparator selected = (BoardComparator) tabbedPane.getSelectedComponent();
			selected.board1.linkHistoryTreeSelectedNode = linkBoard1ToTree.isSelected();
			selected.board1.linkBoard();
		});
		linkBoard1ToTree.setEnabled(false);
		Application.keyboardSettingsPanel.registerOtherShortcut("linkBoard1ToTree", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Link Left Board to its History Tree's Selected Node"), true, KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, linkBoard1ToTree, 0);
		
		linkBoard2ToTree.addActionListener(event ->
		{
			BoardComparator selected = (BoardComparator) tabbedPane.getSelectedComponent();
			selected.board2.linkHistoryTreeSelectedNode = linkBoard2ToTree.isSelected();
			selected.board2.linkBoard();
		});
		linkBoard2ToTree.setEnabled(false);
		Application.keyboardSettingsPanel.registerOtherShortcut("linkBoard2ToTree", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Link Right Board to its History Tree's Selected Node"), true, KeyEvent.VK_2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, linkBoard2ToTree, 0);
		
		newItem.addActionListener(event ->
		{
			makeNewComparator();
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("newBoardComparator", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "New Board Comparator"), true, KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, newItem, 0);
		
		deleteItem.addActionListener(event ->
		{
			tabbedPane.removeTabAt(tabbedPane.getSelectedIndex());
			for (int t = 0; t < tabbedPane.getTabCount(); t++)
			{
				tabbedPane.setTitleAt(t, "Board Comparator " + (t + 1));
			}
		});
		deleteItem.setEnabled(false);
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteBoardComparator", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Close this Board Comparator"), true, KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, deleteItem, 0);
		
		swapBoards.addActionListener(event ->
		{
			BoardComparator selectedComparator = (BoardComparator) tabbedPane.getSelectedComponent();
			selectedComparator.swapBoards();
			
			linkBoard1ToTree.setSelected(selectedComparator.board1.linkHistoryTreeSelectedNode);
			linkBoard2ToTree.setSelected(selectedComparator.board2.linkHistoryTreeSelectedNode);
			linkBoard1ToTree.setEnabled(selectedComparator.board1.chosenTab != null);
			linkBoard2ToTree.setEnabled(selectedComparator.board2.chosenTab != null);
			
			setChosenInformation(selectedComparator.board1, selectedComparator.board1.getChosenBoardInfo());
			setChosenInformation(selectedComparator.board2, selectedComparator.board2.getChosenBoardInfo());
			repaint();
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("swapBoardComparatorBoards", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Swap Board Positions"), true, KeyEvent.VK_S, 0, null, swapBoards, 0);
		swapBoards.setEnabled(false);
		
		options.add(newItem);
		options.add(deleteItem);
		
		options.addSeparator();
		options.add(boardComparatorSettings);
		options.add(boardComparatorColorSettings);
		
		menuBar = new JMenuBar();
		menuBar.add(options);
		
		boards = new JMenu("Boards");
		boards.setMnemonic('B');
		board1Info = new JMenuItem("--", 'L'); // left board: xxx
		board1Info.addActionListener(event ->
		{
			BoardComparator comparator = (BoardComparator) tabbedPane.getSelectedComponent();
			comparator.chooseBoard1();
			linkBoard1ToTree.setEnabled(comparator.board1.chosenTab != null);
			repaint();
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("board1Info", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Choose Board 1"), true, KeyEvent.VK_1, 0, null, board1Info, 0);
		
		board2Info = new JMenuItem("--", 'R'); // right board: xxx
		board2Info.addActionListener(event ->
		{
			BoardComparator comparator = (BoardComparator) tabbedPane.getSelectedComponent();
			comparator.chooseBoard2();
			linkBoard2ToTree.setEnabled(comparator.board1.chosenTab != null);
			repaint();
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("board2Info", KeyboardSettingsPanel.getMenuItemString("Board Comparator", "Show Board 2"), true, KeyEvent.VK_2, 0, null, board2Info, 0);
		board1Info.setEnabled(false);
		board2Info.setEnabled(false);
		boards.add(board1Info);
		boards.add(board2Info);
		
		boards.addSeparator();
		
		boards.add(linkBoard1ToTree);
		boards.add(linkBoard2ToTree);
		
		boards.addSeparator();
		boards.add(swapBoards);
		
		menuBar.add(boards);
		setJMenuBar(menuBar);
		
		scrollPane = new JScrollPane(tabbedPane);
		add(scrollPane, BorderLayout.CENTER);
		setLocationByPlatform(true);
		
		GeneralSettingsPanel.registerComponentAndSetFontSize(this);
	}
	
	public void setChosenInformation(BoardViewPane pane, String newInfo)
	{
		BoardComparator selectedComparator = ((BoardComparator) tabbedPane.getSelectedComponent());
		assert selectedComparator != null;
		
		if (pane == selectedComparator.board1)
		{
			board1Info.setText("Left Board Comparator's Board: " + newInfo);
		}
		else if (pane == selectedComparator.board2)
		{
			board2Info.setText("Right Board Comparator's Board: " + newInfo);
		}
	}
	
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
	
	public void tabWasClosed(SudokuTab tab)
	{
		for (int t = 0; t < tabbedPane.getTabCount(); t++)
		{
			((BoardComparator) tabbedPane.getComponentAt(t)).tabWasClosed(tab);
		}
	}
	
	public void updateChosenBoardInfos()
	{
		for (int t = 0; t < tabbedPane.getTabCount(); t++)
		{
			((BoardComparator) tabbedPane.getComponentAt(t)).updateChosenBoardInfos();
		}
	}
}