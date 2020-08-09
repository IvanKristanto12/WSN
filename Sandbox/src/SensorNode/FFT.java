package SensorNode;

public class FFT {

	public int length;

	public FFT(int length) {
		this.length = length;
	}

	public int bitReverse(int n, int bits) {
		if (n == 0) {
			return 0;
		}

		String temp = Integer.toBinaryString(n);
		if (temp.length() < bits) {
			int k = bits - temp.length();
			for (int i = 0; i < k; i++) {
				temp = "0" + temp;
			}
		}

		int res = 0;
		for (int i = temp.length() - 1; i >= 0; i--) {
			if (temp.charAt(i) == '1') {
				res += Math.pow(2, i);
			}
		}

		return res;
	}

	// Cooley-Tukey FFT
	public Complex[] fft(Complex[] input) {
		int bits = (int) (Math.log(input.length) / Math.log(2));
		Complex[] finalOrder = new Complex[input.length];
		for (int i = 0; i < input.length; i++) {
			int order = bitReverse(i, bits);
			finalOrder[i] = input[order];
		}

		for (int N = 2; N <= finalOrder.length; N = N * 2) {
			for (int i = 0; i < finalOrder.length; i += N) {
				for (int k = 0; k < N / 2; k++) {

					Complex first = finalOrder[i + k];
					Complex second = finalOrder[i + k + (N / 2)];

					double w = (-2 * Math.PI * k) / (double) N;
					Complex exp = (new Complex(Math.cos(w), Math.sin(w)).kali(second));

					finalOrder[i + k] = first.tambah(exp);
					finalOrder[i + k + (N / 2)] = first.kurang(exp);
				}
			}
		}
		return finalOrder;
	}

}