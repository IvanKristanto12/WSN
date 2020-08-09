package UserApp;

public class Plotting {

	public String plotSense;
	public String plotTime;

	public boolean makePlot;
	public static String senseResultDefaultPath = "C:\\Users\\Ifunk\\Desktop\\SKRIPSI\\Program Skripsi\\Sandbox\\SenseResult";

	public void plotRealTime(String time, String senseResult) {
		plotSense = senseResult;
		plotTime = time;
	}

}
