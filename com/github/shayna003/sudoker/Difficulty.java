package com.github.shayna003.sudoker;

import java.awt.*;

/**
 * @since 2-20-2021
 */
public enum Difficulty 
{
	BASIC(Color.BLACK, "Basic"), 
	EASY(Color.GREEN, "Easy"), 
	MODERATE(Color.ORANGE, "Moderate"), 
	HARD(Color.RED, "Hard");
		
	public final Color text_color; // currently not used due to inconsistent looks across different look and feels
	public final String text;
		
	Difficulty(Color c, String s)
	{
		this.text_color = c;
		this.text = s;
	}
}
