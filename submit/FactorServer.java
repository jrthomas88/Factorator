package model;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 * FactorServer.java
 * Runs on a host machine.  Is given or can generate some number n where n is
 * a product of primes.  Establishes a server which client machines can
 * connect to.  This server maintains the clients and sub-servers such that
 * the clients are tasked with factoring n, and will report to this server
 * when found.
 *
 * @author Jon Thomas
 */

public class FactorServer {

    public static final int SERVERPORT = 10188;
    private static int bits = -1;
    private FactorType winner;
    private int clientCount = 0; //maintains a count of clients
    private BigInteger origN; // original value of n
    private BigInteger n; // the number that will be factored
    private BigInteger p; // the two factors of n (if known)
    private BigInteger q;
    private List<BigInteger> factors; // list of all found factors
    private List<BigInteger> jobs; // list of integers to factor
    private boolean complete = false; // whether factorization is done
    private FactorData data; // data generated about n
    private List<Socket> sockets;

    private long startTime;
    private long endTime;

    private List<String> subserverAddresses;
    private List<Integer> subserverPorts;

    // hostnames for sub-servers
    private String td2host;
    private String tdrnhost;
    private String fermathost;
    private String pollardhost;

    private Semaphore mutex;

    /**
     * FactorServer
     * Constructor for this server.  Initializes information about n, and
     * launches the server listener in another thread.
     *
     * @param args command line arguments passed from main
     */

    private FactorServer(String[] args) {

        init(args);

        printInfo();

        mutex = new Semaphore(1);

        ServerListener listener = new ServerListener();
        Thread listenThread = new Thread(listener);
        listenThread.start();

        FactorLauncher launcher = new FactorLauncher();
        Thread t = new Thread(launcher);
        t.start();
    }

    /**
     * main
     * Creates a server object, passing the command arguments to it
     *
     * @param args command line arguments
     */

    public static void main(String[] args) {

        System.out.println("FactorServer started");
        FactorServer server = new FactorServer(args);

    }

    /**
     * printInfo
     * Prints to the screen data regarding n
     */

    private void printInfo() {

        System.out.println("*-----------------------------*");
        System.out.println("*         FactorServer        *");
        System.out.println("*-----------------------------*");
        System.out.println("\nn = " + n);
        System.out.println("p = " + p);
        System.out.println("q = " + q);
        System.out.println("\n*-----------------------------*\n");

    }

    /**
     * init
     * Based on the values in the command line arguments, sets n (and
     * potentially p and q) so that clients who connect to this server can be
     * passed information about the number they are to factor.
     *
     * @param args command line arguments passed from main
     */
    private void init(String[] args) {

        // instantiate lists
        factors = new LinkedList<>();
        jobs = new LinkedList<>();
        sockets = new LinkedList<>();
        subserverAddresses = new LinkedList<>();
        subserverPorts = new LinkedList<>();

        //args[0] is a flag, either -n, -g, -r, or -pq
        if (args.length < 2) {
            System.err.println("java FactorServer [flags] [numbers(s)]");
            System.exit(1);
        }

        // factor given integer n
        // args: -n [integer]
        if (args[0].equals("-n")) {
            n = new BigInteger(args[1]);
        }

        // randomly select two fixed bit length integers
        // args: -g [bit-length]
        else if (args[0].equals("-g")) {
            int bitLength = Integer.parseInt(args[1]);
            bits = bitLength;
            p = BigInteger.probablePrime(bitLength, new Random());
            q = BigInteger.probablePrime(bitLength, new Random());
            n = p.multiply(q);
        }

        // randomly select a number, potentially with several factors
        // args: -r [bit-length]
        else if (args[0].equals("-r")) {
            int bitLength = Integer.parseInt(args[1]);
            bits = bitLength;
            n = new BigInteger(bitLength, new Random());
        }

        // given two integers p and q, factor n=pq
        // args: -pq [factor1] [factor2]
        else if (args[0].equals("-pq")) {
            if (args.length < 3) {
                System.err.println("java FactorServer -pq [factor1] [factor2]");
                System.exit(1);
            }
            p = new BigInteger(args[1]);
            q = new BigInteger(args[2]);
            n = p.multiply(q);
        }

        // not a valid argument
        else {
            System.err.println("FactorServer valid arguments: -n, -g, -pq");
            System.exit(1);
        }

        data = new FactorData(n);
        //jobs.add(n); // n is the first factor to find
        origN = n;
    }

    /**
     * handleReadData
     * Is called when a FactorData object is read from one of the
     * clients/subservers.  Analyzes this object to determine why it was
     * sent, and will message clients based on that information.
     *
     * @param data FactorData object sent by a client
     */

