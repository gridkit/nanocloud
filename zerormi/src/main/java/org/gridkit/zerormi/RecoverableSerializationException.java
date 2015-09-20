package org.gridkit.zerormi;

@SuppressWarnings("serial") 
class RecoverableSerializationException extends RuntimeException {

    public RecoverableSerializationException(Exception e) {
        super(e);
    }	    
}