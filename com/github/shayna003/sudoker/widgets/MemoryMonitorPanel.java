package com.github.shayna003.sudoker.widgets;

import com.github.shayna003.sudoker.swingComponents.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.time.*;

/**
 * This panel monitors the memory usage of the application
 * by plotting the amount of used memory, showing memory information in JLabels,
 * and providing a button that calls {@code System.gc()}.
 * I think it's just the heap memory usage(objects) though
 * @since 3-31-2021
 * last modified: 5-12-2021
 */
@SuppressWarnings("CanBeFinal")
public class MemoryMonitorPanel extends JPanel
{
	MemoryGraphPanel memoryGraphPanel;

	JLabel usedMemoryLabel;
	JLabel freeMemoryLabel;
	JLabel totalMemoryLabel;
	JLabel totalFreeMemoryLabel;
	JLabel maxMemoryLabel;
	
	JButton performGC;

	void updateColors()
	{
		Color tmp = UIManager.getColor("Panel.background");
		Color tmp2 = tmp == null ? Color.WHITE : tmp.brighter();
		memoryGraphPanel.graph_color = tmp2;
		memoryGraphPanel.grid_color = tmp2.darker();
		memoryGraphPanel.repaint();
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		if (memoryGraphPanel != null)
		{
			updateColors();
		}
	}
	
	@SuppressWarnings("CanBeFinal")
	public enum MemoryUnit
	{
		BYTE(1, "Byte"), KILOBYTE(1024, "KB"), MEGABYTE(KILOBYTE.bytes * KILOBYTE.bytes, "MB"), GIGABYTE(KILOBYTE.bytes * MEGABYTE.bytes, "GB"), TERABYTE(KILOBYTE.bytes * GIGABYTE.bytes, "TB");
		
		long bytes;
		String abbreviation;
		
		MemoryUnit(long bytes, String abbreviation)
		{
			this.bytes = bytes;
			this.abbreviation = abbreviation;
		}
	}
	
	static class MemoryValue
	{
		long bytes;
		double value; // in the unit MemoryUnit
		MemoryUnit unit;
		
		MemoryValue() { }
		
		public MemoryValue(long bytes)
		{
			setBytes(bytes);
		}
		
		public MemoryValue(double value, MemoryUnit unit)
		{
			this.value = value;
			this.unit = unit;
			this.bytes = Math.round(value * unit.bytes);
		}
		
		@Override 
		public String toString()
		{
			return String.format("%.2f %s", value, unit.abbreviation);
		}
		
		public void setValue(double newValue)
		{
			if (value != newValue)
			{
				value = newValue;
				bytes = (long) (newValue * unit.bytes);
			}
		}
		
		public void setUnit(MemoryUnit newUnit)
		{
			if (newUnit != unit)
			{
				this.unit = newUnit;
				this.value = bytes / (double) newUnit.bytes;
			}
		}
		
		public void setBytesAndUnit(long bytes, MemoryUnit newUnit)
		{
			this.bytes = bytes;
			this.unit = newUnit;
			this.value = bytes / (double) newUnit.bytes;
		}
		
		/**
		 * Round value to the nearest integer >= value
		 * Not used
		 */
		public void roundUp()
		{
			value = Math.ceil(value);
			bytes = Math.round(value * unit.bytes);
		}
		
		/**
		 * Round value to the nearest integer <= value
		 * Not used
		 */
		public void roundDown()
		{
			value = Math.floor(value);
			bytes = Math.round(value * unit.bytes);
		}
		
		public void setBytes(long bytes)
		{
			this.bytes = bytes; 
			if (bytes >= MemoryUnit.TERABYTE.bytes)
			{
				this.unit = MemoryUnit.TERABYTE;
				this.value = (double) (bytes) / MemoryUnit.TERABYTE.bytes;
			}
			else if (bytes >= MemoryUnit.GIGABYTE.bytes)
			{
				this.unit = MemoryUnit.GIGABYTE;
				this.value = (double) (bytes) / MemoryUnit.GIGABYTE.bytes;
			}
			else if (bytes >= MemoryUnit.MEGABYTE.bytes)
			{
				this.unit = MemoryUnit.MEGABYTE;
				this.value = (double) (bytes) / MemoryUnit.MEGABYTE.bytes;
			}
			else if (bytes >= MemoryUnit.KILOBYTE.bytes)
			{
				this.unit = MemoryUnit.KILOBYTE;
				this.value = (double) (bytes) / MemoryUnit.KILOBYTE.bytes;
			}
			else
			{
				this.unit = MemoryUnit.BYTE;
				this.value = bytes;
			}
		}
	}
	
