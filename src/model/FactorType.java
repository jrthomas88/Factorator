package model;

/**
 * FactorType
 *
 * @author Jon Thomas
 * Holds enums that are to be used by the clients/servers.  Each enum
 * represents which factoring algorithm a client will end up using.
 */

public enum FactorType {

    // all of the factoring types, including server types and none
    TD2, TDRN, POLLARDS, FERMAT, TD2Server, TDRNServer, POLLARDSServer,
    FERMATServer, NONE;

    /**
     * asInt
     * Return this algorithm type represented as a number.  E.g. TD2 = 0,
     * TDRN = 1, etc.
     *
     * @param type the type to convert
     * @return int corresponding to which algorithm is being used
     */
    public static int asInt(FactorType type) {
        if (type == TD2Server || type == TD2) {
            return 0;
        } else if (type == TDRNServer || type == TDRN) {
            return 1;
        } else if (type == FERMATServer || type == FERMAT) {
            return 2;
        } else if (type == POLLARDSServer || type == POLLARDS) {
            return 3;
        }

        return -1;
    }

    /**
     * getAddress
     * Given a FactorType, return the server port where that algorithm is
     * supposed to connect.
     *
     * @param type the FactorType being queried
     * @return the port number of where the sub-server is
     */
    public static int getAddress(FactorType type) {
        if (type == TD2Server || type == TD2) {
            return FactorSubServer.TD2PORT;
        } else if (type == TDRNServer || type == TDRN) {
            return FactorSubServer.TDRNPORT;
        } else if (type == FERMATServer || type == FERMAT) {
            return FactorSubServer.FERMATPORT;
        } else if (type == POLLARDSServer || type == POLLARDS) {
            return FactorSubServer.POLLARDPORT;
        }

        return -1;
    }

    /**
     * toString
     * Returns a string representing this FactorType
     *
     * @param type the type to check
     * @return a String of that FactorType
     */
    public static String toString(FactorType type) {
        if (type == TD2Server || type == TD2) {
            return "TD2";
        } else if (type == TDRNServer || type == TDRN) {
            return "TDRN";
        } else if (type == FERMATServer || type == FERMAT) {
            return "Fermat";
        } else if (type == POLLARDSServer || type == POLLARDS) {
            return "Pollard's";
        }

        return null;
    }
}
