package com.github.shayna003.sudoker.prefs.components;

import com.github.shayna003.sudoker.prefs.*;
import com.github.shayna003.sudoker.swingComponents.*;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.*;

@SuppressWarnings("CanBeFinal")
public class FontChooserPanel extends JPanel implements PrefsComponent
{
	SettingsPanel settingsPanel;
	String name; //the title of the displayed border
	public Font chosenFont;

	public ColorComponent colorComponent;
	ButtonGroup nameButtonGroup;
	JRadioButton fontFaceButton;
	JRadioButton fontFamilyButton;
	
	//sets font to null to use the UI's default, the components using this font chooser panel's font
	//will first set their font to null, then set this FontChooserPanel's font to their resulting font (which should be the UI's default font if their parents have not set their font).
	JButton useDefault; 

	JComboBox<String> faceCombo;
	JComboBox<String> familyCombo;
	
	public JCheckBox bold;
	public JCheckBox italics;
	
	public int style;
	
	public String fontName;
	
	public TextSliderPanel sliderPanel;
	
	String settingName;
	String defaultName;
	int defaultStyle;
	int defaultSize;
	
	public boolean shouldApplyChanges;

	public void resetToDefault()
	{
		colorComponent.resetToDefault();
		setFont(new Font(defaultName, defaultStyle, defaultSize), colorComponent.color);
	}
	
	static String[] faces = new String[] 
	{
		Font.SERIF, Font.SANS_SERIF, Font.MONOSPACED, Font.DIALOG, Font.DIALOG_INPUT
	};

	public boolean isDefault()
	{
		return useDefault.isSelected();
	}
	
	public void saveSettings(Preferences node)
	{
		node.put(settingName + ".Name", fontName);
		node.putInt(settingName + ".Style", style);
		sliderPanel.saveSettings(node);
		colorComponent.saveSettings(node);
	}
	
	/* 
	 * Uses the supplied defaults if did not obtain valid value from node
	 */
	public void loadSettings(Preferences node)
	{
		useDefault.setSelected(node.getBoolean(settingName + ".useDefault", false));
		colorComponent.loadSettings(node);
		sliderPanel.loadSettings(node);
		
		setFont(new Font(node.get(settingName + ".Name", defaultName), node.getInt(settingName + ".Style", defaultStyle), sliderPanel.getValue()), colorComponent.color);
	}
	
	/**
	 * Currently only called by GridPanel
	 */
	public void setFontIgnoreChanges(Font newFont, Color color)
	{
		shouldApplyChanges = false;
		setFont(newFont, color);
		shouldApplyChanges = true;
	}
	
	public void setFont(Font newFont, Color color)
	{
		this.colorComponent.setColorIgnoreChanges(color);
		int isFace = -1;
		for (int i = 0; i < faces.length; i++)
		{
			if (newFont.getFamily().equals(faces[i]))
			{
				isFace = i;
				break;
			}
		}
		
		if (isFace >= 0)
		{
			fontFaceButton.setSelected(true);
			faceCombo.setEnabled(true);
			familyCombo.setEnabled(false);
			faceCombo.setSelectedIndex(isFace);
			fontName = faceCombo.getItemAt(isFace);
		}
		else
		{
			boolean fontIncluded = false;
			
			for (int i = 0; i < familyCombo.getModel().getSize(); i++)
			{
				if (newFont.getFamily().equals(familyCombo.getItemAt(i)))
				{
					fontFamilyButton.setSelected(true);
					faceCombo.setEnabled(false);
					familyCombo.setEnabled(true);
					familyCombo.setSelectedIndex(i);
					fontName = familyCombo.getItemAt(i);
					fontIncluded = true;
					break;
				}
			}

			if (!fontIncluded) 
			{
				fontFaceButton.setSelected(true);
				faceCombo.setEnabled(true);
				familyCombo.setEnabled(false);
				faceCombo.setSelectedIndex(0); //Serif
				fontName = faces[0];
			}
		}

		if (newFont.getStyle() == Font.PLAIN)
		{
			style = Font.PLAIN;
			bold.setSelected(false);
			italics.setSelected(false);
		}
		else if (newFont.getStyle() == Font.BOLD)
		{
			style = Font.BOLD;
			bold.setSelected(true);
			italics.setSelected(false);
		}
		else if (newFont.getStyle() == Font.ITALIC)
		{
			style = Font.ITALIC;
			bold.setSelected(false);
			italics.setSelected(true);
		}
		else 
		{
			style = Font.BOLD + Font.ITALIC;
			bold.setSelected(true);
			italics.setSelected(true);
		}
		
		sliderPanel.setValue(newFont.getSize());
		setFont();
	}
	
