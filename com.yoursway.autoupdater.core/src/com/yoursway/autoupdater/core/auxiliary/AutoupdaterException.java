package com.yoursway.autoupdater.core.auxiliary;

public class AutoupdaterException extends Exception {
    private static final long serialVersionUID = 506249096687036113L;
    
    public AutoupdaterException(String message) {
        super(message);
    }
    
    public AutoupdaterException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AutoupdaterException(Throwable cause) {
        super("AutoupdaterException: " + cause.getMessage(), cause);
    }
}
