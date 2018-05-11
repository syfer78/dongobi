package me.genomatch.storage;

import me.genomatch.storage.sharefile.exception.ItemNotFoundException;
import me.genomatch.storage.sharefile.exception.LogInFailedException;
import me.genomatch.storage.sharefile.exception.RequestFailedException;
import me.genomatch.storage.sharefile.exception.UnknownException;

import java.io.OutputStream;

public interface FileStorage {
    void createUserFolder(String userName) throws UnknownException, RequestFailedException, LogInFailedException;
    void downloadFile(FileInfo fileInfo, OutputStream os) throws ItemNotFoundException, UnknownException, RequestFailedException, LogInFailedException;
    FileInfo getFileInfo(String srcFilePath) throws ItemNotFoundException, UnknownException, RequestFailedException, LogInFailedException;
    FileInfo getFileInfoFromShareId(String shareId) throws UnknownException, RequestFailedException, LogInFailedException;
}
