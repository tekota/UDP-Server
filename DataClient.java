import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DataClient {
    private DatagramSocket socket; // UDP socket for sending and receiving packets
    private InetAddress address; // Server IP address
    private int port; // Server port number
    private byte[] buf; // Buffer to store outgoing messages

    /**
     * Constructs a DataClient instance with specified server address and port
     * 
     * @param address The IP address of the server
     * @param port    The port number of ther server
     * @throws SocketExpcetion      If the socket could not be opened, or the socket
     *                              could not binf to the specified local port
     * @throws UnknownHostException If the IP address of the host could not be
     *                              determined
     */

    public DataClient(String address, int port) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    /**
     * Sends a predefined number of packets to the server, alternating between two
     * sequence numbers.
     * 
     * @param sender   the name of the sender
     * @param receiver the name of the receiver
     */

    public void sendPackets(String sender, String receiver) {
        try {
            int sequenceNumber = 0; // Initial sequence number
            for (int i = 0; i < 1000; i++) { // Loop to send a fixed number of packets
                // Construct the message with sender, receiver, and the current sequence number
                String message = sender + " " + receiver + " " + sequenceNumber;
                buf = message.getBytes(); // convert message to bytes
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
                sequenceNumber = 1 - sequenceNumber; // Alternate sequence number between 0 and 1
                Thread.sleep(500); // Pause to simulate network delay or processing time/

            }
            sendEndSignal(); // Send a specific message to indicate the end of transmission
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Properly handle thread interruption
        }
    }

    /**
     * sends an "END" signal to server to indicate that no more packets will be sent
     * 
     * @throws IOException if an I/O error occurs
     */

    private void sendEndSignal() throws IOException {
        String message = "END";
        buf = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

    /**
     * closes the DatagramSocket and releases any system resources associated with
     * it
     */

    public void close() {
        socket.close();
    }

    /**
     * Main method to create a DataClient instance and start sending packets
     * 
     * @param args command line arguments
     */

    public static void main(String[] args) {
        try {
            DataClient client = new DataClient("localhost", 4445); // replace localhost and 4455 with actual server
                                                                   // address and port
            client.sendPackets("A", "B");
            client.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

}