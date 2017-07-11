package com.alibaba.middleware.topkn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker1会接收master提供的题目信息，并且得到计算结果后返回给master Created by wanshao on 2017/6/29.
 */
public class TopknWorker {

    private static final int masterPort = 5527;
    private static String masterHostAddress;
    private final String dataDirPath = "/Users/wanshao/work/final_data";
    private Logger logger = LoggerFactory.getLogger(TopknWorker.class);

    private long k;
    private int n;

    public static void main(String[] args) throws IOException {

        masterHostAddress = args[0];
        new TopknWorker().connect(masterHostAddress, masterPort);
    }

    public void connect(String host, int port) {
        logger.info("begin to connect " + host + ":" + port);
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress(masterHostAddress, masterPort));
            socketChannel.configureBlocking(true);
            logger.info("Connected to server: " + socketChannel);
            Thread readThread = new Thread(new WorkerReadThread(socketChannel));
            readThread.start();
            readThread.join();
            Thread writeThread = new Thread(new WorkerWriteThread(socketChannel, k, n));
            writeThread.start();
            writeThread.join();
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    class WorkerWriteThread implements Runnable {

        private static final int WRITE_BUFFER_SIZE = 1024;
        private SocketChannel socketChannel;
        private Logger logger = LoggerFactory.getLogger(WorkerReadThread.class);
        // 比赛输入
        private long k;
        private int n;

        public WorkerWriteThread(SocketChannel socketChannel, long k, int n){
            this.socketChannel = socketChannel;
            this.k = k;
            this.n = n;
        }

        @Override
        public void run() {
            processAndSendResult(k, n);
        }

        /**
         * 根据比赛输入来计算结果,并且发送结果给master
         *
         * @return
         */
        private void processAndSendResult(long k, int n) {

            logger.info("begin to process topKN problem");

            try {
                logger.info("Begin to send topkn result to master " + socketChannel.getRemoteAddress());
                ByteBuffer sendBuffer = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
                // process topKN problem
                String data = "I am worker, and I have received data from master: k is " + k + " and n is " + n;
                byte[] sendData = data.getBytes();
                sendBuffer.clear();
                sendBuffer.put(sendData);
                sendBuffer.flip();
                while (sendBuffer.hasRemaining()) {
                    socketChannel.write(sendBuffer);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * worker可以使用该线程发送处理后的结果给worker Created by wanshao on 2017/7/8 0008.
     */
    class WorkerReadThread implements Runnable {

        private static final int READ_BUFFER_SIZE = 1024 * 1024;
        private SocketChannel socketChannel;
        private Logger logger = LoggerFactory.getLogger(WorkerReadThread.class);

        public WorkerReadThread(SocketChannel socketChannel){
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {

            try {
                logger.info("Begin to read input data from master " + socketChannel.getRemoteAddress());
                ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
                readBuffer.clear();
                int readBytes = socketChannel.read(readBuffer);
                if (readBuffer.remaining() >= 0) {
                    // do something with the result
                    readBuffer.flip();
                    k = readBuffer.getLong();
                    n = readBuffer.getInt();
                    readBuffer.clear();
                    logger.info("Receive input data, k is " + k + " and n is " + n);
                }

                logger.info("Reading intput data from master is finished...");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}