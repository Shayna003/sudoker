package com.github.shayna003.sudoker;

import javax.swing.*;
import java.awt.*;

/**
 * @since 6-3-2021
 */
public class AboutDialog extends JFrame
{
	public JLabel titleLabel;
    public JLabel versionLabel;
    public JLabel authorLabel;
    public JLabel dateLabel;
    public JLabel websiteLabel;

    public AboutDialog()
    {
        setTitle("About Sudoker");

        titleLabel = new JLabel(Application.application_name);
        versionLabel = new JLabel("Version: " + Application.application_version);
        authorLabel = new JLabel("Created By: Shayna Xu");
        dateLabel = new JLabel("Date: 6/3/2021");
        websiteLabel = new JLabel("Website: https://shayna003.github.io/sudoker");

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(titleLabel);
        contentPanel.add(versionLabel);
        contentPanel.add(authorLabel);
        contentPanel.add(dateLabel);
        contentPanel.add(websiteLabel);

        int insets = 20;
        contentPanel.setBorder(BorderFactory.createEmptyBorder(insets, insets, insets, insets));

        add(contentPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public void showUp()
    {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}