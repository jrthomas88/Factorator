package model;

import java.math.BigInteger;

/**
 * FactorMath.java
 *
 * @author Jon Thomas
 * <p>
 * Contains several algorithms and static methods used in factorization.
 * This makes extensive use of Java's BigInteger class for integer
 * representation due to the potential size of the integers needing to be
 * represented.  This class contains four primary factoring algorithms.
 */

public class FactorMath {

    /* constants */
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private static boolean printOutput = false;

    /**
     * findFactorPollards
     * Given a integer num, a starting value twoToImodN, a starting power
     * start, and an ending bound, use Pollard's p-1 algorithm to try and
     * compute a factor of num.  This algorithm takes twoToImodN to the bound!
     * power, and checks if the gcd(twoToImodN-1,num) has a non-trivial value
     * .  If the gcd returns 1, then the chosen bound isn't large enough.  If
     * the gcd returns num, the bound was too large.  This method returns a
     * BigInteger array.  In index zero is the result of the gcd comparision.
     * In the event that index zero is a trivial factor, then the second
     * index contains the ending value of twoToImodN.  If the first index
     * holds 1, then this function can be run at with twoToIModN with
     * starting index bound+1 to avoid redoing work.
     *
     * @param num        The value needing factoring
     * @param twoToImodN The starting value wich will be brought to the
     *                   bound! power.  If this is the first run, use 2.
     * @param start      The starting index.  If this is the first run, use 1.
     * @param bound      The maximum bound to calculate to.  If too small, this
     *                   function returns 1.  If too big, this function returns
     *                   num.  The value shouldn't need to be larger than sqrt(num).
     * @return an array of BigIntegers.  Index 0 holds the result of the
     * function.  Index 1 holds the ending value of twoToImodN.
     */
    public static BigInteger[] findFactorPollards(BigInteger num, BigInteger
            twoToImodN, BigInteger start, BigInteger bound) {

        // check if num is even, if so return 2
        if (num.mod(twoToImodN).equals(ZERO)) {
            BigInteger[] retval = new BigInteger[2];
            retval[0] = twoToImodN;
            retval[1] = twoToImodN;
            return retval;
        }

        // start counting from the beginning
        BigInteger i = start;

        // while i is less than the bound
        while (i.compareTo(bound) <= 0) {
            twoToImodN = twoToImodN.modPow(i, num); // tmn = tmn^i mod num

            // if we equal one, the one to any power will just be one
            // we can stop here
            if (twoToImodN.equals(ONE)) {
                break;
            }

            i = i.add(ONE); // i++
        }

        // b = tmn - 1
        BigInteger b = twoToImodN.subtract(ONE);
        b = b.gcd(num); // either 1, num, or a factor

        // we need to return two values
        // firstly, the value of b as it may be a factor
        // in the event b is either num or 1, then we'll need twoToImodN
        // that way, the next iteration can pick up where we left off
        BigInteger[] returnVal = new BigInteger[2];
        returnVal[0] = b;
        returnVal[1] = twoToImodN;

        return returnVal;

    } /*findFactorPollards*/

    /**
     * findFactorFermat
     * Runs Fermat's Factoring Algorithm.  This attempts to find two values,
     * a and b, such that n is a^2-b^2.  This allows us to factor num by
     * using a-b as a non-trivial factor of num.  In this example, startVal =
     * a, and b = a^2-num.
     *
     * @param num      the number we're seeking a factor of
     * @param startVal the value we're starting at.  Must be ceiling of sqrt
     *                 (num) or higher.
     * @param attempts number of times we will attempt
     * @return a factor if one is found, null if not
     */
    public static BigInteger findFactorFermat(BigInteger num, BigInteger
            startVal, long attempts) {

        // if num is even, return 2 as a factor
        if (num.mod(TWO).equals(ZERO)) {
            return TWO;
        }

        // confirm that the start val is > sqrt(num)
        // this algorithm likely won't work if it's not
        BigInteger[] rootN = sqrt(num);

        // if there's no remainder, then num is a square
        if (rootN[1].equals(ZERO)) {
            return rootN[0];
        }

        if (rootN[0].compareTo(startVal) > 0) {
            throw new IllegalArgumentException("startVal must be greater than" +
                                               " sqrt(num)");
        }

        // b2 = s^2 - n
        BigInteger boundSquared = startVal.pow(2).subtract(num);

        // b = sqrt(b2)
        BigInteger[] bound = sqrt(boundSquared);

        // track iterations so we quit when needed
        long iterations = 0;

        // default return is null
        // null signifies that no factor was found
        BigInteger factor = null;

        while (iterations < attempts) { // run 'iterations' times

            // bound[1] holds fractional component of sqrt
            // if bound[1] is zero, then bound is a perfect sqrt and a factor
            // has been found
            if (bound[1].equals(ZERO)) {
                factor = startVal.subtract(bound[0]);
                break;
            }

            // otherwise, add 1 to startval and recompute
            startVal = startVal.add(ONE);
            boundSquared = startVal.pow(2).subtract(num);
            bound = sqrt(boundSquared);
            iterations++;
        }

        // return a factor if one has been found
        // factor may be null otherwise
        return factor;

    } /*findFactorFermat*/

