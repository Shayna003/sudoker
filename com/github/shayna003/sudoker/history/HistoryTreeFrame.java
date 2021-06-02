package com.github.shayna003.sudoker.history;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * One instance per application
 * @since 4-18-2021
 */
@SuppressWarnings("CanBeFinal")
public class HistoryTreeFrame extends JFrame
{
	public JTabbedPane windows; // contains JTabbedPanes
	JMenu options;
	JMenuBar menuBar;
	
	JMenuItem importHistory;
	JMenuItem undo;
	JMenuItem redo;
	JMenuItem deleteNode;
	JMenuItem deleteNodeAndChildren;
	JMenuItem treeSettingsItem;
	
	public HistoryTreeFrame()
	{
		options = new JMenu("Options");
		options.setMnemonic('O');
		
		// dummy values for now
		importHistory = new JMenuItem("Import History From SelectedNode");
		// so that this item appears in the correct order in KeyboardSettingsPanel
		Application.keyboardSettingsPanel.registerOtherShortcut("importHistory", KeyboardSettingsPanel.getMenuItemString("History Tree", "Import History From Selected Node"), true, KeyEvent.VK_ENTER, 0, null, null, 0);
		
		undo = new JMenuItem("Undo");
		redo = new JMenuItem("Redo");
		deleteNode = new JMenuItem("Delete Selected Node");
		// so that this item appears in the correct order in KeyboardSettingsPanel
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteTreeNode", KeyboardSettingsPanel.getMenuItemString("History Tree", "Delete Selected Node"), true, KeyEvent.VK_BACK_SPACE, 0, null, null, 0);
		
		deleteNodeAndChildren = new JMenuItem("Delete Selected Node and All of Its Children");
		// so that this item appears in the correct order in KeyboardSettingsPanel
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteAllChildTreeNode", KeyboardSettingsPanel.getMenuItemString("History Tree", "Delete Selected Node and All of Its Children"), true, KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, null, 0);
		
		treeSettingsItem = new JMenuItem("\u2699 History Tree Settings", 'S');
		treeSettingsItem.addActionListener(event ->
		{
			Application.preferenceFrame.showUp(Application.historyTreeSettingPanel);
		});
		Application.keyboardSettingsPanel.registerOtherShortcut("treeSettings", KeyboardSettingsPanel.getMenuItemString("History Tree", "\u2699 History Tree Settings"), true, KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), null, treeSettingsItem, 0);
		
		options.add(importHistory);
		options.add(undo);
		options.add(redo);
		
		options.addSeparator();
		options.add(deleteNode);
		options.add(deleteNodeAndChildren);
		
		options.addSeparator();
		options.add(treeSettingsItem);
		menuBar = new JMenuBar();
		menuBar.add(options);
		setJMenuBar(menuBar);
		
		windows = new JTabbedPane();
		windows.addChangeListener(event ->
		{
			selectionChanged();
		});
		
		setTitle("History Trees");
		add(windows, BorderLayout.CENTER);
		pack();
		setLocationByPlatform(true);
	}
	
	public void selectionChanged()
	{
		HistoryTreePanel panel = getSelectedHistoryTreePanel();
		
		if (panel != null)
		{
			importHistory.setAction(panel.historyTree.importHistory);
			undo.setAction(panel.historyTree.undo);
			redo.setAction(panel.historyTree.redo);
			deleteNode.setAction(panel.historyTree.deleteSelectedNode);
			deleteNodeAndChildren.setAction(panel.historyTree.deleteSelectedNodeAndItsChildren);
		}
	}
	
	public JTabbedPane getSelectedWindow()
	{
		return (JTabbedPane) windows.getSelectedComponent();
	}
	
	public HistoryTreePanel getSelectedHistoryTreePanel()
	{
		JTabbedPane selectedWindow = getSelectedWindow();
		if (selectedWindow == null) return null;
		return (HistoryTreePanel) selectedWindow.getSelectedComponent();
	}

	public HistoryTreePanel makeTreeForTab(SudokuTab s)
	{
		HistoryTreePanel panel = new HistoryTreePanel(s.board.creationEvent);
		panel.setName(s.getName());
		s.owner.historyTrees.addTab(panel.getName(), null, panel, "History Tree for this Board");
		if (s.owner.historyTrees.getTabCount() == 1) 
		{
			s.owner.historyTrees.setSize(s.owner.historyTrees.getPreferredSize());
			windows.setSize(windows.getPreferredSize());
			pack();
		}
		return panel;
	}
	
	public JTabbedPane makeTabbedPaneForWindow(ApplicationFrame frame)
	{
		JTabbedPane pane = new JTabbedPane();
		pane.addChangeListener(event ->
		{
			selectionChanged();
		});
		pane.setName(frame.getName());
		windows.addTab(pane.getName(), null, pane, "History Trees for this Window's Boards");
		if (windows.getTabCount() == 1)
		{
			windows.setSize(windows.getPreferredSize());
			pack();
		}
		frame.historyTrees = pane;
		return pane;
	}
	
	public HistoryTreePanel makeNewTabbedPaneAndTree(SudokuTab s)
	{
		JTabbedPane pane = makeTabbedPaneForWindow(s.owner);
		return makeTreeForTab(s);
	}
	
	/**
	 * Things don't fit on the screen if you call JFrame.setDefaultLookAndFeelDecorated(true) 
	 * When the look and feel is Metal
	 */
	@Override
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets i = getToolkit().getScreenInsets(getGraphicsConfiguration());
		int insetsWidth = i.left + i.right;
		int insetsHeight = i.top + i.bottom;
		return new Dimension(d.width + insetsWidth > screenSize.width ? screenSize.width - insetsWidth : d.width, d.height + insetsHeight > screenSize.height ? screenSize.height - insetsHeight : d.height);
	}
}