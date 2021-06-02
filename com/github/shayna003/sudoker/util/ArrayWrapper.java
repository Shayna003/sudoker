package com.github.shayna003.sudoker.util;

import java.util.Arrays;

/**
 * For use in a JComboBox
 * More specifically, DigitsAndIndexesPanel of PreferenceFrame
 */
@SuppressWarnings("CanBeFinal")
public class ArrayWrapper<T>
{
	public T[] elements;
	
	public ArrayWrapper(T[] t)
	{
		this.elements = t;
	}
	
	@Override
	public String toString()
	{
		return Arrays.toString(elements);
	}
}