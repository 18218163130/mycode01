package com.nio.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 21:35
 * @Description:
 */
public class IoArgs {
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException{
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException{
        return channel.write(buffer);
    }

    public String bufferString(){
        // 去掉换行符
        return new String(byteBuffer,0,buffer.position()-1);
    }

    /**
     * IoArgs事件回调
     */
    public interface IoArgeEventListener{
        // IoArgs开始时回调
        void onStarted(IoArgs args);
        // 完成是回调
        void onCompleted(IoArgs args);
    }

}
