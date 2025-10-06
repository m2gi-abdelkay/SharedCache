package jvn.Coordinator;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import jvn.Utils.JvnRemoteCoord;

public class Coordinator {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello from Coordinator!");
        System.out.println("Creating coordinator interface...");
        JvnCoordImpl jvnCoordImpl = new JvnCoordImpl();
        JvnRemoteCoord jvn_stub = (JvnRemoteCoord) UnicastRemoteObject.exportObject(jvnCoordImpl, 0);
        Registry registry = null;
        if(args.length > 0 )
        {
            registry = LocateRegistry.getRegistry(Integer.parseInt(args[0]));

        }
        else{
            registry = LocateRegistry.getRegistry();
        }

        registry.rebind("Javanaise", jvn_stub);

        System.out.println("Coordinator ready!");
        
    }
}
