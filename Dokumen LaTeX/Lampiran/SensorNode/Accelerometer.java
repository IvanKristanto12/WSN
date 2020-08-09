package SensorNode;

import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.spi.NativeSPI;

public class Accelerometer {
	private ADXL345 sensor;
	private GPIO accelCs;

	public void init() throws Exception {
		// init ADXL345
		accelCs = NativeGPIO.getInstance(20);
		NativeSPI spi = NativeSPI.getInstance(0);
		spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);
		sensor = new ADXL345(spi, accelCs);

		sensor.open();
		sensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
		sensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
		sensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);
	}

	public float[] sense() throws Exception {
		short[] temp = new short[3];
		float[] result = new float[3];
		sensor.getValuesRaw(temp, 0);
		sensor.convertRaw(temp, 0, result, 0);
		for (int i = 0; i < 3; i++) {
			result[i] *= sensor.getConversionScale();
		}

		return result;
	}

}
