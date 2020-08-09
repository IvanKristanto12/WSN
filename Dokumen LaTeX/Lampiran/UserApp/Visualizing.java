package UserApp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * 
 * @author Ivan Kristanto <ivankristanto12@gmail.com>
 *
 */
public class Visualizing extends JPanel {

	public volatile boolean canPlot;
	public boolean showed;

	public Plotting plotMaker;
	public JFrame frame;
	public JFreeChart chart;
	public XYPlot plot;
	public String sensorId;

	private TimeSeries senseResult;

	public Visualizing(String sensorId) {
		super(new BorderLayout());
		this.plotMaker = new Plotting();
		this.sensorId = sensorId;
		showed = false;

		this.senseResult = new TimeSeries("Acceleration");
		this.senseResult.setMaximumItemAge(10000);

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(this.senseResult);

		DateAxis domain = new DateAxis("Time");
		NumberAxis range = new NumberAxis("Amplitude");
		domain.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		range.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
		domain.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));
		range.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));

		XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
		renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.GREEN);

		renderer.setSeriesStroke(0, new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		renderer.setSeriesStroke(1, new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		plot = new XYPlot(dataset, domain, range, renderer);
		domain.setAutoRange(true);
		domain.setLowerMargin(0.0);
		domain.setUpperMargin(0.0);
		domain.setTickLabelsVisible(true);

		range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		chart = new JFreeChart("Visualizing " + sensorId, new Font("SansSerif", Font.BOLD, 24), plot, true);

		ChartUtilities.applyCurrentTheme(chart);

		ChartPanel chartPanel = new ChartPanel(chart, true);
		chartPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
				BorderFactory.createLineBorder(Color.black)));
		add(chartPanel);

	}
	
	private void addSenseObservation(long timeMilis, double senseRes) {
		this.senseResult.addOrUpdate(new Millisecond(new Date(timeMilis)), senseRes);
	}


	public void showPlot() {
		if (!showed) {
			this.showed = true;
			frame = new JFrame("Visualizing " + sensorId);
			Visualizing panel = this;
			frame.getContentPane().add(panel, BorderLayout.CENTER);
			frame.setBounds(200, 120, 700, 500);
			frame.setVisible(true);

			panel.new DataGenerator(0).start();
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.out.println(sensorId + " Monitor exit..");
				}
			});

		}
	}

	class DataGenerator extends Timer implements ActionListener {
		
		DataGenerator(int interval) {
			super(interval, null);
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent event) {
			if (canPlot) {
				canPlot = false;
				addSenseObservation(Long.parseLong(plotMaker.plotTime), Double.parseDouble(plotMaker.plotSense));
			}
		}
	}
}
