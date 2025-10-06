/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn.Server;

import java.rmi.server.UnicastRemoteObject;

import jvn.Utils.JvnException;
import jvn.Utils.JvnLocalServer;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnRemoteServer;
import jvn.Coordinator.JvnCoordImpl;

import java.io.*;
import java.util.HashMap;

enum LockStatus {
	NL,
	RC,
	WC,
	R,
	W,
	RWC
}

public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton  
	private static JvnServerImpl js = null;
	private static JvnCoordImpl() javanaise = null;
	private Map<Integer, JvnObject> idToObjMap = new HashMap<>();
	private Map<Integer, LockStatus> lockMap = new HashMap<>();

  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		// super();
		// to be completed
		if (js == null){
			try {
				js = new JvnServerImpl();
				Registry registry = LocateRegistry.getRegistry();
				javanaise = (JvnCoordImpl) registry.lookup("Javanaise");
			} catch (Exception e){
				return null;
			}

		}
		return js;
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.Utils.JvnException {
    // to be completed
		try{
			idToObjMap.clear();
			lockMap.clear();
			javanaise = null;
			js = null;
		} catch (Exception e){
			System.err.println(e.message);
		}
		return;
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.Utils.JvnException { 
		// to be completed

		jo = (JvnObject) o;
		int oid = javanaise.jvnGetObjectId();
		idToObjMap.put(oid, jo);
		// jo.jvnSetObjectId();
		lockMap.put(oid, LockStatus.WriteLock);
		return jo;
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.Utils.JvnException {
		javanaise.jvnRegisterObject(jon, jo, this);
		// to be completed 
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.Utils.JvnException {
		JvnObject jo = javanaise.jvnLookupObject(jon, this);
		return jo;
    // to be completed 
	//	return null;
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
	   Serializable lastState = javanaise.jvnLockRead(joi, this);
	   lockMap.put(joi, LockStatus.R);
	   idToObjMap.put(joi, (JvnObject) lastState);
	   return lastState;
		// to be completed 
		// return null;

	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
	   Serializable lastState = javanaise.jvnLockWrite(joi, this);
	   lockMap.put(joi, LockStatus.W);
	   idToObjMap.put(joi, (JvnObject) lastState);
	   return lastState;
		// to be completed 
		// return null;
	}	

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.Utils.JvnException {
		// to be completed
		JvnObject jo = idToObjMap.get(joi);
		jo.jvnInvalidateReader();
		lockMap.put(joi, LockStatus.NL);
		return;
	  }
	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.Utils.JvnException { 
		// to be completed
	  	JvnObject jo = idToObjMap.get(joi); // get object ref
		jo = jo.jvnInvalidateWriter(); // wait for last value after write
		idToObjMap.put(joi, jo); // update locally
	  	lockMap.put(joi, LockStatus.NL); // remove lock
		return jo; // return updated object
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.Utils.JvnException { 
		// to be completed
		JvnObject jo = idToObjMap.get(joi); // get object ref
		jo = jo.jvnInvalidateWriterForReader(); // get last value after write
		idToObjMap.put(joi, jo); // update locally
		lockMap.put(joi, LockStatus.RC) // reduce lock to read
		return jo; // return updated object

	 };

}

 
