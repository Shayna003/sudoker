package com.github.shayna003.sudoker.swingComponents;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A elliptical button mainly used for the '?' help buttons
 * @version 0.00 
 * @since 12-9-2020
 * last modified: 3-22-2021
 */
@SuppressWarnings("CanBeFinal")
public class RoundButton extends JButton
{
    boolean isPressed = false;
    public Ellipse2D.Float border = new Ellipse2D.Float();
    public Ellipse2D.Float button = new Ellipse2D.Float();
    public int border_width = 1;
    
    Color borderColor;
    Color color1;
    Color color2;
    Color color1Pressed;
    Color color2Pressed;
    
    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        if (border != null)
        {
            setButtonSize();
        }
    }
    
    public void setButtonSize()
    {
        Rectangle2D bounds = SwingUtil.getStringBounds(this, getText(), getFont());
        
        if (isCircular())
        {
            double diameter = Math.max(bounds.getWidth(), bounds.getHeight());
            button.setFrame(border_width, border_width, diameter * 1.5, diameter * 1.5);
            border.setFrame(0, 0, diameter * 1.5 + border_width * 2, diameter * 1.5 + border_width * 2);
        }
        else 
        {
            button.setFrame(border_width, border_width, bounds.getWidth(), bounds.getHeight());
            border.setFrame(0, 0, bounds.getWidth() + border_width * 2, bounds.getHeight() + border_width * 2);
        }
    }
    
    @Override 
    public void updateUI()
    {
        super.updateUI();
        updateColors();
    }
    
    public void updateColors()
    {
        Color tmp = UIManager.getColor("Button.background");
        color1 = tmp == null ? new Color(198, 198, 198) : SwingUtil.deriveColor(tmp, 0.9f);
        color2 = tmp == null ? new Color(220, 220, 220) : SwingUtil.deriveColor(tmp, 1.1f);
        color1Pressed = SwingUtil.deriveColor(color1, 0.9f);
        color2Pressed = SwingUtil.deriveColor(color2, 0.9f);
        
        tmp = UIManager.getColor("Panel.background");
        borderColor = tmp == null ? new Color(150, 150, 150) : SwingUtil.deriveColor(tmp, 0.8f);
        setBorder(BorderFactory.createLineBorder(borderColor, border_width));
    }
    
    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }
    
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension((int) Math.ceil(border.getWidth()), (int) Math.ceil(border.getHeight()));
    }
    
    public boolean isCircular()
    {
        return border.getWidth() == border.getHeight();
    }

    /**
     * Border inclusive
     */
    public boolean contains(Point p)
    {
        return border.contains(p.getX(), p.getY());
    }
    
    public void setBorderWidth(int width)
    {
        border_width = width;
        button.setFrame(border_width, border_width, button.getWidth(), button.getHeight());
        border.setFrame(0, 0, button.getWidth() + border_width * 2, button.getHeight() + border_width * 2);
    }
    
    /**
     * @param circular if true, resize button to a circle using the
     * larger number of width and height of d,
     * else use width and height of d
     */
    public void setDimension(Dimension d, boolean circular)
    {
        if (circular)
        {
            int diameter = Math.max(d.width, d.height);
            button.setFrame(border_width, border_width, diameter, diameter);
            border.setFrame(0, 0, diameter + border_width * 2, diameter + border_width * 2);
        }
        else 
        {
            button.setFrame(border_width, border_width, d.width, d.height);
            border.setFrame(0, 0, d.width + border_width * 2, d.height + border_width * 2);
        }
        revalidate();
    }

    public RoundButton()
    {
        updateColors();
        addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent event)
            {
                isPressed = true;
                repaint();
            }

            public void mouseReleased(MouseEvent event)
            {
                isPressed = false;
                repaint();
            }
        });
    }

    /**
     * @param circular if true, make a circular button using the
     * larger number of width and height of bounding rectangle of s,
     * else use width and height of bounding rectangle of s
     */
    public RoundButton(String s, boolean circular)
    {
        this();
        setText(s);

        Rectangle2D bounds = SwingUtil.getStringBounds(this, getText(), getFont());
        
        if (circular)
        {
            double diameter = Math.max(bounds.getWidth(), bounds.getHeight());
            button.setFrame(border_width, border_width, diameter * 1.5, diameter * 1.5);
            border.setFrame(0, 0, diameter * 1.5 + border_width * 2, diameter * 1.5 + border_width * 2);
        }
        else 
        {
            button.setFrame(border_width, border_width, bounds.getWidth(), bounds.getHeight());
            border.setFrame(0, 0, bounds.getWidth() + border_width * 2, bounds.getHeight() + border_width * 2);
        }
    }

    /**
     * makes a circular button with radius as @param radius
     */
    public RoundButton(String s, float radius)
    {
        this();
        setBorderPainted(false);
        button.setFrame(border_width, border_width, radius * 2,radius * 2);
        border.setFrame(0, 0, radius * 2 + border_width * 2, radius * 2 + border_width * 2);
    }
    
    /**
     * makes a circular button with diameter as @param diameter
     */
    public RoundButton(String s, int diameter)
    {
        this();
        button.setFrame(border_width, border_width, diameter, diameter);
        border.setFrame(0, 0, diameter + border_width * 2,  diameter + border_width * 2);
    }
    
    /**
     * @param circular if true, make a circular button using the
     * larger number of width and height of d,
     * else use width and height of d
     */
    public RoundButton(String s, Dimension d, boolean circular)
    {
        this();
        setText(s);
        setDimension(d, circular);
    }

    /**
     * To get a consistent look across different look and feels
     */
    @Override
    public void paint(Graphics g)
    {
        paintComponent(g);
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        
        // this makes the text appear centered
        g2.setFont(new Font(Font.MONOSPACED, getFont().getStyle(), getFont().getSize()));
        g2.setColor(borderColor);

        g2.fill(border);
        if (isPressed)
        {
            g2.setPaint(new GradientPaint(new Point(0, 0), color1Pressed, new Point(0, getHeight()), color2Pressed));
        }
        else
        {
            g2.setPaint(new GradientPaint(new Point(0, 0), color1, new Point(0, getHeight()), color2));
        }
        g2.fill(button);
        g2.setColor((Color) UIManager.getLookAndFeelDefaults().get("Button.foreground"));

        Rectangle2D bounds = SwingUtil.getStringBounds(getText(), getFont(), g2.getFontRenderContext());
        g2.drawString(getText(), (int) (border_width + (button.getWidth() - bounds.getWidth()) / 2), (int) (border_width + -bounds.getY() + (button.getHeight() - bounds.getHeight()) / 2));
    }
}
