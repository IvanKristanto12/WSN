package UserApp;

/**
 *
 * @author Ivan Kristanto <ivankristanto12@gmail.com>
 */

public class Plotting {

	public String plotSense;
	public String plotTime;

	public boolean makePlot;
	public static String senseResultDefaultPath;

	public void plotRealTime(String time, String senseResult) {
		plotSense = senseResult;
		plotTime = time;
	}

}
