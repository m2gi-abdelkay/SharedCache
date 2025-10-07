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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnRemoteCoord;
import jvn.Utils.JvnRemoteServer;

import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{

  private static final int LOCK_READ =  0;
  private static final int LOCK_WRITE = 1;
  private static int start_id = 1;
  private ConcurrentHashMap<String,JvnObject> registrationMap;
  private ConcurrentHashMap<Integer, JvnObject> objectIdsMap;
  private ConcurrentHashMap<Integer,Integer> lockHashMap;
  private ConcurrentHashMap<Integer, HashSet<JvnRemoteServer>> serverHashMap;
	

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
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.Utils.JvnException {
    start_id++;
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

    if(registrationMap.contains(jon))
    {
      throw new JvnException("Object name is already assigned to an object, please change object name.");
    }

    objectIdsMap.put(jo.jvnGetObjectId(), jo);
    registrationMap.put(jon, jo);
    return;
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.Utils.JvnException{
    if(!registrationMap.contains(jon))
    {
      return null;
    }
    return registrationMap.get(jon);
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

    //This is the same joi that another server may have the lock on. You don't care about other joi's
    // For the same joi, check whether someone has a lock on it or not
    // if not, then assign joi -> READ_LOCK and joi -> jvnRemoteServer to know which server to call eventually for the invalidation

    if(!lockHashMap.contains(joi))
    {
      //This means no other server has a read or write lock on the object. 
      //Store the joi to the read_lock and remember the server that has this lock (cf cas de figure 2)
      lockHashMap.put(joi, LOCK_READ);
      HashSet<JvnRemoteServer> serverArray = new HashSet<>();
      serverArray.add(js); 
      serverHashMap.put(joi, serverArray);
      return objectIdsMap.get(joi);
    }

    if(lockHashMap.contains(joi))
    {
      //This means that a/many server(s) has/have a lock on our shared object. We retrieve the list of servers and the type of the lock:
      int lock_type = lockHashMap.get(joi);
      HashSet<JvnRemoteServer> serverArray = serverHashMap.get(joi);
      //Determine type of lock:
      if(lock_type == LOCK_READ)
      {
        // This means that one (or many) servers have a read lock on the object
        //Check whether we have one server in our list and it's the same server asking for the lock (cf cohérence cas de figure 4)
        if(serverArray.size() == 1 && serverArray.contains(js))
        {
          //then in this case we simply return the object
          return objectIdsMap.get(joi);
        }

        //Otherwise, we can check whether we have multiple servers (should be guaranteed atp) and assign a read lock to this server (cohérence cas de figure 3):
        if(serverArray.size()>1)
        {
          //No duplication concerns, we have a hashset
          serverArray.add(js);
          serverHashMap.put(joi, serverArray);
          //Since objetct is being read, then the version stored in our object map should be the latest version:
          return objectIdsMap.get(joi);
        }

      }
      if(lock_type == LOCK_WRITE){
        //The lock type that we have is a WRITE LOCK
        //This means that one server has a write lock on our object
        //First, we check whether the server that is asking for a read lock is the same as the one that has the write lock
        //(since we only have one write lock, the serverArray list should always be 1)
        if(serverArray.contains(js))
        {
          //Then do nothing (an update is required at the JvnObjectLevel, see cas de cohérence figure 5)
          return objectIdsMap.get(joi);
        }

        //If the write lock is in another server (cf cohérence cas de figure 6):
        if(!serverArray.contains(js))
        {
          //Then we must invalidate, get the server which has the lock:
          JvnRemoteServer serverWithWLock = serverArray.iterator().next();
          JvnObject updatedObject = (JvnObject) serverWithWLock.jvnInvalidateWriterForReader(joi);
          //Store the updated object:
          objectIdsMap.put(joi, updatedObject);
          //Change the nature of the lock on the object:
          lockHashMap.put(joi, LOCK_READ);
          //Update the lock list:
          HashSet<JvnRemoteServer> newServerSet = new HashSet<>();
          newServerSet.add(serverWithWLock);
          newServerSet.add(js);
          serverHashMap.put(joi, newServerSet);
          //return the updated object
          return updatedObject;
          
        }
      }

    }
    //Should never happen
    return null;
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
    //This is the same joi that another server may have the lock on. You don't care about other joi's
    // For the same joi, check whether someone has a lock on it or not
    // if not, then assign joi -> WRITE_LOCK and joi -> jvnRemoteServer to know which server to call eventually for the invalidation

    if(!lockHashMap.contains(joi))
    {
      //This means no other server has a read or write lock on the object. 
      //Store the joi to the read_lock and remember the server that has this lock (cf cas de figure 2)
      lockHashMap.put(joi, LOCK_WRITE);
      HashSet<JvnRemoteServer> serverArray = new HashSet<>();
      serverArray.add(js); 
      serverHashMap.put(joi, serverArray);
      return objectIdsMap.get(joi);
    }

    if(lockHashMap.contains(joi))
    {
      int lock_type = lockHashMap.get(joi);
      HashSet<JvnRemoteServer> serverArray = serverHashMap.get(joi);
      if(lock_type == LOCK_WRITE)
      {
        //If another server has a write lock, first
        //Check whether it is the same server as the one that is asking:
        if(serverArray.contains(js))
        {
          //Then in this case, do nothing and wait until someone else asks for the lock:
          return objectIdsMap.get(joi);
        }

        //Otherwise, this means someone else has the lock. We must invalidate the writer (cf cohérence cas de figure 7)
        if(!serverArray.contains(js))
        {
          //Get the server that has the lock
          JvnRemoteServer serverWithWLock = serverArray.iterator().next();
          JvnObject updatedObject = (JvnObject) serverWithWLock.jvnInvalidateWriter(joi);
          //Store the updated object:
          objectIdsMap.put(joi, updatedObject);
          //No need to update the type of lock on the object (still a write lock)
          ///However, update the server that has the lock:
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
        if(serverArray.contains(js))
          serverArray.remove(js);
        
        for(JvnRemoteServer jvnServer : serverArray)
        {
          
          jvnServer.jvnInvalidateReader(joi);
          
        }
        //Change lock on object:
        lockHashMap.put(joi, LOCK_WRITE);
        //Add server requesting lock
        HashSet<JvnRemoteServer> newServerSet = new HashSet<>();
        newServerSet.add(js);
        serverHashMap.put(joi, newServerSet);
        //Since readers, object has been subjected to no modification since last update:
        return objectIdsMap.get(joi);
      }
    }
    return null;
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
    //The server (js) is terminating. This means:
    // - On all the joi's, must find the joi's where this server has a lock on and remove it from that list
    // - If the server is the only one that has a lock on an object, mark the object as unlocked (remove it from the locked hash map and server hash map)
    for(Integer joi : serverHashMap.keySet())
    {
      //For each joi, receive the list of servers that have a lock on it
      HashSet<JvnRemoteServer> servers = serverHashMap.get(joi);

      //if the list of servers that have a lock on it include the server that is terminating
      if(servers.contains(js))
      {
        //Then remove it
        servers.remove(js);

        //If the list is now empty (was the last element)
        if(servers.isEmpty())
        {
          //Then mark the object as unlocked
          lockHashMap.remove(joi);
          serverHashMap.remove(joi);
        }
        else{
          //Update the list of servers
          serverHashMap.put(joi, servers);
        }
      }

    }
        
    }

  
}

 
