package com.nio.clink.impl;


import com.nio.clink.core.IoProvider;
import com.nio.clink.util.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: dongwf
 * @Date: 2020/5/14 23:19
 * @Description:
 */
public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 作为锁，是否处于注册中
    private final AtomicBoolean isRegInput = new AtomicBoolean(false);
    private final AtomicBoolean isRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;
    private final ExecutorService inputHandlPool;
    private final ExecutorService outputHandlPool;

    private final HashMap<SelectionKey,Runnable> inputCallbackMap = new HashMap<SelectionKey, Runnable>();
    private final HashMap<SelectionKey,Runnable> outputCallbackMap = new HashMap<SelectionKey, Runnable>();

    public IoSelectorProvider() throws IOException {
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        this.inputHandlPool = Executors.newFixedThreadPool(4,new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        this.outputHandlPool= Executors.newFixedThreadPool(4,new IoProviderThreadFactory("IoProvider-Output-Thread-"));

        // 开始输入输出侦听
        startRead();
        startWrite();
    }

    /**
     * 开始写
     */
    private void startWrite() {
        final Thread thread = new Thread("Clink WriteSelector Thread"){
            @Override
            public void run() {
                while(!isClosed.get()){
                    try {
                        if(writeSelector.select()==0){
                            waitSelection(isRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            handleSelection(selectionKey,SelectionKey.OP_WRITE,outputCallbackMap,outputHandlPool);
                        }
                        // 清空结合防止重复处理
                        selectionKeys.clear();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        // 设置线程优先级
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * 开始读
     */
    private void startRead() {
        final Thread thread = new Thread("Clink ReadSelector Thread"){
            @Override
            public void run() {
                while(!isClosed.get()){
                    try {
                        if(readSelector.select()==0){
                            waitSelection(isRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            handleSelection(selectionKey,SelectionKey.OP_READ,inputCallbackMap,inputHandlPool);
                        }
                        // 清空结合防止重复处理
                        selectionKeys.clear();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        // 设置线程优先级
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, isRegInput, inputCallbackMap, callback)!=null;
    }

    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, isRegOutput, outputCallbackMap, callback)!=null;
    }

    public void unregisterInput(SocketChannel channel) {
        unRegisterSelection(channel,readSelector,inputCallbackMap);
    }

    public void unregisterOutput(SocketChannel channel) {
        unRegisterSelection(channel,writeSelector,outputCallbackMap);
    }

    /**
     * 关闭
     * @throws IOException
     */
    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            inputHandlPool.shutdownNow();
            outputHandlPool.shutdownNow();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            writeSelector.wakeup();
            readSelector.wakeup();
            CloseUtils.close(readSelector,writeSelector);
        }
    }

    /**
     *
     * @param locker
     */
    private static void waitSelection(final AtomicBoolean   locker){
        synchronized (locker){
            if(locker.get()){
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 对注册进行抽象
     * @param socketChannel
     * @param selector
     * @param registerOps
     * @param locker 是否处于某个过程
     * @param map
     * @param runnable
     */
    private static SelectionKey registerSelection(SocketChannel socketChannel,Selector selector,int registerOps,AtomicBoolean locker,HashMap<SelectionKey,Runnable> map,Runnable runnable){
        synchronized (locker){
            // 设置锁定状态
            locker.set(true);
            try {
                selector.wakeup();
                SelectionKey key = null;
                if (socketChannel.isRegistered()) {
                    // 查询是否已经注册过
                    key = socketChannel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }
                if (key == null) {
                    // 注册selector得到key
                    key = socketChannel.register(selector, registerOps);
                    // 注册回调
                    map.put(key,runnable);
                }
                return key;
            }catch (ClosedChannelException e){
                return null;
            }finally{
                 // 解除锁定状态
                locker.set(false);
                try{
                    // 通知
                    locker.notify();
                }catch (Exception e){
                }
            }
        }
    }

    /**
     * 移除注册
     * @param socketChannel
     * @param selector
     * @param map
     * @return
     */
    private static void unRegisterSelection(SocketChannel socketChannel,Selector selector,HashMap<SelectionKey,Runnable> map){
       if(socketChannel.isRegistered()){
           SelectionKey key = socketChannel.keyFor(selector);
           if(key != null){
               key.cancel(); // 取消侦听
               map.remove(key);
               selector.wakeup();
           }
       }
    }

    /**
     * 处理readSelector
     * @param key
     * @param keyOps
     * @param inputCallbackMap
     */
    private static void handleSelection(SelectionKey key, int keyOps, HashMap<SelectionKey, Runnable> inputCallbackMap,ExecutorService inputHandlPool) {
        // 重点 ,取消继续对keyOps的侦听
        key.interestOps(key.readyOps()& ~keyOps);
        Runnable runnable = null;
        try{
            runnable = inputCallbackMap.get(key);
        }catch (Exception e){
        }

        if(runnable!=null && !inputHandlPool.isShutdown()){
            // 异步调度
            inputHandlPool.execute(runnable);
        }
    }


    /**
     * 自定义的线程工厂，主要是修改线程的前缀
     */
    static class IoProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = prefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
