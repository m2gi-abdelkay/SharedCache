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
    private Class<?> implementationClass; // Cache the implementation class for annotation lookup

    public JvnHandler(JvnObject jo) throws JvnException {
        this.jo = jo;
        // Cache the implementation class so we can check annotations even if obj becomes null
        Serializable obj = jo.jvnGetSharedObject();
        if (obj != null) {
            this.implementationClass = obj.getClass();
        }
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

        try {
            // Check annotations on the interface method first
            boolean readAnnotated = method.isAnnotationPresent(ReadLock.class);
            boolean writeAnnotated = method.isAnnotationPresent(WriteLock.class);
            
            // If not on interface, check the cached implementation class (works even if obj is null)
            if (!readAnnotated && !writeAnnotated && implementationClass != null) {
                try {
                    Method implMethod = implementationClass.getMethod(method.getName(), method.getParameterTypes());
                    readAnnotated = implMethod.isAnnotationPresent(ReadLock.class);
                    writeAnnotated = implMethod.isAnnotationPresent(WriteLock.class);
                } catch (NoSuchMethodException nsme) {
                    System.out.println("[JvnHandler] No impl method found in cached class");
                }
            }

            System.out.println("[JvnHandler] method=" + method.getName() + ", readAnn=" + readAnnotated + ", writeAnn=" + writeAnnotated);

            // Acquire lock based on annotations
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

            // After lock acquisition, get the target object (should be updated by lock methods)
            Object target = jo.jvnGetSharedObject();
            if (target == null) {
                jo.resetState();
                throw new JvnException("Shared object is null after lock acquisition - coordinator may be down");
            }

            // Find the implementation method and invoke it
            Method implMethod = null;
            try {
                implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException nsme) {
                // No implementation method found, will use interface method
            }

            Object res;
            if (implMethod != null) {
                System.out.println("[JvnHandler] Calling impl method!");
                res = implMethod.invoke(target, args);
            } else {
                res = method.invoke(target, args);
            }

            return res;

        } catch (Throwable e) {
            System.err.println("[JvnHandler] Error during method invocation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (lockAcquired) {
                try { jo.jvnUnLock(); } catch (Exception ex) { System.err.println("unlock error: " + ex.getMessage()); }
            }
        }
    }
    
}
