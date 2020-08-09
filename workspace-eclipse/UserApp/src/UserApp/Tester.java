package UserApp;

import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.virtenio.commander.io.DataConnection;
import com.virtenio.commander.toolsets.preon32.Preon32Helper;

public class Tester {

	private static Thread senseThread;
	private static BufferedInputStream in;
	private static DataConnection conn;
	private static boolean isSensing;
	private static int totalSensor;

	private static HashMap<String, Visualizing> vMap;

	private static HashMap<String, double[][]> sensorAmplitudeResult;

	public static void main(String[] args) throws Exception {

//		// init BaseStation
		Tester tester = new Tester();
		tester.context_set("context.set.1");
		tester.time_synchronize();

		Preon32Helper nodeHelper = new Preon32Helper("COM7", 115200);
		conn = nodeHelper.runModule("basestation");
		in = new BufferedInputStream(conn.getInputStream());
		conn.flush();

		System.out.println("\n\n");

		// Input
		Scanner sc = new Scanner(System.in);
		System.out.println("Please insert total Sensor Node (not including basestation) : ");
		totalSensor = sc.nextInt();

		while (true) {
			System.out.println(
					"OPTION \n 1 : Check Online Node \n 2 : Sense \n 3 : Stop Sensing \n 4 : Exit \n Enter option number : ");
			int input = sc.nextInt();
			if (isSensing) {
				if (input == 1 || input == 2 || input == 4) {
					System.out.println("IN SENSING STATE.. TO STOP USE OPTION '3' ");
					input = 0;
				} else {
					conn.write(input);
				}
			} else {
				conn.write(input);
			}
			byte[] buffer = new byte[1024];
			if (input == 1) {
				System.out.println("ONLINE NODE :");
				Thread.sleep(1000);
				while (in.available() > 0) {
					in.read(buffer);
					conn.flush();
					String s = new String(buffer);
					String[] res = s.split("_");
					for (int i = 0; i < res.length; i++) {
						if (res[i].length() != 0) {
							if (res[i].charAt(0) != '@') {
								String[] temp = res[i].split("#");
								if (temp.length > 1)
									System.out.println(
											"> " + temp[0] + "Time: " + formatTimetoString(Long.parseLong(temp[1])));
							}
						}
					}
				}
				System.out.println(" - CHECKING DONE - \n");
			} else if (input == 2) {
				System.out.println("SENSING..");
				isSensing = true;
				vMap = new HashMap<String, Visualizing>();
				sensorAmplitudeResult = new HashMap<String, double[][]>();

				for (int i = 1; i <= totalSensor; i++) {
					vMap.put("Sensor" + i, new Visualizing("Sensor" + i));
					vMap.get("Sensor" + i).showed = false;
					vMap.get("Sensor" + i).showPlot();
					vMap.get("Sensor" + i).canPlot = false;
					sensorAmplitudeResult.put("Sensor" + i, new double[128][128]);
				}

				if (senseThread == null) {
					senseThread = new Thread() {
						public void run() {
							while (true && isSensing) {
								byte[] buffer = new byte[1024];
								try {
									if (in.available() > 0) {
										in.read(buffer);
										conn.flush();
										String s = new String(buffer);

										String[] res = s.split("_");
										for (int i = 0; i < res.length; i++) {
											if (res[i].trim().length() != 0) {
												if (res[i].charAt(0) != '@' && res[i].charAt(0) != 'A'
														&& res[i].charAt(0) != 'd') {
													String[] str = res[i].split(" ");
													String save = formatTimetoString(Long.parseLong(str[2])) + " "
															+ str[3];
													saveSenseResult(str[0], save);

													int segment = str.length - 8;
													int idx = Integer.parseInt(str[1]);

													for (int k = 0; k < segment; k++) {
														sensorAmplitudeResult.get(str[0].trim())[k][idx] = Double
																.parseDouble(str[k + 4]);
													}
													if (idx == 127) {
														double[] resTime = new double[128];
														for (int j = 0; j < 128; j++) {
															System.out.println();
															resTime[j] = j * Double.parseDouble(str[str.length - 1]);
														}
														double deltaTime = Double.parseDouble(str[str.length - 1]);
														double fs = Double.parseDouble(str[str.length - 2]);
														double overlap = Double.parseDouble(str[str.length - 3]);
														int windowSize = Integer.parseInt(str[str.length - 4]);
														new Spectrogram("Spectrogram " + str[0],
																sensorAmplitudeResult.get(str[0]), resTime, fs,
																windowSize, overlap, deltaTime);
													}

													// plotting
													vMap.get(str[0].trim()).plotMaker.plotRealTime(str[2], str[3]);
													vMap.get(str[0].trim()).canPlot = true;
												}
											}
										}
									}
								} catch (NumberFormatException e) {
								} catch (IOException e) {
								}
							}
						}
					};
					senseThread.start();
//					}

				}

				// Stop Sensing
			} else if (input == 3) {
				if (isSensing) {
					System.out.println("STOP SENSING.. WAIT FOR ALL SENSOR STOP SENSING..");
					for (int i = 1; i <= totalSensor; i++) {
						Visualizing temp = vMap.get("Sensor" + i);
						temp.frame.dispatchEvent(new WindowEvent(temp.frame, WindowEvent.WINDOW_CLOSING));
					}
					isSensing = false;
					senseThread = null;
					System.out.println("- STOP SENSING DONE -\n");
				} else {
					System.out.println("NOT IN SENSING STATE..");
				}

			} else if (input == 4) {
				System.out.println("TERMINATING PROGRAM..");
				isSensing = false;
				Thread.sleep(1000);
				System.out.println(" - PROGRAM TERMINATED - ");
				System.exit(0);
			} else {
				System.out.println("WRONG INPUT");
			}

		}

	}

	// console build ant
	private static DefaultLogger getConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		return consoleLogger;
	}

	// ant build time synchronize
	private void time_synchronize() throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("C:\\Users\\Ifunk\\Desktop\\SKRIPSI\\Program Skripsi\\Sandbox\\build.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);
		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);
			String target = "cmd.time.synchronize";
			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
		}
	}

	// set context basestation
	private void context_set(String target) throws Exception {
		DefaultLogger consoleLogger = getConsoleLogger();
		File buildFile = new File("C:\\Users\\Ifunk\\Desktop\\SKRIPSI\\Program Skripsi\\Sandbox\\buildUser.xml");
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		antProject.addBuildListener(consoleLogger);
		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.ProjectHelper", helper);
			helper.parse(antProject, buildFile);
			antProject.executeTarget(target);
			antProject.fireBuildFinished(null);
		} catch (BuildException e) {
		}
	}

	// save data
	private static void saveSenseResult(String sensorName, String msg) {
		File resFolder = new File("SenseResult");
		if (!resFolder.exists()) {
			resFolder.mkdir();
		}

		vMap.get(sensorName).plotMaker.senseResultDefaultPath = resFolder.getAbsolutePath();

		Date date = new Date(System.currentTimeMillis());
		String path = resFolder.getAbsolutePath() + "\\" + sensorName + "-"
				+ new SimpleDateFormat("ddMMyyyy").format(date) + ".txt";
		try {
			FileWriter fileWriter = new FileWriter(path, true);
			BufferedWriter buffWriter = new BufferedWriter(fileWriter);
			File dataFile = new File(path);

			buffWriter.append(msg + "\n");
			buffWriter.close();
			fileWriter.close();
		} catch (Exception e) {
		}
	}

	private static String formatTimetoString(long timeInMillis) {
		Date date = new Date(timeInMillis);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		return simpleDateFormat.format(date);
	}

}
