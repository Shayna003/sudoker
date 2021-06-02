package com.github.shayna003.sudoker.swingComponents;

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;

/**
 * This Metal Theme resembles "Dark Mode".
 * @since 4-21-2021
 */
@SuppressWarnings("CanBeFinal")
public class DarkMetalTheme extends DefaultMetalTheme
{
	@Override
	public ColorUIResource getPrimary1()
	{
		return new ColorUIResource(139, 159, 199);
	}
	
	@Override
	public ColorUIResource getPrimary2()
	{
		return new ColorUIResource(99, 79, 129);
	}
	
	@Override
	public ColorUIResource getPrimary3()
	{
		return new ColorUIResource(150, 120, 125);
	}
	
	@Override
	public ColorUIResource getSecondary1()
	{
		return new ColorUIResource(216, 169, 169);
	}
	
	@Override
	public ColorUIResource getSecondary2()
	{
		return new ColorUIResource(103, 109, 169);
	}
	
	@Override
	public ColorUIResource getSecondary3()
	{
		return new ColorUIResource(54, 53, 62);
	}

	@Override
	public FontUIResource getSubTextFont()
	{
		return super.getSubTextFont();
	}
		
	static ColorUIResource white = new ColorUIResource(219, 219, 219);
	static ColorUIResource black = new ColorUIResource(39, 39, 39);
	
	@Override
	public String getName()
	{
		return "DarkMetal";
	}

	@Override
	protected ColorUIResource getWhite() { return white; }

	@Override
	protected ColorUIResource getBlack() { return black; }

	@Override
	public ColorUIResource getFocusColor() { return getSecondary1(); }

	@Override
	public  ColorUIResource getDesktopColor() { return getPrimary2(); }

	@Override
	public ColorUIResource getControl() { return getSecondary3(); }

	@Override
	public ColorUIResource getControlShadow() { return getPrimary2(); }

	@Override
	public ColorUIResource getControlDarkShadow() { return getPrimary1(); }

	@Override
	public ColorUIResource getControlInfo() { return white; }

	@Override
	public ColorUIResource getControlHighlight() { return getSecondary2(); }

	@Override
	public ColorUIResource getControlDisabled() { return getSecondary2(); }

	@Override
	public ColorUIResource getPrimaryControl() { return getPrimary3(); }

	@Override
	public ColorUIResource getPrimaryControlShadow() { return getPrimary2(); }

	@Override
	public ColorUIResource getPrimaryControlDarkShadow() { return getSecondary1(); }

	@Override
	public ColorUIResource getPrimaryControlInfo() { return white; }

	@Override
	public ColorUIResource getPrimaryControlHighlight() { return getPrimary1(); }

	@Override
	public ColorUIResource getSystemTextColor() { return white; }

	@Override
	public ColorUIResource getControlTextColor() { return getControlInfo(); }

	@Override
	public ColorUIResource getInactiveControlTextColor() { return getControlDisabled(); }

	@Override
	public ColorUIResource getInactiveSystemTextColor() { return white; }

	@Override
	public ColorUIResource getUserTextColor() { return white; }

	@Override
	public ColorUIResource getTextHighlightColor() { return getSecondary2(); }

	@Override
	public ColorUIResource getHighlightedTextColor() { return getControlTextColor(); }

	@Override
	public ColorUIResource getWindowBackground() { return black; }

	@Override
	public ColorUIResource getWindowTitleBackground() { return getPrimary3(); }

	@Override
	public ColorUIResource getWindowTitleForeground() { return white; }

	@Override
	public ColorUIResource getWindowTitleInactiveBackground() { return getSecondary3(); }

	@Override
	public ColorUIResource getWindowTitleInactiveForeground() { return getSecondary2(); }

	@Override
	public ColorUIResource getMenuBackground() { return getSecondary3(); }

	@Override
	public ColorUIResource getMenuForeground() { return white; }

	@Override
	public ColorUIResource getMenuSelectedBackground() { return getPrimary2(); }

	@Override
	public ColorUIResource getMenuSelectedForeground() { return white; }

	@Override
	public ColorUIResource getMenuDisabledForeground() { return getSecondary2(); }

	@Override
	public ColorUIResource getSeparatorBackground() { return getSecondary1(); }

	@Override
	public ColorUIResource getSeparatorForeground() { return white; }

	@Override
	public ColorUIResource getAcceleratorForeground() { return white; }
	
	@Override
	public ColorUIResource getAcceleratorSelectedForeground() { return white; }
}