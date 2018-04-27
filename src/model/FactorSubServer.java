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

public class FactorSubServer {

    public static final int TD2PORT = 12486;
    public static final int TDRNPORT = 10897;
    public static final int FERMATPORT = 12458;
    public static final int POLLARDPORT = 11489;
    private FactorType type;
    private boolean complete = false;

    private List<Socket> clients;

    private Socket server;
    private int socketPort;
    private String serverHost;

    private BigInteger tdLowerBound;
    private BigInteger tdUpperBound;
    private BigInteger incrementAmount;

    private BigInteger fermatStartValue;

    private BigInteger pollardBase;
    private BigInteger pollardLBound;
    private BigInteger pollardUBound;
    private BigInteger pollardPower;

    private boolean ready = false;

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

    private void printInfo() {
        System.out.println("*-----------------------------*");
        System.out.println("*       FactorSubServer       *");
        System.out.println("*-----------------------------*");
        System.out.println("\nMain subserver for " + FactorType.toString(type));
        System.out.println("\n*-----------------------------*\n");
    }

    public boolean isReady() {
        return ready;
    }

    public void setServerHost(String hostname) {
        try {
            server = new Socket(hostname, FactorServer.SERVERPORT);
            serverHost = hostname;
            socketPort = FactorServer.SERVERPORT;
            FactorData data = new FactorData(null);
            String myname = "name:" + FactorType.asInt(type);
            myname += InetAddress.getLocalHost().getHostAddress();
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

    private void handleReadData(FactorData data) {
        String message = data.getMessage();

        System.out.println("SubServer read message: " + message);

        if (message.equals("quit")) {
            complete = true;
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
            System.exit(0);
        }

        if (message.equals("start")) {
            int clientCount = clients.size();

            /*--------------------------------
                Handling the trial divisions
             ---------------------------------*/

            // Trial Division from 2/RN
            if (type == FactorType.TD2Server || type == FactorType.TDRNServer) {
                BigInteger[] bounds = data.getTDBounds();
                tdLowerBound = bounds[0];
                tdUpperBound = bounds[1];

                incrementAmount = tdUpperBound.subtract(tdLowerBound);
                incrementAmount = incrementAmount.divide(BigInteger.valueOf
                        (clientCount));
                incrementAmount = incrementAmount.min(BigInteger.valueOf
                        (100_000_000));

                System.out.println("Sending data to clients");
                for (Socket client : clients) {

                    if (type == FactorType.TD2Server) {
                        tdUpperBound = tdLowerBound.add(incrementAmount);
                    } else {
                        tdLowerBound = tdUpperBound.subtract(incrementAmount);
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

    private void outputData(FactorData data, Socket client) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream
                    (client.getOutputStream());
            outputStream.writeObject(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setServer() {
        try {
            server = new Socket(serverHost, socketPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(Socket socket, FactorData data) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.
                    getOutputStream());
            outputStream.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

                    sendData(mySocket, data);
                }

                handleReadData(data);

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }
}
