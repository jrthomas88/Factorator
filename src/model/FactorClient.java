package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * FactorClient.java
 *
 * @author Jon Thomas
 * <p>
 * This object is to serve as a client that
 * will connect to a given sub-server.  This object will send
 * communications back to this sub-server about its progress on factoring
 * a number.  This object will attempt one of four factoring algorithms as
 * dictated by its sub-server.  Factoring calculations are handled by the
 * FactorMath.java class.  This client should be able to run on the same
 * machine as one sub-server or main server.
 */

public class FactorClient {

    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private Thread listenThread;
    // instance variables
    private FactorType type; // what algorithm are we performing
    private FactorData data; // data relating to the factored integer
    private boolean completed = false;
    private Socket subsocket; // the sub-server socket
    private Socket socket; // the server socket
    private int sserverport;
    private String ssaddress = null;
    private String serverAddress;
    private int myPort;
    private String myAddress;


    /**
     * FactorClient
     * The constructor for this class.  Takes a FactorData object as a
     * parameter.  This object contains a number to factor along with data
     * relating to that number.
     *
     * @param host The string of the host server location
     */

    public FactorClient(String host) {
        serverAddress = host;
        type = FactorType.NONE; // no algorithm until a server decides
        this.data = new FactorData(null);
        data.setMessage("new client");
        startListener();

        try {
            socket = new Socket(host, FactorServer.SERVERPORT);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket
                    .getOutputStream());

            outputStream.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            listenThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * main
     * Connects to a server and creates a client
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("No host name provided\njava FactorClient " +
                               "[hostname]");
        }
        String host = args[0];

