package jvn.Handler;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Annotations.*;

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
        
        boolean lockAcquired = false;
        System.out.println("[JvnHandler] I'm here!");
        Object target = jo.jvnGetSharedObject();

        try {
            // locate implementation method on the real target class
            Method implMethod = null;
            try {
                implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException nsme) {
                System.out.println("No impl method found");
                // no impl method found; that's fine, we'll invoke via interface method
            }

            // check annotations on interface method first, then on implementation
            boolean readAnnotated = method.isAnnotationPresent(ReadLock.class)
                    || (implMethod != null && implMethod.isAnnotationPresent(ReadLock.class));
            boolean writeAnnotated = method.isAnnotationPresent(WriteLock.class)
                    || (implMethod != null && implMethod.isAnnotationPresent(WriteLock.class));

            System.out.println("[JvnHandler] method=" + method.getName() + ", readAnn=" + readAnnotated + ", writeAnn=" + writeAnnotated + ", implMethodPresent=" + (implMethod!=null));

            if (readAnnotated) {
                System.out.println("[JvnHandler] lock read");
                jo.jvnLockRead();
                lockAcquired = true;
            } else if (writeAnnotated) {
                System.out.println("[JvnHandler] lock write");
                jo.jvnLockWrite();
                lockAcquired = true;
            } else {
                System.out.println("[JvnHandler] no lock annotation found");
            }

            // invoke implementation method if available, otherwise invoke the interface method
            Object res;
            if (implMethod != null) {
                System.out.println("[JvnHandler] Calling impl method!");
                res = implMethod.invoke(target, args);
            } else {
                res = method.invoke(target, args);
            }

            return res;

        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (lockAcquired) {
                try { jo.jvnUnLock(); } catch (Exception ex) { System.err.println("unlock error: " + ex.getMessage()); }
            }
        }
    }
    
}
