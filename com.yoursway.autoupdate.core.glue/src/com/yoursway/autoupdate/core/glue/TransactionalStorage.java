package com.yoursway.autoupdate.core.glue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.yoursway.utils.DelegatingInputStream;
import com.yoursway.utils.DelegatingOutputStream;

public class TransactionalStorage implements Storage {
    
    private final File mainFile;
    
    private final File logFile;
    
    private boolean isWriting = false;
    
    private int readers = 0;
    
    public TransactionalStorage(File mainFile, File logFile) {
        if (mainFile == null)
            throw new NullPointerException("mainFile is null");
        if (logFile == null)
            throw new NullPointerException("logFile is null");
        this.mainFile = mainFile;
        this.logFile = logFile;
        restoreFromPossibleCrash();
    }
    
    private void restoreFromPossibleCrash() {
        if (mainFile.exists()) {
            if (logFile.exists()) {
                if (!logFile.delete())
                    throw new TransactionalStorageNonOperational("Cannot delete " + logFile);
            }
        } else {
            if (logFile.exists())
                renameLogIntoMain();
        }
    }
    
    private void renameLogIntoMain() {
        if (!logFile.renameTo(mainFile))
            throw new TransactionalStorageNonOperational("Cannot rename " + logFile + " into " + mainFile);
    }
    
    public InputStream openRead() {
        enterReader();
        try {
            try {
                FileInputStream in = new FileInputStream(mainFile);
                return new DelegatingInputStream(in) {
                    
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            leaveReader();
                        }
                    }
                    
                };
            } catch (RuntimeException e) {
                leaveReader();
                throw e;
            } catch (Error e) {
                leaveReader();
                throw e;
            }
        } catch (FileNotFoundException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    
    public OutputStream openWrite() {
        enterWriter();
        try {
            try {
                if (!mainFile.exists()) {
                    new FileOutputStream(mainFile).close();
                }
                return new DelegatingOutputStream(new FileOutputStream(logFile)) {
                    
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                            commit();
                        } finally {
                            leaveWriter();
                        }
                    }
                    
                };
            } catch (RuntimeException e) {
                leaveWriter();
                throw e;
            } catch (Error e) {
                leaveWriter();
                throw e;
            }
        } catch (IOException e) {
            throw new TransactionalStorageNonOperational(e);
        }
    }
    
    protected void commit() {
        if (!mainFile.delete())
            throw new TransactionalStorageNonOperational("Cannot delete " + mainFile);
        renameLogIntoMain();
    }
    
    private synchronized void enterReader() {
        if (isWriting)
            throw new IllegalStateException("Cannot read from transactional storage while writing into it");
        readers++;
    }
    
    private synchronized void leaveReader() {
        if (isWriting)
            throw new AssertionError("isWriting == true");
        if (readers == 0)
            throw new IllegalStateException("readers == 0");
        readers--;
    }
    
    private synchronized void enterWriter() {
        if (readers > 0)
            throw new IllegalStateException("Cannot write into transactional storage while reading from it");
        isWriting = true;
    }
    
    private synchronized void leaveWriter() {
        if (!isWriting)
            throw new IllegalStateException("isWriting == false");
        if (readers > 0)
            throw new AssertionError("readers >  0");
        isWriting = false;
    }
    
}
