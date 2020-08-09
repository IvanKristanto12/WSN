package SensorNode;

public class Complex {
    public double re;
    public double im;
 
    public Complex(double r, double i) {
        this.re = r;
        this.im = i;
    }
 
    public Complex tambah(Complex x) {
    	Complex res = new Complex(this.re + x.re, this.im + x.im);
        return res;
    }
 
    public Complex kurang(Complex x) {
    	Complex res = new Complex(this.re - x.re, this.im - x.im);
        return res;
    }
 
    
    public Complex kali(Complex x) {
    	Complex res = new Complex(this.re * x.re - this.im * x.im, this.re * x.im + this.im * x.re);
        return res;
    }
    
    public double absolute() {
    	return Math.sqrt(this.re * this.re + this.im * this.im );
    }
 
    @Override
    public String toString() {
        return this.re+" ,"+ this.im;
    }
}