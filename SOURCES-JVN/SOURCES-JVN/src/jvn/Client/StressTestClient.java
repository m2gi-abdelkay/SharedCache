package jvn.Client;

import jvn.Utils.Sentence;
import jvn.Handler.JvnHandler;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StressTestClient {

    public static void main(String[] args) {
        

        if (args.length > 0 && "child".equals(args[0])){
            mainChild(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        Scanner scanner = new Scanner(System.in);
        try {
            // Prompt user for the number of processes and operations
            System.out.print("Enter the number of concurrent processes: ");
            int numProcesses = scanner.nextInt();

            System.out.print("Enter the number of operations per process: ");
            int numOperations = scanner.nextInt();

            // Launch multiple processes
            List<Process> processes = new ArrayList<>();
            for (int i = 0; i < numProcesses; i++) {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "java", 
                        "-cp", 
                        System.getProperty("java.class.path"), 
                        StressTestClient.class.getName(), 
                        "child",
                        String.valueOf(numOperations)
                );
                processes.add(processBuilder.start());
            }

            // Wait for all processes to complete
            for (Process process : processes) {
                process.waitFor();
            }

            System.out.println("[StressTestClient] All processes completed successfully.");
        } catch (Exception e) {
            System.err.println("[StressTestClient] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    // Method to handle stress test logic for a single process
    private static void runStressTest(int numOperations) {
        try {
            // Initialize JVN server
            JvnServerImpl server = JvnServerImpl.jvnGetServer();

            // Lookup the shared "IRC" object
            JvnObject sharedObject = server.jvnLookupObject("IRC");
            if (sharedObject == null) {
                System.out.println("[StressTestClient] Shared object not found. Creating a new one...");
                sharedObject = server.jvnCreateObject((Serializable) new irc.SentenceImpl());
                sharedObject.jvnUnLock();
                Integer res = server.jvnRegisterObject("IRC", sharedObject);
                if (res == 1){
                    sharedObject = server.jvnLookupObject("IRC");
                    if (sharedObject == null){
                        System.exit(1);
                    }
                }
            }

            // Create a proxy for the shared object
            Sentence sentenceProxy = (Sentence) JvnHandler.newInstance(sharedObject);

            // Perform operations
            for (int i = 0; i < numOperations; i++) {
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

            System.out.println("[StressTestClient] Stress test completed successfully.");
        } catch (Exception e) {
            System.err.println("[StressTestClient] Error: " + e.getMessage());
            e.printStackTrace();
        }
        return;
    }

    // Entry point for child processes
    public static void mainChild(String[] args) {
        if (args.length < 1) {
            System.err.println("[StressTestClient] Missing argument for number of operations.");
            System.exit(1);
        }

        int numOperations = Integer.parseInt(args[0]);
        runStressTest(numOperations);
        System.exit(0);
    }
}