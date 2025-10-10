
package jvn.Utils; 

import java.io.Serializable;

public enum JvnSTATES {
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
            wait();
        } catch (InterruptedException e){
            System.err.println(e.message);
        }
    }

    private void customNotify() {
        System.out.println("Notifying...");
        notify();
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException{
        switch (state){
            case JvnSTATES.RC:
                state = JvnSTATES.R;
                break;
            case JvnSTATES.WC:
                state = JvnSTATES.RWC;
                break;
            case JvnSTATES.W:
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
            case JvnSTATES.W :
                state = JvnSTATES.WC;
                break;
            case JvnSTATES.R:
                state = JvnSTATES.RC;
                break;
            case JvnSTATES.RWC:
                state = JvnSTATES.WC;
                break;
        }

        customNotify();
        // function to notify, to be implemented and called her
    }


    @Override
    public synchronized void jvnInvalidateReader() throws JvnException{
        switch (state){
            case JvnSTATES.R:
                customWait();
                // wait function to be implemented and called here
                state = JvnSTATES.NL;
                break;
            case JvnSTATES.RC:
                state = JvnSTATES.NL;
                break;
            default:
                System.out.println("InvalidateReader called on state : " + state);
        }
    }


    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException{
        switch (state){
            case JvnSTATES.RWC:
                customWait();
                state = JvnSTATES.NL;
                break;
            case JvnSTATES.W:
                customWait();
                state = JvnSTATES.NL;
                break;
            case JvnSTATES.WC:
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
            case JvnSTATES.RWC:
                customWait();
                state = JvnSTATES.RC;
                break;
            case JvnSTATES.WC:
                state = JvnSTATES.RC;
                break;
            case JvnSTATES.W:
                customWait();
                state = JvnSTATES.RC;
                break;
            default:
                System.out.println("InvalidateWriterForReader called on state : " + state);
        }

        return obj;
    } 




}