    private void handleReadData(FactorData data, Socket socket) {

        String message = data.getMessage(); // message client sent

        System.out.println("Received message from client: " + message);

        if (message.equals("factor found")) { // if we found a factor

            try {
                mutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            BigInteger factor = data.getFactor();
            winner = data.getType();

            // if the factor is not prime, it will need to be factored
            if (!factor.isProbablePrime(100)) {
                jobs.add(factor);
            }

            // if this number factors n
            // if it doesn't do nothing, as the client messed up
            // possible we're receiving messages for old values of n
            if (n.mod(factor).equals(BigInteger.ZERO)) {
                factors.add(factor); // add to list of factors
                System.out.println("Factor found: " + factor);
                n = n.divide(factor); // n = n/factor

                // if new n is prime, check if we're done
                if (n.isProbablePrime(100)) {
                    if (jobs.isEmpty()) { // if there aren't more integers
                        endTime = System.nanoTime();
                        complete = true;
                        factors.add(n); // add n as a factor
                        factors.sort(BigInteger::compareTo); // sort list
                        System.out.println("All prime factors have been found");
                        System.out.println("Last algorithm to return factor: " +
                                           "" + FactorType.toString(data.
                                getType()));
                        sendData("quit");
                        printOutput();
                    } else {
                        n = jobs.remove(0); // get next integer to factor
                        System.out.println("Now factoring n = " + n);
                        // send this new n to the clients
                        sendData(null);
                    }
                } else {
                    // send new data to clients
                    sendData(null);
                }
            } else {
                System.out.println(factor + " is not a factor of " + n);
            }

            mutex.release();

        } else if (message.equals("new client")) {

            int portNo = data.getClientPort();
            String clientAdd = data.getClientAddress();
            data = prepareData();
            FactorType type = data.getType();

            String sshost = null;

            if (type == FactorType.TD2) {
                sshost = td2host;
            } else if (type == FactorType.TDRN) {
                sshost = tdrnhost;
            } else if (type == FactorType.FERMAT) {
                sshost = fermathost;
            } else if (type == FactorType.POLLARDS) {
                sshost = pollardhost;
            }

            System.out.println("connecting to " + clientAdd + " at port " + portNo);

            try {
                socket = new Socket(clientAdd, portNo);
            } catch (IOException e) {
                e.printStackTrace();
            }

            data.setSubservername(sshost);

            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(socket
                        .getOutputStream());
                outputStream.writeObject(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (message.equals("request sserver")) {

            try {
                int clientSocket = data.getClientPort();
                String clientAddress = data.getClientAddress();
                Socket s = new Socket(clientAddress, clientSocket);
                ObjectOutputStream outputStream = new ObjectOutputStream(s
                        .getOutputStream());
                FactorType type = data.getType();
                String hostName = null;
                if (type == FactorType.TD2) {
                    hostName = td2host;
                } else if (type == FactorType.TDRN) {
                    hostName = tdrnhost;
                } else if (type == FactorType.FERMAT) {
                    hostName = fermathost;
                } else if (type == FactorType.POLLARDS) {
                    hostName = pollardhost;
                }
                data.setMessage("name:" + hostName);
                outputStream.writeObject(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (message.substring(0, 5).equals("name:")) {

            FactorType type = data.getType();
            String hostname = message.substring(6);

            if (type == FactorType.TD2Server) {
                td2host = hostname;
                subserverAddresses.add(td2host);
                subserverPorts.add(FactorSubServer.TD2PORT);
            } else if (type == FactorType.TDRNServer) {
                tdrnhost = hostname;
                subserverAddresses.add(tdrnhost);
                subserverPorts.add(FactorSubServer.TDRNPORT);
            } else if (type == FactorType.FERMATServer) {
                fermathost = hostname;
                subserverAddresses.add(fermathost);
                subserverPorts.add(FactorSubServer.FERMATPORT);
            } else if (type == FactorType.POLLARDSServer) {
                pollardhost = hostname;
                subserverAddresses.add(pollardhost);
                subserverPorts.add(FactorSubServer.POLLARDPORT);
            }

            sendData(message);
        }
    }

    private void printOutput() {
        System.out.println("*-----------------------*");
        System.out.print(origN + " = ");
        for (int i = 0; i < factors.size(); i++) {
            BigInteger b = factors.get(i);
            System.out.print(b);
            if (i < factors.size() - 1) {
                System.out.print(" * ");
            } else {
                System.out.println();
            }
        }
        System.out.println("*-----------------------*");

        try {
            BufferedWriter writer = new BufferedWriter
                    (new FileWriter("results.txt"));
            writer.write("Factorization Results\n\n" + origN + " = ");
            for (int i = 0; i < factors.size(); i++) {
                writer.write("" + factors.get(i));
                if (i != factors.size() - 1) {
                    writer.write(" * ");
                }
            }
            writer.write("\n\n");
            long[] time = new long[2];
            long nanoseconds = endTime - startTime;
            time[0] = nanoseconds / 1000000000;
            time[1] = nanoseconds % 1000000000;
            time[1] = time[1] / 1000000;
            writer.write(time[0] + " seconds, " + time[1] + " millis\n");
            writer.write("Bit length: " + bits + "\n");
            writer.write("Winning algorithm: " + FactorType.toString(winner));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * sendData
     * Create a FactorData object and send it to the clients
     */
    private void sendData(String options) {
        data = prepareData(); // prepare data object

        if (options != null) {

            if (options.equals("quit")) {
                data.setMessage("quit");
            }

            if (options.length() > 5 && options.substring(0, 5).equals
                    ("name:")) {
                data.setMessage(options);
                for (Socket s : sockets) {
                    try {

                        ObjectOutputStream outputStream = new ObjectOutputStream(s
                                .getOutputStream());
                        outputStream.writeObject(data); // send the data object
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            if (options.equals("start")) {
                data.setMessage(options);
                startTime = System.nanoTime();
            }
        }

        System.out.println("Sending data to all sub-servers.");

        // for every subserver
        for (int i = 0; i < subserverPorts.size(); i++) {
            int port = subserverPorts.get(i);
            String address = subserverAddresses.get(i);
            try {
                Socket s = new Socket(address, port);
                ObjectOutputStream outputStream = new ObjectOutputStream(s
                        .getOutputStream());
                outputStream.writeObject(data); // send the data object
                System.out.println("Sent to subserver #" + i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * prepareData
     * Using the current value of n, create a new FactorData object that
     * contains information needed to factor n.
     *
     * @return the newly created FactorData object
     */

    private FactorData prepareData() {
        FactorData data = new FactorData(n); // create the object
        data.setMessage("new value");

        // set what type client will be
        if (subserverPorts.size() == 0) {
            data.setType(FactorType.TD2Server);
        } else if (subserverPorts.size() == 1) {
            data.setType(FactorType.TDRNServer);
        } else if (subserverPorts.size() == 2) {
            data.setType(FactorType.FERMATServer);
        } else if (subserverPorts.size() == 3) {
            data.setType(FactorType.POLLARDSServer);
        } else if (clientCount % 4 == 0) {
            data.setType(FactorType.TD2);
        } else if (clientCount % 4 == 1) {
            data.setType(FactorType.TDRN);
        } else if (clientCount % 4 == 2) {
            data.setType(FactorType.FERMAT);
        } else if (clientCount % 4 == 3) {
            data.setType(FactorType.POLLARDS);
        }

        clientCount++;

        return data;
    }

    /**
     * ServerListener
     * Creates a passive listener that waits for machines to connect to this
     * machine.  Creates a new reader that will process data sent to this
     * machine.
     */
    private class ServerListener implements Runnable {

        @Override
        public void run() {

            try {
                ServerSocket listen = new ServerSocket(SERVERPORT, 10);
                while (!complete) {
                    System.out.println("Server waiting for connection");

                    Socket socket = listen.accept();

                    System.out.println("Connection found");

                    // determine if we've met socket before
                    boolean add = true;
                    for (Socket s : sockets) {
                        if (s.getInetAddress().equals(socket
                                .getInetAddress())) {
                            add = false;
                        }
                    }

                    for (String s : subserverAddresses) {
                        if (s.equals(socket
                                .getInetAddress().getHostName())) {
                            add = false;
                        }
                    }

                    if (add) {
                        sockets.add(socket);
                    }

                    ServerReader reader = new ServerReader(socket);
                    Thread readThread = new Thread(reader);
                    readThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * ServerReader
     * Reads data received from a specific socket.  Sends data to
     * handleReadData to be processed.
     */

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
                inputStream = new ObjectInputStream(mySocket.getInputStream());
                try {
                    data = (FactorData) inputStream.readObject();
                } catch (java.io.EOFException e) {
                    return;
                }
                handleReadData(data, mySocket);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private class FactorLauncher implements Runnable {

        @Override
        public void run() {
            System.out.println("Server is setting up.  Please make sure all " +
                               "clients have connected and type 'factor' to " +
                               "begin the program.");

            Scanner in = new Scanner(System.in);
            while (in.hasNext()) {
                String input = in.next();
                if (input.equals("factor")) {
                    System.out.println("Begin factoring!");

                    //sendData("start");

                    break;
                }
            }
            in.close();

            sendData("start");
        }
    }

}
