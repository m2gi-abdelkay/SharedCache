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



public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton  
	private static JvnServerImpl js = null;
	private JvnRemoteCoord javanaiseCoord;
	private Map<Integer, JvnObject> objCache;


  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();

		Registry registry = LocateRegistry.getRegistry();
		javanaiseCoord = (JvnCoordImpl) registry.lookup("Javanaise"); // add name later attribute in coord

		this.objCache = new HashMap<>();

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
			javanaiseCoord.jvnTerminate(this);
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

		try {
			int oid = javanaiseCoord.jvnGetObjectId();
			return new JvnObjectImpl(o, oid);
		} catch (RemoteException e) {
			System.err.println(e.message);
		}
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.Utils.JvnException {
		this.objCache.put(jo.jvnGetObjectId(), jo);

		try {
			javanaiseCoord.jvnRegisterObject(jon, jo, this);
		} catch (RemoteException e) {
			System.err.println(e.message);
		}
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.Utils.JvnException {
		try {
			JvnObject jo = javanaiseCoord.jvnLookupObject(jon, this);
			if (jo != null) {
				this.objCache.put(jo.jvnGetObjectId(), jo);
			}
			return jo;

		} catch (RemoteException e) {
			System.err.println(e.message);
		}

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
	   try {
		   Serializable state = javanaiseCoord.jvnLockRead(joi, this);
		   return state;
	   } catch (RemoteException e) {
		   System.err.println(e.message);
	   }
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
	   try {
		   Serializable state = javanaiseCoord.jvnLockWrite(joi, this);
		   return state;
	   } catch (RemoteException e) {
		   System.err.println(e.message);
	   }
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
		this.objCache.get(joi).jvnInvalidateReader();
	  }
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.Utils.JvnException { 
		// to be completed
	  	return this.objCache.get(joi).jvnInvalidateWriter();
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
		return this.objCache.get(joi).jvnInvalidateWriterForReader();

	 };

}

 
