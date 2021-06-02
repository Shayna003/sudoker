package com.github.shayna003.sudoker.prefs;

import com.github.shayna003.sudoker.*;
import com.github.shayna003.sudoker.prefs.components.PrefsButtonGroup;
import com.github.shayna003.sudoker.prefs.components.PrefsCheckBox;
import com.github.shayna003.sudoker.prefs.components.PrefsNumberSpinner;
import com.github.shayna003.sudoker.solver.*;
import com.github.shayna003.sudoker.swingComponents.GBC;

import java.awt.*;
import javax.swing.*;
import java.util.prefs.*;
import java.io.*;

/**
 * @since 2-25-2021
 * I guess some settings that can go here include:
 * Dnd solving tree technique order
 * Solver output verbosity
 * Each step stops after 1. one round of searching for a technique, 2. finding the first instance of a technique
 */
@SuppressWarnings("CanBeFinal")
public class SolverSettingsPanel extends JPanel implements SettingsPanel
{
	SingleSettingsFile settingsFile;
	PreferenceFrame preferenceFrame;
	
	JPanel modelSolvingTreePanel;
	public SolvingTechniqueTree modelSolvingTree; // all other SolvingTechniqueTrees are created by cloning this tree
	public PrefsNumberSpinner maxSolutionsForSolveAll;
	public PrefsCheckBox clearSolverOutputWhenAppendingMessage;

	// for "returnFirst" in Solver
	public JRadioButton returnFirstMatch;
	public JRadioButton findAllCases;
	public PrefsButtonGroup returnFirstButtonGroup;

	boolean initializing;
		
	public SolverSettingsPanel(PreferenceFrame preferenceFrame)
	{
		initializing = true;
		this.preferenceFrame = preferenceFrame;
		
		settingsFile = new SingleSettingsFile(new File(Application.preferenceFolder, "solver_settings.xml"));
		modelSolvingTree = new SolvingTechniqueTree();

		returnFirstMatch = new JRadioButton("Return First Case");
		findAllCases = new JRadioButton("Find All Cases");
		returnFirstButtonGroup = new PrefsButtonGroup(null, "returnFirstButtonGroup", 0, returnFirstMatch, findAllCases);

		maxSolutionsForSolveAll = new PrefsNumberSpinner("maxSolutionsForSolveAll", 2, 10_0000, 10, 500, null, 5);

		clearSolverOutputWhenAppendingMessage = new PrefsCheckBox("clearSolverOutputBeforeAppend", "Clear Solver Output When Adding New Messages", true);

		JButton setToDefault = new JButton("Set to Default Order and State");
		setToDefault.addActionListener(event ->
		{
			modelSolvingTree.resetToDefault();
		});
		
		/*JButton cloneButton = new JButton("create clone");
		cloneButton.addActionListener(event ->
		{
			SolvingTechniqueTree.createTreeFrame(modelSolvingTree.clone());
		});*/
		
		modelSolvingTreePanel = new JPanel(new BorderLayout());
		modelSolvingTreePanel.setBorder(BorderFactory.createTitledBorder("Default Order and State of Solving Techniques for Take Step Upon Tab Creation:"));
		modelSolvingTreePanel.add(new JScrollPane(modelSolvingTree), BorderLayout.CENTER);
		modelSolvingTreePanel.add(setToDefault, BorderLayout.SOUTH);

		JPanel contentPanel = new JPanel(new GridBagLayout());
		contentPanel.add(clearSolverOutputWhenAppendingMessage, new GBC(0, 0, 2, 1).setAnchor(GBC.WEST));
		contentPanel.add(new JLabel("Maximum number of solutions to look for in \"Solution Count\": "), new GBC(0, 1, 2, 1).setAnchor(GBC.WEST));
		contentPanel.add(maxSolutionsForSolveAll, new GBC(2, 1));

		contentPanel.add(new JLabel("For each Solving Technique in Take Step:"), new GBC(0, 2, 2, 1).setAnchor(GBC.WEST));
		contentPanel.add(returnFirstMatch, new GBC(0, 3).setAnchor(GBC.WEST));
		contentPanel.add(findAllCases, new GBC(1, 3).setAnchor(GBC.WEST));

		setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.NORTH);
		add(modelSolvingTreePanel, BorderLayout.CENTER);
		
		loadSettings(settingsFile);
		initializing = false;
	}
	
	public void loadSettings(SingleSettingsFile settingsFile)
	{
		Preferences node = settingsFile.node;
		Preferences solvingTreeDataNode = node.node("solvingTreeData");
		modelSolvingTree.loadSettings(solvingTreeDataNode);
		maxSolutionsForSolveAll.loadSettings(settingsFile.node);
		returnFirstButtonGroup.loadSettings(settingsFile.node);
		clearSolverOutputWhenAppendingMessage.loadSettings(settingsFile.node);
	}
	
	public void saveSettings(SingleSettingsFile settingsFile, boolean saveToFile)
	{
		Preferences node = settingsFile.node;
		Preferences solvingTreeDataNode = node.node("solvingTreeData");
		modelSolvingTree.saveSettings(solvingTreeDataNode);
		maxSolutionsForSolveAll.saveSettings(settingsFile.node);
		returnFirstButtonGroup.saveSettings(settingsFile.node);
		clearSolverOutputWhenAppendingMessage.saveSettings(settingsFile.node);
		
		if (saveToFile) settingsFile.save();
	}
	
	public void applyChanges() { }

	@Override
	public SingleSettingsFile getSettingsFile()
	{
		return settingsFile;
	}
}