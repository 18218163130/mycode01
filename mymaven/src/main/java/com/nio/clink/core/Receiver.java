package com.nio.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:03
 * @Description:
 */
public interface Receiver extends Closeable {

    boolean receiverAsync(IoArgs.IoArgeEventListener listener) throws IOException;
}
