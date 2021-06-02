package com.github.shayna003.sudoker.history;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.history.HistoryTree.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * This class contains a history tree, to display information about an edit.
 * An instance of this class binds with a Board of a SudokuTab within an ApplicationFrame. 
 * Instances of the class get added into a JTabbedPane representing Boards of a single ApplicationFrame.
 * @since 4-17-2021
 */
@SuppressWarnings("CanBeFinal")
public class HistoryTreePanel extends JPanel
{
	SudokuTab treePanelOwner;
	
	public HistoryTree historyTree;
	JScrollPane treePane;
	JPanel treePanel;
	TitledBorder treeTitledBorder;
	
	// contains all the labels
	JTable editInfoTable;
	
	// text remains the same
	JPanel editInfoPanel;
	JLabel stepNumberLabel;
	JLabel creationOrderLabel;
	JLabel editTypeLabel;
	JLabel editDescriptionLabel;
	JLabel timeOfEditLabel;
	
	// text changes
	JLabel stepNumber;
	JLabel creationOrder;
	JLabel editType;
	JLabel editDescription;
	JLabel timeOfEdit;
	
	public HistoryTreePanel(Edit creationEvent)
	{
		treePanelOwner = creationEvent.board.boardOwner;
		
		treePanel = new JPanel(new BorderLayout());
		treeTitledBorder = BorderFactory.createTitledBorder("History Tree | Node Count: | Row Count: ");
		treePanel.setBorder(treeTitledBorder);
		
		editInfoPanel = new JPanel (new BorderLayout());
		stepNumberLabel = new JLabel("Step number: ");
		creationOrderLabel = new JLabel("Creation Order: ");
		editTypeLabel = new JLabel("Edit Type: ");
		editDescriptionLabel = new JLabel("Edit Description: ");
		timeOfEditLabel = new JLabel("Time of Edit: ");
		
		stepNumber = new JLabel();
		creationOrder = new JLabel();
		editType = new JLabel();
		editDescription = new JLabel();
		timeOfEdit = new JLabel();
		
		Object[][] data = new Object[][] 
		{
			{ stepNumberLabel, stepNumber },
			{ creationOrderLabel, creationOrder},
			{ editTypeLabel, editType },
			{ editDescriptionLabel, editDescription},
			{ timeOfEditLabel, timeOfEdit}
		};
		
		editInfoTable = new JTable(new AbstractTableModel()
		{
			@Override
			public int getRowCount() { return 5; }
			
			@Override
			public int getColumnCount() { return 2; }
			
			@Override
			public Object getValueAt(int r, int c)
			{
				return ((JLabel) data[r][c]).getText();
			}
			
			@Override
			public String getColumnName(int c)
			{
				return c == 0 ? "Name" : "Value";
			}
		});
		editInfoTable.setEnabled(false); // to make it unable to be selected
		
		JScrollPane scrollPane = new JScrollPane(editInfoTable);
		scrollPane.setPreferredSize(new Dimension(editInfoTable.getPreferredSize().width + scrollPane.getMinimumSize().width, editInfoTable.getPreferredSize().height + scrollPane.getMinimumSize().height));//+ split_pane.getDividerSize()
		editInfoPanel.add(scrollPane, BorderLayout.CENTER);
		
		Rectangle2D bounds = editInfoTable.getFont().getStringBounds("Edit Description: ", getFontMetrics(editInfoTable.getFont()).getFontRenderContext());
		TableColumn c = editInfoTable.getColumn("Name");
		int preferredWidthForNameColumn = (int) (bounds.getWidth() * 1.1 + 5);
		c.setPreferredWidth(preferredWidthForNameColumn);
		c.setMaxWidth(preferredWidthForNameColumn);
		c.setMinWidth(preferredWidthForNameColumn);
		editInfoPanel.setBorder(BorderFactory.createTitledBorder("Edit Information"));
		
		// HistoryTree's constructor sets the historyTree field
		new HistoryTree(HistoryTreePanel.this, creationEvent);
		
		treePane = new JScrollPane(historyTree);
		treePane.setPreferredSize(new Dimension(800, 500));
		treePanel.add(treePane, BorderLayout.CENTER);
		
		setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, editInfoPanel);
		add(splitPane, BorderLayout.CENTER);
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (editInfoTable != null)
		{
			editInfoTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
			editInfoTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
		}
	}
	
	void updateEditInformation()
	{
		if (historyTree.selectedNode == null)
		{
			stepNumber.setText("");
			creationOrder.setText("");
			editType.setText("");
			editDescription.setText("");
			timeOfEdit.setText("");
		}
		else 
		{
			HistoryStamp selectedStamp = (HistoryStamp) historyTree.selectedNode.getUserObject();
			stepNumber.setText(String.valueOf(selectedStamp.stepNumber));
			creationOrder.setText(String.valueOf(selectedStamp.creationOrder));
			editType.setText(selectedStamp.edit.editType.shortName);
			editDescription.setText(selectedStamp.edit.description);
			timeOfEdit.setText(selectedStamp.edit.timeString);
		}
		editInfoTable.revalidate();
		editInfoTable.repaint();
	}
}