package jvn.Handler;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import jvn.Annotations.*;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;

public class JvnHandler implements InvocationHandler {

    private JvnObject jo;

    public JvnHandler(JvnObject jo){
        this.jo = jo;
    }


    public static Object newInstance(JvnObject jo) throws JvnException {
        if (jo != null){
            Serializable obj = jo.jvnGetSharedObject();
            return Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new JvnHandler(jo));
        }
        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        
        // TODO Auto-generated method stub
        //throw new UnsupportedOperationException("Unimplemented method 'invoke'");

        try {
            if (method.isAnnotationPresent(ReadLock.class)){ // add annotation here
                System.out.println("[JvnHandler] lock read");
                jo.jvnLockRead();
            } else if (method.isAnnotationPresent(WriteLock.class)){ // add annotation here
                System.out.println("[JvnHandler] lock write");
                jo.jvnLockWrite();
            }




            Object res = method.invoke(jo.jvnGetSharedObject(), args);
            jo.jvnUnLock();

            return res;
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
    
}
