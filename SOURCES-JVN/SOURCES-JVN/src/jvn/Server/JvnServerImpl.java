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
	private JvnRemoteCoord javanaiseCoord;
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

	// Try to re-resolve coordinator stub if RemoteException occurs
	private synchronized void ensureCoordConnected() {
		try {
			Registry registry = LocateRegistry.getRegistry();
			JvnRemoteCoord coord = (JvnRemoteCoord) registry.lookup("Javanaise");
			this.javanaiseCoord = coord;
		} catch (Exception e) {
			System.err.println("[JvnServerImpl] Failed to reconnect to coordinator: " + e.getMessage());
		}
	}
	
	/**
	 * Check if coordinator has restarted by comparing epochs.
	 * If coordinator restarted, invalidate all local locks.
	 */
	/* 
	private synchronized void checkCoordinatorEpoch() {
		try {
			long currentEpoch = javanaiseCoord.getCoordinatorEpoch();
			if (cachedCoordinatorEpoch != -1 && currentEpoch != cachedCoordinatorEpoch) {
				System.out.println("[JvnServerImpl] Coordinator restart detected! Old epoch=" + cachedCoordinatorEpoch + ", new epoch=" + currentEpoch);
				System.out.println("[JvnServerImpl] Invalidating all local locks in cache...");
				// Coordinator restarted - invalidate all local lock states
				for (JvnObject jo : objCache.values()) {
					try {
						// Force objects back to NL (No Lock) state regardless of current state
						if (jo != null) {
							((JvnObjectImpl) jo).resetState(); // Handles all states including write locks
						}
					} catch (Exception e) {
						// Ignore errors during invalidation
					}
				}
			}
			cachedCoordinatorEpoch = currentEpoch;
		} catch (RemoteException e) {
			System.err.println("[JvnServerImpl] Failed to check coordinator epoch: " + e.getMessage());
		}
	}
	*/


	
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
			System.err.println("[JvnServerImpl] Remote exception in jvnTerminate: " + ex.getMessage());
			// Try to reconnect and retry once
			ensureCoordConnected();
			try {
				javanaiseCoord.jvnTerminate(this);
			} catch (Exception e) {
				System.err.println("[JvnServerImpl] Failed to terminate on coordinator (retry failed)");
			}
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
			System.err.println("[JvnServerImpl] Remote exception in jvnCreateObject: " + e.getMessage());
			// Try to reconnect and retry once
			ensureCoordConnected();
			try {
				int oid = javanaiseCoord.jvnGetObjectId();
				javanaiseCoord.jvnLockWrite(oid, this);
				return new JvnObjectImpl(o, oid, "WRITE");
			} catch (Exception ex) {
				throw new JvnException("Failed to create object due to remote exception (retry failed)");
			}
		}
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
        @Override
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.Utils.JvnException {
		this.objCache.put(jo.jvnGetObjectId(), jo);

		System.out.println("C####### dans jvnregister ##############");


		///displayCache();

		try {
			javanaiseCoord.jvnRegisterObject(jon, jo, this);
		} catch (RemoteException e) {
			System.err.println("[JvnServerImpl] Remote exception in jvnRegisterObject: " + e.getMessage());
			// Try to reconnect and retry once
			ensureCoordConnected();
			try {
				javanaiseCoord.jvnRegisterObject(jon, jo, this);
			} catch (Exception ex) {
				throw new JvnException("Failed to register object due to remote exception (retry failed)");
			}
		}
		catch (JvnException e)
		{
			System.out.println("Caught error in registration!");
		}

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
		// Check if coordinator restarted
		//checkCoordinatorEpoch();
		
		try {
			JvnObject jo = javanaiseCoord.jvnLookupObject(jon, this);
			JvnObjectImpl localJo = new JvnObjectImpl(jo.jvnGetSharedObject(),jo.jvnGetObjectId(),"");
			this.objCache.put(localJo.jvnGetObjectId(), localJo);
			return localJo;
		} catch (RemoteException e) {
			System.err.println("[JvnServerImpl] Remote exception in jvnLookupObject: " + e.getMessage());
			// Try to reconnect and retry once
			ensureCoordConnected();
			try {
				JvnObject jo = javanaiseCoord.jvnLookupObject(jon, this);
				JvnObjectImpl localJo = new JvnObjectImpl(jo.jvnGetSharedObject(),jo.jvnGetObjectId(),"");
				this.objCache.put(localJo.jvnGetObjectId(), localJo);
				return localJo;
			} catch (Exception ex) {
				System.err.println("[JvnServerImpl] Remote exception on retry: " + ex.getMessage());
				return null;
			}
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
		// Check if coordinator restarted before acquiring lock
	   ensureCoordConnected();
	   //checkCoordinatorEpoch();
	   
	   try {
						 Serializable state = javanaiseCoord.jvnLockRead(joi, this);
						 System.out.println("[JvnServerImpl] jvnLockRead returned for joi=" + joi + " objId=" + System.identityHashCode(state) + " state=" + state.getClass().getName());
						 return state;
	   } catch (RemoteException e) {
		   System.err.println("Remote exception error caught: " + e.getMessage());
		   // Try to re-resolve coordinator and retry once
		   ensureCoordConnected();
		   try {
			   Serializable state = javanaiseCoord.jvnLockRead(joi, this);
			   System.out.println("[JvnServerImpl] (retry) jvnLockRead returned for joi=" + joi + " objId=" + System.identityHashCode(state));
			   return state;
		   } catch (Exception ex) {
			   System.err.println("Remote exception on retry: " + ex.getMessage());
			   return null;
		   }
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
	   // Check if coordinator restarted before acquiring lock
	   ensureCoordConnected();
	   //checkCoordinatorEpoch();
	   
	   try {
						 Serializable state = javanaiseCoord.jvnLockWrite(joi, this);
						 System.out.println("[JvnServerImpl] jvnLockWrite returned for joi=" + joi + " objId=" + System.identityHashCode(state) + " state=" + state.getClass().getName());
						 return state;
	   } catch (RemoteException e) {
		   System.err.println("Remote exception error caught: " + e.getMessage());
		   // Try to re-resolve coordinator and retry once
		   ensureCoordConnected();
		   try {
			   Serializable state = javanaiseCoord.jvnLockWrite(joi, this);
			   System.out.println("[JvnServerImpl] (retry) jvnLockWrite returned for joi=" + joi + " objId=" + System.identityHashCode(state));
			   return state;
		   } catch (Exception ex) {
			   System.err.println("Remote exception on retry: " + ex.getMessage());
			   return null;
		   }
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
	@Override
	public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.Utils.JvnException { 
		// to be completed
		return this.objCache.get(joi).jvnInvalidateWriter();
	}

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



	@Override
	public void resetObjectState() throws RemoteException, JvnException {
		for (JvnObject jo : objCache.values()) {
			try {
				// Force objects back to NL (No Lock) state regardless of current state
				if (jo != null) {
					((JvnObjectImpl) jo).resetState(); // Handles all states including write locks
				}
			} catch (Exception e) {
				// Ignore errors during invalidation
			}
		}
	}

}

 
