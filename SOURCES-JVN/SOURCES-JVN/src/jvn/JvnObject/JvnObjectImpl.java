
package jvn.JvnObject; 

import java.io.Serializable;
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
    private JvnSTATES state;

    public String name;



    public JvnObjectImpl(Serializable o, int id){
        this.obj = o;
        this.id = id;
        this.state = JvnSTATES.W;
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
    }


    @Override
    public synchronized void jvnLockWrite() throws JvnException{
        if (state != JvnSTATES.WC) {
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
            case RWC -> {
                customWait();
                state = JvnSTATES.RC;
            }
            case WC -> state = JvnSTATES.RC;
            case W -> {
                customWait();
                state = JvnSTATES.RC;
            }
            default -> System.out.println("InvalidateWriterForReader called on state : " + state);
        }

        return obj;
    } 




}