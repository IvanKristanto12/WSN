package SensorNode;

public class ShortTimeFourierTransform implements Runnable {

	public Complex[] input;
	public Complex[] output;
	public double[][] amplitude;
	public FFT fft;
	public int N = 128;
	public double overlap = 0; 
	public int windowSize = 64;
	public double fs; 
	public int segment; 

	public ShortTimeFourierTransform(FFT fft, Complex[] input, double fs) {
		this.input = input;
		this.fft = fft;
		this.segment = (int) Math.floor((N - windowSize) / (windowSize - (overlap * windowSize))) + 1;
		this.amplitude = new double[segment][N];
		this.fs = fs;
	}

	// rectangle window
	public void STFT() {
		int idx = 0;
		while (idx < segment) {
			Complex[] windowedInput = new Complex[fft.length]; // panjang arr window sama dengan fft
			for (int i = 0; i < windowSize; i++) {
				int inputIdx;
				if (overlap == 0) {
					inputIdx = i + (idx * (windowSize - 1));
				} else {
					inputIdx = (int) (i + (idx * (windowSize - 1) * overlap));
				}

				
				//Pemilihan window
				windowedInput[inputIdx] = rectangularWindow(input[inputIdx]);
//				windowedInput[inputIdx] = hannWindow(input[inputIdx], i);
//				windowedInput[inputIdx] = hammWindow(input[inputIdx], i);

			}
			
			for (int i = 0; i < windowedInput.length; i++) {
				if (windowedInput[i] == null) {
					windowedInput[i] = new Complex(0, 0);
				}
			}

			output = this.fft.fft(windowedInput);
			System.out.println("selesai");
			for (int i = 0; i < output.length; i++) {
				amplitude[idx][i] = output[i].absolute();
			}
			idx++;
		}
	}

	@Override
	public void run() {
		this.STFT();
	}

	// w[n] = 1
	public Complex rectangularWindow(Complex input) {
		return input.kali(new Complex(1, 0));
	}

	// w[n] = 0.5 - 0.5 * cos (2 pi * i /N)
	public Complex hannWindow(Complex input, int idx) {
		double multiplier = 0.5 - 0.5 * Math.cos(2 * Math.PI * idx / N);
		return input.kali(new Complex(multiplier, 0));
	}

	// w[n] = 0.54 - 0.46 * cos (2 pi * i /N)
	public Complex hammWindow(Complex input, int idx) {
		double multiplier = 0.54 - 0.46 * Math.cos(2 * Math.PI * idx / N);
		return input.kali(new Complex(multiplier, 0));
	}

}
