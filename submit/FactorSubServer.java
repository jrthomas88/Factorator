package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * FactorSubServer.java
 * <p>
 * This object is created by a client when that client is one of the first
 * four to connect to a given FactorServer.  Depending on the FactorType
 * passed in with the FactorData object, this SubServer starts a ServerSocket
 * at a well-known port number and listens for clients to connect with it.
 * It keeps a list of all connected clients and sends to them info about a
 * number to factor.
 */
public class FactorSubServer {

    public static final int TD2PORT = 12486;
    public static final int TDRNPORT = 10897;
    public static final int FERMATPORT = 12458;
    public static final int POLLARDPORT = 11489;
    private FactorType type;
    private boolean complete = false; // true when factoring is done

    private List<Socket> clients; // a list of client sockets

    private Socket server; // socket for main server
    private int socketPort;
    private String serverHost;

    // bounds and info for TD algs
    private BigInteger tdLowerBound;
    private BigInteger tdUpperBound;
    private BigInteger incrementAmount;

    // fermat starting value
    private BigInteger fermatStartValue;

    // info for Pollard's
    private BigInteger pollardBase;
    private BigInteger pollardLBound;
    private BigInteger pollardUBound;
    private BigInteger pollardPower;

    // equals true when listener ready to start
    private boolean ready = false;

    /**
     * FactorSubServer
     * initialize all data values and create a server listener.
     *
     * @param data The FactorData object about what number to factor
     */
    FactorSubServer(FactorData data) {
        clients = new LinkedList<>();
        type = data.getType();
        fermatStartValue = BigInteger.ZERO;
        pollardBase = BigInteger.valueOf(2);
        pollardUBound = BigInteger.valueOf(100);
        pollardLBound = BigInteger.ONE;
        printInfo();
        ServerListener listener = new ServerListener();
        Thread listenThread = new Thread(listener);
        listenThread.start();
    }

    /**
     * printInfo
     * Just prints a display message to the terminal.  Has no functional
     * value, just for presentation.
     */
    private void printInfo() {
        System.out.println("*-----------------------------*");
        System.out.println("*       FactorSubServer       *");
        System.out.println("*-----------------------------*");
        System.out.println("\nMain subserver for " + FactorType.toString(type));
        System.out.println("\n*-----------------------------*\n");
    }

