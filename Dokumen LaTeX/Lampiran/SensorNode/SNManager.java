package SensorNode;

import java.io.IOException;
import java.util.Random;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import com.virtenio.vm.Time;

public class SNManager {

	private static final Accelerometer sensor = new Accelerometer();
	private static boolean isSensing = true;
	private static SenseController sc = new SenseController(sensor);
	private static Complex[] input;
	private static Thread sensingThread;

	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	private static final String sensorId = "Sensor1";
	private static int SENSOR_NODE_ADDRESS = node_list[1];
	private static int baseStationAddr = node_list[0];
	private static int[] nextNode = { node_list[2]  };
	private static int[] previousNode = { node_list[0] };

	private static boolean sendAck;
	private static boolean already;

	public static void main(String[] args) throws Exception {
		sendAck = false;
		already = false;
		sensor.init();
		System.out.println(sensorId + " Waiting for task..");
		runs();
	}

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, SENSOR_NODE_ADDRESS, SENSOR_NODE_ADDRESS, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			receive(fio);

		} catch (Exception e) {
		}
	}

	public static void receive(final FrameIO fio) {
		Thread thread = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] content = frame.getPayload();
						String str = new String(content, 0, content.length);
						if (str.length() > 2) {
							if (str.substring(0, 4).equalsIgnoreCase("ACK2") && !sendAck) {
								if (str.substring(4).equals(sensorId)) {
									already = true;
									Thread.sleep(20);
								} else {
									if (nextNode.length != 0) {
										System.out.println("toNextNode1 " + str);
										forwardMsgToNextNode(str, fio);
									}
								}
							} else if (str.substring(0, 3).equalsIgnoreCase("ACK")) {
								if (str.substring(3).equals(sensorId) && !already) {
									sendAck = true;
									Thread.sleep(20);
								} else {
									if (nextNode.length != 0) {
										System.out.println("toNextNode2 " + str);
										forwardMsgToNextNode(str, fio);
									}
								}
							}
						}

						// forward msg to previous
						if (str.charAt(0) != '@') {
							forwardMsgToPreviousNode(str, fio);
						}
						// online status
						else if (str.substring(0, 2).equalsIgnoreCase("@1")) {
							System.out.println("Online");
							long curTime = Long.parseLong(str.substring(2));
							Time.setCurrentTimeMillis(curTime);
							curTime = Time.currentTimeMillis();
							forwardMsgToNextNode("@1" + curTime, fio);
							forwardMsgToPreviousNode("1" + sensorId + " ONLINE #" + curTime, fio);

						} // start sensing
						else if (str.equalsIgnoreCase("@2")) {
							System.out.println("Start Sensing..");
							forwardMsgToNextNode("@2", fio);
							isSensing = true;

							sensingThread = new Thread() {
								public void run() {
									Thread computingThread = null;
									while (isSensing) {
										try {
											input = sc.createSample(sc.X_AXIS);
											FFT fft = new FFT(sc.N);

											ShortTimeFourierTransform stft = new ShortTimeFourierTransform(fft, input,
													sc.fs);
											computingThread = new Thread(stft);
											computingThread.start();
											while (true) {
												if (!computingThread.isAlive()) {
													Complex[] FFTcomputed = stft.output;
													double[][] amplitude = stft.amplitude;
													for (int j = 0; j < stft.N; j++) {
														String res = "2" + sensorId + " " + j + " " + sc.time[j] + " "
																+ sc.senseResult[j];
														for (int i = 0; i < stft.segment; i++) {
															res += " " + amplitude[i][j];
														}
														res += " " + stft.windowSize + " " + stft.overlap + " "
																+ stft.fs + " " + sc.deltaTime;

														sendAck = false;
														already = false;
														while (!sendAck) {
															already = false;
															System.out.println("send A");
															forwardMsgToPreviousNode("A" + sensorId, fio);
															Thread.sleep(new Random().nextInt(50) + 10);
														}
														System.out.println("send result");
														while (!already) {
															sendAck = false;
															forwardMsgToPreviousNode(res, fio);
															Thread.sleep(new Random().nextInt(50) + 10);
														}
														already = false;

														System.out.println("sending data [" + j + "]..");
														Thread.sleep(10);
													}

													forwardMsgToPreviousNode("4doneA" + sensorId, fio);

													System.out.println("DONE");
													break;
												}
											}
										} catch (InterruptedException e) {
										}
									}
								}
							};
							sensingThread.start();

						} // stop sensing
						else if (str.equalsIgnoreCase("@3")) {
							forwardMsgToNextNode("@3", fio);
							isSensing = false;
							System.out.println("Stop Sensing.. Waiting for task..");
							System.exit(0);

						} // exit
						else if (str.equalsIgnoreCase("@4")) {
							forwardMsgToNextNode("@4", fio);
							System.out.println("Exiting..");
							System.exit(0);
						}

					} catch (IOException e) {
					} catch (InterruptedException e) {
					}
				}
			}
		};
		thread.start();
	}

	public static void send(String msg, int src, int dest, FrameIO fio) {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
		final Frame sentFrame = new Frame(frameControl);
		sentFrame.setDestPanId(COMMON_PANID);
		sentFrame.setDestAddr(dest);
		sentFrame.setSrcAddr(src);
		sentFrame.setPayload(msg.getBytes());
		try {
			fio.transmit(sentFrame);
		} catch (Exception e) {
		}
	}

	private static void forwardMsgToNextNode(String msg, FrameIO fio) {
		for (int i = 0; i < nextNode.length; i++) {
			send(msg, SENSOR_NODE_ADDRESS, nextNode[i], fio);
		}
	}

	private static void forwardMsgToPreviousNode(String msg, FrameIO fio) {
		for (int i = 0; i < previousNode.length; i++) {
			send(msg, SENSOR_NODE_ADDRESS, previousNode[i], fio);
		}
	}

}
