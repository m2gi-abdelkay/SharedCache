package jvn.JvnObject;

import java.io.Serializable;

import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;

public class MyObject implements JvnObject {
    // Add fields as needed
    private int id;
    private String name;

    public MyObject(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void jvnSetObjectId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyObject{id=" + id + ", name='" + name + "'}";
    }

    @Override
    public void jvnLockRead() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnLockRead'");
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnLockWrite'");
    }

    @Override
    public void jvnUnLock() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnUnLock'");
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnGetObjectId'");
    }

    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnGetSharedObject'");
    }

    @Override
    public void jvnInvalidateReader() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnInvalidateReader'");
    }

    @Override
    public Serializable jvnInvalidateWriter() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnInvalidateWriter'");
    }

    @Override
    public Serializable jvnInvalidateWriterForReader() throws JvnException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'jvnInvalidateWriterForReader'");
    }
}