	public MemoryMonitorPanel()
	{
		super(new BorderLayout());

		memoryGraphPanel = new MemoryGraphPanel();
		updateColors();
		memoryGraphPanel.setBorder(BorderFactory.createTitledBorder("Used Memory"));
		add(memoryGraphPanel, BorderLayout.CENTER);

		JPanel labelPanel = new JPanel(new GridBagLayout());
		usedMemoryLabel = new JLabel("Used Memory: ");
		freeMemoryLabel = new JLabel("Allocated Free Memory: ");
		totalMemoryLabel = new JLabel("Allocated Memory: ");
		totalFreeMemoryLabel = new JLabel("Total Available Free Memory: ");
		maxMemoryLabel = new JLabel("Total Memory Designated to JVM: ");
		
		labelPanel.add(usedMemoryLabel, new GBC(0, 0).setAnchor(GBC.WEST).setInsets(5));
		labelPanel.add(freeMemoryLabel, new GBC(0, 1).setAnchor(GBC.WEST).setInsets(5));
		labelPanel.add(totalMemoryLabel, new GBC(0, 2).setAnchor(GBC.WEST).setInsets(5));
		labelPanel.add(totalFreeMemoryLabel, new GBC(0, 3).setAnchor(GBC.WEST).setInsets(5));
		labelPanel.add(maxMemoryLabel, new GBC(0, 4).setAnchor(GBC.WEST).setInsets(5));
		
		performGC = new JButton("Perform Garbage Collection");
		performGC.setToolTipText("Call System.gc() to free up some memory");
		performGC.addActionListener(event ->
		{
			System.gc();
		});
		labelPanel.add(performGC, new GBC(0, 5));
		add(labelPanel, BorderLayout.EAST);
		
		memoryGraphPanel.timer.start();
	}
	
	/**
	 * Contains a MemoryGraph and paints y-axis and x-axis labels around it,
	 * along with a Label with text "Used: xxx" at the right side of the graph
	 */
	@SuppressWarnings("CanBeFinal")
	class MemoryGraphPanel extends JPanel
	{
		@SuppressWarnings("CanBeFinal")
		class DataPoint
		{
			MemoryValue value;
			Point point;
			
			DataPoint(MemoryValue value, Point point)
			{
				this.value = value;
				this.point = point;
			}
			
			/**
			 * Not used
			 */
			@Override
			public String toString()
			{
				return "DataPoint[value=" + value + ",point=" + point + "]";
			}
		}
		
		@SuppressWarnings("CanBeFinal")
		class TimeStamp
		{
			LocalTime time;
			int x_index; // that corresponds to a data point, a value between 0 and DATA_POINTS
			
			public TimeStamp(int x_index)
			{
				time = LocalTime.now();
				this.x_index = x_index;
			}
			
			@Override
			public String toString()
			{
				return String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
			}
		}
		
		static final int DATA_POINTS = 51; // it's preferred that GRAPH_WIDTH is divisible by (DATA_POINTS - 1)
		MemoryGraph memoryGraph;
		
		//regarding y_axis_marks;
		public static final int MAX_NUMBER_OF_MARKS = 10;
		ArrayList<MemoryValue> y_axis_marks;
		final double[] Y_AXIS_SNAPS = new double[] { 1000, 500, 200, 100, 50, 25, 10, 5, 2, 1, 0.5, 0.25, 0.2, 0.1, 0.05 };
		
		// index of currently chosen y axis interval in Y_AXIS_SNAPS
		int snap_index;
		
		// distance between graph's left and y-axis mark's right, also the distance between left of y-axis mark and right of y-axis label text
		static final int Y_AXIS_MARK_GAP = 8;
		static final int Y_AXIS_MARK_LENGTH = 10;
		
		// gap between the first axis mark and the top and bottom edge of the graph
		static final int Y_INSETS = 10;
		
		// distance between graph's bottom and x-axis mark's top, also the distance between bottom of x-axis mark and top of x-axis label text
		static final int X_AXIS_MARK_GAP = 12;
		static final int X_AXIS_MARK_LENGTH = 10;
		
		static final int USED_LABEL_GAP = 10; // gap between graph's right edge and left of "Used: xxx" label
		
		// space between left of panel and left of graph
		int GRAPH_X;//= 100;
		
		// space between top of panel and top of graph
		static final int GRAPH_Y = 30;
		
