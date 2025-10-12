

import java.io.Serializable;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Utils.JvnSTATES;



public class JvnObjectImpl implements JvnObject{
    private Serializable obj; 
    private int id;
    public String name;
    private JvnSTATES state;

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
            this.wait();
        } catch (InterruptedException e){
            System.err.println(e.getMessage());
        }
    }

    private void customNotify() {
        System.out.println("Notifying...");
        this.notify();
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
            case R:
            case RWC:
            case W:
                // already have the lock
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
                this.customWait();
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
            case RWC:
                customWait();
                state = JvnSTATES.RC;
                break;
            case WC:
                state = JvnSTATES.RC;
                break;
            case W:
                customWait();
                state = JvnSTATES.RC;
                break;
            default:
                System.out.println("InvalidateWriterForReader called on state : " + state);
        }

        return obj;
    } 

}