package UserApp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;


public class Spectrogram {

	private static double maxAmp;
	private static int length;
	private static double[][] resAmplitude;
	private static double[] resTime;
	private static double fs;
	private static int windowSize;
	private static double overlap;
	private static String title;
	private static double deltaTime;

	public Spectrogram(String title, double[][] resA, double[] resTime, double fs, int windowSize, double overlap,
			double deltaTime) {
		this.windowSize = windowSize;
		this.deltaTime = deltaTime;
		this.overlap = overlap;
		this.title = title;
		this.resAmplitude = resA;
		this.resTime = resTime;
		this.length = resA[0].length;
		this.maxAmp = 0;
		this.fs = fs;
		for (int i = 0; i < resA.length; i++) {
			for (int j = 0; j < resA[0].length; j++) {
				if (this.maxAmp < resA[i][j]) {
					this.maxAmp = resA[i][j];
				}
			}
		}

		JFrame f = new JFrame(title);
		ChartPanel chartPanel = new ChartPanel(createChart(createDataset())) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(640, 480);
			}
		};
		chartPanel.setMouseZoomable(true, false);
		f.add(chartPanel);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	private static JFreeChart createChart(XYDataset dataset) {
		NumberAxis xAxis = new NumberAxis("Time (ms)");
		NumberAxis yAxis = new NumberAxis("Frequency");
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
		XYBlockRenderer r = new XYBlockRenderer();
		SpectrumPaintScale ps = new SpectrumPaintScale(0, maxAmp);

		r.setPaintScale(ps);
		r.setBlockWidth(deltaTime);
		plot.setRenderer(r);
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
		NumberAxis scaleAxis = new NumberAxis("Amplitude");
		scaleAxis.setAxisLinePaint(Color.white);
		scaleAxis.setTickMarkPaint(Color.white);
		PaintScaleLegend legend = new PaintScaleLegend(ps, scaleAxis);

		legend.setSubdivisionCount(resAmplitude.length);

		legend.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
		legend.setPadding(new RectangleInsets(10, 10, 10, 10));
		legend.setStripWidth(20);
		legend.setPosition(RectangleEdge.RIGHT);
		legend.setBackgroundPaint(Color.WHITE);
		chart.addSubtitle(legend);
		chart.setBackgroundPaint(Color.white);
		return chart;
	}

	private static XYZDataset createDataset() {
		DefaultXYZDataset dataset = new DefaultXYZDataset();
		int idx = 0;
		for (int i = 0; i < resAmplitude[0].length; i++) {
			double[][] data = new double[3][length];
			for (int j = 0; j < resAmplitude[0].length / 2; j++) {
				data[0][j] = resTime[i];
				data[1][j] = j;
				data[2][j] = resAmplitude[idx][j];
				dataset.addSeries("Series" + i, data);
			}
			if (overlap == 0 && windowSize != 0) {
				if (i % (windowSize - 1) == 0 && i > 1) {
					idx++;
				}
			} else {
				if (i % ((windowSize * overlap) + 1) == 0 && i > 1) {
					idx++;
				}
			}

		}

		return dataset;
	}

	private static class SpectrumPaintScale implements PaintScale {

		private final double lowerBound;
		private final double upperBound;

		public SpectrumPaintScale(double lowerBound, double upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		@Override
		public double getLowerBound() {
			return lowerBound;
		}

		@Override
		public double getUpperBound() {
			return upperBound;
		}

		@Override
		public Paint getPaint(double value) {
			float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
			float scaledH = scaledValue;
			return Color.getHSBColor(0, 0, scaledH);
		}
	}
}
