package com.github.shayna003.sudoker;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;

/**
 * This class displays the open windows and tabs in a JTree.
 * Need to override the toString() methods of an ApplicationFrame and SudokuTab
 * @since 4-16-2021
 */
@SuppressWarnings("CanBeFinal")
public class OpenWindowsAndTabs extends JPanel
{
	JDialog tabChooserDialog; // shows up to let the user select a SudokuTab
	public JTree tree; // shows hierarchy of tabs and windows
	WindowsAndTabsTreeModel model;
	JPanel treePanel; // contains the tree
	String root = "Open Windows And Tabs"; // root of the tree
	
	// for tabChooserDialog
	boolean okClicked;
	JPanel buttonPanel;
	JButton okButton;
	JButton cancelButton;
	
	// constants to determine the mode of choosing an item and making the ok button enabled
	int mode = CHOOSE_TAB;
	static final int CHOOSE_WINDOW = 0;
	static final int CHOOSE_TAB = 1;
	
	@SuppressWarnings("CanBeFinal")
    class WindowsAndTabsTreeModel implements TreeModel
	{
		EventListenerList listenerList = new EventListenerList();
		
		@Override
		public Object getRoot()
		{
			return root;
		}
		
		@Override
		public int getChildCount(Object parent)
		{
			if (parent.equals(root)) return Application.openWindows.size();
			else if (parent instanceof ApplicationFrame) return ((ApplicationFrame) parent).tabbedPane.getTabCount();
			else return 0;
		}
		
		@Override
		public Object getChild(Object parent, int index)
		{ 
			if (parent.equals(root)) return Application.openWindows.get(index);
			else return ((ApplicationFrame) parent).tabbedPane.getComponentAt(index); 
		}
		
		@Override
		public int getIndexOfChild(Object parent, Object child)
		{
			if (parent.equals(root)) return Application.openWindows.indexOf((ApplicationFrame) child);
			else return ((ApplicationFrame) parent).tabbedPane.indexOfComponent((SudokuTab) child);
		}
		
		@Override
		public boolean isLeaf(Object node)
		{
			return getChildCount(node) == 0;
		}
		
		/**
		 * Not used because this tree does not enable editing
		 */
		@Override
		public void valueForPathChanged(TreePath path, Object newValue)
		{
		}
		
		@Override
		public void addTreeModelListener(TreeModelListener l)
		{
			listenerList.add(TreeModelListener.class, l);
		}
		
		@Override
		public void removeTreeModelListener(TreeModelListener l)
		{
			listenerList.remove(TreeModelListener.class, l);
		}
		
		protected void fireTreeStructureChanged(TreeModelEvent event)
		{
			for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class))
			{
				l.treeStructureChanged(event);
			}
		}
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (tree != null) 
		{
			tree.updateUI();
			DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
			renderer.setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
			renderer.setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
		}	
	}
	
	public OpenWindowsAndTabs()
	{
		super(new BorderLayout());
		model = new WindowsAndTabsTreeModel();
		tree = new JTree(model);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setRootVisible(false);
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent event)
			{
				if (mode == CHOOSE_TAB)
				{
					okButton.setEnabled(tree.getLastSelectedPathComponent() instanceof SudokuTab);
				}
				else 
				{
					okButton.setEnabled(tree.getLastSelectedPathComponent() instanceof ApplicationFrame);
				}
			}
		});
		
		JScrollPane pane = new JScrollPane(tree);
		treePanel = new JPanel(new BorderLayout());
		treePanel.add(pane, BorderLayout.CENTER);
		treePanel.setBorder(BorderFactory.createTitledBorder("Open Windows and Tabs"));
		
		JPanel buttonPanel = new JPanel();
		okButton = new JButton("Ok");
		okButton.addActionListener(event ->
		{
			okClicked = true;
			tabChooserDialog.setVisible(false);
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(event ->
		{
			tabChooserDialog.setVisible(false);
		});
		
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		add(treePanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		
		tabChooserDialog = new JDialog();
		tabChooserDialog.setModal(true);
		tabChooserDialog.add(OpenWindowsAndTabs.this);
		tabChooserDialog.pack();
	}
	
	public SudokuTab showTabChooserDialog(Component owner)
	{
		okClicked = false;
		mode = CHOOSE_TAB;
		okButton.setEnabled(tree.getSelectionCount() == 1 && tree.getLastSelectedPathComponent() instanceof SudokuTab);
		
		tabChooserDialog.setTitle("Choose A Tab");
		tabChooserDialog.setLocationRelativeTo(owner);
		tabChooserDialog.setVisible(true);
		okButton.requestFocus();
		if (okClicked)
		{
			return (SudokuTab) tree.getLastSelectedPathComponent();
		}
		else 
		{
			return null;
		}
	}
	
	public ApplicationFrame showWindowChooserDialog(Component owner)
	{
		okClicked = false;
		mode = CHOOSE_WINDOW;
		okButton.setEnabled(tree.getSelectionCount() == 1 && tree.getLastSelectedPathComponent() instanceof ApplicationFrame);
		
		tabChooserDialog.setTitle("Choose A Window");
		tabChooserDialog.setLocationRelativeTo(owner);
		tabChooserDialog.setVisible(true);
		okButton.requestFocus();
		if (okClicked)
		{
			return (ApplicationFrame) tree.getLastSelectedPathComponent();
		}
		else 
		{
			return null;
		}
	}
	
	/**
	 * Refreshes the tree, called by Application
	 */
	public void addWindow(ApplicationFrame frame)
	{
		model.fireTreeStructureChanged(new TreeModelEvent(tree, new TreePath(root)));
	}
	
	/**
	 * Refreshes the tree, called by Application
	 */
	public void addTab(ApplicationFrame frame, SudokuTab tab)
	{
		model.fireTreeStructureChanged(new TreeModelEvent(tree, new TreePath(new Object[] { root, frame })));
	}
	
	/**
	 * Refreshes the tree, called after renaming a window or tab
	 */
	public void windowChanged(ApplicationFrame frame)
	{
		model.fireTreeStructureChanged(new TreeModelEvent(tree, new TreePath(new Object[] { root, frame })));
	}
	
	/**
	 * Refreshes the tree, called after merging all tabs to a single window by an ApplicationFrame
	 */
	public void rootChanged()
	{
		model.fireTreeStructureChanged(new TreeModelEvent(tree, new TreePath(root)));
	}
}