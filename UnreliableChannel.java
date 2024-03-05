import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class UnreliableChannel {

    public Server server;
    public Client A;
    public Client B;

    public UnreliableChannel(Server server, Client A, Client B) {
        this.server = server;
        this.A = A;
        this.B = B;
    }

    // Path: Client.java <-- Hania
    class Client {

    }

    // Path: Server.java
    class Server {

        private double p; // probability of packet loss
        private double minDelay, maxDelay;

        private int packetsRecievedA = 0;
        private int packetsRecievedB = 0;
        private int packetsDroppedA = 0;
        private int packetsDroppedB = 0;
        private int packetDelayedA = 0;
        private int packetDelayedB = 0;

        private double totalDelayA = 0;
        private double totalDelayB = 0;

        // constructor
        public Server(double p, double minDelay, double maxDelay) {
            this.p = p;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
        }

        public void listen(int port) {
            try {
                DatagramSocket ds = new DatagramSocket(port);
                System.out.println("Server is listening on port " + port);

                while (true) {
                    byte[] recieve = new byte[65535];
                    // creating a packet to recieve the data and storing it in recieve
                    DatagramPacket dpr = new DatagramPacket(recieve, recieve.length);
                    ds.receive(dpr);

                    // process the packet to determine if it should be dropped or delayed
                    rnd(dpr);

                }
            } catch (IOException e) {
                System.out.println("Error in communicatin with server.");
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
            String destination = clientInfo[1];
            String message = clientInfo[2];

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
                } else {
                    packetsDroppedB++;
                }
                System.out.println(" the packet from client " + client + " was dropped: " + str); // to be removed later
                return;

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
                } else {
                    packetsRecievedB++; // increment the number of packets recieved by B
                    packetDelayedB++; // increment the number of packets delayed by B
                    totalDelayB += delay; // add the delay to the total delay of B
                }

                System.out.println("The packet from client " + client + " was delayed by " + delay + " ms: "); // to be removed later
            }
        }

        public void getStats() {
            System.out.println("Packets recieved from A: " + packetsRecievedA + " | " + "Lost: " + packetsDroppedA
                    + " | " + "Delayed: " + packetDelayedA);
            System.out.println("Packets recieved from B: " + packetsRecievedB + " | " + "Lost: " + packetsDroppedB
                    + " | " + "Delayed: " + packetDelayedB);
            System.out.println("Average delay from A to B: " + totalDelayA / packetDelayedA + " ms.");
            System.out.println("Average delay from B to A: " + totalDelayB / packetDelayedB + " ms.");
        }
    }
}