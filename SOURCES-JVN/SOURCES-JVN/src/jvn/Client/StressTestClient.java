package jvn.Client;

import jvn.Utils.Sentence;
import jvn.Handler.JvnHandler;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Scanner;

public class StressTestClient {


    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)){
            // Prompt user for the number of clients and operations
            System.out.print("Enter the number of concurrent clients: ");
            int numClients = scanner.nextInt();

            System.out.print("Enter the number of operations per client: ");
            int numOperations = scanner.nextInt();
            
            // Initialize JVN server
            JvnServerImpl server = JvnServerImpl.jvnGetServer();

            // Lookup the shared "IRC" object
            JvnObject sharedObject = server.jvnLookupObject("IRC");
            if (sharedObject == null) {
                System.out.println("[StressTestClient] Shared object not found. Creating a new one...");
                sharedObject = server.jvnCreateObject((Serializable) new irc.SentenceImpl());
                sharedObject.jvnUnLock();
                server.jvnRegisterObject("IRC", sharedObject);
            }

            // Create a proxy for the shared object
            Sentence sentenceProxy = (Sentence) JvnHandler.newInstance(sharedObject);

            // Create a thread pool to simulate concurrent clients
            ExecutorService executor = Executors.newFixedThreadPool(numClients);

            // Submit tasks for each client
            for (int i = 0; i < numClients; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < numClients; j++) {
                        try {
                            // Randomly decide to read or write
                            if (ThreadLocalRandom.current().nextBoolean()) {
                                // Perform a read operation
                                String value = sentenceProxy.read();
                                System.out.println("[StressTestClient] Read value: " + value);
                            } else {
                                // Perform a write operation
                                String newValue = "Message-" + ThreadLocalRandom.current().nextInt(1000);
                                sentenceProxy.write(newValue);
                                System.out.println("[StressTestClient] Wrote value: " + newValue);
                            }
                        } catch (Exception e) {
                            System.err.println("[StressTestClient] Error during operation: " + e.getMessage());
                        }
                    }
                });
            }

            // Shutdown the executor service gracefully
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(100); // Wait for all tasks to complete
            }

            
            System.out.println("[StressTestClient] Stress test completed successfully.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("[StressTestClient] Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}