package SensorNode;

import com.virtenio.vm.Time;

public class SenseController {
	private Accelerometer sensor;
	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;
	public final int N = 128;
	public long [] time; 
	public double fs;
	public double [] senseResult;
	public double deltaTime;

	public SenseController(Accelerometer sensor) {
		this.sensor = sensor;
		this.senseResult = new double[N];
	}

	private float[] sensing() {
		try {
			return sensor.sense();
		} catch (Exception e) {
			System.out.println("Sense ERROR");
		}
		return null;
	}

	public Complex[] createSample(int axis) throws InterruptedException {
		Complex[] res = new Complex[N];
		time = new long[N];
		for (int i = 0; i < N; i++) {
			senseResult[i] = this.sensing()[axis];			
			res[i] = new Complex(senseResult[i], 0);
			time[i] = Time.currentTimeMillis();
			Thread.sleep(16);
		}
		this.fs = N / ((time[N-1] - time[0])/1000); //sampling rate
		this.deltaTime = time[1] - time[0];
		return res;
	}


}
