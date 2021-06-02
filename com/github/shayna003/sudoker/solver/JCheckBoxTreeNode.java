package com.github.shayna003.sudoker.solver;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.GeneralSettingsPanel;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.*;
import java.awt.event.*;
import java.util.*;

/**
 * @since 2-20-2021
 * Later make this entire file into inner classes of the SolvingTechniqueTree
 * This class implements Comparable for sorting the selected tree paths of SolvingTechniqueTree
 * Instead of using the order of selection when doing drag and drop
 */
@SuppressWarnings("CanBeFinal")
public abstract class JCheckBoxTreeNode extends JPanel implements Comparable<JCheckBoxTreeNode>
{
	/**
	 * Used in loading nodes from preference file
	 */
	public static class JCheckBoxTreeNodeComparator implements Comparator<JCheckBoxTreeNode>
	{
		public int compare(JCheckBoxTreeNode a, JCheckBoxTreeNode b) {
			return a.treePosition - b.treePosition;
		}
	}

	TreePath path;
	boolean expanded;

	SolvingTechniqueTree owner;
	Difficulty difficulty;
	JCheckBox checkBox;
	JPopupMenu popupMenu;
	String name;
	String suffix = "";

	// used in sorting the tree nodes with a comparator
	int treePosition;

	public void clearSuffix()
	{
		suffix = "";
		owner.model.fireTreeNodeChanged(new TreeModelEvent(owner, path));
	}

	public void setSuffix(int newSuffixInt)
	{
		suffix = ": " + newSuffixInt;
		owner.model.fireTreeNodeChanged(new TreeModelEvent(owner, path));
	}

	/**
	 * Assuming both nodes have the same parent
	 */
	public int compareTo(JCheckBoxTreeNode other)
	{
		return owner.model.getIndexOfChild(path.getParentPath().getLastPathComponent(), this) - owner.model.getIndexOfChild(other.path.getParentPath().getLastPathComponent(), other);
	}

	{
		setOpaque(false);
		popupMenu = new JPopupMenu();
		GeneralSettingsPanel.registerComponentAndSetFontSize(popupMenu);
	}

	public void updateUI() {
		if (checkBox != null) checkBox.updateUI();
	}

	public void checkBoxClicked()
	{
		if (checkBox.isEnabled())
		{
			setCheckBoxSelected(!checkBox.isSelected());
		}
	}

	/**
	 * Changes the background color to serve as a visual clue that
	 * This node has been selected/deselected
	 */
	public void setSelected(boolean selected)
	{
		setOpaque(selected);
	}

	public boolean isCheckBoxSelected()
	{
		return checkBox.isSelected();
	}

	public void setCheckBoxSelected(boolean selected)
	{
		checkBox.setSelected(selected);
	}

	@Override
	public String toString()
	{
		if (name.endsWith("Eliminate") && suffix.length() == 0)
		{
			return name + "    "; // to not have the tree readjust the JScrollPane every time eliminate finds something
		}
		else return name + suffix;
	}
}

@SuppressWarnings("CanBeFinal")
class SolvingTechniqueNode extends JCheckBoxTreeNode
{
	public SolvingTechnique technique;
	ArrayList<SolvingTechniqueNode> children;
	SolvingTechniqueNode parentNode;
	DifficultyNode difficultyNode;
	
	public Action quickFindAndTakeStep;

	@Override
	public void setCheckBoxSelected(boolean selected)
	{
		super.setCheckBoxSelected(selected);

		if (children != null)
		{
			for (SolvingTechniqueNode c : children)
			{
				c.setCheckBoxEnabled(checkBox.isEnabled() && checkBox.isSelected());
			}
		}
	}

	public void setCheckBoxEnabled(boolean b)
	{
		checkBox.setEnabled(b);

		if (children != null)
		{
			for (SolvingTechniqueNode c : children)
			{
				c.setCheckBoxEnabled(checkBox.isEnabled() && checkBox.isSelected());
			}
		}
	}

	public SolvingTechniqueNode(SolvingTechnique technique, SolvingTechniqueTree owner, DifficultyNode difficultyNode, boolean selected, boolean expanded)
	{
		this(technique, owner, difficultyNode);
		setCheckBoxSelected(selected);
		this.expanded = expanded;
	}
	
