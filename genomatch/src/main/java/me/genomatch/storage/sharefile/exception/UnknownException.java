package me.genomatch.storage.sharefile.exception;

import java.io.IOException;

public class UnknownException extends Exception {
    public UnknownException(String msg) {
        super(msg);
    }

    public UnknownException(Exception e) {
        super(e);
    }
}