	public void setFont()
	{
		chosenFont = new Font(fontName, style, sliderPanel.getValue());
		if (shouldApplyChanges) settingsPanel.applyChanges();
	}
	
	public void setFontToDefault()
	{
		chosenFont = null;
		settingsPanel.applyChanges();
	}
	
	/**
	 * @param name The title of the TitledBorder around this panel
	 */
	public FontChooserPanel(String name, String settingName, String defaultName, int defaultStyle, int defaultSize, int min, int max, int majorTickSpacing, int minorTickSpacing, Color defaultColor, SettingsPanel settingsPanel, ColorChooserDialogOwner colorChooserDialogOwner)
	{
		long start = System.currentTimeMillis();
		this.name = name;
		
		shouldApplyChanges = false;
		this.defaultName = defaultName;
		this.defaultStyle = defaultStyle;
		this.defaultSize = defaultSize;
		
		this.settingsPanel = settingsPanel;
		this.settingName = settingName;

		this.sliderPanel = new TextSliderPanel(settingName + ".Size", JSlider.HORIZONTAL, "Size", min, max, defaultSize, true, true, majorTickSpacing, minorTickSpacing, true, null, true, true, event ->
		{
			sliderPanel.updateText();
			setFont();
		});
		
		familyCombo = new JComboBox<>();
		
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		String[] fontNames = PreferenceFrame.allFontFamilyNames;
		//69, 59, 73
		for (String s : fontNames)
		{
			model.addElement(s);
		}
		familyCombo.setModel(model);
		faceCombo = new JComboBox<>(faces);
		
		faceCombo.addActionListener(event ->
		{
			fontName = faceCombo.getItemAt(faceCombo.getSelectedIndex());
			setFont();
		});
		
		familyCombo.addActionListener(event ->
		{
			fontName = familyCombo.getItemAt(familyCombo.getSelectedIndex());
			setFont();
		});

		fontFaceButton = new JRadioButton("Font Face");
		fontFaceButton.addActionListener(event ->
		{
			faceCombo.setEnabled(true);
			familyCombo.setEnabled(false);
			fontName = faceCombo.getItemAt(faceCombo.getSelectedIndex());
			setFont();
		});
		fontFamilyButton = new JRadioButton("Font Family");
		fontFamilyButton.addActionListener(event ->
		{
			faceCombo.setEnabled(false);
			familyCombo.setEnabled(true);
			fontName = familyCombo.getItemAt(familyCombo.getSelectedIndex());
			setFont();
		});
		useDefault = new JButton("Set to Look and Feel's Default");
		useDefault.addActionListener(event ->
		{
			setFontToDefault();
		});

		nameButtonGroup = new ButtonGroup();
		nameButtonGroup.add(fontFaceButton);
		nameButtonGroup.add(fontFamilyButton);

		bold = new JCheckBox("Bold");
		bold.addActionListener(event ->
		{
			if (bold.isSelected()) style += Font.BOLD;
			else style -= Font.BOLD;
			setFont();
		});
		
		italics = new JCheckBox("Italics");
		italics.addActionListener(event ->
		{
			if (italics.isSelected()) style += Font.ITALIC;
			else style -= Font.ITALIC;
			setFont();
		});
		
		setLayout(new GridBagLayout());
		
		add(useDefault, new GBC(0, 0, 2, 1).setAnchor(GBC.WEST));
		add(fontFaceButton, new GBC(0, 1).setAnchor(GBC.WEST));
		add(faceCombo, new GBC(1, 1).setAnchor(GBC.WEST));
		add(fontFamilyButton, new GBC(0, 2).setAnchor(GBC.WEST));
		add(familyCombo, new GBC(1, 2).setAnchor(GBC.WEST));

		add(new JPanel() {{add(bold); add(italics);}}, new GBC(2, 1));
		add(sliderPanel, new GBC(2, 0));
		
		colorComponent = new ColorComponent(settingName + ".Color", defaultColor, defaultColor, settingsPanel, colorChooserDialogOwner, "Font Color");
		add(new JPanel () {{add(new JLabel("Color", SwingConstants.CENTER)); add(colorComponent);}}, new GBC(2, 2));
		
		setBorder(BorderFactory.createTitledBorder(name));
		shouldApplyChanges = true;
	}
}