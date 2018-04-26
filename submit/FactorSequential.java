package model;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class FactorSequential {

    private static BigInteger n;
    private static BigInteger p;
    private static BigInteger q;
    private static FactorData data;
    private static BigInteger origN;
    private static int bits;
    private static long startTime;
    private static long endTime;

    private static boolean complete;
    private static Semaphore mutex;
    private static Semaphore block;

    public static void main(String[] args) {

        mutex = new Semaphore(1);
        block = new Semaphore(1);

        try {
            block.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //args[0] is a flag, either -n, -g, -r, or -pq
        if (args.length < 2) {
            System.err.println("java FactorServer [flags] [numbers(s)]");
            System.exit(1);
        }

        // factor given integer n
        // args: -n [integer]
        switch (args[0]) {
            case "-n":
                n = new BigInteger(args[1]);
                break;

            // randomly select two fixed bit length integers
            // args: -g [bit-length]
            case "-g": {
                int bitLength = Integer.parseInt(args[1]);
                bits = bitLength;
                p = BigInteger.probablePrime(bitLength, new Random());
                q = BigInteger.probablePrime(bitLength, new Random());
                n = p.multiply(q);
                break;
            }

            // randomly select a number, potentially with several factors
            // args: -r [bit-length]
            case "-r": {
                int bitLength = Integer.parseInt(args[1]);
                bits = bitLength;
                n = new BigInteger(bitLength, new Random());
                break;
            }

            // given two integers p and q, factor n=pq
            // args: -pq [factor1] [factor2]
            case "-pq":
                if (args.length < 3) {
                    System.err.println("java FactorServer -pq [factor1] [factor2]");
                    System.exit(1);
                }
                p = new BigInteger(args[1]);
                q = new BigInteger(args[2]);
                n = p.multiply(q);
                break;

            // not a valid argument
            default:
                System.err.println("FactorServer valid arguments: -n, -g, -pq");
                System.exit(1);
        }

        data = new FactorData(n);
        //jobs.add(n); // n is the first factor to find
        origN = n;

        // create worker threads
        TD2Factorize td2 = new TD2Factorize(data);
        TDRNFactorize tdrn = new TDRNFactorize(data);
        FermatFactorize fermat = new FermatFactorize(data);
        PollardsFactorize pollards = new PollardsFactorize(data);

        Thread td2Thread = new Thread(td2);
        Thread tdrnThread = new Thread(tdrn);
        Thread fermatThread = new Thread(fermat);
        Thread pollardsThread = new Thread(pollards);

        startTime = System.nanoTime();

        td2Thread.start();
        tdrnThread.start();
        fermatThread.start();
        pollardsThread.start();

        try {
            block.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        endTime = System.nanoTime();
        long[] time = new long[2];
        long nanoseconds = endTime - startTime;
        time[0] = nanoseconds / 1000000000;
        time[1] = nanoseconds % 1000000000;
        time[1] = time[1] / 1000000;

        System.out.println("Factorization complete!");
        p = data.getFactor();
        q = n.divide(p);
        System.out.println(n + " = " + p + " * " + q);
        System.out.println("Winner: " + FactorType.toString(data.getType()));
        System.out.println("time: " + time[0] + " seconds, " + time[1] + " millis");
        System.exit(0);
    }

    private static void checkCompletion() {
        try {
            mutex.acquire();

            if (complete) {
                System.exit(0);
            }

            mutex.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class TD2Factorize implements Runnable {

        private FactorData factorData;

        public TD2Factorize(FactorData data) {
            factorData = data;
        }

        @Override
        public void run() {

            BigInteger num = factorData.getNum(); // the number to factor
            BigInteger[] bounds = factorData.getTDBounds(); // the upper/lower bounds

            // factor num
            BigInteger factor = FactorMath.findFactorTD2(num, bounds[0], bounds[1]);

            checkCompletion();

            data.setType(FactorType.TD2);

            data.addFactor(factor);

            block.release();

        }
    }

    private static class TDRNFactorize implements Runnable {
        FactorData factorData;

        public TDRNFactorize(FactorData data) {
            factorData = data;
        }


        @Override
        public void run() {
            BigInteger num = factorData.getNum(); // number to factor
            BigInteger[] bounds = factorData.getTDBounds(); // upper or lower bounds

            // factor num
            BigInteger factor = FactorMath.findFactorTDRN(num, bounds[0],
                    bounds[1]);

            checkCompletion();

            data.setType(FactorType.TDRN);

            data.addFactor(factor);

            block.release();
        }

    }

    private static class FermatFactorize implements Runnable {
        FactorData factorData;

        public FermatFactorize(FactorData data) {
            factorData = data;
        }

        @Override
        public void run() {
            BigInteger num = data.getNum();
            BigInteger start = data.getFermatStartVal();
            long attempts = data.getAttempts();

            while (true) {

                BigInteger factor = FactorMath.findFactorFermat(num, start, attempts);

                checkCompletion();

                if (factor == null) {
                    data.setFermatStartVal(start.add(BigInteger.valueOf(attempts)));
                } else {
                    data.addFactor(factor);
                    data.setType(FactorType.FERMAT);
                    break;
                }
            }
            block.release();
        }
    }

    private static class PollardsFactorize implements Runnable {

        FactorData factorData;
        BigInteger ONE = BigInteger.ONE;
        BigInteger TWO = BigInteger.valueOf(2);

        public PollardsFactorize(FactorData data) {
            factorData = data;
        }

        @Override
        public void run() {
            BigInteger num = data.getNum(); // get number to factor
            BigInteger[] factor; // index 0 will hold factor, index 1 will hold
            // the next major power if desired

            while (true) {

                checkCompletion();

                BigInteger[] bounds = factorData.getPollardsBounds();

                // create a bound somewhere between the upper and lower bounds
                BigInteger bound = bounds[0].add(bounds[1].divide(TWO));
                // if the bounds are too close to each other, then our starting
                // base value likely isn't good.  Reset the base to a new value.
                if (bound.subtract(bounds[0]).compareTo(ONE) <= 0) {
                    if (bounds[1].subtract(bound).compareTo(ONE) <= 0) {
                        factorData.incrementPollardsBase();
                        continue;
                    }
                }

                // get factor
                factor = FactorMath.findFactorPollards(num, factorData.getTwoToImodN(),
                        factorData.getStartP(), bound);

                // if factor == 1, then our bound was too small
                if (factor[0].equals(ONE)) {
                    factorData.setLowerBoundP(bound); // set the lower bound to be
                    // higher
                    factorData.setTwoToImodN(factor[1]); // carry over same power
                    factorData.setStartP(bound.add(ONE)); // set power to bound+1
                    factorData.setBoundAmount(bound);
                } else if (factor[0].equals(num)) {
                    // if factor == num, our bound is too high
                    factorData.setUpperBoundP(bound); // set new upper bound
                    factorData.resetStartTMN(); // reset the power
                    factorData.setBoundAmount(bound);
                } else {
                    // we found a factor
                    data.addFactor(factor[0]); // add factor to data list
                    data.setType(FactorType.POLLARDS);
                    break;
                }
            }
            block.release();
        }
    }
}
