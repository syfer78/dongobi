package me.genomatch.storage.sharefile.exception;

public class LogInFailedException extends Exception {
    public LogInFailedException(String msg) {
        super(msg);
    }

    public LogInFailedException(Exception e) {
        super(e);
    }
}
