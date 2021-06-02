package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.history.Edit;
import com.github.shayna003.sudoker.history.EditType;
import com.github.shayna003.sudoker.prefs.components.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.prefs.*;
import java.util.logging.*;
import java.io.*;

/**
 * @since 2-20-2021
 * A JTree used for showing and traversing solving techniques when solving a sudoku.
 * This component can be used in regular solving, as well as used in giving generating options
 * Current thoughts:
 * A solving tree in the preference frame for arbitrating the initial state (node order, checkbox, and even expansion perhaps) and order of solving techniques upon new creation, for example, by making a new tab. Already created trees will not update after changing this setting. Because solving might be ongoing... Or schedule an update.. whatever no thanks.

	Right now deselecting a difficulty node's checkbox only disables its children's checkboxes, and clicking again enables them back.
	
	How about make it so that in every solving iteration, a solving technique will be invoked only if it's checkbox is selected, enabled, and the node is visible (with its difficulty node expanded)?
	or just make processed hidden nodes' parents expand to show them...
	
	the pro of the first is that you can simple iterate though the tree using its getRowCount() method that returns the number of visible nodes
	
 * Basic techniques and the "solving techniques root" cannot be selected and included in a dnd.
 * cannot try to drop things before or within BASIC section

 * Regarding other dnd rules:
 * 1 solution:
	can only change order of stuff within the same difficulty, and can dnd difficulty within the difficulty level hierarchy
	
	another solution: (complex)
		visually indicate difficulty on individual labels etc, but what about the node groupings according to difficulty? ... model controller view... two views, same model? but no, they refer to two models... ugh...
	!!But all ot this depends on the assumption that I will populate the solving list with a bunch of items... which might take a long time from now to happen...
		
 * A side note: the component returned by getTreeCellRenderComponent() is only used for its paint() method, and event detection etc will not work until editing starts
 */
public class SolvingTechniqueTree extends JTree implements PrefsComponent, Cloneable, Comparator<JCheckBoxTreeNode>
{	
	/**
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	public int compare(JCheckBoxTreeNode a, JCheckBoxTreeNode b)
	{
		return a.treePosition - b.treePosition;
	}

	public ArrayList<DifficultyNode> difficultyNodes = new ArrayList<DifficultyNode>(Difficulty.values().length);
	
	public EnumMap<Difficulty, ArrayList<SolvingTechniqueNode>> techniqueNodesByDifficulty = new EnumMap<Difficulty, ArrayList<SolvingTechniqueNode>>(Difficulty.class);
	
	SolvingTechniqueTreeModel model;
	SolvingTechniqueTreeRenderer renderer;
	Board board;
	boolean isModelTree;
	
	public static void createTreeFrame(SolvingTechniqueTree tree)
	{
		JFrame frame = new JFrame();
		frame.add(new JScrollPane(tree), BorderLayout.CENTER);
		
		JButton setToDefault = new JButton("Set to Default Order and State");
		setToDefault.addActionListener(event ->
		{
			tree.resetToDefault();
		});
		
		JButton cloneButton = new JButton("create clone");
		cloneButton.addActionListener(event ->
		{
			createTreeFrame(tree.clone());
		});
		
		
		frame.add(setToDefault, BorderLayout.SOUTH);
		frame.add(cloneButton, BorderLayout.NORTH);
		
		frame.setTitle("Keep up the good work! I can do this!");
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setLocationByPlatform(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	@Override
	public void updateUI()
	{
		Application.solverTreeLogger.log(Level.FINE, "updateUI for tree");
		if (difficultyNodes != null)
		{
			for (DifficultyNode d : difficultyNodes)
			{
				d.updateUI();
				for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(d.difficulty))
				{
					st.updateUI();
				}
			}
		}

		super.updateUI();
		if (renderer != null)
		{
			renderer.setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
			renderer.setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
			renderer.setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
		}
	}

	/**
	 * Need to extend DefaultTreeCellRenderer to get default behaviour for showing selection
	 */
	class SolvingTechniqueTreeRenderer extends DefaultTreeCellRenderer
	{
		public SolvingTechniqueTreeRenderer()
		{
			super();
			setBackgroundNonSelectionColor(new Color(0, 0, 0, 0)); // for Nimbus look and feel
			setOpenIcon(null);
			setClosedIcon(null);
			setLeafIcon(null);
		}
		
		@Override 
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			l.setOpaque(false);
			JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
			p.setOpaque(false);
			p.setBackground(new Color(0, 0, 0, 0));
			
			TreePath path = getPathForRow(row);
			if (path == null) // I don't know why this happens when you change the look and feel
			{
				return p;
			}
			Object obj = path.getLastPathComponent();
			if (obj instanceof JCheckBoxTreeNode)
			{		
				p.add(((JCheckBoxTreeNode) obj).checkBox);	
			}
			p.add(l);
			
