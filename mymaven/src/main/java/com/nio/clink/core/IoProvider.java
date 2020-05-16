package com.nio.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 22:54
 * @Description:
 */
public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel,HandleInputCallback callback);
    boolean registerOutput(SocketChannel channel,HandleOutputCallback callback);

    void unregisterInput(SocketChannel channel);
    void unregisterOutput(SocketChannel channel);

    /**
     * 当可以socketchannel可以输入时回调
     */
    abstract class HandleInputCallback implements Runnable{
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    /**
     * 当socketChannel可以输出数据是回调
     */
    abstract class HandleOutputCallback implements Runnable{
        private Object attach;
        public void setAttach(Object attach) {
            this.attach = attach;
        }
        public void run() {
            canProviderOutput(attach);
        }

        protected abstract void canProviderOutput(Object attach);
    }
}