		// size of graph
		static final int GRAPH_WIDTH = 400;
		static final int GRAPH_HEIGHT = 300;
		
		final Color USED_MEMORY_COLOR = new Color(250, 65, 90);
		Color grid_color;
		Color graph_color;
		final Stroke STROKE = new BasicStroke(1);
		
		ArrayDeque<DataPoint> usedMemory;
		ArrayDeque<TimeStamp> timeStamps;
		
		// timer that keeps on sampling points to be added to the graph, and updates label texts
		Timer timer;
		
		static final int SAMPLE_DELAY = 500; // in milliseconds, time interval between adding new points to the graph
		static final int X_AXIS_TIME_INTERVAL = 5000; // in milliseconds, should be larger than and divisible by SAMPLE_DELAY, time interval between adding a time stamp to the graph
		int points_sampled = (X_AXIS_TIME_INTERVAL / SAMPLE_DELAY) - 1; // used to determine when a new time stamp should be added
		
		// to configure preferredSize
		int max_y_axis_label_width;
		int max_x_axis_label_height;
		int max_used_label_width;
		
		// min and max of y_axis_marks
		MemoryValue y_axis_max;
		MemoryValue y_axis_min;
		boolean needToReconfigureYAxis;
		
		// min and max point values
		long max = Long.MIN_VALUE;
		long min = Long.MAX_VALUE;
		
		// bevel border of MemoryGraph
		Border border;
		Insets border_insets;
		
		public Dimension getPreferredSize()
		{
			computeSizes();
			
			return new Dimension(memoryGraph.getPreferredSize().width + GRAPH_X + (int) ((max_used_label_width + 1) * 1.5) + USED_LABEL_GAP, memoryGraph.getPreferredSize().height + GRAPH_Y + (int) ((max_x_axis_label_height + 1) * 1.5) + X_AXIS_MARK_LENGTH + X_AXIS_MARK_GAP * 2);
		}
		
		void computeSizes()
		{
			FontRenderContext context = getFontMetrics(getFont()).getFontRenderContext();
			Rectangle2D bounds = getFont().getStringBounds("999 Bytes", context);
			max_y_axis_label_width = (int) bounds.getWidth();
			bounds = getFont().getStringBounds("24:00:00", context);
			max_x_axis_label_height = (int) bounds.getHeight();
			bounds = getFont().getStringBounds("Used: 999.00 Bytes", context);
			max_used_label_width = (int) bounds.getWidth();
			
			GRAPH_X = (int) ((max_y_axis_label_width + 1) * 1.5) + Y_AXIS_MARK_LENGTH + Y_AXIS_MARK_GAP * 2;
		}
		
		public String getLessPreciseString(MemoryValue mv)
		{
			if (snap_index < 10) // y_axis_marks are whole numbers
			{
				return ((int) (mv.value)) + " " + mv.unit.abbreviation;
			}
			else if (snap_index != 11 && snap_index != 14) // 1 decimal place
			{
				return String.format("%.1f %s", mv.value, mv.unit.abbreviation);
			}
			else // 2 decimal places
			{
				return this.toString();
			}
		}
		
		public MemoryGraphPanel()
		{
			super();
			setLayout(null);
			memoryGraph = new MemoryGraph();
			computeSizes();
			memoryGraph.setBounds(GRAPH_X, GRAPH_Y, memoryGraph.getPreferredSize().width, memoryGraph.getPreferredSize().height);
			add(memoryGraph);
		}
		
		void configureYAxisMax()
		{
			if (y_axis_max == null || y_axis_max.bytes < max || y_axis_max.bytes - max > (max - min) * 2)
			{
				// use max + 1 to avoid division by 0 in xxx / (y_axis_max - y_axis_min)
				if (y_axis_max == null)
				{
					y_axis_max = new MemoryValue(max + 1);
				}
				else 
				{
					y_axis_max.setBytes(max + 1);
				}
				
				if (y_axis_min != null) y_axis_min.setUnit(y_axis_max.unit);
				needToReconfigureYAxis = true;
			}
		}
		
		/**
		 * y_axis_min's unit is always set to the same as y_axis_max's unit
		 */
		void configureYAxisMin()
		{
			if (y_axis_min == null || y_axis_min.bytes > min || min - y_axis_min.bytes > (max - min) * 2)
			{
				if (y_axis_min == null)
				{
					y_axis_min = new MemoryValue();
				}
				y_axis_min.setBytesAndUnit(min, y_axis_max.unit);
				needToReconfigureYAxis = true;
			}
		}
		
