package com.nio.clink.core;

import com.nio.clink.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:06
 * @Description:
 */
public class Connector implements Closeable , SocketChannelAdapter.OnChannelStatusChangedListener{
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException{
        this.channel = socketChannel;
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel,context.getIoProvider(),this);
        this.sender = adapter;
        this.receiver = adapter;

        readNextMessage();

    }

    /**
     * 读取数据
     */
    private void readNextMessage(){
        if(this.receiver!=null){
            try {
                receiver.receiverAsync(echoReceiverListener);
            }catch (IOException e){
                System.out.println("接收数据异常");
            }
        }
    }

    /**
     * 消息接受者
     */
    private IoArgs.IoArgeEventListener echoReceiverListener = new IoArgs.IoArgeEventListener() {
        public void onStarted(IoArgs args) {

        }

        public void onCompleted(IoArgs args) {
            // 打印数据，并读取下一条数据
            onReceiveNewMessage(args.bufferString());
            readNextMessage();
        }
    };


    /**
     * 打印接收到的消息
     * @param str
     */
    protected void onReceiveNewMessage(String str){
        System.out.println(key+":"+str);
    }

    public void close() throws IOException {

    }

    public void onChannelClosed(SocketChannel socketChannel) {

    }
}
