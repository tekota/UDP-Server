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

        private double p;
        private double minDelay, maxDelay;

        private int packetsRecievedA = 0;
        private int packetsRecievedB = 0;
        private int packetsDropped = 0;
        private int packetsDelayed = 0;

        private double totalDelayA = 0;
        private double totalDelayB = 0;

        public Server(double p, double minDelay, double maxDelay) {
            this.p = p;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
        }

        public void listen(int port){
            try {
                DatagramSocket ds = new DatagramSocket(port);
                System.out.println("Server is listening on port " + port);

                while (true) {
                    byte[] recieve = new byte[65535];
                    // creating a packet to recieve the data and storing it in recieve
                    DatagramPacket dpr = new DatagramPacket(recieve, recieve.length);
                    ds.receive(dpr);

                    // process the packet to determine if it should be dropped or delayed
                    processPacket(dpr);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processPacket (DatagramPacket dpr){
            
        }
    }
}