		/**
		* Round up the y_axis_max and round down the y_axis_min
		* According to Y_AXIS_SNAPS
		*/
		void compute_y_axis_marks()
		{	
			double range = y_axis_max.value - y_axis_min.value;
			double new_max;
			double new_min;
			
			for (snap_index = 4; snap_index < Y_AXIS_SNAPS.length; snap_index++)
			{
				if (range > Y_AXIS_SNAPS[snap_index])
				{
					if (snap_index < Y_AXIS_SNAPS.length - 1) snap_index++;
					break;
				}
			}

			int number_of_marks;
			if (snap_index < Y_AXIS_SNAPS.length) snap_index++;
			do
			{
				if (snap_index > 0) snap_index--;
				new_max = y_axis_max.value + Y_AXIS_SNAPS[snap_index] - (y_axis_max.value % Y_AXIS_SNAPS[snap_index]);
				new_min = y_axis_min.value - (y_axis_min.value % Y_AXIS_SNAPS[snap_index]);

				number_of_marks = (int) (1 + Math.round((new_max - new_min) / Y_AXIS_SNAPS[snap_index]));
			}
			while(number_of_marks > MAX_NUMBER_OF_MARKS && snap_index > 0);
			
			y_axis_max.setValue(new_max);
			y_axis_min.setValue(new_min);
			
			MemoryUnit unit = y_axis_max.unit;

			y_axis_marks = new ArrayList<MemoryValue>(number_of_marks);
			for (int i = 0; i < number_of_marks; i++)
			{
				y_axis_marks.add(new MemoryValue(new_min + i * Y_AXIS_SNAPS[snap_index], unit));
			}
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			if (usedMemory.size() > 1)
			{
				Graphics2D g2 = (Graphics2D) g;
				
				for (int i = y_axis_marks.size() - 1; i >= 0; i--)
				{
					String message = getLessPreciseString(y_axis_marks.get(i));

					FontRenderContext context = g2.getFontRenderContext();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
					double ascent = -bounds.getY();
					int y = GRAPH_Y + border_insets.top + Y_INSETS + (GRAPH_HEIGHT - Y_INSETS * 2) / (y_axis_marks.size() - 1) * (y_axis_marks.size() - i - 1);
					
					g2.drawString(message, (float) (GRAPH_X - Y_AXIS_MARK_GAP * 2 - Y_AXIS_MARK_LENGTH - bounds.getWidth()), (float) (ascent + y - bounds.getHeight() / 2));
					
					g2.drawLine(GRAPH_X - Y_AXIS_MARK_GAP - Y_AXIS_MARK_LENGTH, y, GRAPH_X - Y_AXIS_MARK_GAP, y);
				}
				//draw vertical bar for y-axis labels
				g2.drawLine(GRAPH_X - Y_AXIS_MARK_GAP - Y_AXIS_MARK_LENGTH / 2, GRAPH_Y + border_insets.top + Y_INSETS, GRAPH_X - Y_AXIS_MARK_GAP - Y_AXIS_MARK_LENGTH / 2, GRAPH_Y + border_insets.top + GRAPH_HEIGHT - Y_INSETS - 1);
				
				// draw horizontal bar for x-axis labels
				g2.drawLine(GRAPH_X + border_insets.left, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_LENGTH / 2 + X_AXIS_MARK_GAP, GRAPH_X + border_insets.left + GRAPH_WIDTH - border_insets.right, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_LENGTH / 2 + X_AXIS_MARK_GAP);
				
				// draw 2 ends of x-axis labels
				g2.drawLine(GRAPH_X + border_insets.left, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP, GRAPH_X + border_insets.left, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP + X_AXIS_MARK_LENGTH);
				
				g2.drawLine(GRAPH_X + GRAPH_WIDTH, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP, GRAPH_X + GRAPH_WIDTH, GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP + X_AXIS_MARK_LENGTH);
				
				// draw x-axis marks
				Iterator<TimeStamp> iter = timeStamps.iterator();
				TimeStamp ts;
				while (iter.hasNext())
				{
					ts = iter.next();

					g2.drawLine(GRAPH_X + border_insets.left + (GRAPH_WIDTH / (DATA_POINTS - 1) * ts.x_index), GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP, GRAPH_X +  + border_insets.left + (GRAPH_WIDTH / (DATA_POINTS - 1) * ts.x_index), GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP + X_AXIS_MARK_LENGTH);
					
					String message = ts.toString();
					FontRenderContext context = g2.getFontRenderContext();
					Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
					double ascent = -bounds.getY();
					
					g2.drawString(message, (float) (GRAPH_X + border_insets.left + (GRAPH_WIDTH / (DATA_POINTS - 1) * ts.x_index) - bounds.getWidth() / 2), (float) (ascent + X_AXIS_MARK_GAP + GRAPH_Y + GRAPH_HEIGHT + X_AXIS_MARK_GAP));
				}
				
				g2.setColor(USED_MEMORY_COLOR);

				String message = "Used: " + usedMemory.getLast().value;
				FontRenderContext context = g2.getFontRenderContext();
				Rectangle2D bounds = g2.getFont().getStringBounds(message, context);
				double ascent = -bounds.getY();
				g2.drawString(message, (float) GRAPH_X + GRAPH_WIDTH + USED_LABEL_GAP, (float) (GRAPH_Y + usedMemory.getLast().point.y + ascent - bounds.getHeight() / 2));
			}
		}
		
