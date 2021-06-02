package com.github.shayna003.sudoker.history;

import com.github.shayna003.sudoker.*;
import java.time.*;
import java.time.format.*;

/**
 * This class records a single edit made to a sudoku board
 * @since 4-5-2021
 */
@SuppressWarnings("CanBeFinal")
public class Edit
{
	public EditType editType;
	public String description;
	LocalDateTime timeOfEdit;
	String timeString;
	public Board board;
	
	public Edit(String description, EditType editType, Board board)
	{
		timeOfEdit = LocalDateTime.now();
		timeString = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).format(timeOfEdit);
		this.description = description;
		this.editType = editType;
		this.board = board;
	}

	@Override 
	public String toString()
	{
		return "Edit[type=" + editType.shortName + "]";
	}
}