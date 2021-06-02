package com.github.shayna003.sudoker.history;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.keys.KeyboardSettingsPanel;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.widgets.*;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * This class shows a visual representation of all the edits made to a board in a top-down hierarchy tree based on number of steps
 * Away from the board's creation.
 * The maximum number of nodes in this tree can be changed in PreferenceFrame, as well as node colors, width, height, row gap and gap between nodes.
 * The purpose of this class is that, after performing a "redo", an alternate history timeline will be created.
 * Although this class tries to be compact in terms of horizontal space, there are still cases that a left shift does not result in
 * The maximum possible shift to the left. (e.g. first child can shift 500, second child can shift 0, and parent can shift 200. First child will then shift 200, but that only shifts the parent by 100, wasting a room for shifting the first child 100 more)
 * 
 * Current Node Rendering Behaviour:
 * All Nodes have same width and height
 * Node to be deleted: painted with 50% alpha
 * Selected Node for view edit information is painted with darker color
 * Current Node depicting current board: is rendered with different color
 * All nodes have raised bevel border except for the selected one
 * @since 4-5-2021
 */
@SuppressWarnings("CanBeFinal")
public class HistoryTree extends JPanel
{
	HistoryTreePanel historyTreePanel;
	
	// each item of the outer ArrayList records the nodes in each row
	// an ArrayList item is to be removed if it is empty
	ArrayList<ArrayList<DefaultMutableTreeNode>> rowsOfNodes; 
	ArrayDeque<DefaultMutableTreeNode> nodesInInsertionOrder;
	
	// maxNodes has to be at least 2
	public static int maxNodes;
	// same width and height for each node
	public static int node_width;
	public static int node_height;
	public static int row_gap;  // gap between each row
	public static int node_gap; // gap between each node in the same row
	
	static 
	{
		maxNodes = (Integer) Application.historyTreeSettingPanel.maxNodesSpinner.getValue();
		node_width = (Integer) Application.historyTreeSettingPanel.nodeWidthSpinner.getValue();
		node_height = (Integer) Application.historyTreeSettingPanel.nodeHeightSpinner.getValue();
		row_gap = (Integer) Application.historyTreeSettingPanel.rowGapSpinner.getValue();
		node_gap = (Integer) Application.historyTreeSettingPanel.nodeGapSpinner.getValue();
	}
	
	int rightMostStampXPosition = 0;
	public DefaultMutableTreeNode currentNode; // reference to the node that refers to the last edit made to the board
	public DefaultMutableTreeNode selectedNode; // selected node in the history tree
	
	// used in determining how undo and redo behaves
	TreeNode[] path;
	
	public AbstractAction undo;
	public AbstractAction redo;
	AbstractAction deleteSelectedNode;
	AbstractAction deleteSelectedNodeAndItsChildren;
	AbstractAction importHistory;
	
	@Override
	public Dimension getPreferredSize()
	{
		// one node gap padding added to the left of left most node, to the right of right most node
		// one node gap padding added above top most node, below bottom most node
		return new Dimension(rightMostStampXPosition + node_width + node_gap, rowsOfNodes.size() * (node_height + node_gap) + node_gap);
	}
	
	void updateNodeCount()
	{
		historyTreePanel.treeTitledBorder.setTitle("History Tree | Node Count: " + nodesInInsertionOrder.size() + " | Row Count: " + rowsOfNodes.size());
		historyTreePanel.treePanel.repaint(0, 0, historyTreePanel.treePanel.getWidth(), historyTreePanel.treeTitledBorder.getBorderInsets(historyTreePanel.treePanel).top);
	}
	
	public HistoryStamp getCurrentStamp()
	{
		return (HistoryStamp) currentNode.getUserObject();
	}
	
	/**
	 * Called by self and BoardComparator when chosen board changes
	 * @return true if the "link" was made
	 */ 
	public boolean setLinkedBoardComparatorBoard(BoardComparator.BoardViewPane boardViewPane)
	{
		if (boardViewPane.linkHistoryTreeSelectedNode && boardViewPane.chosenTab != null && boardViewPane.chosenTab.board == historyTreePanel.treePanelOwner.board)
		{
			if (selectedNode != null)
			{
				HistoryStamp selectedStamp = (HistoryStamp) selectedNode.getUserObject();
				boardViewPane.setChosenBoard(selectedStamp.data);
			}
			else 
			{
				boardViewPane.setChosenBoard((Board) null);
			}
			return true;
		}
		else 
		{
			return false;
		}
	}
	
	/**
	 * Called after selected node changes
	 */
	public void setLinkedBoardComparatorBoards()
	{
		if (Application.boardComparatorFrame != null)
		{
			for (int t = 0; t < Application.boardComparatorFrame.tabbedPane.getTabCount(); t++)
			{
				BoardComparator boardComaprator = (BoardComparator) Application.boardComparatorFrame.tabbedPane.getComponentAt(t);
				
				setLinkedBoardComparatorBoard(boardComaprator.board1);
				setLinkedBoardComparatorBoard(boardComaprator.board2);
			}
		}
	}
	
