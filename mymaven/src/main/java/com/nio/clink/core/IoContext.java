package com.nio.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:12
 * @Description:
 */
public class IoContext{
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    public IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }
    public IoProvider getIoProvider(){return this.ioProvider;}

    public static IoContext get(){return INSTANCE;}

    public static StartedBoot setup(){return new StartedBoot();}

    public  static void close() throws IOException {
        if(INSTANCE!=null){
            INSTANCE.callClose();
        }
    }

    public void callClose() throws IOException{
        ioProvider.close();
    }


    public static class StartedBoot{
        private IoProvider ioProvider;
        private StartedBoot(){}
        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
