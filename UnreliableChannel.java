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

        // Store client sockets for server
        server.clientASocket = A.socket;
        server.clientBSocket = B.socket;

        Thread serverThread = new Thread(() -> server.listen());
        serverThread.start();

        A.run(1000);
        B.run(1000);
        server.getStats();
    }
}

// Path: Client.java <-- Hania
class Client {
    public String name;
    public DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int packetCount = 0;
    private int packetsReceivedCount = 0;

    public Client(String name, String serverAddress, int serverPort) throws Exception {
        this.name = name;
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.serverPort = serverPort;
    }

    private void sendPacket(String message) throws IOException {
        String packetData = name + " " + (this.name.equals("A") ? "B" : "A") + " " + packetCount;
        byte[] buffer = packetData.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
        socket.send(packet);
        packetCount++;
    }

    private void receivePacket() throws IOException {
        byte[] buffer = new byte[65535];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());
        if (!message.equals("END")) {
            packetsReceivedCount++;
        }
    }

    public void run(int n) throws Exception {
        while (packetCount < n) {
            sendPacket("Data");
            receivePacket();
            // Thread.sleep(500);
        }

        sendPacket("END");
       // System.out.println(this.name + ": Packets received from other user: " + packetsReceivedCount);
    }
}

// Path: Server.java
class Server {

    private double p; // probability of packet loss
    private double minDelay, maxDelay;
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
        // String destination = clientInfo[1];
        // String message = clientInfo[2];

        // increment the number of packets recieved by the client
        if (client.equals("A")) {
            packetsRecievedA++;
        } else {
            packetsRecievedB++;
        }

        Random rnd = new Random();
        double r = rnd.nextDouble();

        // if the random number is less than p, the packet is dropped
        if (r <= p) {
            if (client.equals("A")) {
                packetsDroppedA++;
            } else if (client.equals("B")){
                packetsDroppedB++;
            }
            //System.out.println("The packet from client " + client + " was dropped");// to be removed later
        } else {
            // if the random number is greater than p, the packet is delayed by a certain
            // amount of time between mindDelay and maxDelay
            long delay = (long) ((Math.random() * (maxDelay - minDelay + 1)) + minDelay);

            // delaying the packet by putting the thread to sleep for the delay time
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                System.out.println("The server was interrupted and could not delay the packet.");
                e.printStackTrace();
            }

            // send the packet to the client
            if (client.equals("A")) {
                packetDelayedA++; // increment the number of packets delayed by A
                totalDelayA += delay; // add the delay to the total delay of A
            } else if (client.equals("B")) {
                packetsRecievedB++; // increment the number of packets recieved by B
                packetDelayedB++; // increment the number of packets delayed by B
                totalDelayB += delay; // add the delay to the total delay of B
            }


           // System.out.println("The packet from client " + client + " was delayed by " + delay + " ms."); // to be
                                                                                                          // removed
                                                                                                          // later
        }
        try {
            String destinationClient = (client.equals("A") ? "B" : "A");
            DatagramSocket destinationSocket = (destinationClient.equals("A") ? clientASocket : clientBSocket);
            destinationSocket.send(dpr);
        } catch (IOException e) {
            System.err.println("Error forwarding packet to destination client.");
            e.printStackTrace();
        }
    }

    public void getStats() {
        System.out.println("Packets recieved from A: " + (packetsRecievedA-1) + " | " + "Lost: " + packetsDroppedA
                + " | " + "Delayed: " + (packetDelayedA-1));
        System.out.println("Packets recieved from B: " + (packetsRecievedA-1) + " | " + "Lost: " + (packetsDroppedB-1)
                + " | " + "Delayed: " + packetDelayedB);
        System.out.println("Average delay from A to B: " + String.format("%.2f", totalDelayA / packetDelayedA) + " ms.");
        System.out.println("Average delay from B to A: " + String.format("%.2f", totalDelayB / packetDelayedB) + " ms.");
    }
}
