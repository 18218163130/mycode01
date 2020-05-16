package com.nio.clink.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * @Auther: dongwf
 * @Date: 2020/5/15 23:13
 * @Description:
 */
public class CloseUtils {
    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
