package model;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * FactorData.java
 * This class is a serializable object designed to be sent between clients
 * and servers doing factorization.  This object serves two purposes: first,
 * it holds the number n to be factored, along with data to assist in
 * factoring it.  Second, it holds a message slot so that a server and client
 * can communicate with each other.
 */

public class FactorData implements Serializable {

    // constants
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.valueOf(2);

    // type
    private FactorType type;

    // server info
    private String subservername;
    private int clientPort;
    private String clientAddress;

    // the composite number. plus a list of found factors
    private BigInteger num;
    private BigInteger rootNum; // sqrt(num)
    private BigInteger factor;
    private String message;

    // data for TD2/TDRN algorithm
    private BigInteger lowerBoundTD; // factor is bigger than this
    private BigInteger upperBoundTD; // factor is less than this

    // data for Fermat's
    private BigInteger fermatStartVal; // starting value for Fermat's

    // data for Pollard's
    private BigInteger twoToImodN; // starting power for pollard's

    private BigInteger base; // baseVal used for pollard's
    private BigInteger startP; // starting power for pollard's
    private BigInteger lowerBoundP; // lower bound for pollard's
    private BigInteger upperBoundP; // upper bound for polard's
    private BigInteger boundAmount; // last used bound value

    /**
     * FactorData
     * This constructor initializes the default values used for all of the
     * bounds and starting values.  It takes the number to be factored, num,
     * as a parameter.
     *
     * @param num the composite number to be factored.
     */
    public FactorData(BigInteger num) {

        if (num == null) { // clients may pass in null so they can send to server
            return;
        }

        this.num = num; // number being factored
        rootNum = FactorMath.sqrt(num)[0]; // sqrt(num)
        factor = null;

        // set data for TD bounds
        lowerBoundTD = TWO; // start counting from the first prime 2
        upperBoundTD = rootNum; // will find a factor before sqrt(num)

        // set data for Fermat's
        fermatStartVal = rootNum.add(ONE); // start must be > sqrt(num)

        // set data for Pollard's
        twoToImodN = TWO; // a^1 = 2
        base = TWO; // a = 2
        startP = ONE; // power base is risen to
        lowerBoundP = ONE; // optimal bound is >=
        upperBoundP = rootNum; // optimal bound is <=

    }

    public BigInteger getPollardBase() {
        return base;
    }

    /**
     * setLowerBoundTD
     * Sets what the lower bound used during trial division should be.  Must
     * be less than the upper bound
     *
     * @param lb the new lower bound
     */
    public void setLowerBoundTD(BigInteger lb) {
        lowerBoundTD = lb;

    }

    /**
     * setUpperBoundTD
     * Sets what the upper bound in trial division should be.  Must be
     * greater than the lower bound.
     *
     * @param ub the new upper bound
     */
    public void setUpperBoundTD(BigInteger ub) {

        upperBoundTD = ub;

        if (upperBoundTD.compareTo(rootNum) > 0) {
            upperBoundTD = rootNum;
        }

    }

    /**
     * getTDBounds
     * Returns an array containing the lower and upper bounds of trial division.
     *
     * @return BigInteger[] holding lowerBoundTD and upperBoundTD
     */
    public BigInteger[] getTDBounds() {

        return new BigInteger[]{lowerBoundTD, upperBoundTD};

    }

    /**
     * getFermatStartVal
     * return the starting value for Fermat's Factoring Algorithm
     *
     * @return BigInteger, fermatStartVal
     */
    public BigInteger getFermatStartVal() {

        return fermatStartVal;

    }

    /**
     * setFermatStartVal
     * sets the new value for Fermat's factoring algorithm.  Must be greater
     * than sqrt(num)+1.
     *
     * @param startVal the new starting value used in Fermat's
     */
    public void setFermatStartVal(BigInteger startVal) {

        if (fermatStartVal.compareTo(rootNum.add(ONE)) < 0) {
            throw new IllegalArgumentException("Fermat's startVal needs to be" +
                                               " > sqrt(num)");
        }

        fermatStartVal = startVal;

    }

    /**
     * resetBasePollards
     * Give a new base to use in Pollard's p-1 algorithm
     *
     * @param base the new base to be using
     */
    public void resetBasePollards(BigInteger base) {

        this.base = base;
        twoToImodN = base;
        lowerBoundP = ONE;
        upperBoundP = rootNum;

    }