	public HistoryTree(HistoryTreePanel historyTreePanel, Edit creationEvent)
	{
		super(null); // don't use a LayoutManager
		setOpaque(true);
		
		this.historyTreePanel = historyTreePanel;
		historyTreePanel.historyTree = HistoryTree.this;
		
		nodesInInsertionOrder = new ArrayDeque<>(maxNodes);
		rowsOfNodes = new ArrayList<>();

		undo = new AbstractAction("Undo")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				importHistoryStampFromNode((DefaultMutableTreeNode) currentNode.getParent(), false);
			}
		};
		undo.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U); 
		// use registerMenuShortcut because this is the same action as application frame's Edit -> Undo
		Application.keyboardSettingsPanel.registerMenuShortcut("undo", KeyboardSettingsPanel.getMenuItemString("Edit", "Undo"), true, KeyEvent.VK_Z, InputEvent.META_DOWN_MASK, undo, null, 0);
		
		redo = new AbstractAction("Redo")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				importHistoryStampFromNode((DefaultMutableTreeNode) (path[getIndexOfCurrentNodeInPath() + 1]), false);
			}
		};
		redo.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		// use registerMenuShortcut because this is the same action as application frame's Edit -> Redo
		Application.keyboardSettingsPanel.registerMenuShortcut("redo", KeyboardSettingsPanel.getMenuItemString("Edit", "Redo"), true, KeyEvent.VK_Z, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, redo, null, 0);
		
		deleteSelectedNode = new AbstractAction("Delete Selected Node")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				deleteNode(selectedNode, true, true);
				updateNodeCount();
			}
		};
		deleteSelectedNode.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteTreeNode", KeyboardSettingsPanel.getMenuItemString("History Tree", "Delete Selected Node"), true, KeyEvent.VK_BACK_SPACE, 0, deleteSelectedNode, null, 0);
		
		deleteSelectedNodeAndItsChildren = new AbstractAction("Delete Selected Node and All of Its Children")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				deleteAllChildNodes(selectedNode);
				updateNodeCount();
			}
		};
		deleteSelectedNodeAndItsChildren.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		Application.keyboardSettingsPanel.registerOtherShortcut("deleteAllChildTreeNode", KeyboardSettingsPanel.getMenuItemString("History Tree", "Delete Selected Node and All of Its Children"), true, KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), deleteSelectedNodeAndItsChildren, null, 0);
		
		importHistory = new AbstractAction("Import History From Selected Node")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				importHistoryStampFromNode(selectedNode, true);
			}
		};
		importHistory.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		Application.keyboardSettingsPanel.registerOtherShortcut("importHistory", KeyboardSettingsPanel.getMenuItemString("History Tree", "Import History From Selected Node"), true, KeyEvent.VK_ENTER, 0, importHistory, null, 0);
		
		updateVisuals();
		addNodeForEdit(creationEvent);
	}
	
	/**
	 * Deletes node and all its child nodes from the tree.
	 * This function is called on the selected node, and is
	 * never called with current node being the selected node
	 * Or current node be a descendant of selected node.
	 */
	void deleteAllChildNodes(DefaultMutableTreeNode node)
	{
		while (node.getChildCount() > 0)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getFirstChild();
			deleteAllChildNodes(child);
		}
		deleteNode(node, true, false);
	}
	
	void setSelectedNode(DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode previousSelected;
		if (node != selectedNode)
		{
			previousSelected = selectedNode;
			
			selectedNode = node;
			setLinkedBoardComparatorBoards();
			
			if (node != null)
			{
				setSelectedBorder();
			}
			
			if (previousSelected != null) 
			{
				HistoryStamp previousSelectedStamp = (HistoryStamp) previousSelected.getUserObject();
				previousSelectedStamp.setBorder(nodeBorder);
			}
			
			setDeleteNodeEnabled();
			boolean canImportHistory = canImportHistory();
			importHistory.setEnabled(canImportHistory);
			
			historyTreePanel.updateEditInformation();
		}
	}
	
	void setDeleteNodeEnabled()
	{
		boolean nodeCanBeDeleted = canNodeBeDeleted(selectedNode);
		deleteSelectedNode.setEnabled(nodeCanBeDeleted);

		deleteSelectedNodeAndItsChildren.setEnabled(selectedNode != null && selectedNode != currentNode && !currentNode.isNodeAncestor(selectedNode));
	}
	
	void setCurrentNode(DefaultMutableTreeNode node, boolean reconfigurePath)
	{
		DefaultMutableTreeNode previousCurrent = null;
		if (node != currentNode)
		{
			previousCurrent = currentNode;
		}
		currentNode = node;
		
		if (previousCurrent != null) ((HistoryStamp) previousCurrent.getUserObject()).repaint();
		(getCurrentStamp()).repaint();
		
		if (reconfigurePath) reconfigurePath();
		setUndoRedoEnabled();
	}
	
	boolean canUndo()
	{
		return currentNode.getParent() != null;
	}
	
	boolean canRedo()
	{
		return path != null && path.length > getIndexOfCurrentNodeInPath() + 1;
	}
	
	int getIndexOfCurrentNodeInPath()
	{
		if (path == null) return -1;
		for (int i = 0; i < path.length; i++)
		{
			if (path[i] == currentNode) return i;
		}
		return -1;
	}
	
	DefaultMutableTreeNode getRedoNode()
	{
		int index = getIndexOfCurrentNodeInPath();
		if (index > path.length - 2) return null;
		return (DefaultMutableTreeNode) path[index + 1];
	}
	
	String getEditTypeStringForNode(TreeNode node)
	{
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode) node;
		HistoryStamp stamp = (HistoryStamp) tn.getUserObject();
		return stamp.edit.editType.shortName;
	}
	
	void setUndoRedoEnabled()
	{
		boolean canUndo = canUndo();
		boolean canRedo = canRedo();
		
		undo.setEnabled(canUndo);
		if (canUndo) undo.putValue(AbstractAction.NAME, "Undo \"" + getEditTypeStringForNode(currentNode) + " \"");
		else undo.putValue(AbstractAction.NAME, "Undo");
		
		if (canRedo) redo.putValue(AbstractAction.NAME, "Redo \"" + getEditTypeStringForNode(getRedoNode()) + " \"");
		else redo.putValue(AbstractAction.NAME, "Redo");
		
		undo.setEnabled(canUndo);
		redo.setEnabled(canRedo);
	}
	
	/**
	 * Sets the undo-redo path to be the path of the furthest child of current node
	 */
	void reconfigurePath()
	{
		DefaultMutableTreeNode tmp = currentNode;
		
		// redo traces back to the furthest child with < 2 children
		while (tmp.getChildCount() > 0 && tmp.getChildCount() < 2)
		{
			tmp = (DefaultMutableTreeNode) tmp.getChildAt(0);
		}
		path = tmp.getPath();
	}
	
	/**
	 * Sets the {@code currentNode} to @param node
	 * Undo and redo does not trigger reconfigurePath,
	 * Only manual clicking on a node in the tree and setting it to the current node does.
	 * Also the path is reconfigured after adding a new node to the history tree.
	 * This function can be triggered by undo, redo, and manual clicking on a node and setting it to the currentNode
	 */
	void importHistoryStampFromNode(DefaultMutableTreeNode node, boolean reconfigurePath)
	{
		assert node != null;
		assert node.getUserObject() != null;
		setCurrentNode(node, reconfigurePath);
		setSelectedNode(node);
		
		HistoryStamp stamp = (HistoryStamp) node.getUserObject();
		
		setUndoRedoEnabled();
		setDeleteNodeEnabled();
		stamp.edit.board.setSudokuAndNotesAndLocks(stamp.data, true);
	}
	
	public void setMaxNodes(int newValue)
	{
		if (maxNodes != newValue)
		{
			maxNodes = newValue;
			markNodesToBeDeleted();
		}
	}
	
	public void setRowGap(int newValue)
	{
		if (newValue != row_gap)
		{
			row_gap = newValue;
			
			for (int r = 0; r < rowsOfNodes.size(); r++)
			{
				int y_position = getYPositionForRow(r);
				for (int c = 0; c < rowsOfNodes.get(r).size(); c++)
				{
					DefaultMutableTreeNode node = rowsOfNodes.get(r).get(c);
					setStampYLocation((HistoryStamp) node.getUserObject(), y_position);
				}
			}
		}
	}
	
	public void setNodeHeight(int newValue)
	{
		if (newValue != node_height)
		{
			node_height = newValue;
			setAllStampLocations();

			Iterator<DefaultMutableTreeNode> iter = nodesInInsertionOrder.iterator();
			while (iter.hasNext())
			{
				HistoryStamp stamp = (HistoryStamp) iter.next().getUserObject();
				stamp.configureDimensions();
				stamp.repaint();
			}
		}
	}
	
	public void setNodeWidth(int newValue)
	{
		if (newValue != node_width)
		{
			node_width = newValue;
			setAllStampLocations();
			
			Iterator<DefaultMutableTreeNode> iter = nodesInInsertionOrder.iterator();
			while (iter.hasNext())
			{
				HistoryStamp stamp = (HistoryStamp) iter.next().getUserObject();
				stamp.configureDimensions();
				stamp.repaint();
			}
		}
	}
	
	public void setNodeGap(int newValue)
	{
		if (newValue != node_gap)
		{
			node_gap = newValue;
			setAllStampLocations();
		}
	}
	
	void setAllStampLocations()
	{
		for (ArrayList<DefaultMutableTreeNode> rowOfNodes : rowsOfNodes)
		{
			for (int c = 0; c < rowOfNodes.size(); c++)
			{
				DefaultMutableTreeNode node = rowOfNodes.get(c);
				setStampBounds((HistoryStamp) node.getUserObject());
			}
		}
	}
	
	/**
	 * Called by PreferenceFrame when user changes the allowed maximum number of nodes in this tree
	 * Or when a node has been added or removed from the tree
	 * Marks nodes that will be deleted as a new node will be created, and unmark the nodes tha will no longer be deleted
	 */
	public void markNodesToBeDeleted()
	{
		int tmp = nodesInInsertionOrder.size();
		Iterator<DefaultMutableTreeNode> iter = nodesInInsertionOrder.iterator();
		
		while (tmp >= maxNodes)
		{
			HistoryStamp stamp = (HistoryStamp) iter.next().getUserObject();
			stamp.setToBeDeleted(true);
			tmp--;
		}
		
		while (iter.hasNext())
		{
			HistoryStamp stamp = (HistoryStamp) iter.next().getUserObject();
			if (stamp.toBeDeleted)
			{
				stamp.setToBeDeleted(false);
			}
			else 
			{
				break;
			}
		}
	}
	
	
	boolean canNodeBeDeleted(DefaultMutableTreeNode node)
	{
		return (node != null && (node != currentNode || node.getParent() != null));
	}
	
	boolean canImportHistory()
	{
		return selectedNode != null && selectedNode != currentNode;
	}
	
	/**
	 * Apparently an error occurs if you call getLastChild when node doesn't have children
	 */
	static DefaultMutableTreeNode getLastChild(TreeNode node)
	{
		if (node == null) return null;
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode) node;
		if (tn.getChildCount() > 0)
		{
			return (DefaultMutableTreeNode) tn.getLastChild();
		}
		else 
		{
			return null;
		}
	}
	
	/**
	 * Apparently an error occurs if you call getFirstChild when node doesn't have children
	 */
	static DefaultMutableTreeNode getFirstChild(TreeNode node)
	{
		if (node == null) return null;
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode) node;
		if (tn.getChildCount() > 0)
		{
			return (DefaultMutableTreeNode) tn.getFirstChild();
		}
		else 
		{
			return null;
		}
	}
	
	/**
	 * If canNodeBeDeleted(node) == true
	 * @param removeFromNodesInInsertionOrder is false when called in addNodeForEdit, true otherwise
	 * @param shouldUndoIfNodeIsCurrentNode is false when called in addNodeForEdit, true otherwise
	 */
	void deleteNode(DefaultMutableTreeNode node, boolean removeFromNodesInInsertionOrder, boolean shouldUndoIfNodeIsCurrentNode)
	{
		HistoryStamp removedStamp = (HistoryStamp) node.getUserObject();
		if (selectedNode == node) setSelectedNode(null);
		if (currentNode == node)
		{
			if (shouldUndoIfNodeIsCurrentNode) undo();
			else 
			{
				(getCurrentStamp()).toBeDeleted = true;
			}
		}
		
		if (removeFromNodesInInsertionOrder)
		{
			nodesInInsertionOrder.remove(node);
		}
		
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
		node.removeFromParent();
		
		DefaultMutableTreeNode firstChild = getFirstChild(node);
		node.removeAllChildren();
		
		// remove node from rowsOfNodes
		ArrayList<DefaultMutableTreeNode> nodesInARow = rowsOfNodes.get(removedStamp.row);
		boolean isLoneNodeInRow = false;

		removedStamp = (HistoryStamp) nodesInARow.remove(removedStamp.column).getUserObject();
		assert removedStamp == node.getUserObject() : removedStamp;
		
		// reconfigure column indexes of nodes
		for (int c = removedStamp.column; c < rowsOfNodes.get(removedStamp.row).size(); c++)
		{
			((HistoryStamp) (rowsOfNodes.get(removedStamp.row).get(c).getUserObject())).column = c;
		}
		
		// remove row and reconfigure row indexes and y positions of nodes
		if (nodesInARow.size() == 0)
		{
			isLoneNodeInRow = true;
			rowsOfNodes.remove(removedStamp.row);
			int y_position;
			for (int r = removedStamp.row; r < rowsOfNodes.size(); r++)
			{
				y_position = getYPositionForRow(r); // y_position is fixed based on row number
				for (DefaultMutableTreeNode n : rowsOfNodes.get(r))
				{
					HistoryStamp stamp = ((HistoryStamp) n.getUserObject());
					stamp.row--;
					setStampYLocation(stamp, y_position);
				}
			}
		}
		
		// if deleted node was in undo-redo path
		for (TreeNode n : path)
		{
			if (n == node)
			{
				path = currentNode.getPath();
				break;
			}
		}
		
		markNodesToBeDeleted();
		
		deleteStamp(removedStamp, isLoneNodeInRow, parentNode, firstChild);
		setUndoRedoEnabled();
	}
	
	/**
	 * Y position is fixed based on row number
	 */
	static int getYPositionForRow(int r)
	{
		return row_gap + r * (node_height + row_gap);
	}
	
	/**
	 * Remove stamp from panel, and reposition some other stamps
	 * And compute preferredLayoutSize again?
	 */
	void deleteStamp(HistoryStamp stamp, boolean wasLastNodeInRow, DefaultMutableTreeNode parentNode, DefaultMutableTreeNode firstChildOfRemovedStamp)
	{
		boolean stampShiftedLeft = false;
		
		// shift first child node left, proposing largest possible shift
		if (firstChildOfRemovedStamp != null)
		{
			HistoryStamp firstChild = (HistoryStamp) firstChildOfRemovedStamp.getUserObject();

			// don't check or shift parentNode
			StampXLocation shiftedAmount = shiftStampLeft(firstChild, firstChild, firstChild.stampXLocation.deriveFromSubtracting(0, 1), true, false, true, true, false, true, 0);
			if (shiftedAmount.getValue() > 0) stampShiftedLeft = true;
		}
		
		// then shift parent node of deleted node to the center of its immediate children
		if (parentNode != null)
		{
			// first shift parent node left, then if parent node is still not in correct position, shift its first child node right to make up the offset,
			// so that child nodes are positioned correctly relative to their parent
			HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();

			if (parentNode.getChildCount() > 0)
			{
				StampXLocation supposedPosition = getParentStampPosition(parentNode);
				StampXLocation difference = StampXLocation.subtract(supposedPosition, parentStamp.stampXLocation);
				if (difference.getValue() < 0) // need to shift parentStamp left, don't check or shift child nodes
				{
					difference.flip();
					StampXLocation shiftedAmount = shiftStampLeft(parentStamp, parentStamp, difference, true, true, false, true, true, false, 0);
					if (shiftedAmount.getValue() > 0) stampShiftedLeft = true;

					if (shiftedAmount.getValue() < difference.getValue()) // need to shift child nodes to the right to center parent, don't shift parent
					{
						StampXLocation offset = StampXLocation.subtract(difference, shiftedAmount);
						HistoryStamp firstChild = (HistoryStamp) getFirstChild(parentNode).getUserObject();
						assert StampXLocation.add(firstChild.stampXLocation, offset).getValue() == difference.getValue() : "difference: " + difference + ", sum: " + StampXLocation.add(firstChild.stampXLocation, offset);
						shiftStampLocation(firstChild, difference, true, false, true, 0);
					}
				}
				else if (difference.getValue() > 0) // need to shift parentStamp right, don't shift child nodes
				{
					shiftStampLocation(parentStamp, supposedPosition, true, true, false, 0);
				}
			}
			else // parent now has no children, can propose a left shift of the largest amount
			{
				// don't need to check or shift child nodes
				StampXLocation shiftedAmount = shiftStampLeft(parentStamp, parentStamp, parentStamp.stampXLocation.deriveFromSubtracting(0, 1), true, true, false, true, true, false, 0);
				if (shiftedAmount.getValue() > 0) stampShiftedLeft = true;
			}
		}
		
		// shift right neighbor node left, proposing largest possible shift
		if (!wasLastNodeInRow && rowsOfNodes.get(stamp.row).size() > stamp.column)
		{
			DefaultMutableTreeNode nodeToRight = rowsOfNodes.get(stamp.row).get(stamp.column);
			HistoryStamp stampToRight = (HistoryStamp) nodeToRight.getUserObject();
			StampXLocation shiftedAmount = shiftStampLeft(stampToRight, stampToRight, stampToRight.stampXLocation.deriveFromSubtracting(0, 1), true, true, true, true, true, true, 0);
			if (shiftedAmount.getValue() > 0) stampShiftedLeft = true;
		}
		
		if (stampShiftedLeft) reconfigureRightMostStampXPosition();
		
		HistoryTree.this.remove(stamp);
		HistoryTree.this.revalidate();
		HistoryTree.this.repaint();
	}
	
	/**
	 * Add stamp to panel, and position it and related stamps correctly
	 * And compute preferredLayoutSize again?
	 */
	void addStamp(HistoryStamp stamp)
	{
		DefaultMutableTreeNode node = stamp.node;
		StampXLocation x_position;

		if (node.getParent() == null) // if parent was a node that got deleted due to reaching max node count limit
		{
			// wrong, need to consider moving nodes to the right as well
			x_position = stamp.column == 0 ? new StampXLocation(0, 1) : ((HistoryStamp) rowsOfNodes.get(stamp.row).get(stamp.column - 1).getUserObject()).stampXLocation.deriveFromAdding(1, 1);
			
			// reposition nodes to the right if gap between new node and node to the right is smaller than node_gap
			if (rowsOfNodes.get(stamp.row).size() > stamp.column + 1)
			{
				HistoryStamp rightSideNeighbor = (HistoryStamp) rowsOfNodes.get(stamp.row).get(stamp.column + 1).getUserObject();
				if (rightSideNeighbor.getX() < x_position.deriveFromAdding(1, 1).getValue())
				{
					shiftStampLocation(rightSideNeighbor, x_position.deriveFromAdding(1, 1), true, true, true, 0);
				}
			}
			stamp.stampXLocation = x_position.clone();
			setStampBounds(stamp);
		}
		else if (node.getParent().getChildCount() > 1) // node already added to parent's list of child nodes by time this function is called
		{
			// add new child node after the last child node of parent, reposition parent to its children's center, then shift parent left because there is now more potentially more space to shift to the left
			
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
			HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();
			
			x_position = ((HistoryStamp) ((DefaultMutableTreeNode) parentNode.getChildAt(parentNode.getChildCount() - 2)).getUserObject()).stampXLocation.deriveFromAdding(1, 1);
			
			stamp.stampXLocation = x_position.clone();
			setStampBounds(stamp);
			setStampXLocation(parentStamp, parentStamp.stampXLocation.deriveFromAdding(0.5, 0.5));

			StampXLocation distanceShiftedLeft = shiftStampLeft(parentStamp, parentStamp, parentStamp.stampXLocation.deriveFromSubtracting(0, 1), true, true, true, true, true, true, 0);
			if (distanceShiftedLeft.getValue() > 0)
			{
				reconfigureRightMostStampXPosition();
			}
			
			// reposition nodes to the right if gap between new node and node to the right is smaller than node_gap
			if (rowsOfNodes.get(stamp.row).size() > stamp.column + 1)
			{
				HistoryStamp rightSideNeighbor = (HistoryStamp) rowsOfNodes.get(stamp.row).get(stamp.column + 1).getUserObject();
				if (rightSideNeighbor.getX() < stamp.stampXLocation.deriveFromAdding(1, 1).getValue())
				{
					shiftStampLocation(rightSideNeighbor, stamp.stampXLocation.deriveFromAdding(1, 1), true, true, true, 0);
				}
			}
			
			// if parent stamp has a right neighbor that is too close to it
			if (rowsOfNodes.get(parentStamp.row).size() > parentStamp.column + 1)
			{
				HistoryStamp parentRightNeighbor = (HistoryStamp) rowsOfNodes.get(parentStamp.row).get(parentStamp.column + 1).getUserObject();
				if (parentRightNeighbor.getX() < parentStamp.stampXLocation.deriveFromAdding(1, 1).getValue())
				{
					shiftStampLocation(parentRightNeighbor, parentStamp.stampXLocation.deriveFromAdding(1, 1), true, true, true, 0);
				}
			}
		}
		else // the first child of a parent node
		{
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
			x_position = ((HistoryStamp) parentNode.getUserObject()).stampXLocation;
			
			if (stamp.column > 0)
			{
				HistoryStamp leftSideNeighbor = (HistoryStamp) rowsOfNodes.get(stamp.row).get(stamp.column - 1).getUserObject();
				if (leftSideNeighbor.getX() > x_position.deriveFromSubtracting(1, 1).getValue()) // adjust self location and shift parent to right
				{
					x_position = leftSideNeighbor.stampXLocation.deriveFromAdding(1, 1);
					shiftStampLocation((HistoryStamp) parentNode.getUserObject(), x_position, true, true, false, 0);
				}
			}
			
			// reposition nodes to the right if gap between new node and node to the right is smaller than node_gap
			if (rowsOfNodes.get(stamp.row).size() > stamp.column + 1)
			{
				HistoryStamp rightSideNeighbor = (HistoryStamp) rowsOfNodes.get(stamp.row).get(stamp.column + 1).getUserObject();
				if (rightSideNeighbor.getX() < x_position.deriveFromAdding(1, 1).getValue())
				{
					shiftStampLocation(rightSideNeighbor, x_position.deriveFromAdding(1, 1), true, true, true, 0);
				}
			}
			stamp.stampXLocation = x_position.clone();
			setStampBounds(stamp);
		}
		
		if (stamp.getX() > rightMostStampXPosition)
		{
			rightMostStampXPosition = stamp.getX();
		}
		HistoryTree.this.add(stamp);
		HistoryTree.this.revalidate();
		HistoryTree.this.repaint();
	}
	
	/**
	 * Recomputes the rightMost stamp x location by checking the rightmost stamp of every row
	 * This function is called if a stamp with rightMostStampXPosition got shifted left
	 */
	void reconfigureRightMostStampXPosition()
	{
		rightMostStampXPosition = node_gap;
		for (int r = 0; r < rowsOfNodes.size(); r++)
		{
			int x = ((HistoryStamp) rowsOfNodes.get(r).get(rowsOfNodes.get(r).size() - 1).getUserObject()).getX();
			if (x > rightMostStampXPosition)
			{
				rightMostStampXPosition = x;
			}
		}
	}

	/**
	 * This recursive function is first called with ancestor_target_stamp equal to stamp_being_evaluated
	 * The actual left shift will only occur if ancestor_target_stamp == stamp_being_evaluated
	 * This function calls itself with ancestor_target_stamp != stamp_being_evaluated to check for the ancestor_target_stamp's children's and parents' capacity to shift left
	 * @param distance the distance to move left, should be positive
	 * @return the corrected amount of difference of the shift, if == 0, then the left shift should be aborted, except for shifting the ancestor_target_stamp's parent stamp 
	 * All child nodes of ancestor_target_stamp will be shifted to the left the same amount
	 */
	StampXLocation shiftStampLeft(HistoryStamp ancestor_target_stamp, HistoryStamp stamp_being_evaluated, StampXLocation distance, boolean checkLeftNeighbor, boolean checkParentNode, boolean checkChildNodes, boolean shiftRightNodes, boolean shiftParentNode, boolean shiftChildNodes, int layer) // layer is for testing
	{
		if (checkLeftNeighbor && stamp_being_evaluated.column > 0 && distance.getValue() > 0)
		{
			DefaultMutableTreeNode leftNeighborNode = rowsOfNodes.get(stamp_being_evaluated.row).get(stamp_being_evaluated.column - 1);
			HistoryStamp leftNeighborStamp = (HistoryStamp) leftNeighborNode.getUserObject();

			// if the left neighbor shares the same ancestor of ancestor_target_stamp as stamp_being_evaluated, it will also be shifted the same amount left
			if (!leftNeighborNode.isNodeAncestor(ancestor_target_stamp.node))
			{
				if (StampXLocation.subtract(stamp_being_evaluated.stampXLocation, distance).getValue() < leftNeighborStamp.stampXLocation.deriveFromAdding(1, 1).getValue())
				{
					distance = StampXLocation.subtract(distance, StampXLocation.subtract(leftNeighborStamp.stampXLocation.deriveFromAdding(1, 1), StampXLocation.subtract(stamp_being_evaluated.stampXLocation, distance)));
					if (distance.getValue() <= 0 && (!shiftParentNode || ancestor_target_stamp != stamp_being_evaluated)) return distance;
				}
			}
		}
		else 
		{
			// if x position of stamp_being_evaluated will be smaller than node_gap
			if (StampXLocation.subtract(stamp_being_evaluated.stampXLocation, distance).getValue() < node_gap)
			{
				distance = stamp_being_evaluated.stampXLocation.deriveFromSubtracting(0, 1);
				if (distance.getValue() <= 0 && (!shiftParentNode || ancestor_target_stamp != stamp_being_evaluated)) return distance;
			}
		}
		
		// readjust shift distance based on child nodes
		if (checkChildNodes && stamp_being_evaluated.node.getChildCount() > 0 && distance.getValue() > 0)
		{
			Enumeration<TreeNode> e = stamp_being_evaluated.node.children();
			int index = 0;
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
				HistoryStamp childStamp = (HistoryStamp) node.getUserObject();
				distance = shiftStampLeft(ancestor_target_stamp, childStamp, distance, index == 0, false, true, false, false, false, layer + 1);

				if (distance.getValue() <= 0)
				{
					if (shiftParentNode && ancestor_target_stamp == stamp_being_evaluated)
					{
						break;
					}
					else 
					{
						return distance;
					}
				}
				index++;
			}
		}
		
		// readjust shift distance based on parent
		if (checkParentNode && stamp_being_evaluated.node.getParent() != null && distance.getValue() > 0)
		{
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) stamp_being_evaluated.node.getParent();
			HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();
			int childIndex = parentNode.getIndex(stamp_being_evaluated.node);
			
			// assume that the parent stamp will make the biggest shift possible to reposition itself in the center of its child stamps
			// if stamp_being_evaluated is the first child of parent, the largest possible shift for parent is distance,
			// otherwise the largest possible shift for parent is distance / 2
			if (childIndex == 0)
			{
				StampXLocation parentShiftDistance = shiftStampLeft(ancestor_target_stamp, parentStamp, distance, true, true, false, false, false, false, layer + 1);
				if (stamp_being_evaluated.getX() > parentStamp.getX())
				{
					StampXLocation tmp = StampXLocation.subtract(StampXLocation.add(parentShiftDistance, stamp_being_evaluated.stampXLocation), parentStamp.stampXLocation);
					distance = tmp.getValue() > distance.getValue() ? distance : tmp;
				}
				else 
				{
					distance = parentShiftDistance;
				}
			}
			else 
			{
				StampXLocation parentShiftDistance = shiftStampLeft(ancestor_target_stamp, parentStamp, distance.deriveFromDividing(2), true, true, false, false, false, false, layer + 1);
				if (stamp_being_evaluated.getX() > parentStamp.getX())
				{
					StampXLocation tmp = parentShiftDistance.deriveFromMultiplying(2);
					tmp = StampXLocation.subtract(StampXLocation.add(tmp, stamp_being_evaluated.stampXLocation), parentStamp.stampXLocation);
					distance = tmp.getValue() > distance.getValue() ? distance : tmp;
				}
				else 
				{
					StampXLocation tmp = parentShiftDistance.deriveFromMultiplying(2);
					distance = tmp.getValue() > distance.getValue() ? distance : tmp;
				}
			}
			
			if (distance.getValue() <= 0  && (!shiftParentNode || ancestor_target_stamp != stamp_being_evaluated)) return distance;

		}

		// if distance still > 0 after recursion, do the shift and all related shifts
		if (ancestor_target_stamp == stamp_being_evaluated)
		{
			if (distance.getValue() > 0)
			{
				if (shiftChildNodes)
				{
					// shift this stamp and all its children the same distance to the left
					shiftStampLeft(ancestor_target_stamp, distance);
				}
				else 
				{
					setStampXLocation(ancestor_target_stamp, StampXLocation.subtract(ancestor_target_stamp.stampXLocation, distance));
				}
			}
			
			// shift parent node if is the last child, or shift has distance of 0 (which means no consequent shifts of stamps to its right)
			// this limits the occurrence of repetitive shifts
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) ancestor_target_stamp.node.getParent();
			if (shiftParentNode && parentNode != null && (getLastChild(parentNode) == stamp_being_evaluated.node || distance.getValue() <= 0))//parentNode.getLastChild() == ancestor_target_stamp.node)
			{
				HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();
				StampXLocation new_parent_x = getParentStampPosition(parentNode);
				if (new_parent_x.getValue() < parentStamp.getX())
				{
					// only shift right nodes and shift parent node
					shiftStampLeft(parentStamp, parentStamp, StampXLocation.subtract(parentStamp.stampXLocation, new_parent_x), false, false, false, true, true, false, layer + 1);
				}
				else if (new_parent_x.getValue() > parentStamp.getX())
				{
					shiftStampLocation(parentStamp, new_parent_x, true, true, false, layer);
				}
			}
			
			if (distance.getValue() > 0)
			{
				// shift stamps located to the right left
				for (int c = ancestor_target_stamp.column + 1; c < rowsOfNodes.get(ancestor_target_stamp.row).size(); c++)
				{
					DefaultMutableTreeNode nodeToRight = rowsOfNodes.get(ancestor_target_stamp.row).get(c);
					HistoryStamp stampToRight = (HistoryStamp) nodeToRight.getUserObject();
					parentNode = (DefaultMutableTreeNode) nodeToRight.getParent();

					// boolean checkLeftNeighbor (shift amount for a node to the right can be smaller than distance)
					distance = shiftStampLeft(stampToRight, stampToRight, distance, true, getFirstChild(parentNode) == nodeToRight, true, false, true, true, layer + 1);

					if (distance.getValue() <= 0)
					{
						break;
					}
				}
			}
		}
		return distance;
	}
	
	/**
	 * Shifts target_stamp and all of its child stamps to the left by distance_to_shift_left
	 */
	void shiftStampLeft(HistoryStamp target_stamp, StampXLocation distance_to_shift_left)
	{
		if (distance_to_shift_left.getValue() > 0)
		{
			setStampXLocation(target_stamp, StampXLocation.subtract(target_stamp.stampXLocation, distance_to_shift_left));
			
			Enumeration<TreeNode> e = target_stamp.node.children();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode c = (DefaultMutableTreeNode) e.nextElement();
				shiftStampLeft((HistoryStamp) c.getUserObject(), distance_to_shift_left);
			}
		}
	}
	
	StampXLocation getParentStampPosition(DefaultMutableTreeNode parentNode)
	{
		HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();
		if (parentNode.getChildCount() == 0)  
		{
			return parentStamp.stampXLocation.clone();
		}
		else if (parentNode.getChildCount() == 1)
		{
			return ((HistoryStamp) getFirstChild(parentNode).getUserObject()).stampXLocation.clone();
		}
		else 
		{
			StampXLocation x_pos_of_first_child = ((HistoryStamp) getFirstChild(parentNode).getUserObject()).stampXLocation;
			StampXLocation x_pos_of_last_child = ((HistoryStamp) getLastChild(parentNode).getUserObject()).stampXLocation;
			
			double nodeWidths = x_pos_of_first_child.nodeWidths;
			double nodeGaps = x_pos_of_first_child.nodeGaps;
			nodeWidths += (x_pos_of_last_child.nodeWidths - x_pos_of_first_child.nodeWidths) / 2;
			nodeGaps += (x_pos_of_last_child.nodeGaps - x_pos_of_first_child.nodeGaps) / 2;
			
			return new StampXLocation(nodeWidths, nodeGaps);
		}
	}
	
	/**
	 * called by shiftStampLocation only
	 */
	void shiftParentNodePosition(DefaultMutableTreeNode parentNode,/* boolean shiftRight,*/ int layer)
	{
		HistoryStamp parentStamp = (HistoryStamp) parentNode.getUserObject();
		StampXLocation new_parent_x = getParentStampPosition(parentNode);
		
		if (new_parent_x.getValue() > parentStamp.getX())
		{
			shiftStampLocation((HistoryStamp) parentNode.getUserObject(), new_parent_x, true, true, false, layer + 1);
		}
	}
	
	void setRightMostXStampPosition(StampXLocation location)
	{
		int x = location.getValue();
		if (x > rightMostStampXPosition)
		{
			rightMostStampXPosition = x;
		}
	}
	
	/**
	 * This recursive function is called to reposition the nodes related to the newly added/removed node
	 * When a node is added, this function might be called to shift @param target_stamp and all related stamps to the right
	 * When a node is deleted, this function might be called to shift @param target_stamp and all related stamps to the left
	 * The function will never be called by the node added/deleted as the @param target_stamp
	 * Either way, this function would only affect the @param target_stamp, stamps to the right, the parent stamp, and child stamps
	 * @param new_x the new x position of stamp, could be smaller or larger than the original x position
	 */
	void shiftStampLocation(HistoryStamp target_stamp, StampXLocation new_x, boolean shiftNodesToRight, boolean shiftParentNode, boolean shiftChildNodes, int layer)
	{
		StampXLocation difference = StampXLocation.subtract(new_x, target_stamp.stampXLocation);

		if (difference.getValue() > 0)
		{
			// set_bounds has already been called when first added
			setStampXLocation(target_stamp, new_x);
			setRightMostXStampPosition(new_x);
			
			if (shiftChildNodes) // shifting a parent shifts the child nodes in the same distance in the same direction, in order to center the parent among its immediate child nodes
			{
				Enumeration<TreeNode> e = target_stamp.node.children();
				while (e.hasMoreElements())
				{
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
					HistoryStamp st = (HistoryStamp) n.getUserObject();

					// only shift nodes to the right of child node row if shifting last child
					shiftStampLocation(st, StampXLocation.add(st.stampXLocation, difference), !e.hasMoreElements(), false, true, layer + 1);
				}
			}
			
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) target_stamp.node.getParent();
			if (shiftParentNode && parentNode != null && getLastChild(parentNode) == target_stamp.node)
			{
				shiftParentNodePosition(parentNode, layer);
			}
			
			if (shiftNodesToRight)
			{
				// only shift parent node after it changes
				// adding or removing a stamp only directly affects the stamps to the right of it in the same row
				for (int c = target_stamp.column + 1; c < rowsOfNodes.get(target_stamp.row).size(); c++)
				{
					DefaultMutableTreeNode node = rowsOfNodes.get(target_stamp.row).get(c);
					HistoryStamp stamp = (HistoryStamp) node.getUserObject();

					HistoryStamp stampToLeft = (HistoryStamp) rowsOfNodes.get(target_stamp.row).get(c - 1).getUserObject();
					
					// as a matter of fact, difference is always > 0
					if ((stampToLeft.getX() > stamp.stampXLocation.deriveFromSubtracting(1, 1).getValue())) // shift right just enough
					{
						shiftStampLocation(stamp, stampToLeft.stampXLocation.deriveFromAdding(1, 1), false, true, true, layer + 1);
					}
					else 
					{
						parentNode = (DefaultMutableTreeNode) node.getParent();
						if (parentNode != null && getFirstChild(parentNode) != node)
						{
							shiftParentNodePosition(parentNode, layer + 1);
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Creates a new child node of the currently selected node
	 */
	public void addNodeForEdit(Edit newEdit)
	{
		HistoryStamp stamp = new HistoryStamp(newEdit);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(stamp);
		stamp.node = newNode;
		
		if (currentNode != null) 
		{
			HistoryStamp currentStamp = (getCurrentStamp());
			stamp.row = currentStamp.row + 1;
			stamp.stepNumber = currentStamp.stepNumber + 1;
		}
		
		if (stamp.row == rowsOfNodes.size())
		{
			ArrayList<DefaultMutableTreeNode> newRow = new ArrayList<>();
			newRow.add(newNode);
			rowsOfNodes.add(stamp.row, newRow);
		}
		else if  (((HistoryStamp) rowsOfNodes.get(stamp.row).get(0).getUserObject()).stepNumber > stamp.stepNumber)
		{
			ArrayList<DefaultMutableTreeNode> newRow = new ArrayList<>();
			newRow.add(newNode);
			rowsOfNodes.add(stamp.row, newRow);
			for (int r = stamp.row + 1; r < rowsOfNodes.size(); r++)
			{
				int y_position = getYPositionForRow(r);
				for (DefaultMutableTreeNode n : rowsOfNodes.get(r))
				{
					HistoryStamp hs = (HistoryStamp) n.getUserObject();
					hs.row++;
					setStampYLocation(hs, y_position);
				}
			}
		}
		else 
		{
			HistoryStamp currentStamp = getCurrentStamp();
			stamp.column = 0;
			for (int c = 0; c < currentStamp.column; c++)
			{
				stamp.column += rowsOfNodes.get(currentStamp.row).get(c).getChildCount();
			}
			stamp.column += currentNode.getChildCount();
			
			for (int c = 0; c < rowsOfNodes.get(stamp.row).size() && (c < stamp.column || ((HistoryStamp) rowsOfNodes.get(stamp.row).get(c).getUserObject()).getX() < currentStamp.getX()); c++)
			{
				if (rowsOfNodes.get(stamp.row).get(c).getParent() == null)
				{
					stamp.column++;
				}
			}
			rowsOfNodes.get(stamp.row).add(stamp.column, newNode);
			
			for (int c = stamp.column + 1; c < rowsOfNodes.get(stamp.row).size(); c++)
			{
				((HistoryStamp) rowsOfNodes.get(stamp.row).get(c).getUserObject()).column++;
			}
		}
		
		if (currentNode != null && !(getCurrentStamp()).toBeDeleted) currentNode.add(newNode);
		
		nodesInInsertionOrder.addLast(newNode);
		addStamp(stamp);
		
		while (nodesInInsertionOrder.size() > maxNodes)
		{
			deleteNode(nodesInInsertionOrder.removeFirst(), false, false);
		}
		
		markNodesToBeDeleted();
		
		path = newNode.getPath();
		setCurrentNode(newNode, true);
		setSelectedNode(newNode);
		
		updateNodeCount();
	}
	
	public void undo()
	{
		undo.actionPerformed(null);
	}
	
	public void redo()
	{
		redo.actionPerformed(null);
	}
	
	Color bgColor;
	public void updateVisuals()
	{
		bgColor = Application.historyTreeSettingPanel.historyTreeBackgroundColor.color;
		repaint();
	}
	
	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
		nodeBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		selectedNodeBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		
		if (nodesInInsertionOrder != null)
		{
			for (DefaultMutableTreeNode n : nodesInInsertionOrder)
			{
				HistoryStamp s = (HistoryStamp) n.getUserObject();
				s.setBorder(nodeBorder);
			}
			if (selectedNode != null) setSelectedBorder();
		}
	}
	
	void setSelectedBorder()
	{
		HistoryStamp selectedStamp = (HistoryStamp) selectedNode.getUserObject();
		selectedStamp.setBorder(selectedNodeBorder);
	}
	
	Border nodeBorder;
	Border selectedNodeBorder;
	
	MouseAdapter stampClickHandler = new MouseAdapter()
	{
		@Override
		public void mousePressed(MouseEvent event)
		{
			setSelectedNode(((HistoryStamp) event.getSource()).node);
		}
	};

	void setStampYLocation(HistoryStamp stamp, int y_position)
	{
		stamp.setLocation(stamp.getX(), y_position);
	}
	
	void setStampXLocation(HistoryStamp stamp, StampXLocation x)
	{
		stamp.stampXLocation.nodeGaps = x.nodeGaps;
		stamp.stampXLocation.nodeWidths = x.nodeWidths;
		stamp.setLocation(x.getValue(), stamp.getY());
	}
	
	void setStampBounds(HistoryStamp stamp)
	{
		stamp.setBounds(stamp.stampXLocation.getValue(), getYPositionForRow(stamp.row), node_width, node_height);
	}
	
	/**
	 * This class allows storing a stamp's x location in terms of number of node widths and node gaps
	 * To support node width and node gap preferences
	 */
	static class StampXLocation implements Cloneable
	{
		double nodeWidths;
		double nodeGaps;

		/**
		 * For Testing
		 */
		@Override
		public String toString()
		{
			return "nodeWidths: " + nodeWidths + ", nodeGaps: " + nodeGaps + ", value: " + getValue();
		}
		
		public static StampXLocation subtract(StampXLocation a, StampXLocation b)
		{
			return new StampXLocation(a.nodeWidths - b.nodeWidths, a.nodeGaps - b.nodeGaps);
		}
		
		public static StampXLocation add(StampXLocation a, StampXLocation b)
		{
			return new StampXLocation(a.nodeWidths + b.nodeWidths, a.nodeGaps + b.nodeGaps);
		}
		
		public StampXLocation deriveFromSubtracting(double nodeWidths, double nodeGaps)
		{
			return new StampXLocation(this.nodeWidths - nodeWidths, this.nodeGaps - nodeGaps);
		}
		
		public StampXLocation deriveFromAdding(double nodeWidths, double nodeGaps)
		{
			return new StampXLocation(this.nodeWidths + nodeWidths, this.nodeGaps + nodeGaps);
		}
		
		public StampXLocation deriveFromMultiplying(double factor)
		{
			return new StampXLocation(this.nodeWidths * factor, this.nodeGaps * factor);
		}
		
		public StampXLocation deriveFromDividing(double factor)
		{
			return new StampXLocation(this.nodeWidths / factor, this.nodeGaps / factor);
		}
		
		public void flip()
		{
			nodeWidths = -nodeWidths;
			nodeGaps = -nodeGaps;
		}
		
		public StampXLocation(double nodeWidths, double nodeGaps)
		{
			this.nodeWidths = nodeWidths;
			this.nodeGaps = nodeGaps;
		}
		
		public int getValue()
		{
			return (int) (nodeWidths * node_width + nodeGaps * node_gap);
		}
		
		@Override
		public StampXLocation clone()
		{
			return new StampXLocation(nodeWidths, nodeGaps);
		}
	}
	
	int stampCreationOrderID = -1; // unique for each stamp, starts at 0
	
	/**
	* A node in a HistoryTree, works similarly as a DefaultMutableTreeNode
	* @since 4-5-2021
	*/
	@SuppressWarnings("CanBeFinal")
	public class HistoryStamp extends JComponent
	{
		StampXLocation stampXLocation;
		
		public int creationOrder; // 1 - infinity
		int column; // column index of its row
		int row; // row number in the history tree, does not necessarily reflect the step number
		public int stepNumber; // creation of board is the 0th step, each preceding step has previous step's stepNumber + 1.
		boolean toBeDeleted; // whether or not the node will be deleted after a new node is added in the history tree, this will affect the transparency of the stamp
		
		Edit edit;
		BoardData data;
		
		Insets border_insets;
		
		DefaultMutableTreeNode node; // the DefaultMutableTreeNode whose userObject is this HistoryTreeNode
		
		int rect_width;
		int rect_height;

		void setToBeDeleted(boolean b)
		{
			toBeDeleted = b;
			setToolTipText(b ? "This node will be deleted after the next edit is made." : "");
			repaint();
		}

		void configureDimensions()
		{
			rect_width = HistoryTree.node_width - border_insets.left - border_insets.right;
			rect_height = HistoryTree.node_height - border_insets.top - border_insets.bottom;
		}
		
		public HistoryStamp(Edit edit)
		{
			this.creationOrder = ++stampCreationOrderID;
			this.edit = edit;
			this.data = new BoardData(edit.board);
			
			setBorder(nodeBorder);
			border_insets = nodeBorder.getBorderInsets(HistoryStamp.this);
			
			configureDimensions();
			
			addMouseListener(stampClickHandler);
			stampXLocation = new StampXLocation(0, 0);
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(HistoryTree.node_width, HistoryTree.node_height);
		}

		/**
		 * For Testing
		 */
		@Override
		public String toString()
		{
			return "HistoryStamp[r:" + row + ", c:" + column + ", s:" + stepNumber + ", CREATION ORDER: " + creationOrder + ", x=" + getX() + ", toBeDeleted:" + toBeDeleted + ",edit:" + edit.toString() + "]";
		}
		
		@Override
		public void setBorder(Border border)
		{
			super.setBorder(border);
			border_insets = getBorder().getBorderInsets(HistoryStamp.this);
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			if (toBeDeleted)
			{
				g2.setComposite(SwingUtil.makeComposite(0.5f));
			}
			
			if (currentNode == this.node && selectedNode == this.node)
			{
				g2.setColor(Application.historyTreeSettingPanel.currentNodeColor.color.darker());
			}
			else if (currentNode == this.node)
			{
				g2.setColor(Application.historyTreeSettingPanel.currentNodeColor.color);
			}
			else if (selectedNode == this.node)
			{
				g2.setColor(Application.historyTreeSettingPanel.editColors.get(edit.editType).color.darker());
			}
			else
			{
				g2.setColor(Application.historyTreeSettingPanel.editColors.get(edit.editType).color);
			}
			
			g2.fillRect(border_insets.left - 1, border_insets.top - 1, rect_width + 2, rect_height + 2);
			
			// draw short name of edit
			String message = edit.editType.shortName;
			FontRenderContext context = g2.getFontRenderContext();
			Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
			double ascent = -bounds.getY();
			
			g2.setColor(Color.BLACK);
			g2.drawString(message, (float) (border_insets.left + (rect_width - bounds.getWidth()) / 2), (float) (border_insets.top + ascent + (rect_height - bounds.getHeight()) / 2));
		}
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g); // paint background on white "canvas", this way you can paint background with transparency
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(bgColor);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		g2.setColor(Color.BLACK);
		// draw lines connecting nodes to show tree hierarchy, no line connection means no direct adjacent relationship in time
		if (rowsOfNodes.size() > 1)
		{
			for (int r = 0; r < rowsOfNodes.size(); r++)
			{
				for (int n = 0; n < rowsOfNodes.get(r).size(); n++)
				{
					if (rowsOfNodes.get(r).get(n).getChildCount() > 0)
					{
						HistoryStamp parentStamp = (HistoryStamp) rowsOfNodes.get(r).get(n).getUserObject();
						Enumeration<TreeNode> e = rowsOfNodes.get(r).get(n).children();

						while (e.hasMoreElements())
						{
							DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
							HistoryStamp childStamp = (HistoryStamp) node.getUserObject();

							// show parent-child relationships
							g2.drawLine(parentStamp.getX() + node_width / 2, parentStamp.getY() + node_height, childStamp.getX() + node_width / 2, childStamp.getY());
						}
					}
				}
			}
		}
	}
}