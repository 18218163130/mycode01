package com.nio.clink.impl;

import com.nio.clink.core.IoArgs;
import com.nio.clink.core.IoProvider;
import com.nio.clink.core.Receiver;
import com.nio.clink.core.Sender;
import com.nio.clink.util.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:21
 * @Description:
 */
public class SocketChannelAdapter implements Sender, Receiver, Closeable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel socketChannel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgeEventListener receiveIoEventListener;
    private IoArgs.IoArgeEventListener sendIoEventListener;


    public SocketChannelAdapter(SocketChannel socketChannel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.socketChannel = socketChannel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        // 设置通道非阻塞
        this.socketChannel.configureBlocking(false);
    }

    public boolean receiverAsync(IoArgs.IoArgeEventListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("当前channel已经关闭");
        }
        receiveIoEventListener = listener;
        return ioProvider.registerInput(socketChannel,inputCallback);
    }

    public boolean sendAsync(IoArgs args, IoArgs.IoArgeEventListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("当前channel已经关闭");
        }
        sendIoEventListener = listener;
        // 当前发送的数据附加到回调中
        outputCallback.setAttach(args);
        return ioProvider.registerOutput(socketChannel,outputCallback);
    }


    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            ioProvider.unregisterInput(socketChannel);
            ioProvider.unregisterOutput(socketChannel);
            CloseUtils.close(socketChannel);
            // 回调当前channel已经关闭
            listener.onChannelClosed(socketChannel);
        }
    }

    /**
     *
     */
    private IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if(isClosed.get()){ return; }
            // TODO
            sendIoEventListener.onCompleted(null);
        }
    };
    /**
     * 当可以读取时回调
     */
    private IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
          if(isClosed.get()){return;}
          IoArgs args = new IoArgs();
          // 转变成局部变量
          IoArgs.IoArgeEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;
          // 回调开始
            if(listener!=null) {
                listener.onStarted(args);
            }
            // 具体读取操作
            try{
                if(args.read(socketChannel)>0 && listener!=null){
                    listener.onCompleted(args);
                }else{
                    throw new IOException("当前信息不能被读取了");
                }
            }catch (IOException e){
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    /**
     * 通道管理回调接口
     */
    public interface OnChannelStatusChangedListener{
        void onChannelClosed(SocketChannel socketChannel);
    }

}