    /**
     * resetStartTMN
     * resets toToIModN to the starting value of base.  Used when needing to
     * re-run Pollard's.
     */
    public void resetStartTMN() {

        twoToImodN = base;

    }

    /**
     * incrementPollardBase
     * Used when the current base used in pollard's has failed to return a
     * value.  Increments the base by one and resets all related fields so
     * that pollard's can be run again.
     */
    public void incrementPollardsBase() {

        base = base.add(ONE);
        twoToImodN = base;
        lowerBoundP = ONE;
        upperBoundP = rootNum;

    }

    /**
     * getTwoToImodN
     * returns the current value of twoTomodN, which should be approximately
     * base^(bound!) mod num.
     *
     * @return twoToImodN, the current power computed by Pollard's
     */
    public BigInteger getTwoToImodN() {

        return twoToImodN;

    }

    /**
     * setTwoToImodN
     * Given a new value, reset tmn to that value.  For Pollard's.  Used when a
     * previous power was too small but we don't want to repeat any work.
     *
     * @param tmn the new twoToImodN value
     */
    public void setTwoToImodN(BigInteger tmn) {

        twoToImodN = tmn;

    }

    /**
     * getPollardsBounds
     * returns the upper and lower bounds for pollard's.
     *
     * @return new BigInteger[], holding lowerBoundP and upperBoundP
     */
    public BigInteger[] getPollardsBounds() {

        return new BigInteger[]{lowerBoundP, upperBoundP};

    }

    /**
     * getNum
     * returns composite value num
     *
     * @return num
     */
    public BigInteger getNum() {

        return num;

    }

    /**
     * getMessage
     * gets message that may have been sent from a client/server
     *
     * @return message sent by either client or server
     */
    public String getMessage() {

        return message;

    }

    /**
     * setMessage
     * sets a message to be sent to a client/server
     *
     * @param message the message to send
     */
    public void setMessage(String message) {

        this.message = message;

    }

    /**
     * addFactor
     * if a factor of num has been found, this method adds it to the list of
     * factors found.
     *
     * @param factor the factor that was found
     */
    public void addFactor(BigInteger factor) {

        if (!num.mod(factor).equals(ZERO)) {
            throw new IllegalArgumentException(factor + " is not a factor of " +
                                               "" + num + ".");
        }

        this.factor = factor;
    }

    /**
     * getFactors
     * return the list of factors.
     *
     * @return List of factors
     */
    public BigInteger getFactor() {

        return factor;

    }

    /**
     * getAttempts
     * return the number of attempts to make using Fermat's
     *
     * @return long, attempts to make
     */
    public long getAttempts() {

        return (long) 1000;

    }

    /**
     * getStartP
     * return the starting location for Pollard's p-1 algorithm
     *
     * @return BigInteger, start value for pollard's
     */
    public BigInteger getStartP() {

        return startP;

    }

    /**
     * setStartP
     * set the starting value to be used in Pollard's p-1 algorithm
     *
     * @param startP the new starting value
     */
    public void setStartP(BigInteger startP) {

        this.startP = startP;

    }

    /**
     * setLowerBoundP
     * Set the lower bound to be used in Pollard's p-1 algorithm
     *
     * @param lb the new lower bound
     */
    public void setLowerBoundP(BigInteger lb) {

        lowerBoundP = lb;

    }

    /**
     * setUpperBoundP
     * Set the upper bound to be used in Pollard's p-1 algorithm
     *
     * @param ub the new upper bound
     */
    public void setUpperBoundP(BigInteger ub) {

        upperBoundP = ub;
        if (upperBoundP.compareTo(rootNum) > 0) {
            upperBoundP = rootNum;
        }

    }

    public FactorType getType() {
        return type;
    }

    public void setType(FactorType type) {
        this.type = type;
    }

    public String getSubservername() {
        return subservername;
    }

    public void setSubservername(String subservername) {
        this.subservername = subservername;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public BigInteger getBoundAmount() {
        return boundAmount;
    }

    public void setBoundAmount(BigInteger boundAmount) {
        this.boundAmount = boundAmount;
    }
}
