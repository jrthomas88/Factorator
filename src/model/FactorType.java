package model;

/**
 * FactorType
 *
 * @author Jon Thomas
 * Holds enums that are to be used by the clients/servers.  Each enum
 * represents which factoring algorithm a client will end up using.
 */

public enum FactorType {

    TD2, TDRN, POLLARDS, FERMAT, TD2Server, TDRNServer, POLLARDSServer,
    FERMATServer, NONE;

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
