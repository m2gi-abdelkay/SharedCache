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
import java.util.List;
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
  private ConcurrentHashMap<Integer, List<JvnRemoteServer>> serverHashMap;
  ArrayList<JvnRemoteServer> serverArray;
	

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
    serverArray = new ArrayList<>();
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
   public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{

    //This is the same joi that another server may have the lock on. You don't care about other joi's
    // For the same joi, check whether someone has a lock on it or not
    // if not, then assign joi -> READ_LOCK and joi -> jvnRemoteServer to know which server to call eventually for the invalidation

    if(!lockHashMap.contains(joi))
    {
      //This means no other server has a read lock on the object. 
      //Store the joi to the read_lock and remember the server that has this lock
      lockHashMap.put(joi, LOCK_READ);
      serverArray.add(js); 
      serverHashMap.put(joi, serverArray);
      return objectIdsMap.get(joi);
    }
  
    



    // to be completed
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
        
    }

  
}

 
