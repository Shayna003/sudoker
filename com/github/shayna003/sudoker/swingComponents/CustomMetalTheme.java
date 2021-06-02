package com.github.shayna003.sudoker.swingComponents;

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;

/**
 * Change this class as needed.
 * @since 3-13-2021
 */
public class CustomMetalTheme extends DefaultMetalTheme
{
	@Override
	public FontUIResource getControlTextFont()
	{
		return super.getControlTextFont();
	}

	@Override
	public FontUIResource getMenuTextFont()
	{
		return super.getMenuTextFont();
	}
	
	@Override
	protected ColorUIResource getPrimary1()
	{
		return new ColorUIResource(107, 130, 243);
	}
	
	@Override
	protected ColorUIResource getPrimary2()
	{
		return new ColorUIResource(173, 201, 244);
	}
	
	@Override
	protected ColorUIResource getPrimary3()
	{
		return new ColorUIResource(160, 224, 233);
	}
	
	@Override
	protected ColorUIResource getSecondary1()
	{
		return new ColorUIResource(2, 29, 39);
	}
	
	@Override
	protected ColorUIResource getSecondary2()
	{
		return new ColorUIResource(195, 223, 242);
	}
	
	@Override
	protected ColorUIResource getSecondary3()
	{
		return new ColorUIResource(236, 244, 251);
	}

	@Override
	public FontUIResource getSubTextFont()
	{
		return super.getSubTextFont();
	}
	
	@Override
	public FontUIResource getSystemTextFont()
	{
		return super.getSystemTextFont();
	}
	
	@Override
	public FontUIResource getUserTextFont()
	{
		return super.getUserTextFont();
	}
	
	@Override
	public FontUIResource getWindowTitleFont()
	{
		return super.getWindowTitleFont();
	}
}