package me.genomatch.storage.sharefile;

import me.genomatch.storage.FileInfo;
import me.genomatch.util.ExceptionMessagemerger;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class ShareFileStorageTest {
    private static ShareFileStorage storage;

    @Before
    public static void testSharefileLogIn() throws Exception {
        storage = new ShareFileStorage("ExiwcRWlCEA6xXEu1DC1qwjT79dY1351", "0nXCnXr3h59No6lreOfr1DSeP8HYscQXBb64mE0v2TGekWSo", "support@diagnomics.com", "G3n0M@tch2017");
    }

    @Test
    public void testCreation() throws Exception {
        storage.createUserFolder("test_auto_app");
    }

    @Test
    public void testDownload() throws Exception {
        FileOutputStream fos = new FileOutputStream("./test.pdf");
        FileInfo fileInfo = storage.getFileInfo("test_user1/report/SkinMatch_Report.pdf");
        storage.downloadFile(fileInfo, fos);
        fos.close();
    }

    @Test
    public void testMerger() {
        Exception te = new IOException("Error!");
        Exception e = new IOException(te);
        String message = ExceptionMessagemerger.mergeMessage(e);
        System.out.println(message);

    }

}
