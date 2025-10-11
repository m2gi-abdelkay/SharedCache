package jvn.Coordinator;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Coordinator {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello from Coordinator!");
        System.out.println("Creating coordinator interface...");
        JvnCoordImpl jvnCoordImpl = new JvnCoordImpl();
        
        // Create the registry programmatically to ensure proper classpath
        Registry registry;
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1099;
        
        try {
            // Try to create a new registry
            registry = LocateRegistry.createRegistry(port);
            System.out.println("Created new RMI registry on port " + port);
        } catch (Exception e) {
            // If registry already exists, get reference to it
            registry = LocateRegistry.getRegistry(port);
            System.out.println("Using existing RMI registry on port " + port);
        }

        registry.rebind("Javanaise", jvnCoordImpl);

        System.out.println("Coordinator ready!");
        
        // Keep the coordinator running
        System.out.println("Coordinator is running... Press Ctrl+C to stop.");
    }
}
