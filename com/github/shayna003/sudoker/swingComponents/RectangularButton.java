package com.github.shayna003.sudoker.swingComponents;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A rectangular JButton that avoids the problem of
 * The UI displaying '...' instead of the supposed text when button's preferred size 
 * is set to be smaller than default size
 * @version 0.00 12-9-2020
 */
@SuppressWarnings("CanBeFinal")
public class RectangularButton extends JButton
{
    public boolean isPressed = false;
    Rectangle2D.Float button = new Rectangle2D.Float();
    int border_width = 1;
    Color borderColor;
    Color color1;
    Color color2;
    Color color1Pressed;
    Color color2Pressed;
    
    @Override 
    public void updateUI()
    {
        super.updateUI();
        updateColors();
    }
    
    void updateColors()
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
    public void setFont(Font font)
    {
        super.setFont(font);
        
        if (button != null)
        {
            Rectangle2D bounds = SwingUtil.getStringBounds(this, getText(), getFont());
            
            if (isSquare())
            {
                int side = bounds.getWidth() >= bounds.getHeight() ? (int) bounds.getWidth() : (int) bounds.getHeight();
                button.setFrame(border_width, border_width, side * 1.5, side * 1.5);
            }
            else
            {
                button.setFrame(border_width, border_width, bounds.getWidth() * 1.5, bounds.getHeight() * 1.5);
            }
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension((int) Math.ceil(button.getWidth() + 2 * border_width), (int) Math.ceil(button.getHeight() + 2 * border_width));
    }
    
    public boolean isSquare()
    {
        return button.getWidth() == button.getHeight(); 
    }
    
    public void setBorderWidth(int width)
    {
        border_width = width;
        setBorder(BorderFactory.createLineBorder(borderColor, border_width));
        button.setFrame(border_width, border_width, button.getWidth(), button.getHeight());
    }

    /**
     * @param square if true, resize button to a square using the
     * larger number of width and height of d,
     * else use width and height of d
     */
    public void setDimension(Dimension d, boolean square)
    {
        if (square)
        {
            int side = Math.max(d.width, d.height);
            button.setFrame(border_width, border_width, side, side);
        }
        else
        {
            button.setFrame(border_width, border_width, d.width, d.height);
        }
        revalidate();
    }
    
    public void unpress()
    {
        isPressed = false;
        repaint();
    }

    public RectangularButton()
    {
        updateColors();
        addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent event)
            {
                if (isEnabled())
                {
                    isPressed = true;
                    repaint();
                }
            }

            public void mouseReleased(MouseEvent event)
            {
                unpress();
            }
        });
    }

    /**
     * @param square if true, make a square button using the
     * larger number of width and height of bounding rectangle of s,
     * else use width and height of bounding rectangle of s
     */
    public RectangularButton(String s, boolean square)
    {
        this();
        setText(s);
        Rectangle2D bounds = SwingUtil.getStringBounds(this, getText(), getFont());
        
        if (square)
        {
            int side = bounds.getWidth() >= bounds.getHeight() ? (int) bounds.getWidth() : (int) bounds.getHeight();
            button.setFrame(border_width, border_width, side * 1.5, side * 1.5);
        }
        else
        {
            button.setFrame(border_width, border_width, bounds.getWidth() * 1.5, bounds.getHeight() * 1.5);
        }
    }

    /**
     * make a square button using side length of @param side
     */
    public RectangularButton(String s, int side)
    {
        this();
        setText(s);
        button.setFrame(border_width, border_width, side * 1.5, side * 1.5);
    }

    /**
     * @param square if true, make a square button using the
     * larger number of width and height of d,
     * else use width and height of d
     */
    public RectangularButton(String s, Dimension d, boolean square)
    {
        this();
        setText(s);
        setDimension(d, square);
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

        if (isPressed)
        {
            g2.setPaint(new GradientPaint(new Point(0, 0), color1Pressed, new Point(0, getHeight()), color2Pressed));
        }
        else
        {

            g2.setPaint(new GradientPaint(new Point(0, 0), color1, new Point(0, getHeight()), color2));
        }
        g2.fill(button);
        getBorder().paintBorder(this, g, (int) Math.ceil(border_width / 2d), (int) Math.ceil(border_width / 2d), (int) button.getWidth() + border_width, (int) button.getHeight() + border_width);

        if (!isEnabled())
        {
            g2.setComposite(SwingUtil.makeComposite(0.3f));
        }

        g2.setColor((Color) UIManager.getLookAndFeelDefaults().get("Button.foreground"));
        Rectangle2D bounds = SwingUtil.getStringBounds(getText(), getFont(), g2.getFontRenderContext());
        g2.drawString(getText(), (int) ((getWidth() - bounds.getWidth()) / 2), (int) (-bounds.getY() + (getHeight() - bounds.getHeight()) / 2));
    }
}
