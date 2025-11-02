
package jvn.JvnObject; 

import java.io.Serializable;

import irc.SentenceImpl;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;

enum JvnSTATES {
        NL,
        R,
        W,
        RC,
        WC,
        RWC
    }

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
        System.out.println("[JvnObjectImpl] jvnGetSharedObject called for id=" + id + " objId=" + System.identityHashCode(obj) + " class=" + obj.getClass().getName());
        return obj;
    }


    private void customWait() {
        try {
            System.out.println("Waiting...");
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e){
            System.err.println("exception caught");
        }
    }

    private void customNotify() {
        System.out.println("Notifying...");
        synchronized (this) {
            notify();
        }
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException{
        System.out.println("[JvnObjectImpl] Executing jvn Lock read with state :" + state);
        switch (state){
            case RC:
                state = JvnSTATES.R;
                break;
            case WC:
                state = JvnSTATES.RWC;
                break;
            case W:
                state = JvnSTATES.R;
                break;
            default:
            obj = JvnServerImpl.jvnGetServer().jvnLockRead(jvnGetObjectId());
            state = JvnSTATES.R;
        }
        System.out.println("[JVN Object " + id + "] State changed to " + state);
    }


    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        System.out.println("[JvnObjectImpl] Executing jvn Lock write with state :" + state);
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
                customWait();
                // wait function to be implemented and called here
                state = JvnSTATES.NL;
                break;
            case RC:
                state = JvnSTATES.NL;
                break;
            default:
                System.out.println("[JvnObjectImpl] InvalidateReader called on state : " + state);
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
                System.out.println("[JvnObjectImpl] InvalidateWriter called on state : " + state);
        }

        return obj;
    }

    @Override 
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        System.out.println("[JvnObjectImpl] s is :" + ((SentenceImpl)obj).read());
        switch (state) {
            case RWC -> {
                customWait();
                state = JvnSTATES.RC;
            }
            case WC -> state = JvnSTATES.RC;
            case W -> {
                customWait();
                state = JvnSTATES.RC;
            }
            default -> System.out.println("[JvnObjectImpl] InvalidateWriterForReader called on state : " + state);
        }

        return obj;
    } 

@Override
public String toString() {
    return "JvnObjectImpl{" +
           "id=" + id +
           ", name='" + name + '\'' +
           ", state=" + state +
           ", obj=" + obj +
           '}';
}




}