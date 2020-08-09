package BaseStation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.driver.timer.NativeTimer;
import com.virtenio.driver.timer.Timer;
import com.virtenio.driver.timer.TimerException;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTException;
import com.virtenio.driver.usart.USARTParams;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.examples.common.USARTConstants;
import com.virtenio.preon32.node.Node;

import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import com.virtenio.vm.Time;
import com.virtenio.vm.event.AsyncEvent;
import com.virtenio.vm.event.AsyncEventHandler;
import com.virtenio.vm.event.AsyncEvents;

public class BSManager {

	private static USART usart;
	private static OutputStream out;

	// Setting Address
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	private static int currAddr = node_list[0];

	private static int[] connectedNodeAddr = new int[] { node_list[1]};

	private static HashMap<String, Integer> nodeAddrMap;

	private static boolean already;
	private static String ackAddr;
	private static String sentResult;


	public static void main(String[] args) throws Exception {
		nodeAddrMap = new HashMap<String, Integer>();
		for (int i = 1; i <= connectedNodeAddr.length; i++) {
			nodeAddrMap.put("ASensor" + i, connectedNodeAddr[i - 1]);
		}

		usart = configUSART();
		out = usart.getOutputStream();

		ackAddr = "";
		sentResult = "";
		already = false;

		new Thread() {
			public void run() {
				runs();
			}
		}.start();
	}

	/**
	 * 
	 */
	public static void runs() {

		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, currAddr, currAddr, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);

			new Thread() {
				public void run() {
					while (true) {
						String res;
						int input;
						try {
							input = usart.read();
							if (input == 1) {
								long curTime = Time.currentTimeMillis();
								res = new String("_BaseStation ONLINE #" + curTime + "_");
								out.write(res.getBytes());
								usart.flush();
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									send("@1" + curTime, currAddr, connectedNodeAddr[i], fio);
								}
							} else if (input == 2) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									send("@2", currAddr, connectedNodeAddr[i], fio);
								}
							} else if (input == 3) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									send("@3", currAddr, connectedNodeAddr[i], fio);
								}
								usart = configUSART();
								out = usart.getOutputStream();
								already = false;
								ackAddr = "";
								sentResult = "";
							} else if (input == 4) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									send("@4", currAddr, connectedNodeAddr[i], fio);
								}
							}
							Thread.sleep(50);
						} catch (USARTException e) {
						} catch (IOException e) {
						} catch (InterruptedException e) {
						}
					}
				}
			}.start();

			new Thread() {
				public void run() {
					receive(fio);
				}
			}.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void receive(final FrameIO fio) {
		while (true) {
			Frame frame = new Frame();
			try {
				fio.receive(frame);
				byte[] content = frame.getPayload();
				String str = new String(content, 0, content.length);
				String res;
				// done
				if (str.trim().charAt(0) == '4') {
					if (str.substring(5).equals(ackAddr)) {
						already = false;
						ackAddr = "";
					}
					res = str.substring(1, 5);
					try {
						out.write(res.getBytes());
						usart.flush();
					} catch (USARTException e) {
					}
				}
				// ASensor#
				if (str.charAt(0) == 'A' && str.length() > 3 && str.charAt(1) == 'S') {
					// first set ackAddr
					if (ackAddr.equals("")) {
						ackAddr = str;
					}
					// send ACKSensor#
					if (ackAddr.length() != 0 && ackAddr.equals(str)) {
						if (ackAddr.equals(str) && already) {
							already = false;
							ackAddr = "";
						} else {
							if (!nodeAddrMap.containsKey(ackAddr)) {
								for (int i = 0; i < connectedNodeAddr.length; i++) {
									send("ACK" + ackAddr.substring(1), currAddr, connectedNodeAddr[i], fio);
								}
							} else {
								send("ACK" + ackAddr.substring(1), currAddr, nodeAddrMap.get(ackAddr), fio);
							}
							sentResult = "";
						}
					}
				} else {
					if (str.charAt(0) == '1') {
						res = "_" + str.substring(1) + "_";
						try {
							out.write(res.getBytes());
							usart.flush();
						} catch (USARTException e) {
						}
					}
					// receive result
					if (str.charAt(0) == '2') {
						res = "_" + str.substring(1) + "_";
						if (sentResult.equals("") && !already && res.substring(1, 8).equals(ackAddr.substring(1))) {
							sentResult = res;
							try {
								out.write(res.getBytes());
								usart.flush();
							} catch (USARTException e) {
							}
						}
						already = true;
					}

					// already send ACK2Sensor#;
					if (already) {
						if (!nodeAddrMap.containsKey(ackAddr)) {
							for (int i = 0; i < connectedNodeAddr.length; i++) {
								send("ACK2" + ackAddr.substring(1), currAddr, connectedNodeAddr[i], fio);
								Thread.sleep(10);
							}
						} else {
							send("ACK2" + ackAddr.substring(1), currAddr, nodeAddrMap.get(ackAddr), fio);
						}
					}
				}
			} catch (IOException e) {
			} catch (InterruptedException e) {
			}
		}

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
			Thread.sleep(50);
		} catch (Exception e) {
		}
		System.out.println(msg);
	}

	private static USART configUSART() {
		USARTParams params = USARTConstants.PARAMS_115200;
		NativeUSART usart = NativeUSART.getInstance(0);
		try {
			usart.close();
			usart.open(params);
			return usart;
		} catch (Exception e) {
			return null;
		}
	}
}
