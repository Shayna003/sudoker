package com.github.shayna003.sudoker.swingComponents;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 *
 * last modified: 3-22-2021
 */
@SuppressWarnings("CanBeFinal")
public class DropDownPanel extends JPanel
{
	int outerInsets = 5;
	String title;
	JLabel titleLabel;
	BasicArrowButton arrowButton;
	boolean collapsed;
	JPanel titlePanel;
	Component content;

	public static final String COLLAPSED_STATE_CHANGE = "isCollapsed";
	@Override
	public Dimension getPreferredSize()
	{
		Insets insets = getBorder().getBorderInsets(DropDownPanel.this);
		int right = insets.right;
		int left = insets.left;
		int top = insets.top;
		int bottom = insets.bottom;
		return new Dimension(content.getPreferredSize().width + left + right, content.isVisible() ? content.getPreferredSize().height + titlePanel.getPreferredSize().height + top + bottom : titlePanel.getPreferredSize().height + top + bottom);
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (titlePanel != null)
		{
			setBackgrounds();
			titleLabel.setFont(UIManager.getFont("TitledBorder.font"));
			titleLabel.setForeground(UIManager.getColor("TitledBorder.titleColor"));
		}	
	}
	
	void setBackgrounds()
	{
		Color titlePanelBackground = UIManager.getColor("Panel.background");//Color.LIGHT_GRAY;
		Color c = titlePanelBackground == null ? Color.LIGHT_GRAY : SwingUtil.deriveColor(titlePanelBackground, 0.8f);
		titlePanel.setBackground(c);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(outerInsets, outerInsets, outerInsets, outerInsets), BorderFactory.createLineBorder(c)));
	}
	
	public DropDownPanel(String title, Component content)
	{
		this.title = title;
		titleLabel = new JLabel(title, SwingConstants.CENTER);
		titleLabel.setFont(UIManager.getFont("TitledBorder.font"));
		titleLabel.setForeground(UIManager.getColor("TitledBorder.titleColor"));
		
		collapsed = false;
		titlePanel = new JPanel(new BorderLayout());
		
		setBackgrounds();
		arrowButton = new BasicArrowButton(SwingConstants.SOUTH);
		
		arrowButton.addActionListener(event ->
		{
			collapsed = !collapsed;
			firePropertyChange(COLLAPSED_STATE_CHANGE, !collapsed, collapsed);
			arrowButton.setDirection(collapsed ? SwingConstants.EAST : SwingConstants.SOUTH);
			content.setVisible(!collapsed);
		});
		
		content.addPropertyChangeListener("preferredSize", new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				revalidate();
			}
		});
		
		if (content instanceof Container)
		{
			((Container) content).addContainerListener(new ContainerListener()
			{
				@Override
				public void componentAdded(ContainerEvent event)
				{
					revalidate();
				}
				
				@Override
				public void componentRemoved(ContainerEvent event)
				{
					revalidate();
				}
			});
		}

		content.addComponentListener(new ComponentListener() 
		{
			@Override
			public void componentResized(ComponentEvent event)
			{
				revalidate();
			}
			
			@Override
			public void componentMoved(ComponentEvent event)
			{
				revalidate();
			}
			
			@Override
			public void componentShown(ComponentEvent event)
			{
				revalidate();
			}
			
			@Override
			public void componentHidden(ComponentEvent event)
			{
				revalidate();
			}
		});
		
		titlePanel.add(arrowButton, BorderLayout.WEST);
		titlePanel.add(titleLabel, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(titlePanel, BorderLayout.NORTH);
		this.content = content;
		add(content, BorderLayout.CENTER);
	}
}