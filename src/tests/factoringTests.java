package tests;

import model.FactorClient;
import model.FactorData;
import model.FactorMath;
import model.FactorType;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class factoringTests {

    private boolean builtPrimes = false;
    private List<BigInteger> primes;

    private void buildPrimes() {
        primes = new ArrayList<>();
        for (int i = 2; i < 1000; i++) {
            BigInteger temp = BigInteger.valueOf(i);
            if (temp.isProbablePrime(100)) {
                primes.add(temp);
                //System.out.println(temp);
            }
        }
        builtPrimes = !builtPrimes;
    }

    @Test
    public void testTD2Factoring() {
        if (!builtPrimes) {
            buildPrimes();
        }

        for (int i = 0; i < primes.size(); i++) {
            for (int j = 0; j < primes.size(); j++) {
                BigInteger temp0 = primes.get(i);
                BigInteger temp1 = primes.get(j);
                BigInteger n = temp0.multiply(temp1);
                BigInteger p = FactorMath.findFactorTD2(n, BigInteger.valueOf
                        (2), FactorMath.sqrt(n)[0]);
                assertTrue(p != null);
                assertTrue(p.equals(temp0) || p.equals(temp1));
            }
        }
    }

    @Test
    public void testTDRNFactoring() {
        if (!builtPrimes) {
            buildPrimes();
        }

        for (int i = 0; i < primes.size(); i++) {
            for (int j = 0; j < primes.size(); j++) {
                BigInteger temp0 = primes.get(i);
                BigInteger temp1 = primes.get(j);
                BigInteger n = temp0.multiply(temp1);
                BigInteger p = FactorMath.findFactorTDRN(n, BigInteger.valueOf
                        (2), FactorMath.sqrt(n)[0]);
                assertTrue(p != null);
                assertTrue(p.equals(temp0) || p.equals(temp1));
            }
        }
    }

    @Test
    public void testFermatsFactoring() {
        if (!builtPrimes) {
            buildPrimes();
        }

        for (int i = 0; i < primes.size(); i++) {
            for (int j = 0; j < primes.size(); j++) {
                BigInteger temp0 = primes.get(i);
                BigInteger temp1 = primes.get(j);
                BigInteger n = temp0.multiply(temp1);
                BigInteger p = null;
                BigInteger sqrtn = FactorMath.sqrt(n)[0].add(BigInteger.ONE);
                long attempts = 10000;

                while (p == null) {
                    p = FactorMath.findFactorFermat(n, sqrtn, attempts);
                    sqrtn = sqrtn.add(BigInteger.valueOf(attempts));
                }

                if (p.equals(BigInteger.ONE)) {
                    System.out.println(temp0 + " " + temp1 + " " + sqrtn
                            .subtract(BigInteger.valueOf(attempts)));
                }

                assertTrue(p.equals(temp0) || p.equals(temp1));
            }
        }
    }

    @Test
    public void testPollardsFactoring() {
        if (!builtPrimes) {
            buildPrimes();
        }

        for (int i = 0; i < primes.size(); i++) {
            for (int j = 0; j < primes.size(); j++) {
                BigInteger temp0 = primes.get(i);
                BigInteger temp1 = primes.get(j);
                BigInteger n = temp0.multiply(temp1);
                BigInteger twoModN = BigInteger.valueOf(2);
                BigInteger original = BigInteger.valueOf(2);
                BigInteger start = BigInteger.ONE;
                BigInteger LowerBound = BigInteger.ONE;
                BigInteger UpperBound = FactorMath.sqrt(n)[0];
                BigInteger[] p;

                while (true) {

                    BigInteger bound = LowerBound.add(UpperBound).divide
                            (BigInteger.valueOf(2));

                    if (UpperBound.subtract(bound).compareTo(BigInteger.ONE)
                        <= 0) {
                        if (bound.subtract(LowerBound).compareTo(BigInteger
                                .ONE) <= 0) {
                            original = original.add(BigInteger.ONE);
                            twoModN = original;
                            LowerBound = BigInteger.ONE;
                            UpperBound = FactorMath.sqrt(n)[0];
                            bound = LowerBound.add(UpperBound).divide
                                    (BigInteger.valueOf(2));
                        }
                    }

                    p = FactorMath.findFactorPollards(n, twoModN,
                            start, bound);

                    if (p[0].equals(BigInteger.ONE)) {

                        LowerBound = bound;

                        twoModN = p[1];
                        start = bound.add(BigInteger.ONE);
                    } else if (p[0].equals(n)) {
                        UpperBound = bound;
                        twoModN = BigInteger.valueOf(2);
                    } else {
                        break;
                    }

                }

                assertTrue(p[0].equals(temp0) || p[0].equals(temp1));
            }
        }
    }

    @Test
    public void testClientTD2() {
        BigInteger p = new BigInteger("6563");
        BigInteger q = new BigInteger("9311");
        BigInteger n = p.multiply(q);

        FactorData data = new FactorData(n);
        FactorClient client = new FactorClient(null);
        client.setType(FactorType.TD2);
        client.startFactoring();
        data = client.getData();
        BigInteger factors = data.getFactor();
        assertTrue(factors.equals(p) || factors.equals(q));
    }

    @Test
    public void testClientTDRN() {
        BigInteger p = new BigInteger("7691");
        BigInteger q = new BigInteger("1987");
        BigInteger n = p.multiply(q);

        FactorData data = new FactorData(n);
        FactorClient client = new FactorClient(null);
        client.setType(FactorType.TDRN);
        client.startFactoring();
        data = client.getData();
        BigInteger factors = data.getFactor();
        assertTrue(factors.equals(p) || factors.equals(q));
    }

    @Test
    public void testClientFermat() {
        BigInteger p = new BigInteger("2017");
        BigInteger q = new BigInteger("1451");
        BigInteger n = p.multiply(q);

        FactorData data = new FactorData(n);
        FactorClient client = new FactorClient(null);
        client.setType(FactorType.FERMAT);
        client.startFactoring();
        data = client.getData();

        while (data.getMessage().equals("no factor found")) {
            client.startFactoring();
        }

        BigInteger factors = data.getFactor();
        assertTrue(factors.equals(p) || factors.equals(q));
    }

    @Test
    public void testClientPollards() {
        BigInteger p = new BigInteger("9001");
        BigInteger q = new BigInteger("5801");
        BigInteger n = p.multiply(q);

        FactorData data = new FactorData(n);
        FactorClient client = new FactorClient(null);
        client.setType(FactorType.FERMAT);
        client.startFactoring();

        BigInteger factors = data.getFactor();
        assertTrue(factors.equals(p) || factors.equals(q));
    }
}