        FactorClient client = new FactorClient(host);
    }

    private void startListener() {
        ClientListener listener = new ClientListener();
        listenThread = new Thread(listener);
        listenThread.start();
        data.setClientPort(myPort);
        data.setClientAddress(myAddress);
    }

    /**
     * setType
     * Use an enum in the FactorType.java file to dictate what factoring
     * algorithm this object is to implement.
     *
     * @param type the algorithm that will be used
     */
    public void setType(FactorType type) {

        this.type = type;
        if (type == FactorType.TD2) {
            sserverport = FactorSubServer.TD2PORT;
        } else if (type == FactorType.TDRN) {
            sserverport = FactorSubServer.TDRNPORT;
        } else if (type == FactorType.FERMAT) {
            sserverport = FactorSubServer.FERMATPORT;
        } else if (type == FactorType.POLLARDS) {
            sserverport = FactorSubServer.POLLARDPORT;
        }

    }

    /**
     * startFactoring
     * Using the value of 'type', launch the requested factoring algorithm
     * If no type has been selected, method prints out a status statement
     */
    public void startFactoring() {

        System.out.println("factoring with " + FactorType.toString(type));

        if (type == FactorType.TD2) {
            factorTD2();
        } else if (type == FactorType.TDRN) {
            factorTDRN();
        } else if (type == FactorType.FERMAT) {
            factorFermat();
        } else if (type == FactorType.POLLARDS) {
            factorPollards();
        } else {
            System.out.println("Client has not been assigned a factoring " +
                               "algorithm.");
        }

    }

    /**
     * getData
     * return the data object for this client
     *
     * @return the FactorData object containing the int to be factored
     */
    public FactorData getData() {

        return data;

    }

    // factorTD2
    // Use trial-division counting up from 2 to determine if a number is a
    // factor.  Factoring is controlled by the FactorMath class.
    private void factorTD2() {

        System.out.println("Factoring using TD2");

        BigInteger num = data.getNum(); // the number to factor
        BigInteger[] bounds = data.getTDBounds(); // the upper/lower bounds

        System.out.println("Bounds are " + bounds[0] + " to " + bounds[1]);

        // factor num
        BigInteger factor = FactorMath.findFactorTD2(num, bounds[0], bounds[1]);

        // send result to sendOutputTD
        sendOutputTD(num, bounds, factor);
    }

    // factorTDRN
    // Use trial-division counting down from sqrt(num) to determine if a number
    // is a factor.  Factoring is controlled by the FactorMath class.
    private void factorTDRN() {

        System.out.println("Factoring using TDRN");

        BigInteger num = data.getNum(); // number to factor
        BigInteger[] bounds = data.getTDBounds(); // upper or lower bounds

        // factor num
        BigInteger factor = FactorMath.findFactorTDRN(num, bounds[0],
                bounds[1]);

        // send output to sendOutputTD
        sendOutputTD(num, bounds, factor);

    }

    // sendOutputTD
    // handles result of TD factoring.  if the factor is null, then no output
    // was reported.  Otherwise, add the factor to data and send to the
    // sub-server
    private void sendOutputTD(BigInteger num, BigInteger[] bounds,
                              BigInteger factor) {

        // if no factor was found
        if (factor == null) {
            System.out.println(num + " has no factor in range [" + bounds[0] +
                               ", " + bounds[1] + "]");
            data.setMessage("failed");
        } else {
            System.out.println("Found factor = " + factor);
            data.addFactor(factor);
            data.setMessage("factor found");
        }
        sendData();
    }

    private void setSubSocket() {
        try {
            subsocket = new Socket(ssaddress, sserverport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData() {
        setSubSocket();
        data.setClientPort(myPort);
        data.setClientAddress(myAddress);
        data.setType(type);
        System.out.println("Client sending data, message = " + data.getMessage());
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(subsocket
                    .getOutputStream());
            outputStream.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // factorFermat
    // use Fermat's Factorization Algorithm to factor a number num.  Info on
    // the factorization algorithm can be found in the FactorMath class.
    // Will run a number of attempts specified in the data object.  If it
    // returns null, then no factor was found in that range.  Call this
    // function again to get another factor.
    private void factorFermat() {

        BigInteger num = data.getNum();
        BigInteger start = data.getFermatStartVal();
        long attempts = data.getAttempts();

        BigInteger factor = FactorMath.findFactorFermat(num, start, attempts);

        if (factor == null) {
            data.setFermatStartVal(start.add(BigInteger.valueOf(attempts)));
            data.setMessage("failed");
        } else {
            System.out.println("Found factor = " + factor);
            data.addFactor(factor);
            data.setMessage("factor found");
        }

        sendData();
    }

    // factorPollards
    // Uses Pollard's p-1 algorithm to try and find a factor of num.  Num is
    // specified in the data object.  Unlike Fermat's, this function will
    // loop endlessly unless interrupted by another process or it finds a
    // factor.
    private void factorPollards() {

        BigInteger num = data.getNum(); // get number to factor
        BigInteger[] factor; // index 0 will hold factor, index 1 will hold
        // the next major power if desired

        BigInteger[] bounds = data.getPollardsBounds();

        // create a bound somewhere between the upper and lower bounds
        BigInteger bound = bounds[0].add(bounds[1].divide(TWO));

        // if the bounds are too close to each other, then our starting
        // base value likely isn't good.  Reset the base to a new value.
        if (bound.subtract(bounds[0]).compareTo(ONE) <= 0) {
            if (bounds[1].subtract(bound).compareTo(ONE) <= 0) {
                data.incrementPollardsBase();
                data.setMessage("failed");
                sendData();
                return;
            }
        }

        // get factor
        factor = FactorMath.findFactorPollards(num, data.getTwoToImodN(),
                data.getStartP(), bound);

        // if factor == 1, then our bound was too small
        if (factor[0].equals(ONE)) {
            data.setLowerBoundP(bound); // set the lower bound to be higher
            data.setTwoToImodN(factor[1]); // carry over same power
            data.setStartP(bound.add(ONE)); // set power to bound+1
            data.setBoundAmount(bound);
            data.setMessage("failed");
            sendData();
        } else if (factor[0].equals(num)) {
            // if factor == num, our bound is too high
            data.setUpperBoundP(bound); // set new upper bound
            data.resetStartTMN(); // reset the power
            data.setBoundAmount(bound);
            data.setMessage("failed");
            sendData();
        } else {
            // we found a factor
            data.addFactor(factor[0]); // add factor to data list
            data.setMessage("factor found"); // set message for server
            sendData();
        }

    }

    private class ClientListener implements Runnable {

        private ServerSocket socketListener;

        ClientListener() {
            try {
                socketListener = new ServerSocket(0);
                myPort = socketListener.getLocalPort();
                myAddress = InetAddress.getLocalHost().getHostName();

                System.out.println("Client creating socket on " + myAddress + " " +
                                   "on port " + myPort);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Socket s;
            while (!completed) {
                try {
                    s = socketListener.accept();
                    ClientReader reader = new ClientReader(s);
                    Thread t = new Thread(reader);
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.exit(0);
        }
    }

    private class ClientReader implements Runnable {

        Socket mySocket;

        ClientReader(Socket socket) {
            mySocket = socket;
        }


        @Override
        public void run() {
            FactorData factorData;

            try {
                ObjectInputStream inputStream = new ObjectInputStream(mySocket
                        .getInputStream());
                factorData = (FactorData) inputStream.readObject();
                String message = factorData.getMessage();

                System.out.println("Client reader read object, message = " +
                                   "" + message);

                if (message.equals("quit")) {
                    completed = true;
                    System.out.println("Client quitting");
                    System.exit(0);
                }

                if (message.equals("run")) {
                    data = factorData;
                    startFactoring();
                }

                if (message.length() >= 5 && message.substring(0, 5)
                        .equals("name:")) {
                    String hostname = message.substring(5);

                    FactorType type = data.getType();

                    if (type == FactorType.TD2) {
                        sserverport = FactorSubServer.TD2PORT;
                    } else if (type == FactorType.TDRN) {
                        sserverport = FactorSubServer.TDRNPORT;
                    } else if (type == FactorType.FERMAT) {
                        sserverport = FactorSubServer.FERMATPORT;
                    } else if (type == FactorType.POLLARDS) {
                        sserverport = FactorSubServer.POLLARDPORT;
                    }

                    ssaddress = hostname;
                    subsocket = new Socket(hostname, sserverport);
                    mySocket = subsocket;
                    socket = mySocket;

                    System.out.println("Client: Changed socket to sub-server");

                    ObjectOutputStream outputStream = new
                            ObjectOutputStream(mySocket
                            .getOutputStream());

                    factorData.setMessage("connect");
                    factorData.setClientAddress(myAddress);
                    factorData.setClientPort(myPort);
                    outputStream.writeObject(factorData);

                }

                if (message.equals("new value")) {
                    FactorType type = factorData.getType();
                    FactorSubServer sserver = null;

                    if (type == FactorType.TD2Server) {
                        sserver = new FactorSubServer(factorData);
                        ssaddress = InetAddress.getLocalHost().getHostAddress();
                        sserverport = FactorSubServer.TD2PORT;
                        data.setType(FactorType.TD2);
                        setType(FactorType.TD2);
                    } else if (type == FactorType.TDRNServer) {
                        sserver = new FactorSubServer(factorData);
                        ssaddress = InetAddress.getLocalHost().getHostAddress();
                        sserverport = FactorSubServer.TDRNPORT;
                        data.setType(FactorType.TDRN);
                        setType(FactorType.TDRN);
                    } else if (type == FactorType.FERMATServer) {
                        sserver = new FactorSubServer(factorData);
                        ssaddress = InetAddress.getLocalHost().getHostAddress();
                        sserverport = FactorSubServer.FERMATPORT;
                        data.setType(FactorType.FERMAT);
                        setType(FactorType.FERMAT);
                    } else if (type == FactorType.POLLARDSServer) {
                        sserver = new FactorSubServer(factorData);
                        ssaddress = InetAddress.getLocalHost().getHostAddress();
                        sserverport = FactorSubServer.POLLARDPORT;
                        data.setType(FactorType.POLLARDS);
                        setType(FactorType.POLLARDS);
                    } else {
                        setType(type);
                        ssaddress = factorData.getSubservername();
                        sserverport = FactorType.getAddress(type);
                    }

                    if (sserver != null) {
                        while (!sserver.isReady()) {
                            Thread.sleep(1000);
                        }
                        sserver.setServerHost(serverAddress);
                    }

                    System.out.println("Contacting subserver at " + ssaddress + "" +
                                       " port " + sserverport);
                    subsocket = new Socket(ssaddress, sserverport);
                    data.setMessage("new");
                    data.setClientPort(myPort);
                    data.setClientAddress(myAddress);

                    ObjectOutputStream outputStream = new ObjectOutputStream
                            (subsocket.getOutputStream());
                    outputStream.writeObject(data);

                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
