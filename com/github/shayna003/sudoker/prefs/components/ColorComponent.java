package com.github.shayna003.sudoker.prefs.components;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.swingComponents.*;
import com.github.shayna003.sudoker.prefs.*;

import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.prefs.*;
import java.util.logging.*;

@SuppressWarnings("CanBeFinal")
public class ColorComponent extends JPanel implements PrefsComponent
{
	String settingName;
	Color defaultColor;
	public Color color;
	ColorDragComponent dragComponent; //to make the drag rectangle look smaller

	int colorWidth;
	int colorHeight;
	int borderWidthWhite;
	int borderWidthBlack;
	int width;	
	int height;
	Color colorWhite; 
	Color colorBlack;
	Color colorInnerBlack;
	Color colorClicked;
	SettingsPanel settingsPanel;
	ColorChooserDialogOwner colorChooserDialogOwner;
	String text; //title for colorChooserDialog

	CompoundBorder border;
	CompoundBorder clicked;
	PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	//a combined approach of TransferHandler and the listeners works better
	class DropTargetHandler extends DropTargetAdapter
	{
		public void drop(DropTargetDropEvent event)
		{
			Transferable tr = event.getTransferable();
			if (tr.isDataFlavorSupported(ColorSelection.colorFlavor))
			{
				event.acceptDrop(DnDConstants.ACTION_COPY);
				event.dropComplete(true);
				try 
				{
					setColor((Color)tr.getTransferData(ColorSelection.colorFlavor));
				}
				catch (UnsupportedFlavorException | IOException e) 
				{
					Application.exceptionLogger.logp(Level.WARNING, getClass().toString(), "DropTargetHandler.drop", "Error when getting transfer data", e);
				}
			}
		}
	}
		
	@SuppressWarnings("CanBeFinal")
	class ColorDragComponent extends JComponent implements DragGestureListener
	{
		DropTarget dropTarget;
		DragSource dragSource;
		DragSourceHandler dragSourceHandler = new DragSourceHandler();
		
		public ColorDragComponent()
		{
			dragSource = new DragSource();
			dropTarget = new DropTarget(this, new DropTargetHandler());
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
		}
		
		public void dragGestureRecognized(DragGestureEvent event) 
		{
			unclick();
			dragSource.startDrag(event, DragSource.DefaultCopyDrop, makeRectangleImage(color, 20, 20), new Point(-10, -10), new ColorSelection(color), dragSourceHandler);
		}
		
		//somehow doesn't need to do anything
		class DragSourceHandler extends DragSourceAdapter { }
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(colorWidth, colorHeight);
		}
		