			return p;
		}
	}
	
	/**
	 * Clicking a checkbox will not select a path
	 * Some paths (root path and BASIC nodes) cannot be selected to prevent dnd
	 */
	class SolvingTechniqueTreeSelectionModel extends DefaultTreeSelectionModel
	{
		@Override 
		public void addSelectionPath(TreePath path)
		{
			Application.solverTreeLogger.log(Level.FINE, "addSelectionPath for " + path);

			if (shouldSelectPath(path))
			{
				super.addSelectionPath(path);
			}
		}
		
		@Override 
		public void addSelectionPaths(TreePath[] paths)
		{
			Application.solverTreeLogger.log(Level.FINE, "addSelectionPaths for " + Arrays.toString(paths));
			Application.solverTreeLogger.log(Level.FINE, "getSelectablePaths: " + Arrays.toString(getSelectablePaths(paths)));
			super.addSelectionPaths(getSelectablePaths(paths));
		}
		
		@Override
		public void setSelectionPath(TreePath path)
		{
			Application.solverTreeLogger.log(Level.FINE, "setSelectionPath to " + path);
			TreePath[] selected = getSelectionPaths();
			if (selected.length == 1 && selected[0].equals(path)) return;
			
			// no selection changes if you click on a chek box
			if (isCheckBoxClicked(path))
			{
				return;
			}
			
			if (isSelectable(path))
			{
				super.setSelectionPath(path);
			}
		}
		
		@Override
		public void setSelectionPaths(TreePath[] paths)
		{
		Application.solverTreeLogger.log(Level.FINE, "setSelectionPaths to " + Arrays.toString(paths));
			TreePath[] result = getSelectablePaths(paths);
			
			if (result.length > 0)
			{
				super.setSelectionPaths(result);
			}
		}
	}
	
	/**
	 * The basic techniques cannot be selected to prevent dnd
	 */
	public static boolean isSelectable(TreePath path)
	{
		Object o = path.getLastPathComponent();
		if (!(o instanceof JCheckBoxTreeNode)) return false;
		if (o instanceof DifficultyNode && ((DifficultyNode) o).difficulty == Difficulty.BASIC) return false;
		if (o instanceof SolvingTechniqueNode)
		{
			return ((SolvingTechniqueNode) o).technique.difficulty != Difficulty.BASIC;
		}
		return true;
	}
	
	/**
	 * Filters out the TreePaths that can't be selected in paths
	 */
	public TreePath[] getSelectablePaths(TreePath[] paths)
	{
		ArrayList<TreePath> selectable = new ArrayList<>();
		for (TreePath path : paths)
		{
			if (shouldSelectPath(path))
			{
				selectable.add(path);
			}
		}
		return selectable.toArray(new TreePath[0]);
	}
	
	/**
	 * @return true if path is already selected, or the selection was not triggered by a mouse click on
	 * the nodes' checkbox
	 */
	public boolean shouldSelectPath(TreePath path)
	{
		return isPathSelected(path) || ((isSelectable(path) && !isCheckBoxClicked(path)));
	}
	
	/**
	 * Determines if the user clicked on a node's checkbox
	 */
	public boolean isCheckBoxClicked(TreePath path)
	{
		if (path == null) return false;
		Point p = getMousePosition(false);
		if (p == null) return false;
		int x = p.x;
		int y = p.y;
		Object o = path.getLastPathComponent();
		
		if (o instanceof JCheckBoxTreeNode)
		{
			Rectangle r = getPathBounds(path);
			if (r != null)
			{
				Dimension d = ((JCheckBoxTreeNode) o).checkBox.getPreferredSize();
				return x <= d.getWidth() + r.x && y <= d.getHeight() + r.y;
			}
		}
		return false;
	}
	
	/**
	 * To prevent clearing selection if trying to expand tree
	 * Alternative is to give a "clear selection button" instead of
	 * Auto clear selection after clicking in the tree but not on a node
	 */
	class TreeWillExpandHandler implements TreeWillExpandListener
	{
		public void treeWillExpand(TreeExpansionEvent event)
		{
			Application.solverTreeLogger.log(Level.FINE, "Tree will expand");
			expanded_or_collapsed_tree = true;
		}
		
		public void treeWillCollapse(TreeExpansionEvent event)
		{
			Application.solverTreeLogger.log(Level.FINE, "Tree will collapse");
			expanded_or_collapsed_tree = true;
		}
	}
	
	/**
	 * To save expansion states in noes to recreate expansion states after cloning or dnd
	 */
	class TreeExpansionHandler implements TreeExpansionListener
	{
		public void treeCollapsed(TreeExpansionEvent event)
		{
			Application.solverTreeLogger.log(Level.FINE, event.getPath().getLastPathComponent() + " collapsed");
			Object o = event.getPath().getLastPathComponent();
			if (o instanceof JCheckBoxTreeNode) ((JCheckBoxTreeNode) (o)).expanded = false;
		}
		
		public void treeExpanded(TreeExpansionEvent event)
		{
			Application.solverTreeLogger.log(Level.FINE, event.getPath().getLastPathComponent() + " expanded");
			Object o = event.getPath().getLastPathComponent();
			if (o instanceof JCheckBoxTreeNode) ((JCheckBoxTreeNode) (o)).expanded = true;
		}
	}
	
	boolean clickedOnAPath = false;
	boolean expanded_or_collapsed_tree = false;
	boolean clickedPopupTrigger = false;
	
	class ClickHandler extends MouseAdapter
	{
		@Override
		public void mouseReleased(MouseEvent event)
		{
			if (contains(event.getPoint()))
			{
				// if didn't click on a node in previous click and no tree expansion/collapse happening,
				// clear selection
				// does not work properly in Mac OSX look and feel because TreeWillExpandListener gets called after this function
				if (!clickedOnAPath && !expanded_or_collapsed_tree && !clickedPopupTrigger)
				{
					Application.solverTreeLogger.log(Level.FINE, "clear selection because no path selected");
					clearSelection();
				}	
			}
			
			clickedOnAPath = false;
			expanded_or_collapsed_tree = false;
			clickedPopupTrigger = false;
		}
		
		@Override
		public void mousePressed(MouseEvent event)
		{
			int x = event.getX();
			int y = event.getY();
			int row = getRowForLocation(x, y);
			
			if (row >= 0)
			{
				clickedOnAPath = true;

				TreePath path = getPathForLocation(x, y);
				if (path != null)
				{
					Object obj = path.getLastPathComponent();

					if (event.isPopupTrigger())
					{
						clickedPopupTrigger = true;
						if (obj instanceof JCheckBoxTreeNode)
						{
							((JCheckBoxTreeNode) obj).popupMenu.show(SolvingTechniqueTree.this, x, y);
							// Exception in thread "AWT-EventQueue-0" java.awt.IllegalComponentStateException: component must be showing on the screen to determine its location
							// can't use obj as invoker
						}
					}
					else if (isSelectable(path) && isCheckBoxClicked(path))
					{
						((JCheckBoxTreeNode) obj).checkBoxClicked();
						repaint();
					}
				}
			}
		}
	}
	
	/**
	 * Do not remove a path if the user clicked on its checkbox
	 */
	@Override
	public void removeSelectionPath(TreePath path)
	{
		Application.solverTreeLogger.log(Level.FINE, "removeSelectionPath for " + path);
		if (isCheckBoxClicked(path))
		{
			return;
		}
		super.removeSelectionPath(path);
	}
	
	void createNodes()
	{
		DifficultyNode df;
		
		ArrayList<SolvingTechnique> techniques;
		ArrayList<SolvingTechniqueNode> techniquesNodes;
		for (Difficulty d : Difficulty.values())
		{
			df = new DifficultyNode(d, this);
			difficultyNodes.add(df);
			
			techniques = SolvingTechnique.get(d);
			techniquesNodes = new ArrayList<>(techniques.size());
			for (SolvingTechnique st : techniques)
			{
				techniquesNodes.add(new SolvingTechniqueNode(st, SolvingTechniqueTree.this, df));
			}
			techniqueNodesByDifficulty.put(d, techniquesNodes);
		}
	}
	
	/**
	 * Only one SolvingTechniqueTree is created in SolverSettingsPanel at first,
	 * Then all subsequent SolvingTechniqueTrees are created from cloning it.
	 */
	@Override
	public SolvingTechniqueTree clone()
	{
		SolvingTechniqueTree copy = new SolvingTechniqueTree(difficultyNodes, techniqueNodesByDifficulty);

		if (isCollapsed(0))
		{
			copy.collapsePath(new TreePath(model.root));
		}

		return copy;
	}
	
	/*
	 * Used for cloning. Traverses all nodes and duplicate their selection and expansion states.
	 */
	private SolvingTechniqueTree(ArrayList<DifficultyNode> sourceDifficultyNodes, EnumMap<Difficulty, ArrayList<SolvingTechniqueNode>> sourceTechniqueNodes)
	{
		super();
		init();
		
		DifficultyNode df;
		for (DifficultyNode node : sourceDifficultyNodes)
		{
			df = new DifficultyNode(node.difficulty, this, node.isCheckBoxSelected(), node.expanded);
			difficultyNodes.add(df);
			
			ArrayList<SolvingTechniqueNode> sourceNodes = sourceTechniqueNodes.get(node.difficulty);
			ArrayList<SolvingTechniqueNode> duplicates = new ArrayList<>(sourceNodes.size());
			
			for (SolvingTechniqueNode st : sourceNodes)
			{
				SolvingTechniqueNode duplicateNode = new SolvingTechniqueNode(st.technique, SolvingTechniqueTree.this, df, st.isCheckBoxSelected(), st.expanded);
				duplicates.add(duplicateNode);
				if (duplicateNode.children != null)
				{
					for (int i = 0; i < duplicateNode.children.size(); i++)
					{
						duplicateNode.children.get(i).setCheckBoxSelected(st.children.get(i).isCheckBoxSelected());
					}
				}
			}
			
			techniqueNodesByDifficulty.put(node.difficulty, duplicates);
		}
		
		model.fireTreeStructureChanged(new TreeModelEvent(SolvingTechniqueTree.this, new Object[] { model.root }));
		
		for (DifficultyNode node : difficultyNodes)
		{
			boolean nodeExpanded = node.expanded;
			for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(node.difficulty))
			{
				if (st.expanded) setExpandedState(st.path, true);
			}
			setExpandedState(node.path, nodeExpanded);
		}
	}
	
	void init()
	{
		model = new SolvingTechniqueTreeModel();
		setModel(model);
		setDragEnabled(true);
		setDropMode(DropMode.INSERT);
		setTransferHandler(new SolvingTechniqueTransferHandler());
		renderer = new SolvingTechniqueTreeRenderer();

		setCellRenderer(renderer);
		
		setEditable(false);
		setShowsRootHandles(true);
		
		addMouseListener(new ClickHandler());
		setToggleClickCount(-1);
		
		setSelectionModel(new SolvingTechniqueTreeSelectionModel());
		
		//to determine if will clear selection after not clicking on a node
		addTreeWillExpandListener(new TreeWillExpandHandler());
		addTreeExpansionListener(new TreeExpansionHandler());
		
		setOpaque(false);
	}
	
	/**
	 * Creates a tree used for showing and traversing solving techniques when solving a sudoku.
	 * This constructor is only called for the model solving tree.
	 * All trees in SolverPanels will be clones of the model solving tree.
	 * Node order, node expansion states and checkbox states will be cloned.
	 */
	public SolvingTechniqueTree()
	{
		super();
		this.isModelTree = true;
		init();

		createNodes();
		model.fireTreeStructureChanged(new TreeModelEvent(SolvingTechniqueTree.this, new Object[] { model.root }));
	}
	
	/**
	 * Sets the order, selection states, and expansion states of all tree nodes to default
	 */
	@Override
	public void resetToDefault()
	{
		techniqueNodesByDifficulty = new EnumMap<Difficulty, ArrayList<SolvingTechniqueNode>>(Difficulty.class);
		difficultyNodes = new ArrayList<>(Difficulty.values().length);
		DifficultyNode df;
		
		ArrayList<SolvingTechnique> techniques;
		ArrayList<SolvingTechniqueNode> techniqueNodes;
		for (Difficulty d : Difficulty.values())
		{
			df = new DifficultyNode(d, this, true, true);
			difficultyNodes.add(df);
			techniques = SolvingTechnique.get(d);
			techniqueNodes = new ArrayList<>(techniques.size());
			for (SolvingTechnique st : techniques)
			{
				SolvingTechniqueNode node = new SolvingTechniqueNode(st, SolvingTechniqueTree.this, df, true, false);
				techniqueNodes.add(node);
				if (node.children != null)
				{
					for (SolvingTechniqueNode c : node.children)
					{
						c.setCheckBoxSelected(true);
					}
				}
			}
			techniqueNodesByDifficulty.put(d, techniqueNodes);
		}
		
		model.fireTreeStructureChanged(new TreeModelEvent(SolvingTechniqueTree.this, getPathForRow(0)));
		
		for (DifficultyNode diffNode : difficultyNodes)
		{
			setExpandedState(diffNode.path, true);
			for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(diffNode.difficulty))
			{
				if (st.children != null) setExpandedState(st.path, true);
			}
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	class SolvingTechniqueTreeModel implements TreeModel
	{
		EventListenerList listenerList = new EventListenerList();
		String root = "Solving Techniques";
		
		public Object getRoot()
		{
			return root;
		}
		
		public int getChildCount(Object parent)
		{
			if (parent.equals(root)) return difficultyNodes.size();
			else if (parent instanceof DifficultyNode) return techniqueNodesByDifficulty.get(((DifficultyNode) parent).difficulty).size();
			else return ((SolvingTechniqueNode) parent).children.size();
		}
		
		public Object getChild(Object parent, int index)
		{
			if (parent.equals(root)) return difficultyNodes.get(index);
			else if (parent instanceof DifficultyNode) return techniqueNodesByDifficulty.get(((DifficultyNode) parent).difficulty).get(index);
			else return ((SolvingTechniqueNode) parent).children.get(index);
		}
		
		public int getIndexOfChild(Object parent, Object child)
		{
			if (parent.equals(root))
			{
				for (int i = 0; i < difficultyNodes.size(); i++)
				{
					if (difficultyNodes.get(i) == child) return i;
				}
				return -1;
			}
			
			ArrayList<SolvingTechniqueNode> child_list;
			
			if (parent instanceof DifficultyNode)
			{
				child_list = techniqueNodesByDifficulty.get(((DifficultyNode) parent).difficulty);
				for (int i = 0; i < child_list.size(); i++)
				{
					if (child_list.get(i) == child) return i;
				}
			}
			else if (parent instanceof SolvingTechniqueNode)
			{
				for (int i = 0; i < ((SolvingTechniqueNode) parent).children.size(); i++)
				{
					if (((SolvingTechniqueNode) parent).children.get(i) == child) return i;
				}
			}
			return -1;
		}
		
		public boolean isLeaf(Object node)
		{
			if (node.equals(root) || node instanceof DifficultyNode) return false;
			else return ((SolvingTechniqueNode) node).children == null;
		}
		
		/**
		 * Not used because this tree does not enable editing
		 */
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
		
		/**
		 * Called after drag and drop
		 */
		protected void fireTreeStructureChanged(TreeModelEvent event)
		{
			for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class))
			{
				l.treeStructureChanged(event);
			}
		}

		/**
		 * Called after takeStep
		 */
		protected void fireTreeNodeChanged(TreeModelEvent event)
		{
			for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class))
			{
				l.treeNodesChanged(event);
			}
		}
	}
	
	/**
	 * @return whether the selected nodes all belong in the same hierarchy,
	 * e.g. EASY & HARD, hidden candidates & naked candidates, naked pairs & naked triples 
	 */
	boolean selectedNodesCanBeTransferred()
	{
		TreePath[] paths = getSelectionPaths();
		if (paths == null || paths.length == 0) return false;
		
		if (paths.length == 1) return true;
		JCheckBoxTreeNode firstNode = (JCheckBoxTreeNode) paths[0].getLastPathComponent();
		
		for (int i = 1; i < paths.length; i++)
		{
			JCheckBoxTreeNode node = (JCheckBoxTreeNode) paths[i].getLastPathComponent();
			if (!node.getClass().toString().equals(firstNode.getClass().toString())) return false;
			
			if (firstNode instanceof SolvingTechniqueNode)
			{
				SolvingTechniqueNode firstTechniqueNode = (SolvingTechniqueNode) firstNode;
				SolvingTechniqueNode techniqueNode = (SolvingTechniqueNode) node;
				if (firstTechniqueNode.technique.group == null ^ techniqueNode.technique.group == null) return false;
				if (firstTechniqueNode.technique.group != null)
				{
					if (!firstTechniqueNode.technique.group.equals(techniqueNode.technique.group)) return false;
				}
			}
		}
		return true;
	}
	
	
	@SuppressWarnings("CanBeFinal")
	class SolvingTechniqueTransferHandler extends TransferHandler
	{
		DataFlavor solvingTechniqueFlavor;
		
		@Override
		public Transferable createTransferable(JComponent c)
		{
			if (selectedNodesCanBeTransferred())
			{
				return new SolvingTechniqueTransferable(SolvingTechniqueTree.this, SolvingTechniqueTree.this.getSelectionPaths());
			}
			else return null;
		}
		
		@Override
		public boolean canImport(TransferHandler.TransferSupport info)
		{
			Transferable t = info.getTransferable();
			if (t.isDataFlavorSupported(solvingTechniqueFlavor))
			{
				try 
				{
					SolvingTechniqueData data = (SolvingTechniqueData) t.getTransferData(solvingTechniqueFlavor);
					if (data.owner != SolvingTechniqueTree.this) 
					{
						return false;
					}

					JTree.DropLocation dropLocation = ((JTree.DropLocation) info.getDropLocation());
					TreePath targetPath = dropLocation.getPath();
					int childIndex = dropLocation.getChildIndex();
					
					Object originalParentNode = data.selectedPaths[0].getParentPath().getLastPathComponent();

					if (targetPath.getLastPathComponent().equals(originalParentNode))
					{
						if (data.selectedPaths[0].getLastPathComponent() instanceof DifficultyNode)
						{
							return childIndex != 0; // cannot drop a group of technique before BASIC
						}
						return true;
					}
					else 
					{
						return false;
					}
				} 
				catch (UnsupportedFlavorException | IOException | ClassCastException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "canImport", "Error when getting transferData from " + t, e);
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		
		@Override
		public int getSourceActions(JComponent c) 
		{
			if (c == SolvingTechniqueTree.this) return TransferHandler.MOVE;
			else return TransferHandler.NONE;
		}
		
		// for drag and drop
		ArrayList<TreePath> dndExpandedNodes = new ArrayList<>();
		
		// if a SolvingTechniqueNode is expanded, but its DifficultyNode is collapsed, the DifficultyNode would be expanded after calls to setExpandedState(path, true)
		HashMap<DifficultyNode, Boolean> difficultyNodeExpansionStates = new HashMap<>();
		
		/**
		 * To have the same expanded state after dnd
		 * For all children of parent
		 */
		void saveExpandedStates(Object parent, TreePath parentPath)
		{
			difficultyNodeExpansionStates.clear();
			dndExpandedNodes.clear();
			Application.solverTreeLogger.log(Level.FINE, "save expanded states for " + parentPath);
			
			if (parent.equals(model.root))
			{
				for (DifficultyNode n : difficultyNodes)
				{
					for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(n.difficulty))
					{
						if (st.expanded) dndExpandedNodes.add(st.path);
					}
					difficultyNodeExpansionStates.put(n, n.expanded);
				}
			}
			else
			{
				JCheckBoxTreeNode parentNode = (JCheckBoxTreeNode) parent;
				if (parentNode.expanded) dndExpandedNodes.add(parentNode.path);
				
				if (parentNode instanceof DifficultyNode)
				{
					for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(parentNode.difficulty))
					{
						if (st.expanded) dndExpandedNodes.add(st.path);
					}
				}
			}
		}
		
		/**
		 * To have the same expanded state after drag and drop
		 */
		void setExpandedStates(TreePath parentPath)
		{
			for (TreePath path : dndExpandedNodes)
			{
				SolvingTechniqueTree.this.setExpandedState(path, true);
			}
			dndExpandedNodes.clear();
			
			if (!difficultyNodeExpansionStates.isEmpty())
			{
				for (DifficultyNode df : difficultyNodes)
				{
					setExpandedState(df.path, difficultyNodeExpansionStates.get(df));
				}
				difficultyNodeExpansionStates.clear();
			}
		}
		
		@Override
		public boolean importData(TransferHandler.TransferSupport info)
		{
			Transferable t = info.getTransferable();
			
			try 
			{
				SolvingTechniqueData data = (SolvingTechniqueData) t.getTransferData(solvingTechniqueFlavor);
				TreePath parentPath = data.selectedPaths[0].getParentPath();
	
				JTree.DropLocation dropLocation = ((JTree.DropLocation) info.getDropLocation());
				TreePath targetPath = dropLocation.getPath();
				int targetChildIndex = dropLocation.getChildIndex();

				// child index is -1 when dragging to the parent node (like dragging into a folder)
				if (targetChildIndex < 0)
				{
					targetChildIndex = model.getChildCount(parentPath.getLastPathComponent());
				}
				
				int childIndex;
				boolean increaseTargetIndex;
				
				Object[] selectedNodes = new Object[data.selectedPaths.length];
				for (int i = 0; i < selectedNodes.length; i++)
				{
					selectedNodes[i] = data.selectedPaths[i].getLastPathComponent();
				}
				
				// to use the order appearing on the screen
				// instead of using the order of selection when doing drag and drop
				Arrays.sort(selectedNodes);
				saveExpandedStates(parentPath.getLastPathComponent(), parentPath);
				
				// remove nodes
				for (int i = 0; i < selectedNodes.length; i++)
				{
					childIndex = model.getIndexOfChild(parentPath.getLastPathComponent(), selectedNodes[i]);
					increaseTargetIndex = childIndex >= targetChildIndex;
					
					if (selectedNodes[i] instanceof DifficultyNode)
					{
						DifficultyNode df = (DifficultyNode) selectedNodes[i];
						difficultyNodes.remove(df);

						if (increaseTargetIndex)
						{
							difficultyNodes.add(targetChildIndex, df);
							targetChildIndex++;
						}
						else 
						{
							difficultyNodes.add(targetChildIndex - 1, df);
						}
					}
					else 
					{
						SolvingTechniqueNode st = (SolvingTechniqueNode) selectedNodes[i];

						if (st.parentNode == null)
						{
							techniqueNodesByDifficulty.get(st.difficulty).remove(st);
							if (increaseTargetIndex)
							{
								techniqueNodesByDifficulty.get(st.difficulty).add(targetChildIndex, st);
								targetChildIndex++;
							}
							else 
							{
								techniqueNodesByDifficulty.get(st.difficulty).add(targetChildIndex - 1, st);
							}
						}
						else 
						{
							st.parentNode.children.remove(st);
							if (increaseTargetIndex)
							{
								st.parentNode.children.add(targetChildIndex, st);
								targetChildIndex++;
							}
							else 
							{
								st.parentNode.children.add(targetChildIndex - 1, st);
							}
						}
					}
				}
				
				// insert nodes
				model.fireTreeStructureChanged(new TreeModelEvent(SolvingTechniqueTree.this, parentPath));
				
				setExpandedStates(parentPath);

				// set selection back to the selected nodes
				// sometimes does not work for some reason
				SolvingTechniqueTree.this.setSelectionPaths(data.selectedPaths);
				return true;
			} 
			catch (UnsupportedFlavorException | IOException | ClassCastException e) 
			{
				Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "canImport", "Error when importing data from " + t, e);
				return false;
			}
		}
		
		@Override
		protected void exportDone(JComponent c, Transferable t, int act)
		{
		}
		
		public SolvingTechniqueTransferHandler()
		{
			super();
			try
			{
				solvingTechniqueFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + SolvingTechnique.class.getName());
			}
			catch (ClassNotFoundException e)
			{
				Application.exceptionLogger.logp(Level.SEVERE, getClass().toString(), "init", "Error when creating DataFlavor from String " + DataFlavor.javaJVMLocalObjectMimeType + ";class=" + SolvingTechnique.class.getName(), e);
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class SolvingTechniqueData
		{
			JTree owner;
			TreePath[] selectedPaths;
			
			public SolvingTechniqueData(JTree owner, TreePath[] selectedPaths)
			{
				this.owner = owner;
				this.selectedPaths = selectedPaths;
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class SolvingTechniqueTransferable implements Transferable
		{
			SolvingTechniqueData data;
			
			
			public SolvingTechniqueTransferable(JTree owner, TreePath[] selectedPaths)
			{
				data = new SolvingTechniqueData(owner, selectedPaths);
			}
			
			@Override
			public SolvingTechniqueData getTransferData(DataFlavor flavor)
			{
				return data;
			}
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return flavor.getRepresentationClass().equals(SolvingTechnique.class);
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[] { solvingTechniqueFlavor };
			}
		}
	}

	public Results takeStepResult; // result of the last "take step"

	// for top-down traversal of solving technique nodes
	boolean isLastTechnique;
	Difficulty currentDifficulty;

	public SolvingTechniqueNode currentSolvingTechnique;
	public SolvingTechniqueNode currentSolvingTechniqueMember;

	int currentSolvingTechniqueIndex; // within the difficulty
	int currentSolvingTechniqueMemberIndex; // within the technique group, will be -1 if a solvingTechnique does not belong to a group
	SolvingTechniqueNode lastFoundTechniqueNode;

	@Override
	public void setExpandedState(TreePath path, boolean expanded)
	{
		super.setExpandedState(path, expanded);
	}

	public String takeStep()
	{
		restartTraversal();
		if (takeStepResult != null && takeStepResult.found > 0) // do the remaining steps of the previous takeStep
		{
			assert lastFoundTechniqueNode != null;
			if (isSelectable(lastFoundTechniqueNode.path))
			{
				setSelectionPath(lastFoundTechniqueNode.path);
			}
			else
			{
				clearSelection();
				setExpandedState(getBasicNode().path, true);
			}

			board.boardOwner.solverPanel.clearSolverHighlights();
			board.setSudoku(takeStepResult.sudoku);
			board.boardOwner.historyTreePanel.historyTree.addNodeForEdit(new Edit("Performed \"" + takeStepResult.solvingTechniqueUsed.name + "\" " + (takeStepResult.found == 1 ? "once." : takeStepResult.found + " times."), EditType.TAKE_STEP, board));
			board.repaint();
			String returnMessage = "Performed \"" + takeStepResult.solvingTechniqueUsed.name + "\".";
			takeStepResult = null;
			return returnMessage;
		}

		do
		{
			SolvingTechniqueNode techniqueNode = getCurrentSolvingTechniqueNode();
			lastFoundTechniqueNode = techniqueNode;
			takeStepResult = techniqueNode.technique.takeStep.apply(board, techniqueNode);
			assert takeStepResult != null;

			techniqueNode.setSuffix(takeStepResult.found);

			if (techniqueNode.parentNode != null)
			{
				techniqueNode.parentNode.setSuffix(takeStepResult.found);
			}

			techniqueNode.difficultyNode.setSuffix(takeStepResult.found);

			if (takeStepResult.found > 0)
			{
				if (isSelectable(techniqueNode.path))
				{
					setSelectionPath(techniqueNode.path);
				}
				else
				{
					clearSelection();
				}
				board.repaint();
				return flattenMessages(takeStepResult.messages);
			}

			if (!traverseToNextSelected())
			{
				clearSelection();
				return "Cannot find a case to use selected solving techniques.";
			}
		}
		while (!isLastTechnique);

		clearSelection();
		return "Cannot find a case to use selected solving techniques.";
	}

	public static String flattenMessages(ArrayList<String> messages)
	{
		StringBuilder b = new StringBuilder();
		for (String  s : messages)
		{
			b.append(s);
			b.append(System.lineSeparator());
		}
		// remove the last line separator
		if (b.length() > 0) b.delete(b.length() - System.lineSeparator().length(), b.length());
		return b.toString();
	}
	
	void restartTraversal()
	{
		isLastTechnique = false;
		currentDifficulty = Difficulty.BASIC;
		currentSolvingTechniqueIndex = 0;
		currentSolvingTechnique = techniqueNodesByDifficulty.get(Difficulty.BASIC).get(0);

		// reset text
		for (int d = 0; d < difficultyNodes.size(); d++)
		{
			difficultyNodes.get(d).clearSuffix();
			SolvingTechniqueNode st;
			for (int i = 0; i < techniqueNodesByDifficulty.get(difficultyNodes.get(d).difficulty).size(); i++)
			{
				st = techniqueNodesByDifficulty.get(difficultyNodes.get(d).difficulty).get(i);
				st.clearSuffix();
				if (st.children != null)
				{
					for (SolvingTechniqueNode child : st.children) child.clearSuffix();
				}
			}
		}
		model.fireTreeNodeChanged(new TreeModelEvent(SolvingTechniqueTree.this, new TreePath(model.root)));
	}

	boolean shouldTraverseToNextTechnique()
	{
		if (isLastTechnique)
		{
			return false;
		}
		if (getCurrentSolvingTechniqueNode().suffix.endsWith("0"))
		{
			return true;
		}
		else
		{
			return !getCurrentSolvingTechniqueNode().isCheckBoxSelected() || (getCurrentSolvingTechniqueNode().difficulty != Difficulty.BASIC && !getCurrentSolvingTechniqueNode().checkBox.isEnabled());
		}
	}
	/**
	 * @return true if traversed to a node that is selected
	 */
	public boolean traverseToNextSelected()
	{
		while (shouldTraverseToNextTechnique())
		{
			traverseNext();
		}
		return getCurrentSolvingTechniqueNode().isCheckBoxSelected() && (getCurrentSolvingTechniqueNode().difficulty == Difficulty.BASIC || getCurrentSolvingTechniqueNode().checkBox.isEnabled());
	}
	
	public void traverseNext()
	{
		if (currentSolvingTechnique.children != null && currentSolvingTechnique.children.size() > currentSolvingTechniqueMemberIndex + 1) // traverse to next child technique
		{
			currentSolvingTechniqueMemberIndex++;
			currentSolvingTechniqueMember = currentSolvingTechnique.children.get(currentSolvingTechniqueMemberIndex);
		}
		else 
		{
			if (techniqueNodesByDifficulty.get(currentDifficulty).size() > currentSolvingTechniqueIndex + 1) // traverse to next solving technique in same difficuly
			{
				currentSolvingTechniqueIndex++;
				currentSolvingTechnique = techniqueNodesByDifficulty.get(currentDifficulty).get(currentSolvingTechniqueIndex);
				currentSolvingTechniqueMemberIndex = 0;
				
				if (currentSolvingTechnique.children != null)
				{
					currentSolvingTechniqueMember = currentSolvingTechnique.children.get(0);
				}
			}
			else // traverse to next difficulty
			{
				do
				{
					if (currentDifficulty.ordinal() + 1 == Difficulty.values().length)
					{
						isLastTechnique = true;
						return;
					}
					currentDifficulty = Difficulty.values()[currentDifficulty.ordinal() + 1];
				}
				while (techniqueNodesByDifficulty.get(currentDifficulty).size() == 0);

				currentSolvingTechniqueIndex = 0;
				currentSolvingTechnique = techniqueNodesByDifficulty.get(currentDifficulty).get(0);
				if (currentSolvingTechnique.children != null)
				{
					currentSolvingTechniqueMemberIndex = 0;
					currentSolvingTechniqueMember = currentSolvingTechnique.children.get(0);
				}
			}
		}
	}
	
	public SolvingTechniqueNode getCurrentSolvingTechniqueNode()
	{
		return currentSolvingTechnique.children != null ? currentSolvingTechniqueMember : currentSolvingTechnique;
	}

	@Override
	public void saveSettings(Preferences prefsNode)
	{
		prefsNode.putBoolean("root_expanded", isExpanded(new TreePath(model.root)));
		for (int d = 0; d < difficultyNodes.size(); d++)
		{
			prefsNode.putBoolean(difficultyNodes.get(d).toString() + "_expanded", difficultyNodes.get(d).expanded);
			
			//don't save selection state and order for BASIC
			if (d > 0)
			{
				prefsNode.putBoolean(difficultyNodes.get(d).toString() + "_selected", difficultyNodes.get(d).isCheckBoxSelected());
				prefsNode.putInt(difficultyNodes.get(d).toString() + "_index", d);
			}
			
			SolvingTechniqueNode st;
			for (int i = 0; i < techniqueNodesByDifficulty.get(difficultyNodes.get(d).difficulty).size(); i++)
			{
				st = techniqueNodesByDifficulty.get(difficultyNodes.get(d).difficulty).get(i);
				prefsNode.putInt(st.toString() + "_index", i);
				prefsNode.putBoolean(st + "_selected", st.isCheckBoxSelected());
				
				if (st.children != null)
				{
					prefsNode.putBoolean(st + "_expanded", st.expanded);
					
					for (int c = 0; c < st.children.size(); c++)
					{
						prefsNode.putBoolean(st.children.get(c).toString() + "_selected", st.children.get(c).isCheckBoxSelected());
						prefsNode.putInt(st.children.get(c).toString() + "_index", c);
					}
				}
			}
		}
	}

	@Override
	public void loadSettings(Preferences prefsNode)
	{
		for (DifficultyNode df : difficultyNodes)
		{
			df.expanded = prefsNode.getBoolean(df + "_expanded", true);
			df.treePosition = prefsNode.getInt(df + "_index", -1);
			df.setCheckBoxSelected(prefsNode.getBoolean(df + "_selected", true));
			
			for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(df.difficulty))
			{
				st.setCheckBoxSelected(prefsNode.getBoolean(st + "_selected", true));
				st.treePosition = prefsNode.getInt(st + "_index", -1);
				
				if (st.children != null)
				{
					st.expanded = prefsNode.getBoolean(st + "_expanded", true);
					
					for (SolvingTechniqueNode c : st.children)
					{
						c.setCheckBoxSelected(prefsNode.getBoolean(c + "_selected", true));
						c.treePosition = prefsNode.getInt(c + "_index", -1);
					}
				}
			}
		}
		
		JCheckBoxTreeNode.JCheckBoxTreeNodeComparator comparator = new JCheckBoxTreeNode.JCheckBoxTreeNodeComparator();
		difficultyNodes.sort(comparator);
		for (DifficultyNode n : difficultyNodes)
		{
			techniqueNodesByDifficulty.get(n.difficulty).sort(comparator);
			for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(n.difficulty))
			{
				if (st.children != null)
				{
					st.children.sort(comparator);
				}
			}
		}
		model.fireTreeStructureChanged(new TreeModelEvent(SolvingTechniqueTree.this, new Object[] { model.root }));
		
		for (DifficultyNode node : difficultyNodes)
		{
			boolean nodeExpanded = node.expanded;
			for (SolvingTechniqueNode st : techniqueNodesByDifficulty.get(node.difficulty))
			{
				if (st.expanded) setExpandedState(st.path, true);
			}
			setExpandedState(node.path, nodeExpanded);
		}
		
		setExpandedState(new TreePath(model.root), prefsNode.getBoolean("root_expanded", true));
	}

	public DifficultyNode getBasicNode()
	{
		return difficultyNodes.get(0);
	}

	public SolvingTechniqueNode getEliminateNode()
	{
		return techniqueNodesByDifficulty.get(Difficulty.BASIC).get(0);
	}
}	

	