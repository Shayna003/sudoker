package com.github.shayna003.sudoker.swingComponents;

import java.awt.*;

/**
 * A helper class for GridBagLayout.
 */
public class GBC extends GridBagConstraints
{
	public GBC(int gridx, int gridy)
	{
		this.gridx = gridx;
		this.gridy = gridy;
	}
	
	public GBC(int gridx, int gridy, int gridwidth, int gridheight)
	{
		this.gridx = gridx;
		this.gridy = gridy;
		this.gridwidth = gridwidth;
		this.gridheight = gridheight;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setFill(int fill)
	{
		this.fill = fill;
		return this;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setAnchor(int anchor)
	{
		this.anchor = anchor;
		return this;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setWeight(double weightx, double weighty)
	{
		this.weightx = weightx;
		this.weighty = weighty;
		return this;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setInsets(int distance)
	{
		this.insets = new Insets(distance, distance, distance, distance);
		return this;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setInsets(int top, int left, int bottom, int right)
	{
		this.insets = new Insets(top, left, bottom, right);
		return this;
	}
	
	/**
	 * @return this object to be able to chain commands
	 */
	public GBC setIpad(int ipadx, int ipady)
	{
		this.ipadx = ipadx;
		this.ipady = ipady;
		return this;
	}
}