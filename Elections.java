import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ElectionVoteCastingApplication {
    private static final int PORT = 8888;
    private static final String MULTICAST_GROUP = "224.0.0.1";
    private static final int ELECTORATE_COUNT = 5;

    public static void main(String[] args) {
        try {
            // Create a multicast socket
            MulticastSocket multicastSocket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            multicastSocket.joinGroup(group);

            // Create electorates (processes)
            Electorate[] electorates = new Electorate[ELECTORATE_COUNT];
            for (int i = 0; i < ELECTORATE_COUNT; i++) {
                electorates[i] = new Electorate(i + 1, multicastSocket);
            }

            // Start electorates
            for (Electorate electorate : electorates) {
                electorate.start();
            }

            // Wait for all electorates to finish
            for (Electorate electorate : electorates) {
                electorate.join();
            }

            // Determine the winner
            Map<Character, Integer> voteCount = new HashMap<>();
            for (Electorate electorate : electorates) {
                char vote = electorate.getVote();
                voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
            }

            int maxVotes = 0;
            char winner = ' ';
            for (Map.Entry<Character, Integer> entry : voteCount.entrySet()) {
                char candidate = entry.getKey();
                int votes = entry.getValue();
                if (votes > maxVotes) {
                    maxVotes = votes;
                    winner = candidate;
                }
            }

            // Display the winner
            System.out.println("The winner is candidate " + winner + " with " + maxVotes + " votes.");

            // Leave the multicast group and close the socket
            multicastSocket.leaveGroup(group);
            multicastSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class Electorate extends Thread {
        private final int electorateId;
        private final MulticastSocket multicastSocket;
        private char vote;

        public Electorate(int electorateId, MulticastSocket multicastSocket) {
            this.electorateId = electorateId;
            this.multicastSocket = multicastSocket;
        }

        public char getVote() {
            return vote;
        }

        @Override
        public void run() {
            try {
                // Generate a random vote
                vote = Math.random() < 0.5 ? 'A' : 'B';

                // Send the vote message as a multicast message
                byte[] message = String.valueOf(vote).getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(MULTICAST_GROUP), PORT);
                multicastSocket.send(packet);

                // Receive vote messages from other electorates
                byte[] buffer = new byte[1];
                for (int i = 0; i < ELECTORATE_COUNT - 1; i++) {
                    packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    char receivedVote = (char) buffer[0];
                    System.out.println("Electorate " + electorateId + " received vote: " + receivedVote);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
