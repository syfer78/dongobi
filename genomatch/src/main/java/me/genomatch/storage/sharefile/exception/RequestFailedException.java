package me.genomatch.storage.sharefile.exception;

public class RequestFailedException extends Exception {
    public RequestFailedException(Exception e) {
        super(e);
    }
}