		/**
		 * For GridBagLayout
		 */
		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		public void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(color);
			g2.fillRect(0, 0, colorWidth, colorHeight);
		}
	}
	
	class ClickHandler extends MouseAdapter
	{
		Color tmp;
		public void mouseClicked(MouseEvent event)
		{
			setBorder(clicked);
			tmp = colorChooserDialogOwner.showColorChooserDialog(text, color);
			unclick();
			setColor(tmp);
		}
		
		public void mousePressed(MouseEvent event)
		{
			setBorder(clicked);
		}
		
		public void mouseReleased(MouseEvent event)
		{
			unclick();
		}
	}
	
	public void resetToDefault()
	{
		setColor(defaultColor);
	}

	@Override
	public void loadSettings(Preferences node)
	{
		setColor(new Color(node.getInt(settingName, defaultColor.getRGB()), true));
	}

	@Override
	public void saveSettings(Preferences node)
	{
		node.putInt(settingName, color.getRGB());
	}
	
	public void unclick()
	{
		setBorder(border);
	}
	
	public void setColorIgnoreChanges(Color color)
	{
		this.color = color;
		dragComponent.repaint();
	}
	
	public void setColor(Color newColor)
	{
		Color previousColor = this.color;
		this.color = newColor;
		
		propertyChangeSupport.firePropertyChange("color", previousColor, newColor);
		dragComponent.repaint();
		settingsPanel.applyChanges();
	}
	
	public void addColorChangeListener(PropertyChangeListener listener) 
	{
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	public void removeColorChangeListener(PropertyChangeListener listener) 
	{
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	public void makeBorder()
	{
		border = BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(colorInnerBlack, borderWidthBlack), BorderFactory.createLineBorder(colorWhite, borderWidthWhite)), BorderFactory.createLineBorder(colorBlack, borderWidthBlack));
		
		clicked = BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(colorInnerBlack, borderWidthBlack), BorderFactory.createLineBorder(colorClicked, borderWidthWhite)), BorderFactory.createLineBorder(colorBlack, borderWidthBlack));
		
		setBorder(border);
	}
	
	public ColorComponent(String settingName, Color color, Color defaultColor, SettingsPanel settingsPanel, ColorChooserDialogOwner colorChooserDialogOwner, String toolTipText)
	{
		this.settingName = settingName;
		this.color = color;
		this.settingsPanel = settingsPanel;
		this.colorChooserDialogOwner = colorChooserDialogOwner;
		this.defaultColor = defaultColor;

		colorWidth = 40;
		colorHeight = 20;
		borderWidthWhite = 8;
		borderWidthBlack = 2;
		width = colorWidth + borderWidthWhite * 2 + borderWidthBlack * 4;
		height = colorHeight + borderWidthWhite * 2 + borderWidthBlack * 4;
		colorWhite = new Color(250, 250, 250);
		colorBlack = new Color(159, 159, 159);
		colorInnerBlack = new Color(119, 119, 119);
		colorClicked = new Color(170, 170, 170);
		dragComponent = new ColorDragComponent();
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		add(dragComponent);

		dragComponent.addMouseListener(new ClickHandler());
		addMouseListener(new ClickHandler());

		text = toolTipText;
		dragComponent.setToolTipText("Set the " + toolTipText);
		makeBorder();
	}
	
	/**
	 * Called by BoxColorComponent
	 * In real practice, only the sizes of the created ColorComponents differ so I might change this constructor in the future.
	 */
	public ColorComponent(String settingName, Color color, Color defaultColor, SettingsPanel settingsPanel, ColorChooserDialogOwner colorChooserDialogOwner, int colorWidth, int colorHeight, Color colorWhite, Color colorBlack, Color colorInnerBlack, Color colorClicked, int borderWidthWhite, int borderWidthBlack, String toolTipText, int index)
	{
		this.settingName = settingName;
		this.defaultColor = defaultColor;
		this.settingsPanel = settingsPanel;
		this.colorChooserDialogOwner = colorChooserDialogOwner;
		this.color = color;
		this.colorWidth = colorWidth;
		this.colorHeight = colorHeight;
		this.colorWhite = colorWhite;
		this.colorBlack = colorBlack;
		this.colorInnerBlack = colorInnerBlack;
		this.colorClicked = colorClicked;
		this.borderWidthWhite = borderWidthWhite;
		this.borderWidthBlack = borderWidthBlack;
		this.width = colorWidth + borderWidthWhite * 2 + borderWidthBlack * 4;
		this.height = colorHeight + borderWidthWhite * 2 + borderWidthBlack * 4;
		dragComponent = new ColorDragComponent();
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		add(dragComponent);
		dragComponent.addMouseListener(new ClickHandler());
		addMouseListener(new ClickHandler());
		text = toolTipText;
		makeBorder();
		dragComponent.setToolTipText("Set the " + toolTipText);
	}
	
	public static Image makeRectangleImage(Color color, int width, int height)
	{
		Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D)image.getGraphics();

		g2.setColor(color);
		g2.fillRect(0, 0, width, height);

		// if transparency is too high, then draw a border around the colored rectangle.
		// the lower the transparency, the higher the transparency of the surrounding rectangle
		if (color.getAlpha() < 51)
		{
			g2.setColor(new Color(119, 119, 119, 255 - color.getAlpha() * 5));
			g2.setStroke(new BasicStroke(2));
			g2.drawRect(1, 1, width - 2, height - 2);
		}

		g2.dispose();
		return image;
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension(width, height);
	}
}