    /**
     * isReady
     * Returns true once the listener is live and clients can start sending
     * to it.
     *
     * @return whether listener is ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * setServerHost
     * Create a connection to the main server and send information about my
     * host name.  This allows the server to know where I am so it can send
     * information to me later.
     *
     * @param hostname the location of the machine this SubServer is running on
     */
    public void setServerHost(String hostname) {
        try {
            // create socket to main server
            server = new Socket(hostname, FactorServer.SERVERPORT);
            serverHost = hostname;
            socketPort = FactorServer.SERVERPORT;
            FactorData data = new FactorData(null);

            // "name:" tells server that this is a new SubServer
            String myname = "name:" + FactorType.asInt(type);

            // get localHost information and add to data
            myname += InetAddress.getLocalHost().getHostAddress();

            //send data to server
            data.setMessage(myname);
            data.setType(type);
            ObjectOutputStream outputStream = new ObjectOutputStream(server
                    .getOutputStream());
            outputStream.writeObject(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handleReadData
     * Given a FactorData object read from a socket, examine that object and
     * determine what actions need to be taken.  Information is found by the
     * getMessage() method on FactorData.
     *
     * @param data the FactorData read from a socket
     */
    private void handleReadData(FactorData data) {
        String message = data.getMessage();

        System.out.println("SubServer read message: " + message);

        // if "quit", then notify all clients that they need to quit
        if (message.equals("quit")) {
            complete = true;
            // skip 0 since it's the same machine I'm on.  When I exit, it'll
            // also exit.
            for (int i = 1; i < clients.size(); i++) {
                Socket client = clients.get(i);
                int port = client.getPort();
                String address = client.getInetAddress().getHostAddress();
                try {
                    client = new Socket(address, port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputData(data, client);
            }
            // then quit myself
            System.exit(0);
        }

        // server has decided it's time to factor
        if (message.equals("start") || message.equals("new value")) {
            int clientCount = clients.size();

            /*--------------------------------
                Handling the trial divisions
             ---------------------------------*/

            // Trial Division from 2/RN
            if (type == FactorType.TD2Server || type == FactorType.TDRNServer) {
                BigInteger[] bounds = data.getTDBounds();
                tdLowerBound = bounds[0];
                tdUpperBound = bounds[1];

                // set bounds
                incrementAmount = tdUpperBound.subtract(tdLowerBound);
                incrementAmount = incrementAmount.divide(BigInteger.valueOf
                        (clientCount));

                incrementAmount = incrementAmount.min(BigInteger.valueOf
                        (100_000_000));


                // send factoring data to clients
                System.out.println("Sending data to clients");
                for (Socket client : clients) {

                    if (type == FactorType.TD2Server) {
                        tdUpperBound = tdLowerBound.add(incrementAmount);
                    } else {
                        tdLowerBound = tdUpperBound.subtract(incrementAmount);

                        System.out.println("Upper bound = " + tdUpperBound + ", " +
                                           "Lower bound = " + tdLowerBound);
                    }

                    data.setUpperBoundTD(tdUpperBound);
                    data.setLowerBoundTD(tdLowerBound);
                    data.setMessage("run");


                    outputData(data, client);

                    if (type == FactorType.TD2Server) {
                        tdLowerBound = tdUpperBound.add(BigInteger.ONE);
                    } else {
                        tdUpperBound = tdLowerBound.subtract(BigInteger.ONE);
                    }
                }

                /*--------------------*
                    Fermat Factoring
                 *--------------------*/

            } else if (type == FactorType.FERMATServer) {
                fermatStartValue = data.getFermatStartVal();
                long attempts = data.getAttempts();

                for (Socket client : clients) {
                    data.setFermatStartVal(fermatStartValue);
                    data.setMessage("run");

                    outputData(data, client);

                    fermatStartValue = fermatStartValue.add(BigInteger
                            .valueOf(attempts));
                }

                /*--------------------------*
                    Pollard's p-1 algorithm
                 *--------------------------*/

            } else if (type == FactorType.POLLARDSServer) {
                BigInteger[] bounds = data.getPollardsBounds();
                pollardLBound = bounds[0];
                pollardUBound = bounds[0];
                for (Socket client : clients) {
                    pollardUBound = pollardUBound.add(BigInteger.valueOf(1000));
                    data.setUpperBoundP(pollardUBound);
                    data.setLowerBoundP(pollardLBound);
                    data.setMessage("run");
                    outputData(data, client);
                }

            }
        }

        // a factor has been found
        // send data to server.  It'll handle it from there.
        if (message.equals("factor found")) {
            System.out.println("Sending data to Server");
            setServer();
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(server
                        .getOutputStream());
                outputStream.writeObject(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // outputData
    // given a data object and a client, output the data file to the machine
    // associated with that socket.
    private void outputData(FactorData data, Socket client) {
        String address = client.getLocalAddress().getHostName();
        int port = client.getPort();
        clients.remove(client);
        try {
            client = new Socket(address,port);
            clients.add(client);
            ObjectOutputStream outputStream = new ObjectOutputStream
                    (client.getOutputStream());
            outputStream.writeObject(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // setServer
    // Establish the socket in which the main server is located
    private void setServer() {
        try {
            server = new Socket(serverHost, socketPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ServerListener
    // Creates a listener that listens on a well-known port for connections
    // to this server.
    private class ServerListener implements Runnable {

        private int port;

        ServerListener() {
            port = FactorType.getAddress(type);
        }

        @Override
        public void run() {

            ServerSocket listen;
            Socket socket;
            ServerReader reader;

            try {
                System.out.println("Subserver creating socket on port #" + port);
                listen = new ServerSocket(port, 10);
                while (!complete) {
                    System.out.println("Sub-Server waiting for connection");

                    ready = true;
                    socket = listen.accept();

                    System.out.println("SubServer: Connection found!");

                    reader = new ServerReader(socket);
                    Thread t = new Thread(reader);
                    t.start();
                }
                System.out.println("SubServer quitting");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerReader implements Runnable {

        private Socket mySocket;

        /**
         * ServerReader
         * Sets socket field to passed in parameter
         *
         * @param socket the socket this is reading from
         */
        ServerReader(Socket socket) {
            mySocket = socket;
        }

        /**
         * run
         * Tries to read data coming in from socket
         */
        @Override
        public void run() {
            ObjectInputStream inputStream;
            FactorData data;
            try {

                if (mySocket.isClosed()) {
                    return;
                }

                try {
                    inputStream = new ObjectInputStream(mySocket.getInputStream());
                } catch (java.io.EOFException e) {
                    return;
                }

                data = (FactorData) inputStream.readObject();

                int clientPort = data.getClientPort();
                String clientAddress = data.getClientAddress();

                Socket s = null;

                for (Socket c : clients) {
                    if (c.getPort() == clientPort && c.getInetAddress()
                            .getHostName().equals(clientAddress)) {
                        s = c;
                    }
                }

                if (s == null && clientAddress != null) {
                    s = new Socket(clientAddress, clientPort);
                    clients.add(s);
                    System.out.println("****CLIENT JOINED****");
                }

                if (data.getMessage().equals("failed")) {

                    if (type == FactorType.TD2Server || type == FactorType
                            .TDRNServer) {
                        if (type == FactorType.TD2Server) {
                            tdLowerBound = tdUpperBound.add(BigInteger.ONE);
                            tdUpperBound = tdLowerBound.add(incrementAmount);
                        } else {
                            tdUpperBound = tdLowerBound.subtract(BigInteger
                                    .ONE);
                            tdLowerBound = tdUpperBound.subtract
                                    (incrementAmount);
                        }
                        data.setUpperBoundTD(tdUpperBound);
                        data.setLowerBoundTD(tdLowerBound);
                    } else if (type == FactorType.FERMATServer) {
                        data.setFermatStartVal(fermatStartValue);
                        long attempts = data.getAttempts();
                        fermatStartValue = fermatStartValue.add(BigInteger
                                .valueOf(attempts));
                    } else if (type == FactorType.POLLARDSServer) {
                        BigInteger[] bounds = data.getPollardsBounds();
                        BigInteger base = data.getPollardBase();
                        BigInteger boundVal = data.getBoundAmount();
                        if (base.compareTo(pollardBase) > 0) {
                            pollardBase = base;
                            pollardLBound = BigInteger.ONE;
                            pollardUBound = BigInteger.valueOf(100);
                        } else if (boundVal.equals(bounds[0])) {
                            if (pollardLBound.compareTo(bounds[0]) > 0) {
                                data.setLowerBoundP(pollardLBound);
                                data.setTwoToImodN(pollardPower);
                            } else {
                                pollardLBound = bounds[0];
                                pollardPower = data.getTwoToImodN();
                            }
                        } else if (boundVal.equals(bounds[1])) {
                            if (bounds[1].compareTo(pollardUBound) < 0) {
                                pollardUBound = bounds[1];
                            } else {
                                data.setUpperBoundP(pollardUBound);
                            }
                        } else {
                            data.resetBasePollards(pollardBase);
                            pollardUBound = pollardUBound.add(BigInteger.valueOf(1000));
                            data.setUpperBoundP(pollardUBound);
                        }
                    }

                    data.setMessage("run");

                    mySocket = new Socket(data.getClientAddress(), data
                            .getClientPort());

                    outputData(data, mySocket);
                }

                handleReadData(data);

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }
}
