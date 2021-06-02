package com.github.shayna003.sudoker.swingComponents;

import com.github.shayna003.sudoker.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.logging.*;

@SuppressWarnings("CanBeFinal")
public class ColorSelection implements Transferable, ClipboardOwner
{
	public static DataFlavor colorFlavor;
	public static String mimeType = DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=java.awt.Color";
	Color color;
	
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	@Override
	public void lostOwnership(Clipboard c, Transferable t) 
	{
	}
		
	public ColorSelection(Color color)
	{
		this.color = color;
		try 
		{
			colorFlavor = new DataFlavor(mimeType);
		}
		catch (ClassNotFoundException e) 
		{
			Application.exceptionLogger.logp(Level.SEVERE, getClass().toString(), "init", "Error when creating DataFlavor from String " + mimeType, e);
		}
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if (flavor.getRepresentationClass().equals(java.awt.Color.class))//== colorFlavor)
		{
			return color; 
		}
		else if (flavor.equals(DataFlavor.stringFlavor))
		{
			return "red = " + color.getRed() + ", green = " + color.getGreen() + ", blue = " + color.getBlue() + ", alpha = " + color.getAlpha();
		}
		throw new UnsupportedFlavorException(flavor);
	}
	
	public DataFlavor[] getTransferDataFlavors()
	{
		return new DataFlavor[] { colorFlavor, DataFlavor.stringFlavor };
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return flavor.getRepresentationClass().equals(java.awt.Color.class) || flavor.equals(DataFlavor.stringFlavor);
	}
}