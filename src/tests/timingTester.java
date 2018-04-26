package tests;

import model.FactorMath;

import java.math.BigInteger;

public class timingTester {

    public static void main(String[] args) {

        BigInteger p = new BigInteger("3286334437387");
        BigInteger q = new BigInteger("25630341639031");
        BigInteger n = p.multiply(q);
        BigInteger rn = FactorMath.sqrt(n)[0];

        System.out.println("n = p * q = " + n);
        System.out.println("Searching for a factor... (this could take " +
                           "awhile)");

        long start = System.nanoTime();

        BigInteger x = FactorMath.findFactorTD2(n, BigInteger.valueOf(2), rn);

        long end = System.nanoTime();

        BigInteger y = n.divide(x);

        long[] time = FactorMath.getTime(end - start);

        System.out.println(n + " = " + x + " * " + y);
        System.out.println("Time: " + time[0] + " seconds, " + time[1] + " millis");

    }

}