    /**
     * findFactorTDRN
     * Use trial division to try and find a factor of num.  This method is
     * similar to TD2, except this counts down from end.  This method also
     * tries using gcd instead of mod to see if a number is a factor in the
     * event that the suspected number is a multiple of a factor.
     *
     * @param num   the number to factor
     * @param start the lower bound of the search range
     * @param end   the upper bound of the search range
     * @return the factor that was found, null if no factor in the specified
     * range exists.
     */
    public static BigInteger findFactorTDRN(BigInteger num, BigInteger
            start, BigInteger end) {

        if (printOutput) {
            System.out.println("Trying to factor n = " + num);
            System.out.println("start = " + start + " end = " + end);
        }

        // if num is one, then one is the only factor
        if (num.equals(ONE)) {
            return ONE;
        }

        // determine if num is even, then return two
        BigInteger gcd = num.mod(TWO);
        if (gcd.equals(ZERO)) {
            return TWO;
        }

        // if end pos is even, subtract one
        if (end.mod(TWO).equals(ZERO)) {
            end = end.subtract(ONE);
        }

        // iterator variable
        BigInteger i = end;

        // while i >= 2
        while (i.compareTo(start) >= 0) {

            // get gcd of num and i
            // use gcd in case i is a multiple of a factor
            gcd = num.gcd(i);

            // if gcd > 1, then gcd is a factor of num
            if (gcd.compareTo(ONE) > 0) {
                return gcd;
            }

            // i -= 2
            i = i.subtract(TWO);
        }

        // if null, then no factor exists in range [start,end]
        return null;

    } /*findFactorTDRN*/

    /**
     * findFactorTD2
     * Uses trial-division counting up from 2 to try and find a factor of num
     * .  It does not matter whether num is a product of two or more primes,
     * or whether num is a prime itself.  This method does not factor num, but
     * will find the smallest prime >= start that is a factor of num.  In the
     * event num is prime, this will run O(root num) iterations before
     * determining that no factor exist.  In that case, it is advisable to
     * confirm that num is not already prime before calling this function.
     *
     * @param num   the number to find a factor of
     * @param start the starting index of what to try
     * @param end   the ending index of what to try
     * @return the smallest prime that is a factor of num between start and
     * end.  Null if no value found.
     */
    public static BigInteger findFactorTD2(BigInteger num, BigInteger
            start, BigInteger end) {

        if (printOutput) {
            System.out.println("factoring num = " + num);
        }

        // if num is 1, then 1 is the only factor
        if (num.equals(ONE)) {
            return ONE;
        }

        // if num is divisible by two, then report two as a factor
        if (num.mod(TWO).equals(ZERO)) {
            return TWO;
        }

        // if start is even, add one to it
        if (start.mod(TWO).equals(ZERO)) {
            start = start.add(ONE);
        }

        // iterator i
        BigInteger i = start;

        // while i is less than the end
        while (i.compareTo(end) <= 0) {

            // if i divides num
            if (num.mod(i).equals(ZERO)) {
                return i; // return i as a factor
            }

            if (printOutput) {
                if (i.mod(BigInteger.valueOf(10000001)).equals(ZERO)) {
                    System.out.println("Testing factor #" + i.subtract(start));
                }
            }

            // i+=2, skip even numbers
            i = i.add(TWO);
        }

        // if no factor exists, return null
        // null = no factor in range [start,end]
        return null;

    } /*findFactorTD2*/

    /**
     * getTime
     * This method takes a number of nanoseconds (as a long) and returns an
     * array of size 2 that contains the number of seconds and milliseconds
     * that number represents.
     *
     * @param nanoseconds number of nanoseconds
     * @return array of size 2 containing seconds, millis
     */
    public static long[] getTime(long nanoseconds) {

        long[] time = new long[2];
        time[0] = nanoseconds / 1000000000;
        time[1] = nanoseconds % 1000000000;
        time[1] = nanoseconds / 1000000;
        return time;

    } /*getTime*/

    /**
     * THE FOLLOWING IS A SQUARE ROOT FUNCTION DESIGNED FOR
     * BIGINTEGER.  THIS CODE WAS OBTAINED FROM THE FOLLOWING
     * URL:
     * <p>
     * https://stackoverflow.com/questions/4407839/
     * how-can-i-find-the-square-root-of-a-java-biginteger
     * <p>
     * Author: Wes
     * https://stackoverflow.com/users/2287613/wes
     * <p>
     * This method computes sqrt(n) in O(n.bitLength()) time,
     * and computes it exactly. By "exactly", I mean it returns
     * not only the (floor of the) square root s, but also the
     * remainder r, such that r >= 0, n = s^2 + r, and
     * n < (s + 1)^2.
     *
     * @param n The argument n, as described above.
     * @return An array of two values, where the first element
     * of the array is s and the second is r, as
     * described above.
     * @throws IllegalArgumentException if n is not nonnegative.
     */
    public static BigInteger[] sqrt(BigInteger n) {

        if (n == null || n.signum() < 0) {
            throw new IllegalArgumentException();
        }

        int bl = n.bitLength();
        if ((bl & 1) != 0) {
            ++bl;
        }

        BigInteger s = ZERO;
        BigInteger r = ZERO;

        while (bl >= 2) {
            s = s.shiftLeft(1);

            BigInteger crumb = n.testBit(--bl)
                    ? (n.testBit(--bl) ? THREE : TWO)
                    : (n.testBit(--bl) ? ONE : ZERO);
            r = r.shiftLeft(2).add(crumb);

            BigInteger d = s.shiftLeft(1);
            if (d.compareTo(r) < 0) {
                s = s.add(ONE);
                r = r.subtract(d).subtract(ONE);
            }
        }

        assert r.signum() >= 0;
        assert n.equals(s.multiply(s).add(r));
        assert n.compareTo(s.add(ONE).multiply(s.add(ONE))) < 0;

        return new BigInteger[]{s, r};

    } /*sqrt*/

    /**
     * toggleOutput
     * Toggles on or off whether these algorithms should print output as they
     * operate.  No output is faster, but harder to debug.
     */
    public static void toggleOutput() {

        printOutput = !printOutput;

    } /*toggleOutput*/
}