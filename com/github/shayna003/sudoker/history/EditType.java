package com.github.shayna003.sudoker.history;

/**
 * @since 4-5-2021
 * So far there are 10 types of edits
 */
@SuppressWarnings("CanBeFinal")
public enum EditType
{
	// this cannot be undone, will be the creation event of the tree
	BOARD_CREATION("Board Creation"),
	
	// things that overrode the current board
	IMPORT("Import"), GENERATE("Generate"), CLONE("Clone"), LOAD_QUICK_SAVE("Load Quick Save"), CLEAR("Clear"), MASS_LOCK_CHANGE("Mass Lock Change"),
	
	// visuals
	ROTATE("Rotate"), FLIP("Flip"),
	
	EDIT_CELL("Edit Cell"),
	
	// solver functions
	TAKE_STEP("Take Step"), QUICK_SOLVE("Quick Solve");
	
	String shortName;
	
	EditType(String shortName)	
	{
		this.shortName = shortName;
	}
}