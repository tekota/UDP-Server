import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class UnreliableChannel {
    public static void main(String[] args) throws Exception {
        Server server = new Server(0.3, 0, 200, 5000);
        Client A = new Client("A", "localhost", server.port);
        Client B = new Client("B", "localhost", server.port);

        // store client sockets for server
        server.clientASocket = A.socket;
        server.clientBSocket = B.socket;

        Thread serverThread = new Thread(() -> server.listen());
        serverThread.start();

        A.run(1000);
        B.run(1000);
        serverThread.join(); // ensures that the server thread finishes before the main thread
    }
}

// Path: Client.java
class Client {
    public String name;// client identifier (A or B)
    public DatagramSocket socket;// UDP socket for sending and receiving packets
    private InetAddress serverAddress;// Server IP address
    private int serverPort;// Server port number
    private int packetCount = 0;// Number of packets sent
    private int packetsReceivedCount = 0; // Number of packets received

    // constructor to intialize client settings
    public Client(String name, String serverAddress, int serverPort) throws Exception {
        this.name = name;
        this.socket = new DatagramSocket(); // create the UDP socket
        this.serverAddress = InetAddress.getByName(serverAddress);// resolving server address
        this.serverPort = serverPort;// assigning server port
    }

    // method to send packets to the server
    private void sendPacket(String message) throws IOException {
        if (!message.equals("END")) { // if not the end signal, construct the packet data
            String packetData = name + " " + (this.name.equals("A") ? "B" : "A") + " "
                    + (packetCount % 2 == 0 ? "0" : "1");
            byte[] buffer = packetData.getBytes();// convert string data to bytes
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);// sending the packet through the socket
            packetCount++;// increment the packet count
        } else {
            // if the message is "END", send the end signal to the server
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        }
    }

    // method to receive packets from the server
    private void receivePacket() throws IOException {
        byte[] buffer = new byte[65535]; // buffer to store incoming data
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);// preparing packet for receiving
        socket.receive(packet);// receiving the packet

        String message = new String(packet.getData(), 0, packet.getLength());
        if (!message.equals("END")) { // if not the end signal, incremet received packet count
            packetsReceivedCount++;
        }
    }

    // run method to handle sending and receiving packets
    public void run(int n) throws Exception {
        while (packetCount < n) {// until the specified number of packets are sent
            sendPacket("Data"); // sending packet
            receivePacket();// receiving packet
            Thread.sleep(500);// wait for half a second before the next packet
        }

        sendPacket("END");// send end signal after all packets are sent
        System.out.println(this.name + ": Packets received from other user: " + packetsReceivedCount);
    }
}

// Path: Server.java
class Server {

    private double p; // probability of packet loss
    private double minDelay, maxDelay; // minimum and maximum delay
    public int port;

    private int packetsRecievedA = 0;
    private int packetsRecievedB = 0;
    private int packetsDroppedA = 0;
    private int packetsDroppedB = 0;
    private int packetDelayedA = 0;
    private int packetDelayedB = 0;

    private double totalDelayA = 0;
    private double totalDelayB = 0;

    public DatagramSocket clientASocket;
    public DatagramSocket clientBSocket;

    // constructor
    public Server(double p, double minDelay, double maxDelay, int port) {
        this.p = p;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.port = port;
    }

    // function to listen for packets from the clients
    public void listen() {
        try (DatagramSocket ds = new DatagramSocket(port)) {
            System.out.println("Server is listening on port " + port);
            System.out.println("Sending and receiving packets...please wait.");
            int clientsFinished = 0;
            while (clientsFinished < 2) {
                byte[] receive = new byte[65535];
                DatagramPacket dpr = new DatagramPacket(receive, receive.length);
                ds.receive(dpr);
                // process the packet
                rnd(dpr);
                if (new String(dpr.getData(), 0, dpr.getLength()).equals("END")) {
                    clientsFinished++;
                }
            }
            ds.close();
            getStats();
        } catch (IOException e) {
            System.out.println("Error in communication with server.");
            e.printStackTrace();
        }
    }

    // function that simulates the packet drop and delay of the server
    private void rnd(DatagramPacket dpr) {
        byte[] data = dpr.getData();
        String str = new String(data, 0, dpr.getLength());

        // split the string to get the client info
        String[] clientInfo = str.split(" ");
        String client = clientInfo[0];

        // increment the number of packets recieved by the client
        if (client.equals("A")) {
            packetsRecievedA++;
        } else if (client.equals("B")) {
            packetsRecievedB++;
        }

        Random rnd = new Random();
        double r = rnd.nextDouble();

        // if the random number is less than p, the packet is dropped
        if (r <= p) {
            if (client.equals("A")) {
                packetsDroppedA++;
            } else if (client.equals("B")) {
                packetsDroppedB++;
            }
            // System.out.println("The packet from client " + client + " was dropped");

            // if the random number is greater than p, the packet is delayed
        } else {
            // if the random number is greater than p, the packet is delayed by a random
            // amount of time between mindDelay and maxDelay
            long delay = (long) ((Math.random() * (maxDelay - minDelay + 1)) + minDelay);

            // delaying the packet by putting the thread to sleep for the delay time
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                System.out.println("The server was interrupted and could not delay the packet.");
                e.printStackTrace();
            }

            // keep track of the packets + delays
            if (client.equals("A")) {
                packetDelayedA++; // increment the number of packets delayed by A
                totalDelayA += delay; // add the delay to the total delay of A
            } else if (client.equals("B")) {
                packetDelayedB++; // increment the number of packets delayed by B
                totalDelayB += delay; // add the delay to the total delay of B
            }

            // System.out.println("The packet from client " + client + " was delayed by " +
            // delay + " ms.");

        }
        // sending the packet to the destination client
        try {
            String destinationClient = (client.equals("A") ? "B" : "A");
            DatagramSocket destinationSocket = (destinationClient.equals("A") ? clientASocket : clientBSocket);
            destinationSocket.send(dpr);
        } catch (IOException e) {
            System.err.println("Error forwarding packet to destination client.");
            e.printStackTrace();
        }

    }

    // function to print the statistics of the server
    public void getStats() {
        System.out.println("Packets recieved from A: " + packetsRecievedA + " | " + "Lost: " + packetsDroppedA
                + " | " + "Delayed: " + packetDelayedA);
        System.out
                .println("Packets recieved from B: " + packetsRecievedB + " | " + "Lost: " + packetsDroppedB
                        + " | " + "Delayed: " + packetDelayedB);
        System.out
                .println("Average delay from A to B: " + String.format("%.2f", totalDelayA / packetDelayedA) + " ms.");
        System.out
                .print("Average delay from B to A: " + String.format("%.2f", totalDelayB / packetDelayedB) + " ms.");
    }
}
