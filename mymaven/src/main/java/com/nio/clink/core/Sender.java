package com.nio.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:02
 * @Description:
 */
public interface Sender extends Closeable {

    boolean sendAsync(IoArgs args,IoArgs.IoArgeEventListener listener) throws IOException;
}
