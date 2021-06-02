package com.github.shayna003.sudoker.prefs.keys;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

@SuppressWarnings("CanBeFinal")
public class KeyBindingsTable extends JTable implements TableCellRenderer//, Comparator<KeyComponentPanel>
{
	ArrayList<KeyComponentPanel> keyComponentPanels;
	KeyBindingsTableEditor editor;
	
	@Override
	public void setFont(Font font)
	{
		super.setFont(font);
		
		if (keyComponentPanels != null && keyComponentPanels.size() > 0)
		{
			setRowHeight(keyComponentPanels.get(0).getPreferredSize().height);
		}
		else 
		{
			setRowHeight(new JButton("Clear").getPreferredSize().height);
		}
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (editor != null) 
		{
			setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
			setSelectionForeground(UIManager.getColor("Table.selectionForeground"));

			if (keyComponentPanels.size() > 0)
			{
				setRowHeight(keyComponentPanels.get(0).getPreferredSize().height);
			}
			else 
			{
				setRowHeight(new JButton("Clear").getPreferredSize().height);
			}
		}	
	}
	
	class KeyBindingsTableEditor extends AbstractCellEditor implements TableCellEditor
	{
		public Object getCellEditorValue()
		{
			return null;
		}
		
		@Override
		public boolean shouldSelectCell(EventObject event)
		{
			return true;
		}
		
		@Override
		public void cancelCellEditing()
		{
			keyComponentPanels.get(getSelectedRow()).stopEditing();
			super.cancelCellEditing();
		}
		
		@Override
		public boolean stopCellEditing()
		{
			keyComponentPanels.get(getSelectedRow()).stopEditing();
			super.stopCellEditing();
			return true;
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean hasFocus, int row, int column)
		{
			return keyComponentPanels.get(row);
		}
	}
	
	@Override 
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		return (KeyComponentPanel )value;
	}
	
	class KeyBindingsTableModel extends AbstractTableModel
	{
		@Override
		public int getRowCount() { return keyComponentPanels.size(); }
		
		@Override
		public int getColumnCount() { return 2; } // item name, shortcut value/edit it, reset value, clear value
		
		@Override
		public Object getValueAt(int r, int c)
		{
			return c == 1 ? keyComponentPanels.get(r) : keyComponentPanels.get(r).itemName;
		}
		
		@Override
		public String getColumnName(int c)
		{
			return c == 0 ? "Item" : "Shortcut";
		}
		
		@Override
		public Class<?> getColumnClass(int column)
		{
			return column == 0 ? String.class : KeyComponentPanel.class;
		}
		
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column == 1;
		}
	}
	
	public KeyBindingsTable(ArrayList<KeyComponentPanel> keyComponentPanels)
	{
		this.keyComponentPanels = keyComponentPanels;
		setModel(new KeyBindingsTableModel());
		editor = new KeyBindingsTableEditor();
		setDefaultEditor(KeyComponentPanel.class, editor);
		setDefaultRenderer(KeyComponentPanel.class, this);
		setRowHeight(new JButton("Clear").getPreferredSize().height);
	}
}