package com.github.shayna003.sudoker;

import com.github.shayna003.sudoker.prefs.PreferenceDialogs;
import com.github.shayna003.sudoker.swingComponents.SwingUtil;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;
import javax.swing.*;
import java.util.logging.*;
import java.io.*;

/**
 * @version 0.10
 * @since 2020-12-4
 * This class enables dragging to switch tab order or dropping into another tabbedPane, and the closing of individual tabs
 * This class is currently only used for SudokuTabs in an ApplicationFrame
 */
@SuppressWarnings("CanBeFinal")
public class CloseableDndTabbedPane extends JTabbedPane implements DragGestureListener
{	
	static final int LINE_WIDTH = 3; //the line painted to indicate tab placement position
	Color lineColor;
	Rectangle lineRect = new Rectangle();
	
	int targetTab = -1;
	ApplicationFrame owner;
	
	//used for a tabFlavor
	static String mimeType = DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=" + CloseableDndTabbedPane.class.getName();
	static DataFlavor tabFlavor;
	
	//to make the close buttons disappear
	TabComponent mouseOverTab = null;
	
	//don't start a drag event if user is clicking on a close button
	boolean closeIconPressed = false;
	
	//don't make close buttons appear is user is doing a drag action
	boolean dragInProcess = false;
	
	DropTarget dropTarget;
	DragSource dragSource;
	DragSourceHandler dragSourceHandler = new DragSourceHandler();
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		Color tmp = UIManager.getColor("TabbedPane.darkShadow");
		lineColor = tmp == null ? new Color(0, 100, 255) : tmp;
		repaint();
	}
	
	public CloseableDndTabbedPane(ApplicationFrame owner)
	{
		this.owner = owner;
		
		try 
		{
			tabFlavor = new DataFlavor(mimeType);
		} 
		catch (ClassNotFoundException e)
		{
			Application.exceptionLogger.logp(Level.SEVERE, getClass().toString(), "init", "Error when creating DataFlavor from String " + mimeType, e);
		}
		
		dragSource = new DragSource();
		dropTarget = new DropTarget(this, new DropTargetHandler());
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		
		addMouseListener(new ClickHandler());
		addMouseMotionListener(new MouseMotionHandler());
	}
	
	@Override
	public void addTab(String title, Component component)
	{
		super.addTab(null, component);
		setTabComponentAt(getTabCount() - 1, new TabComponent(component, title));
		setToolTipTextAt(getTabCount() - 1, title);
	}
	
	public void addTab(String title, Component component, String toolTipText)
	{
		super.addTab(null, component);
		setToolTipTextAt(getTabCount() - 1, toolTipText);
		setTabComponentAt(getTabCount() - 1, new TabComponent(component, title));
	}
	
	/**
	 * Title and Icon is set to null
	 */
	public void insertTab(String title, Component component, String toolTipText, int index)
	{
		super.insertTab(null, null, component, toolTipText, index);
		setTabComponentAt(index, new TabComponent(component, title));
	}
	
	/**
	 * A component with a close button and a title
	 */
	@SuppressWarnings("CanBeFinal")
	public class TabComponent extends JLabel implements DragGestureListener
	{
		DragSource dragSource = new DragSource();
		String title;
		ImageIcon appearIcon;
		int icon = 0;
		public static final int APPEAR = 0;
		public static final int OVER = 1;
		public static final int PRESSED = 2;
		ImageIcon[] icons = new ImageIcon[3];
		Rectangle2D iconRect; // an underlying rectangle to detect if mouse is within the close "button"
		
		boolean paintIcon = false;
		Component tabContent;
		
		int width;
		int height;
		
		public void setTitle(String newTitle)
		{
			title = newTitle;
			repaint();
		}
		
		/*
		 * This is needed because you can't simply pass the event to dragGestureRecognized because the coordinates are not the same as the TabbedPane's
		 */
		public void dragGestureRecognized(DragGestureEvent event) 
		{
			Point startPoint = SwingUtilities.convertPoint(TabComponent.this, event.getDragOrigin(), CloseableDndTabbedPane.this);
			CloseableDndTabbedPane.this.startDrag(event, startPoint);
		}
		
		public Dimension getPreferredSize()
		{
			return new Dimension(width, height);
		}
		
		public TabComponent(Component tabContent, String title)
		{
			this.tabContent = tabContent; //to determine which tab to close
			this.title = title;
			
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
			
			//it is expected that all of these images have the same dimensions, preferably rectangular
			icons[APPEAR] = SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/close_button/appear.png"));
			icons[OVER] = SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/close_button/over.png"));
			icons[PRESSED] = SwingUtil.getImageIcon(ApplicationLauncher.class.getResource("resources/images/close_button/pressed.png"));
			
			setOpaque(false);
			addMouseListener(new MouseHandler());
			addMouseMotionListener(new CloseButtonMotionHandler());

			FontRenderContext context = getFontMetrics(getFont()).getFontRenderContext();
			Rectangle2D bounds =  getFont().getStringBounds(title, context);
			width = (int) (bounds.getWidth() * 1.2 + icons[0].getIconWidth() * 2.2);
			height = (int) (bounds.getHeight() * 1.5);
			
			iconRect = new Rectangle(0, (height - icons[0].getIconHeight()) / 2, icons[0].getIconWidth(), icons[0].getIconHeight());
		}
		
		public void setPaintIcon(boolean paintIcon)
		{
			this.paintIcon = paintIcon;
			repaint();
		}

		public void setIcon(int type)
		{
			icon = type;
			repaint();
		}
		
		class CloseButtonMotionHandler extends MouseMotionHandler
		{
			public void mouseMoved(MouseEvent event)
			{
				super.mouseMoved(SwingUtilities.convertMouseEvent(TabComponent.this, event, CloseableDndTabbedPane.this));
				if (iconRect.contains(event.getPoint()))
				{
					setIcon(OVER);
				}
				else 
				{
					setIcon(APPEAR);
				}
			}
		}
		
		class MouseHandler extends ClickHandler
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				setIcon(APPEAR);
				if (!dragInProcess)
				{
					setPaintIcon(true);
				}	
			}
			
			@Override
			public void mouseExited(MouseEvent event)
			{
				setIcon(APPEAR);
				setPaintIcon(false);	
			}
			
			@Override
			public void mousePressed(MouseEvent event)
			{
				super.mousePressed(SwingUtilities.convertMouseEvent(TabComponent.this, event, CloseableDndTabbedPane.this));
				if (iconRect.contains(event.getPoint()))
				{
					closeIconPressed = true;
					setIcon(PRESSED);
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent event)
			{
				setIcon(APPEAR);
			}
			
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (iconRect.contains(event.getPoint()))
				{
					setPaintIcon(false);

					int result = JOptionPane.showOptionDialog(CloseableDndTabbedPane.this, "Do you want to save data for this tab before closing it?", "Closing Tab", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Yes", "No", "Cancel"}, "Cancel");
					if (result == 0 || result == 1) // yes or no
					{
						Application.closeTab((SudokuTab) CloseableDndTabbedPane.this.getComponentAt(CloseableDndTabbedPane.this.indexOfComponent(tabContent)), result == 0);
					}
				}
			}
		}
		
		public void paint(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			if (paintIcon) g2.drawImage(icons[icon].getImage(), 0, (height - icons[icon].getIconHeight()) / 2, null);
			
			Rectangle2D bounds = getFont().getStringBounds(title, g2.getFontRenderContext());
			
			double ascent = -bounds.getY();
			g2.drawString(title, (int)(width - bounds.getWidth()) / 2, (int) (ascent + (height - bounds.getHeight()) / 2));
		}
	}
	
	class MouseMotionHandler extends MouseMotionAdapter
	{
		public void mouseMoved(MouseEvent event)
		{
			int index = indexAtLocation(event.getX(), event.getY());
			if (mouseOverTab != null && (index < 0 || getTabComponentAt(index) != mouseOverTab))
			{
				mouseOverTab.setPaintIcon(false);
			}
			if (index >= 0 && !dragInProcess)
			{
				mouseOverTab = (TabComponent) getTabComponentAt(index);
				mouseOverTab.setPaintIcon(true);
			}
		}
	}
		
	/**
	 * Shows a line to indicate of the drop location of the tab in dnd
	 */
	public void showLine(int index)
	{
		if (index < 0) 
		{
			targetTab = -1;
			repaint();
			return;
		}
		Rectangle rect;

		// tabs are horizontal
		if (getTabPlacement() == JTabbedPane.TOP || getTabPlacement() == JTabbedPane.BOTTOM)
		{
			if (index != getTabCount())
			{
				rect = getBoundsAt(index);
				lineRect.setRect(rect.x - LINE_WIDTH / 2d, rect.y, LINE_WIDTH, rect.height);
			}
			else
			{
				rect = getBoundsAt(getTabCount() - 1);
				lineRect.setRect(rect.x + rect.width - LINE_WIDTH / 2d, rect.y, LINE_WIDTH, rect.height);
			}
		}
		else // tabs are vertical
		{
			if (index != getTabCount())
			{
				rect = getBoundsAt(index);
				lineRect.setRect(rect.x, rect.y - LINE_WIDTH / 2d, rect.width, LINE_WIDTH);
			}
			else
			{
				rect = getBoundsAt(getTabCount() - 1);
				lineRect.setRect(rect.x, rect.y + rect.height - LINE_WIDTH / 2d, rect.width, LINE_WIDTH);
			}
		}
		repaint();
	}
	
	/**
	 * @return The index of the place a tab should be dropped at, -1 if none found
	 */
	public int getTargetTabIndex(Point point)
	{
		boolean isHorizontal = getTabPlacement() == JTabbedPane.TOP || getTabPlacement() == JTabbedPane.BOTTOM;
		Rectangle r;
		for (int i = 0; i < getTabCount(); i++)
		{
			r = getBoundsAt(i);
			if (isHorizontal) 
			{
				r.setRect(r.x - r.width / 2d, r.y, r.width, r.height);
				if (point.x >= r.x && point.x <= r.getMaxX())
				{
					targetTab = i;
					return i;
				}
			}
			else 
			{
				r.setRect(r.x, r.y - r.height / 2d, r.width, r.height);
				if (point.y >= r.y && point.y <= r.getMaxY())
				{
					targetTab = i;
					return i;
				}
			}
		}
		
		// testing for right most / bottom most spot of the tabs
		r = getBoundsAt(getTabCount() - 1);
		if (isHorizontal) 
		{
			r.setRect(r.x + r.width / 2d, r.y, r.width, r.height);
			if (point.x >= r.x && point.x <= r.getMaxX())
			{
				targetTab = getTabCount();
				return getTabCount(); 
			}
		}
		else 
		{
			r.setRect(r.x, r.y + r.height / 2d, r.width, r.height);
			if (point.y >= r.y && point.y <= r.getMaxY())
			{
				targetTab = getTabCount();
				return getTabCount(); 
			}
		}

		// not a valid drop spot
		targetTab = -1;
		return -1;
	}
	
	public void performDnd(TabData data, int targetTab, DropTargetDropEvent event)
	{
		if (!(data.source == CloseableDndTabbedPane.this && CloseableDndTabbedPane.this.getTabCount() == 1))
		{
			if (data.component instanceof SudokuTab)
			{
				SudokuTab tab = (SudokuTab) data.component;
				data.source.remove(tab);
				data.source.owner.historyTrees.remove(tab.historyTreePanel);
				
				if (data.source.getTabCount() == 0)
				{
					Application.closeWindow(data.source.owner, true, false);
				}
				else if (data.source != CloseableDndTabbedPane.this)
				{
					Application.openWindowsAndTabs.windowChanged(data.source.owner);
				}
				
				insertTab(data.title, data.component, data.tip, Math.min(targetTab, getTabCount()));
				owner.historyTrees.insertTab(tab.getName(), null, tab.historyTreePanel, "History Tree for this Board", Math.min(targetTab, owner.historyTrees.getTabCount()));
				tab.owner = owner;
				
				Application.openWindowsAndTabs.windowChanged(owner);
				setSelectedIndex(targetTab >= getTabCount() ? getTabCount() - 1 : targetTab);
				
				if (Application.boardComparatorFrame != null) Application.boardComparatorFrame.updateChosenBoardInfos();
			}
			else 
			{
				data.source.remove(data.component);
				insertTab(data.title, data.component, data.tip, Math.min(targetTab, getTabCount()));
				setSelectedIndex(targetTab >= getTabCount() ? getTabCount() - 1 : targetTab);
			}
			
			if (event != null)
			{
				event.acceptDrop(DnDConstants.ACTION_MOVE);
				event.dropComplete(true);
			}

		}
	}
	
	class DropTargetHandler extends DropTargetAdapter
	{
		@Override
		public void dragExit(DropTargetEvent event)
		{
			dragInProcess = false;
			targetTab = -1;
			repaint();
		}
		
		@Override
		public void dragOver(DropTargetDragEvent event)
		{
			dragInProcess = true;
			Point point = event.getLocation();
			
			Transferable t = event.getTransferable();
			if (t.isDataFlavorSupported(tabFlavor))
			{
				showLine(getTargetTabIndex(point));
			}
		}
		
		@Override
		public void drop(DropTargetDropEvent event)
		{
			dragInProcess = false;
			Transferable t = event.getTransferable();

			if (targetTab >= 0 && t.isDataFlavorSupported(tabFlavor))
			{
				if (t.isDataFlavorSupported(tabFlavor))
				{
					try 
					{
						TabData data = (TabData) t.getTransferData(tabFlavor);
						if (event.getDropAction() == DnDConstants.ACTION_MOVE)
						{
							performDnd(data, targetTab, event);
						}
					} 
					catch (UnsupportedFlavorException e) 
					{
						Application.exceptionLogger.logp(Level.SEVERE, getClass().toString(), "drop", "Error when trying to import data", e);
					}
					catch (IOException e)
					{
						Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "drop", "Error when trying to import data", e);
					}
				}
			}
			targetTab = -1;
			repaint();
		}
	}
	
	class DragSourceHandler extends DragSourceAdapter
	{
		@Override
		public void dragDropEnd(DragSourceDropEvent event) 
		{
			targetTab = -1;
			repaint();
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	public static class TabData
	{
		Component component; //the component added in a tab, currently just SudokuTab
		CloseableDndTabbedPane source;
		String title;
		String tip;
		int sourceTabIndex;
		
		public TabData(Component component, CloseableDndTabbedPane source, String title, String tip, int sourceTabIndex)
		{
			this.component = component;
			this.source = source;
			this.title = title;
			this.tip = tip;
			this.sourceTabIndex = sourceTabIndex;
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	static class TabTransferable implements Transferable
	{
		TabData data; 
		
		public TabTransferable(TabData data) 
		{
			this.data = data;
		}
		
		@Override
		public Object getTransferData(DataFlavor flavor) 
		{
			if (flavor.equals(tabFlavor) || flavor.getRepresentationClass().equals(CloseableDndTabbedPane.class))
			{
				return data;
			}
			else return null;
		}
		
		@Override
		public DataFlavor[] getTransferDataFlavors() 
		{
			return new DataFlavor[] { tabFlavor };
		}
		
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) 
		{
			return flavor.getRepresentationClass().equals(CloseableDndTabbedPane.class);
		}
	}
	
	class ClickHandler extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent event)
		{
			closeIconPressed = false;
			repaint();
			Point p = event.getPoint();
			int tab = indexAtLocation(p.x, p.y);
			if (tab >= 0) setSelectedIndex(tab);
		}
	}	
	
	@Override
	public void dragGestureRecognized(DragGestureEvent event) 
	{
		Point startPoint = event.getDragOrigin();
		startDrag(event, startPoint);
	}
	
	public void startDrag(DragGestureEvent event, Point startPoint)
	{
		int tab = indexAtLocation(startPoint.x, startPoint.y);
		
		if (tab >= 0 && !closeIconPressed)
		{
			Rectangle bounds = getBoundsAt(tab);
			((TabComponent) getTabComponentAt(tab)).setPaintIcon(false);
			dragSource.startDrag(event, Cursor.getDefaultCursor(), getTabImage(tab), new Point(-(bounds.width / 2), -(bounds.height / 2)), new TabTransferable(new TabData(this.getComponentAt(tab), this, ((TabComponent) (getTabComponentAt(tab))).title, getToolTipTextAt(tab), tab)), dragSourceHandler);
		}
	}
	
	//quality is bad if you have your use own paint methods, somehow just setting a title and using paint() gives okay quality
	public Image getTabImage(int index)
	{
		Rectangle rect = getBoundsAt(index);
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // this seem to have the most effect
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		paint(g2);
		return image.getSubimage(rect.x,rect.y,rect.width,rect.height);
	}
	
	/**
	 * Currently not used
	 * Returns a scaled snapshot of the content of tab at the specified index
	 * @param scale a number between 1 and 0 acting as a percent
	 */
	public Image getContentImage(int index, float scale)
	{
		BufferedImage image = new BufferedImage(getComponentAt(index).getWidth(), getComponentAt(index).getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		getComponentAt(index).paint(g);
		return image.getScaledInstance((int) (getComponentAt(index).getWidth() * scale), (int) (getComponentAt(index).getHeight() * scale), Image.SCALE_SMOOTH);
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (targetTab >= 0) 
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(lineColor);
			g2.fill(lineRect);
		}
	}
}