	public SolvingTechniqueNode(SolvingTechnique technique, SolvingTechniqueTree owner, DifficultyNode difficultyNode)
	{
		this(technique, owner);
		
		this.difficultyNode = difficultyNode;
		if (technique.difficulty != Difficulty.BASIC) checkBox.setEnabled(difficultyNode.isCheckBoxSelected());
		
		path = new TreePath(new Object[] { owner.model.root, difficultyNode, this });
		
		int childrenCount = technique.isTechniqueGroup ? technique.members.length : 0;
		if (childrenCount > 0)
		{
			children = new ArrayList<>(childrenCount);
			for (int i = 0; i < childrenCount; i++)
			{
				children.add(new SolvingTechniqueNode(technique.members[i], owner, this));
			}
		}
	}
	
	private SolvingTechniqueNode(SolvingTechnique technique, SolvingTechniqueTree owner, SolvingTechniqueNode parentNode)
	{
		this(technique, owner);
		this.parentNode = parentNode;
		this.difficultyNode = parentNode.difficultyNode;
		if (technique.difficulty != Difficulty.BASIC) checkBox.setEnabled(parentNode.checkBox.isEnabled());
		path = new TreePath(new Object[] { owner.model.root, difficultyNode, parentNode, this });
	}
	
	private SolvingTechniqueNode(SolvingTechnique technique, SolvingTechniqueTree owner)
	{
		this.owner = owner;
		this.technique = technique;
		this.name = technique.toString();
		this.difficulty = technique.difficulty;
		checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		
		if (technique.difficulty == Difficulty.BASIC)
		{
			checkBox.setSelected(true);
			checkBox.setEnabled(false);
		}
		
		if (!owner.isModelTree)
		{
			quickFindAndTakeStep = new AbstractAction("Quick find and take step for \"" + this + "\"")
			{
				public void actionPerformed(ActionEvent event)
				{
					owner.board.boardOwner.solverPanel.clearSolverHighlightsCompletely();

					// for the solving techniques to work properly
					if (difficulty != Difficulty.BASIC)
					{
						DifficultyNode basicNode = owner.getBasicNode();
						SolvingTechniqueNode eliminateNode = owner.getEliminateNode();
						Results eliminateResults = SolvingTechnique.ELIMINATE.takeStep.apply(owner.board, eliminateNode);
						if (eliminateResults.found > 0)
						{
							owner.takeStepResult = eliminateResults;
							owner.lastFoundTechniqueNode = eliminateNode;
							owner.clearSelection();

							eliminateNode.setSuffix(eliminateResults.found);
							basicNode.setSuffix(eliminateResults.found);
							owner.setExpandedState(basicNode.path, true);

							owner.board.repaint();
							owner.board.boardOwner.solverPanel.appendMessage(owner.flattenMessages(eliminateResults.messages), true);
							return;
						}
					}

					Results results = technique.takeStep.apply(owner.board, SolvingTechniqueNode.this);
					if (children != null)
					{
						if (results != null)
						{
							owner.takeStepResult = results;
							setSuffix(results.found);

							if (owner.isSelectable(owner.lastFoundTechniqueNode.path))
							{
								owner.setSelectionPath(owner.lastFoundTechniqueNode.path);
							}
							else
							{
								owner.clearSelection();
								owner.setExpandedState(owner.getBasicNode().path, true);
							}
							owner.board.repaint();
							owner.board.boardOwner.solverPanel.appendMessage(owner.flattenMessages(results.messages), true);
						}
						else
						{
							setSuffix(0);
							owner.board.boardOwner.solverPanel.appendMessage("Cannot find a case to use \"" + technique + "\".", true);
							owner.clearSelection();
						}
					}
					else
					{
						assert results != null;

						setSuffix(results.found);

						if (results.found > 0)
						{
							owner.takeStepResult = results;
							owner.lastFoundTechniqueNode = SolvingTechniqueNode.this;

							if (results.found > 0)
							{
								if (owner.isSelectable(path))
								{
									owner.setSelectionPath(path);
								}
								else
								{
									owner.clearSelection();
									owner.setExpandedState(owner.getBasicNode().path, true);
								}
								owner.board.repaint();
								owner.board.boardOwner.solverPanel.appendMessage(owner.flattenMessages(results.messages), true);
							}
						}
						else
						{
							owner.board.boardOwner.solverPanel.appendMessage("Cannot find a case to use \"" + technique + "\".", true);
							owner.clearSelection();
						}
					}
				}
			};
			popupMenu.add(quickFindAndTakeStep);
		}
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		if (children != null)
		{
			for (SolvingTechniqueNode st : children)
			{
				st.updateUI();
			}
		}
	}