		class MemoryGraph extends JComponent
		{	
			void configureMaxMin()
			{
				min = Long.MAX_VALUE;
				max = Long.MIN_VALUE;
				Iterator<DataPoint> iter = usedMemory.iterator();
				DataPoint dp;
				while (iter.hasNext())
				{
					dp = iter.next();
					if (dp.value.bytes > max)
					{
						max = dp.value.bytes;
					}
					if (dp.value.bytes < min)
					{
						min = dp.value.bytes;
					}
				}
			}
			
			void addPoint(long usedMemoryBytes)
			{	
				needToReconfigureYAxis = false;
				
				if (usedMemory.size() == DATA_POINTS)
				{
					usedMemory.removeFirst();
					
					// x-axis marks only start moving left when the points start moving left
					if (timeStamps.size() > 0)
					{
						TimeStamp tmp = timeStamps.getFirst();
						if (tmp.x_index <= 1)
						{
							timeStamps.removeFirst();
						}
					}
					Iterator<TimeStamp> iter = timeStamps.iterator();
					while (iter.hasNext())
					{
						iter.next().x_index--;
					}
				}
				
				configureMaxMin();
				if (usedMemoryBytes > max)
				{
					max = usedMemoryBytes;
				}
				
				if (usedMemoryBytes < min)
				{
					min = usedMemoryBytes;
				}
				
				configureYAxisMax();
				configureYAxisMin();

				if (needToReconfigureYAxis)
				{
					compute_y_axis_marks();
				}
				
				if (needToReconfigureYAxis)
				{
					reconfigurePoints(usedMemory);
				}
				else if (usedMemory.size() == DATA_POINTS - 1)
				{
					reconfigureXCoordinates(usedMemory);
				}
				
				usedMemory.add(new DataPoint(new MemoryValue(usedMemoryBytes), new Point((int) (GRAPH_WIDTH / (DATA_POINTS - 1.f) * usedMemory.size()), (int) (GRAPH_HEIGHT - Y_INSETS - (GRAPH_HEIGHT - 2 * Y_INSETS) * (usedMemoryBytes - y_axis_min.bytes) / (y_axis_max.bytes - y_axis_min.bytes)))));
			}
			
			void reconfigureXCoordinates(ArrayDeque<DataPoint> points)
			{
				Iterator<DataPoint> iter = points.iterator();
				int index = 0;
				while (iter.hasNext())
				{
					DataPoint p = iter.next();
					p.point.setLocation(GRAPH_WIDTH / (DATA_POINTS - 1) * index, p.point.y);
					index++;
				}
			}
			
			// concurrency issues?
			void reconfigurePoints(ArrayDeque<DataPoint> points)
			{
				Iterator<DataPoint> iter = points.iterator();
				int index = 0;
				while (iter.hasNext())
				{
					DataPoint p = iter.next();
					p.point.setLocation(GRAPH_WIDTH / (DATA_POINTS - 1) * index, (int) (GRAPH_HEIGHT - Y_INSETS - (GRAPH_HEIGHT - 2 * Y_INSETS) * (p.value.bytes - y_axis_min.bytes) / (y_axis_max.bytes - y_axis_min.bytes)));
					index++;
				}
			}
			
