/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn.Server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import jvn.JvnObject.JvnObjectImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnLocalServer;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnRemoteCoord;
import jvn.Utils.JvnRemoteServer;



public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton  
	private static JvnServerImpl js = null;
	private final JvnRemoteCoord javanaiseCoord;
	private Map<Integer, JvnObject> objCache;


  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();

		Registry registry = LocateRegistry.getRegistry();
		javanaiseCoord = (JvnRemoteCoord) registry.lookup("Javanaise"); // add name later attribute in coord

		this.objCache = new HashMap<>();

	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		System.out.println("[jvnServerImpl] Executing jvnGetServer...");
		if (js == null){
			try {
				System.out.println("Server is null initially.");
				js = new JvnServerImpl();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		System.out.println("Server isn't null.");
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	@Override
	public  void jvnTerminate()
	throws jvn.Utils.JvnException {
    // to be completed
		try{
			javanaiseCoord.jvnTerminate(this);
		} catch (JvnException e){
			System.err.println(e.getMessage());
		} catch (RemoteException ex) {
			System.out.println("Erreuir in remote exception in jvnTerminate");
            }
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
        @Override
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.Utils.JvnException { 
		try {
			int oid = javanaiseCoord.jvnGetObjectId();
			javanaiseCoord.jvnLockWrite(oid, this);
			return new JvnObjectImpl(o, oid, "WRITE");
		} catch (RemoteException e) {
			throw new JvnException("Failed to create object due to remote exception");
		}
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
        @Override
	public  Integer jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.Utils.JvnException {
		this.objCache.put(jo.jvnGetObjectId(), jo);

		try {
			return javanaiseCoord.jvnRegisterObject(jon, jo, this);
		} catch (RemoteException e) {
			System.out.println("Caught remite exception");
		}
		catch (JvnException e)
		{
			System.out.println("Caught error in registration!");
		}
		return 1;
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	@Override
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.Utils.JvnException {
		try {
			JvnObject jo = javanaiseCoord.jvnLookupObject(jon, this);
			JvnObjectImpl localJo = new JvnObjectImpl(jo.jvnGetSharedObject(),jo.jvnGetObjectId(),"");
			this.objCache.put(localJo.jvnGetObjectId(), localJo);
			return localJo;
		} catch (RemoteException e) {
			System.err.println("remote exception caught");
			return null;
		}
		catch (JvnException e)
		{
			System.err.println(e.getMessage());
			return null;
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
	@Override
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
	   try {
						 Serializable state = javanaiseCoord.jvnLockRead(joi, this);
						 System.out.println("[JvnServerImpl] jvnLockRead returned for joi=" + joi + " objId=" + System.identityHashCode(state) + " state=" + state.getClass().getName());
						 return state;
	   } catch (RemoteException e) {
		   System.err.println("Remote exception erreur caught");
		   return null;
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
	@Override
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
	   try {
						 Serializable state = javanaiseCoord.jvnLockWrite(joi, this);
						 System.out.println("[JvnServerImpl] jvnLockWrite returned for joi=" + joi + " objId=" + System.identityHashCode(state) + " state=" + state.getClass().getName());
						 return state;
	   } catch (RemoteException e) {
		   System.err.println("Remote exception erreur caught");
		   return null;
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
	@Override
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
	@Override
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.Utils.JvnException { 
		// to be completed
		return this.objCache.get(joi).jvnInvalidateWriterForReader();

	 };

	@Override
public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
        return false;
    }
    JvnServerImpl other = (JvnServerImpl) obj;
    // Add comparison logic for fields if necessary
    return true; // Adjust based on fields to compare
}

}

 
