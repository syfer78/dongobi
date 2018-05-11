package me.genomatch.util;

import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

public class ExceptionMessagemergerTest {
    @Before
    public static void testSharefileLogIn() throws Exception {

    }

    @Test
    public void testMerger() {
        Exception te = new IOException("Error!");
        Exception e = new IOException(te);
        String message = ExceptionMessagemerger.mergeMessage(e);
        System.out.println(message);

    }
}
