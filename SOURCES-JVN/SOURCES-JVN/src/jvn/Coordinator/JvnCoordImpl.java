/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */ 

package jvn.Coordinator;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnRemoteCoord;
import jvn.Utils.JvnRemoteServer;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{

  private static final int LOCK_READ =  0;
  private static final int LOCK_WRITE = 1;
  private static int start_id = 1;
  //private long coordinatorEpoch = 0; // Incremented on each coordinator restart
  private ConcurrentHashMap<String,JvnObject> registrationMap;
  private ConcurrentHashMap<Integer, Serializable> objectIdsMap;
  private ConcurrentHashMap<Integer,Integer> lockHashMap;
  private ConcurrentHashMap<Integer, HashSet<JvnRemoteServer>> serverHashMap;
  // Map to store per-object locks for fine-grained synchronization
  private ConcurrentHashMap<Integer, Object> objectLocks;

  // Use a concurrent Set to allow safe concurrent add/remove while iterating.
  // ConcurrentHashMap.newKeySet() provides a thread-safe, weakly-consistent view.
  private java.util.Set<JvnRemoteServer> listOfServers = ConcurrentHashMap.newKeySet();
	



  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
  * Default constructor
  * @throws JvnException
  **/
	JvnCoordImpl() throws Exception {
		registrationMap = new ConcurrentHashMap<>();
  lockHashMap = new ConcurrentHashMap<>();
  serverHashMap = new ConcurrentHashMap<>();
  objectIdsMap = new ConcurrentHashMap<>();
  objectLocks = new ConcurrentHashMap<>();

  // Try to load persisted coordinator state (if any)
  try {
    loadState();
  } catch (Exception e) {
    System.out.println("[JvnCoordinator] No previous state to load or failed to load: " + e.getMessage());
  }
  
  // Increment epoch on each startup so servers can detect restart
  //coordinatorEpoch++;
  //System.out.println("[JvnCoordinator] Coordinator epoch: " + coordinatorEpoch);
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  @Override
  public synchronized int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.Utils.JvnException {
    start_id++;
    System.out.println("[JvnCoordinator] Will assign Id :" + start_id + " to new object");
    return start_id;
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
  public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) throws RemoteException, JvnException {

    if(registrationMap.containsKey(jon))
    {
      System.out.println("[JvnCoordinator] Cannot register already registered object!");
      throw new JvnException(" Object name is already assigned to an object, please change object name.");
    }

    System.out.println("[JvnCoordinator] About to register object :" + jon + " with id:" + jo.jvnGetObjectId());
    objectIdsMap.put(jo.jvnGetObjectId(), jo.jvnGetSharedObject());
    listOfServers.add(js);
    registrationMap.put(jon, jo);
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  @Override
  public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.Utils.JvnException{
    if(!registrationMap.containsKey(jon))
    {
      System.out.println("[JvnCoordinator] Object: " + jon + " NOT FOUND!");
      throw new JvnException("Object not found!");
    }
    System.out.println("[JvnCoordinator] Object :" + jon + " FOUND!");
    listOfServers.add(js);
    return registrationMap.get(jon);
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
  @Override
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{

    // Get or create a lock object for this specific joi to enable fine-grained synchronization
    System.out.println("[JvnCoordinator] Attempting to acquire a read lock for object :" + joi + " by server :"+ js);
    Object lockObject = objectLocks.computeIfAbsent(joi, k -> new Object());
    
    synchronized(lockObject) {
      //This is the same joi that another server may have the lock on. You don't care about other joi's
      // For the same joi, check whether someone has a lock on it or not
      // if not, then assign joi -> READ_LOCK and joi -> jvnRemoteServer to know which server to call eventually for the invalidation

  if(!lockHashMap.containsKey(joi))
      {
        System.out.println("[JvnCoordinator] No other server has acquired a lock on object: " + joi + ", granting READ lock...");
        //This means no other server has a read or write lock on the object. 
        //Store the joi to the read_lock and remember the server that has this lock (cf cas de figure 2)
        lockHashMap.put(joi, LOCK_READ);
        HashSet<JvnRemoteServer> serverArray = new HashSet<>();
        serverArray.add(js); 
        serverHashMap.put(joi, serverArray);
        return objectIdsMap.get(joi);
      }

  if(lockHashMap.containsKey(joi))
      {
        System.out.println("[JvnCoordinator] Object with id :" + joi + " already has a lock assigned to it. Determining type of lock...");
        //This means that a/many server(s) has/have a lock on our shared object. We retrieve the list of servers and the type of the lock:
        int lock_type = lockHashMap.get(joi);
        HashSet<JvnRemoteServer> serverArray = serverHashMap.get(joi);
        //Determine type of lock:
            if(lock_type == LOCK_READ)
        {
          System.out.println("[JvnCoordinator] Object with id :" + joi + " has a READ lock assigned to it.");
          // This means that one (or many) servers have a read lock on the object
          //Check whether we have one server in our list and it's the same server asking for the lock (cf cohérence cas de figure 4)
          System.out.println("[JvnCoordinator] Checking who has the lock...");
          if(serverArray.size() == 1 && serverArray.contains(js))
          {
            System.out.println("Server :" + js + " is requesting a READ lock even though it already has one. Returning the object as it is.");
            //then in this case we simply return the object
            return objectIdsMap.get(joi);
          }

          //Otherwise, add this server to the read lock (cohérence cas de figure 3):
          //No duplication concerns, we have a hashset
          System.out.println("[JvnCoordinator] Check completed, adding server to the list of servers that have the lock.");
          serverArray.add(js);
          serverHashMap.put(joi, serverArray);
          //Since object is being read, then the version stored in our object map should be the latest version:
          return objectIdsMap.get(joi);

        }
          if(lock_type == LOCK_WRITE){
          System.out.println("[JvnCoordinator] Object with id :" + joi + " has a WRITE lock assigned to it.");
          System.out.println("[JvnCoordinator] Checking whether it is the same server....");

          //The lock type that we have is a WRITE LOCK
          //This means that one server has a write lock on our object
          //First, we check whether the server that is asking for a read lock is the same as the one that has the write lock
          //(since we only have one write lock, the serverArray list should always be 1)
          if(serverArray.contains(js))
          {
            System.out.println("[JvnCoordinator] Server :" + js + " is requesting a READ lock even though it has a WRITE (greedy ass) lock. Returning object as it is.");
            //Then do nothing (an update is required at the JvnObjectLevel, see cas de cohérence figure 5)
            return objectIdsMap.get(joi);
          }

          System.out.println("[JvnCoordinator] Check completed, another server has the lock.");
          //If the write lock is in another server (cf cohérence cas de figure 6):
            if(!serverArray.contains(js))
          {
            System.out.println("[JvnCoordinator] Must invalidate other server...");
            //Then we must invalidate, get the server which has the lock:
            JvnRemoteServer serverWithWLock = serverArray.iterator().next();
            System.out.println("[JvnCoordinator] Invalidating server :" + serverWithWLock);
            Serializable updatedObject = serverWithWLock.jvnInvalidateWriterForReader(joi);
            //Store the updated object:
            System.out.println("[JvnCoordinator] Received updated object " + updatedObject);
            objectIdsMap.put(joi, updatedObject);
            //Change the nature of the lock on the object:
            System.out.println("[JvnCoordinator] Changing type of lock to LOCK READ");
            lockHashMap.put(joi, LOCK_READ);
            //Update the lock list:
            System.out.println("[JvnCoordinator] Updating lock list adding current server " + js + " and server with previous WRITE LOCK " + serverWithWLock );
            HashSet<JvnRemoteServer> newServerSet = new HashSet<>();
            newServerSet.add(serverWithWLock);
            newServerSet.add(js);
            serverHashMap.put(joi, newServerSet);
            System.out.println("[JvnCoordinator] Job completed!");
            //return the updated object
            return updatedObject;
            
          }
        }

      }
      //Should never happen
      return null;
    }
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
  @Override
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    
    // Get or create a lock object for this specific joi to enable fine-grained synchronization
        System.out.println("[JvnCoordinator] Attempting to acquire a write lock for object :" + joi + " by server :"+ js);

    Object lockObject = objectLocks.computeIfAbsent(joi, k -> new Object());
    
    synchronized(lockObject) {
      //This is the same joi that another server may have the lock on. You don't care about other joi's
      // For the same joi, check whether someone has a lock on it or not
      // if not, then assign joi -> WRITE_LOCK and joi -> jvnRemoteServer to know which server to call eventually for the invalidation

      if(!lockHashMap.containsKey(joi))
      {
        System.out.println("[JvnCoordinator] No other server has acquired a lock on object:" + joi + ", granting WRITE lock...");

        //This means no other server has a read or write lock on the object. 
        //Store the joi to the read_lock and remember the server that has this lock (cf cas de figure 2)
        lockHashMap.put(joi, LOCK_WRITE);
        HashSet<JvnRemoteServer> serverArray = new HashSet<>();
        serverArray.add(js); 
        serverHashMap.put(joi, serverArray);
        return objectIdsMap.get(joi);
      }

      if(lockHashMap.containsKey(joi))
      {
        System.out.println("[JvnCoordinator] Object with id :" + joi + " already has a lock assigned to it. Determining type of lock...");

        int lock_type = lockHashMap.get(joi);
        HashSet<JvnRemoteServer> serverArray = serverHashMap.get(joi);
        if(lock_type == LOCK_WRITE)
        {
          System.out.println("[JvnCoordinator] Object with id :" + joi + " has a WRITE lock assigned to it.");
          System.out.println("[JvnCoordinator] Checking whether it is the same server....");

          //If another server has a write lock, first
          //Check whether it is the same server as the one that is asking:
          if(serverArray.contains(js))
          {
            System.out.println("[JvnCoordinator] Server: " + js + " is trying to acquire a WRITE lock even though it has one. Return object as it is...");
            //Then in this case, do nothing and wait until someone else asks for the lock:
            return objectIdsMap.get(joi);
          }
          System.out.println("[JvnCoordinator] Another server has the write lock. Must invalidate.");
          //Otherwise, this means someone else has the lock. We must invalidate the writer (cf cohérence cas de figure 7)
          if(!serverArray.contains(js))
          {
            //Get the server that has the lock
            System.out.println("[JvnCoordinator] Invalidating other server...");
            JvnRemoteServer serverWithWLock = serverArray.iterator().next();
            Serializable updatedObject = (Serializable) serverWithWLock.jvnInvalidateWriter(joi);
            System.out.println("[JvnCoordinator] Server: " + serverWithWLock + " has been invalidated.");
            //Store the updated object:
            System.out.println("[JvnCoordinator] Received updated object " + updatedObject + " and storing it.");
            objectIdsMap.put(joi, updatedObject);
            //No need to update the type of lock on the object (still a write lock)
            ///However, update the server that has the lock:
            System.out.println("[JvnCoordinator] Updating list of servers, only the current server " + js + "has a lock on the object");
            HashSet<JvnRemoteServer> newServerSet = new HashSet<>();
            newServerSet.add(js);
            serverHashMap.put(joi, newServerSet);
            //Return the updated object
            return updatedObject;
          }
        }
        
        if(lock_type == LOCK_READ)
        {
          //Here we know that one or many servers have a read lock on the object. (Potentially the server calling itself has a reader lock)
          //In this case, we must invalidate all readers (except potentially yourself)
          System.out.println("[JvnCoordinator] Object with id :" + joi + " has a READ lock assigned to it.");

          System.out.println("[JvnCoordinator] Must invalidate all other readers. Checking whether the current server is among the servers that have a read lock on the object...");
          if(serverArray.contains(js))
          {
            System.out.println("[JvnCoordinator] Server:" + js + " is among the servers with a read lock. Removing them...");
            serverArray.remove(js);
          }
          System.out.println("[JvnCoordinator] Invalidating all servers with a read lock on object " +joi+"...");
          for(JvnRemoteServer jvnServer : serverArray)
          {
      
            jvnServer.jvnInvalidateReader(joi);
            System.out.println("[JvnCoordinator] Server : " + jvnServer + " has been invalidated.");
            
          }
          //Change lock on object:
          System.out.println("[JvnCoordinator] Changing lock to WRITE LOCK");
          lockHashMap.put(joi, LOCK_WRITE);
          //Add server requesting lock
          System.out.println("[JvnCoordinator] Updating list of servers, only the current server " + js + "has a lock on the object");
          HashSet<JvnRemoteServer> newServerSet = new HashSet<>();
          newServerSet.add(js);
          serverHashMap.put(joi, newServerSet);
          //Since readers, object has been subjected to no modification since last update:
          return objectIdsMap.get(joi);
        }
      }
      return null;
    }
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
  @Override
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
    //The server (js) is terminating. This means:
    // - On all the joi's, must find the joi's where this server has a lock on and remove it from that list
    // - If the server is the only one that has a lock on an object, mark the object as unlocked (remove it from the locked hash map and server hash map)
    
    // We need to collect the JOIs to process to avoid ConcurrentModificationException
    // and also to synchronize on each object individually
    System.out.println("[JvnCoordinator] About to terminate Server: " + js);
    for(Integer joi : serverHashMap.keySet())
    {
      // Get the lock for this specific object to ensure thread safety
      Object lockObject = objectLocks.computeIfAbsent(joi, k -> new Object());
      
      synchronized(lockObject) {
        //For each joi, receive the list of servers that have a lock on it
        HashSet<JvnRemoteServer> servers = serverHashMap.get(joi);

        //if the list of servers that have a lock on it include the server that is terminating
        if(servers != null && servers.contains(js))
        {
          //Then remove it
          System.out.println("[JvnCoordinator] Removing server from list of servers that have a lock on object id:" + joi);
          servers.remove(js);

          //If the list is now empty (was the last element)
          if(servers.isEmpty())
          {
            System.out.println("[JvnCoordinator] This was the last server having a lock on the object, removing lock from object and removing list of servers...");
            //Then mark the object as unlocked
            lockHashMap.remove(joi);
            serverHashMap.remove(joi);
            // Clean up the object lock as well since no one is using this object anymore
            objectLocks.remove(joi);
          }
          else{
            System.out.println("[JvnCoordinator] List of servers is updating...");
            //Update the list of servers
            serverHashMap.put(joi, servers);
          }
        }
      }
    }
        
    }

    /**
     * Persist coordinator state to disk so it can be recovered after a crash.
     * NOTE: We do NOT persist lockHashMap or serverHashMap because:
     * - lockHashMap would be stale (servers may have crashed/restarted)
     * - serverHashMap contains RMI stubs which are not serializable and invalid after restart
     * After coordinator restart, all locks are cleared and servers must re-acquire them.
     * We persist coordinatorEpoch so it increments across restarts.
     */
    public synchronized void saveState() {
        File f = new File("jvn_coord_state.ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            CoordinatorState state = new CoordinatorState();
            state.startId = start_id;
            //state.coordinatorEpoch = coordinatorEpoch;
            state.objectIdsMap = new ConcurrentHashMap<>(this.objectIdsMap);
            state.registrationMap = new ConcurrentHashMap<>(this.registrationMap);
            oos.writeObject(state);
            System.out.println("[JvnCoordinator] State saved to " + f.getAbsolutePath()); //+ " (epoch=" + coordinatorEpoch + ")");
        } catch (IOException e) {
            System.err.println("[JvnCoordinator] Failed to save state: " + e.getMessage());
        }
    }

    /**
     * Load coordinator state from disk if available.
     * Locks are cleared on restart - servers must re-acquire them.
     */
    public synchronized void loadState() throws IOException, ClassNotFoundException {
        File f = new File("jvn_coord_state.ser");
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            CoordinatorState state = (CoordinatorState) ois.readObject();
            if (state != null) {
                start_id = state.startId;
                //coordinatorEpoch = state.coordinatorEpoch;
                if (state.objectIdsMap != null) this.objectIdsMap.putAll(state.objectIdsMap);
                if (state.registrationMap != null) this.registrationMap.putAll(state.registrationMap);
                // Clear locks - servers must re-acquire after coordinator restart
                this.lockHashMap.clear();
                this.serverHashMap.clear();
                System.out.println("[JvnCoordinator] State loaded from " + f.getAbsolutePath() + " - all locks cleared");
            }
        }
    }
    
    /**
     * Get the current coordinator epoch. Servers use this to detect coordinator restarts.
     * @return current epoch number
     */
    @Override
    public synchronized long getCoordinatorEpoch() throws RemoteException {
      return 0;
    }

    public void informServers() throws RemoteException, JvnException
    {
    // Iterate over the concurrent set. Use a small local collection to record
    // servers that failed to respond and remove them afterwards to avoid
    // modifying the set while iterating (even though the concurrent set
    // tolerates it, removal during iteration can still produce surprising
    // results). This also allows us to handle RemoteException/JvnException
    // per-server without aborting the whole operation.
    if(listOfServers.isEmpty())
    {
      System.out.println("[JvnCoordImpl] No servers were registered, exiting...");
      return;
    }

    java.util.Set<JvnRemoteServer> toRemove = new java.util.HashSet<>();
    for (JvnRemoteServer server : listOfServers) {
      try {
        System.out.println("[JvnCoordImpl] About to inform server :" + server);
        server.resetObjectState();
      } catch (RemoteException | JvnException e) {
        System.err.println("[JvnCoordImpl] Failed to inform server " + server + ": " + e.getMessage());
        // Mark the server for removal from our set of known servers
        toRemove.add(server);
      } catch (Exception e) {
        System.err.println("[JvnCoordImpl] Unexpected error while informing server " + server + ": " + e.getMessage());
        toRemove.add(server);
      }
    }
    if (!toRemove.isEmpty()) {
      listOfServers.removeAll(toRemove);
    }
    }

    private static class CoordinatorState implements Serializable {
        private static final long serialVersionUID = 1L;
        int startId;
        //long coordinatorEpoch;
        ConcurrentHashMap<Integer, Serializable> objectIdsMap;
        ConcurrentHashMap<String, JvnObject> registrationMap;
    }  
}

 