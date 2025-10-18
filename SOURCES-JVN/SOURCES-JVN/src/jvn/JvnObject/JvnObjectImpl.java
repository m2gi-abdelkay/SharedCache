
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
=======
package jvn.JvnObject; 
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java

import java.io.Serializable;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
import jvn.Utils.JvnSTATES;


=======

enum JvnSTATES {
        NL,
        R,
        W,
        RC,
        WC,
        RWC
    }
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java

public class JvnObjectImpl implements JvnObject{
    private Serializable obj; 
    private int id;
    public String name;
    private JvnSTATES state;

    public JvnObjectImpl(Serializable o, int id, String lockMode){
        this.obj = o;
        this.id = id;
        if(lockMode.equals("WRITE"))
            this.state = JvnSTATES.W;
        else
            this.state = JvnSTATES.NL;
        
    }


    @Override 
    public int jvnGetObjectId() throws JvnException{
        return id;
    } 


    @Override
    public Serializable jvnGetSharedObject() throws JvnException{
        return obj;
    }


    private void customWait() {
        try {
            System.out.println("Waiting...");
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
            this.wait();
        } catch (InterruptedException e){
            System.err.println(e.getMessage());
=======
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e){
            System.err.println("exception caught");
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
        }
    }

    private void customNotify() {
        System.out.println("Notifying...");
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
        this.notify();
=======
        synchronized (this) {
            notify();
        }
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException{
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java

=======
        System.out.println("Executing jvn Lock read with state :" + state);
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
        switch (state){
            case RC:
                state = JvnSTATES.R;
                break;
            case WC:
                state = JvnSTATES.RWC;
                break;
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
            case R:
            case RWC:
            case W:
                // already have the lock
=======
            case W:
                state = JvnSTATES.R;
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
                break;
            default:
            obj = JvnServerImpl.jvnGetServer().jvnLockRead(jvnGetObjectId());
            state = JvnSTATES.R;
        }
        System.out.println("[JVN Object " + id + "] State changed to " + state);
    }


    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        if (state != JvnSTATES.WC && state != JvnSTATES.W) {
            obj = JvnServerImpl.jvnGetServer().jvnLockWrite(jvnGetObjectId());
        }
        state = JvnSTATES.W;
    }


    @Override
    public synchronized void jvnUnLock() throws JvnException{
        switch(state) {
            case W :
                state = JvnSTATES.WC;
                break;
            case R:
                state = JvnSTATES.RC;
                break;
            case RWC:
                state = JvnSTATES.WC;
                break;
        }

        customNotify();
        // function to notify, to be implemented and called her
    }


    @Override
    public synchronized void jvnInvalidateReader() throws JvnException{
        switch (state){
            case R:
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
                this.customWait();
=======
                customWait();
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
                // wait function to be implemented and called here
                state = JvnSTATES.NL;
                break;
            case RC:
                state = JvnSTATES.NL;
                break;
            default:
                System.out.println("InvalidateReader called on state : " + state);
        }
    }


    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException{
        switch (state){
            case RWC:
                customWait();
                state = JvnSTATES.NL;
                break;
            case W:
                customWait();
                state = JvnSTATES.NL;
                break;
            case WC:
                state = JvnSTATES.NL;
                break;
            default:
                System.out.println("InvalidateWriter called on state : " + state);
        }

        return obj;
    }

    @Override 
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException{
        switch (state) {
<<<<<<< HEAD:SOURCES-JVN/SOURCES-JVN/src/jvn/Utils/JvnObjectImpl.java
            case RWC:
                customWait();
                state = JvnSTATES.RC;
                break;
            case WC:
                state = JvnSTATES.RC;
                break;
            case W:
=======
            case RWC -> {
                customWait();
                state = JvnSTATES.RC;
            }
            case WC -> state = JvnSTATES.RC;
            case W -> {
>>>>>>> 6de5189bb8588da2a0e4301d93bd86c1786c40f7:SOURCES-JVN/SOURCES-JVN/src/jvn/JvnObject/JvnObjectImpl.java
                customWait();
                state = JvnSTATES.RC;
            }
            default -> System.out.println("InvalidateWriterForReader called on state : " + state);
        }

        return obj;
    } 

}