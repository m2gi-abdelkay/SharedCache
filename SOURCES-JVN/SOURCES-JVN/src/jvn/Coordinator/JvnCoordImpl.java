/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */ 

package jvn.Coordinator;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnRemoteCoord;
import jvn.Utils.JvnRemoteServer;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	

	private static final long serialVersionUID = 1L;

  private int objectIdCounter;


    private final Map<String, Integer> nameToId;

    private final Map<Integer, JvnObject> idToObject;

    private final Map<Integer, Set<JvnRemoteServer>> readers;

    private final Map<Integer, JvnRemoteServer> writers;

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
    super();
		try {
      try {
          
        LocateRegistry.createRegistry(1099); //default port
        System.out.println("RMI registry created at port 1099.");
      } catch (ExportException ee) {
          
        System.out.println("Registry RMI already exists.");
      }
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind("Javanaise", this);
      System.out.println("JvnCoord bound in registry.");
    } catch (RemoteException re) {
        System.err.println(re.getMessage());
        throw re;
    }
    this.objectIdCounter = 0;
    this.nameToId = new HashMap<>();
    this.idToObject = new HashMap<>();
    this.readers = new HashMap<>();
    this.writers = new HashMap<>();


	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.Utils.JvnException {
    objectIdCounter++;
    System.out.println("New object ID allocated: " + objectIdCounter);
    return objectIdCounter;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  @Override
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) throws RemoteException, JvnException {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'jvnRegisterObject'");
  }
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.Utils.JvnException{
    
    //check if the object exists
    Integer oid = nameToId.get(jon);
    if (oid == null) {
        System.out.println("[Coordinator] Lookup: " + jon + " not found.");
        return null;
    }
    //recover the object
    JvnObject jo = idToObject.get(oid);
    if (jo == null) {
        throw new jvn.Utils.JvnException("Inconsistent state: ID " + oid + " not found for object " + jon);
    }
    //add the server to the readers list
    readers.computeIfAbsent(oid, k -> new HashSet<>()).add(js);

    System.out.println("[Coordinator] Lookup: " + jon + " found (ID = " + oid + "), reader added.");

    return jo;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    
     JvnObject jo = idToObject.get(joi);
    if (jo == null) {
        throw new jvn.Utils.JvnException("Object with ID " + joi + " not found.");
    }

    JvnRemoteServer writer = writers.get(joi);
    Serializable updatedState = null;


    if (writer != null) {
        try {
            //invalidate the writer and get the updated state
            updatedState = writer.jvnInvalidateWriterForReader(joi);
        } catch (Exception e) {
            System.err.println(" Writer invalidation failed");
        }
        // Remove the writer from the map
        writers.remove(joi);

        
        if (updatedState != null) {
            jo.setSerializableObject(updatedState);
        }
    }

    readers.computeIfAbsent(joi, k -> new HashSet<>()).add(js);

    System.out.println("[Coordinator] Read lock granted for object " + joi);

    return jo.getSerializableObject();
    
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
    return null;
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {

    System.out.println("Termination requested by a server.");

    for (Map.Entry<Integer, Set<JvnRemoteServer>> entry : readers.entrySet()) {
        Set<JvnRemoteServer> serverSet = entry.getValue();
        if (serverSet.contains(js)) {
            serverSet.remove(js);
            System.out.println("[Coordinator] Removed server from readers of object " + entry.getKey());
        }
    }

  
    for (Map.Entry<Integer, JvnRemoteServer> entry : writers.entrySet()) {
        Integer joi = entry.getKey();
        JvnRemoteServer writer = entry.getValue();
        if (writer.equals(js)) {
            writers.remove(joi);
            System.out.println("[Coordinator] Removed server from writers of object " + joi);
        }
    }

    System.out.println("Server terminated successfully.");
    }


}

 