			public MemoryGraph()
			{
				usedMemory = new ArrayDeque<DataPoint>(DATA_POINTS);
				timeStamps = new ArrayDeque<TimeStamp>(DATA_POINTS / (X_AXIS_TIME_INTERVAL / SAMPLE_DELAY));
				
				timer = new Timer(SAMPLE_DELAY, event ->
				{
					points_sampled++;
					if (points_sampled == (X_AXIS_TIME_INTERVAL / SAMPLE_DELAY))
					{
						points_sampled = 0;
						timeStamps.add(new TimeStamp(usedMemory.size()));
					}
					
					Runtime r = Runtime.getRuntime();
					long max = r.maxMemory();
					long total = r.totalMemory();
					long free = r.freeMemory();
					long used = total - free;
					
					addPoint(used);
					MemoryGraphPanel.this.repaint();
					
					usedMemoryLabel.setText("Used Memory: " + new MemoryValue(used));
					totalMemoryLabel.setText("Allocated Memory: " + new MemoryValue(total));
					freeMemoryLabel.setText("Allocated Free Memory: " + new MemoryValue(free));
					totalFreeMemoryLabel.setText("Total Available Free Memory: " + new MemoryValue(max - used));
					maxMemoryLabel.setText("Total Memory Designated to JVM: " + new MemoryValue(max));
					
				});

				border = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
				setBorder(border);
				border_insets = border.getBorderInsets(MemoryGraph.this);
			}
			
			@Override
			public Dimension getPreferredSize()
			{	
				return new Dimension(GRAPH_WIDTH + border_insets.left + border_insets.right, GRAPH_HEIGHT + border_insets.top + border_insets.bottom);
			}
			
			@Override
			public void paintComponent(Graphics g)
			{
				g.setColor(graph_color);
				g.fillRect(border_insets.left, border_insets.top, GRAPH_WIDTH, GRAPH_HEIGHT);
				if (usedMemory.size() > 1)
				{
					Graphics2D g2 = (Graphics2D) g;
					g2.setStroke(STROKE);
					g2.setColor(grid_color);
					for (int y = 0; y < y_axis_marks.size() ; y++)
					{
						g2.drawLine(border_insets.left, border_insets.top + Y_INSETS + (GRAPH_HEIGHT - Y_INSETS * 2) / (y_axis_marks.size() - 1) * y, GRAPH_WIDTH, border_insets.top + Y_INSETS + (GRAPH_HEIGHT - Y_INSETS * 2) / (y_axis_marks.size() - 1) * y);
					}
					
					Iterator<TimeStamp> iter = timeStamps.iterator();
					TimeStamp ts;
					while (iter.hasNext())
					{
						ts = iter.next();

						g2.drawLine(border_insets.left + (GRAPH_WIDTH / (DATA_POINTS - 1) * ts.x_index), border_insets.top + Y_INSETS, border_insets.left + (GRAPH_WIDTH / (DATA_POINTS - 1) * ts.x_index), border_insets.top + GRAPH_HEIGHT - Y_INSETS - 1);
					}
					
					g2.setColor(USED_MEMORY_COLOR);
					Iterator<DataPoint> iter2 = usedMemory.iterator();
					DataPoint previousPoint = iter2.next();
					DataPoint dataPoint;
					while (iter2.hasNext())
					{
						dataPoint = iter2.next();
						g2.drawLine(previousPoint.point.x, previousPoint.point.y, dataPoint.point.x, dataPoint.point.y);
						previousPoint = dataPoint;
					}
				}
			}
		}
	}
	
	/**
	 * @return Total used memory in bytes
	 */
	public static long getUsedMemory()
	{
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}
	
	/**
	 * @return Total free memory in bytes
	 */
	public static long getTotalFreeMemory()
	{
		return Runtime.getRuntime().maxMemory() - getUsedMemory();
	}
	
	public static JFrame getMemoryMonitorFrame()
	{
		JFrame f = new JFrame("Memory Monitor")
		{
			/**
			* Things don't fit on the screen if you call JFrame.setDefaultLookAndFeelDecorated(true) 
			* When the look and feel is Metal
			*/
			@Override
			public Dimension getPreferredSize()
			{
				Dimension d = super.getPreferredSize();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Insets i = getToolkit().getScreenInsets(getGraphicsConfiguration());
				int insetsWidth = i.left + i.right;
				int insetsHeight = i.top + i.bottom;
				return new Dimension(d.width + insetsWidth > screenSize.width ? screenSize.width - insetsWidth : d.width, d.height + insetsHeight > screenSize.height ? screenSize.height - insetsHeight : d.height);
			}
		};
		MemoryMonitorPanel p = new MemoryMonitorPanel();
		f.add(p);
		f.pack();
		return f;
	}
}