	public Results returnResultsOfAChildNode()
	{
		Results results;
		for (SolvingTechniqueNode child : children)
		{
			if (child.isCheckBoxSelected())
			{
				results = child.technique.takeStep.apply(owner.board, SolvingTechniqueNode.this);
				child.setSuffix(results.found);

				if (results.found > 0)
				{
					owner.lastFoundTechniqueNode = child;
					return results;
				}
			}
		}
		return null;
	}
}

class DifficultyNode extends JCheckBoxTreeNode
{
	AbstractAction quickFindAndTakeStep;
	boolean initializing = true;
	/*
	 * e.g.
	 * BASIC 0
	 * Hard 1
	 * Easy 2
	 * Moderate 3
	 */

	@Override
	public void setCheckBoxSelected(boolean selected)
	{
		super.setCheckBoxSelected(selected);

		if (difficulty != Difficulty.BASIC)
		{
			for (SolvingTechniqueNode n : owner.techniqueNodesByDifficulty.get(difficulty))
			{
				n.setCheckBoxEnabled(checkBox.isSelected());
			}
		}
	}

	public DifficultyNode(Difficulty difficulty, SolvingTechniqueTree owner, boolean selected, boolean expanded)
	{
		this(difficulty, owner);
		initializing = true;
		checkBox.setSelected(selected);
		this.expanded = expanded;
		initializing = false;
	}

	public DifficultyNode(Difficulty difficulty, SolvingTechniqueTree owner)
	{
		this.owner = owner;
		this.difficulty = difficulty;
		this.name = difficulty.toString();
		
		this.path = new TreePath( new Object[] { owner.model.root, this });

		checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		if (difficulty == Difficulty.BASIC)
		{
			checkBox.setSelected(true);
			checkBox.setEnabled(false);
		}

		if (!owner.isModelTree)
		{
			quickFindAndTakeStep = new AbstractAction("Quick find and take step for \"" + this + "\"")
			{
				public void actionPerformed(ActionEvent event)
				{
					owner.board.boardOwner.solverPanel.clearSolverHighlightsCompletely();

					// for the solving techniques to work properly
					if (difficulty != Difficulty.BASIC)
					{
						DifficultyNode basicNode = owner.getBasicNode();
						SolvingTechniqueNode eliminateNode = owner.getEliminateNode();
						Results eliminateResults = SolvingTechnique.ELIMINATE.takeStep.apply(owner.board, eliminateNode);
						if (eliminateResults.found > 0)
						{
							owner.takeStepResult = eliminateResults;
							owner.lastFoundTechniqueNode = eliminateNode;
							owner.clearSelection();

							eliminateNode.setSuffix(eliminateResults.found);
							basicNode.setSuffix(eliminateResults.found);
							owner.setExpandedState(basicNode.path, true);

							owner.board.repaint();
							owner.board.boardOwner.solverPanel.appendMessage(owner.flattenMessages(eliminateResults.messages), true);
							return;
						}
					}

					Results results = returnResultsOfAChildNode();
					if (results != null)
					{
						owner.takeStepResult = results;

						if (owner.isSelectable(owner.lastFoundTechniqueNode.path))
						{
							owner.setSelectionPath(owner.lastFoundTechniqueNode.path);
						}
						else
						{
							owner.clearSelection();
							owner.setExpandedState(owner.getBasicNode().path, true);
						}
						owner.board.repaint();
						owner.board.boardOwner.solverPanel.appendMessage(owner.flattenMessages(results.messages), true);
					}
					else
					{
						owner.board.boardOwner.solverPanel.appendMessage("Cannot find a case to use \"" + difficulty + "\".", true);
						owner.clearSelection();
					}
				}
			};
		}
		popupMenu.add(quickFindAndTakeStep);
		initializing = false;
	}

	Results returnResultsOfAChildNode()
	{
		Results results;
		for (SolvingTechniqueNode node : owner.techniqueNodesByDifficulty.get(difficulty))
		{
			if (node.isCheckBoxSelected())
			{
				results = node.technique.takeStep.apply(owner.board, node);
				if (results == null || results.found == 0)
				{
					node.setSuffix(0);
				}
				else
				{
					node.setSuffix(results.found);
					setSuffix(results.found);

					if (node.children == null) owner.lastFoundTechniqueNode = node;
					return results;
				}
			}
		}
		setSuffix(0);
		return null;
	}
}