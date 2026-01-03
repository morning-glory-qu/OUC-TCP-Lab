package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import java.util.TimerTask;

public class SenderSlidingWindow {
    private final int windowSize;
    private final SenderWindowElem[] window;
    private int baseIndex; // 指向窗口中最早发送的未确认数据包
    private int nextToSendIndex; // 指向下一个待发送的数据包
    private int rearIndex; // 窗口中最后一个数据包的位置
    private TCP_Sender sender;

    // 移除全局定时器，改为每个包独立管理
    // private UDT_Timer timer;

    public SenderSlidingWindow(int windowSize, TCP_Sender sender) {
        this.sender = sender;
        this.windowSize = windowSize;
        this.window = new SenderWindowElem[windowSize];
        for (int i = 0; i < windowSize; i++) {
            this.window[i] = new SenderWindowElem();
        }
        this.baseIndex = 0;
        this.nextToSendIndex = 0;
        this.rearIndex = 0;
        // 移除全局定时器初始化
    }

    // 为每个包创建独立的重传任务
    public static class SelectiveRetransmitTask extends TimerTask {
        private TCP_Sender sender;
        private SenderSlidingWindow window;
        private int packetIndex; // 特定包在窗口中的索引

        public SelectiveRetransmitTask(TCP_Sender sender, SenderSlidingWindow window, int packetIndex) {
            this.sender = sender;
            this.window = window;
            this.packetIndex = packetIndex;
        }

        public void run() {
            // 选择性重传：只重传超时的特定包
            window.retransmitSinglePacket(packetIndex);
        }
    }

    private int getIndex(int sequence) {
        return sequence % windowSize;
    }

    public boolean isFull() {
        return rearIndex - baseIndex == windowSize;
    }

    private boolean isEmpty() {
        return baseIndex == rearIndex;
    }

    private boolean isAllSent() {
        return nextToSendIndex == rearIndex;
    }

    public void pushPacket(TCP_PACKET packet) {
        if (isFull()) {
            throw new IllegalStateException("Cannot push packet. Sender window is full.");
        }
        int currentIndex = getIndex(rearIndex);
        window[currentIndex].setElem(packet, SenderFlag.NOT_ACKED.ordinal());
        rearIndex++;
    }

    // 选择性重传：只重传单个超时包
    private void retransmitSinglePacket(int packetIndex) {
        int actualIndex = getIndex(packetIndex);
        SenderWindowElem elem = window[actualIndex];

        if (!elem.isAcked()) {
            TCP_PACKET packet = elem.getTcpPacket();
            System.out.println("Timeout! Retransmitting packet with seq: " +
                    packet.getTcpH().getTh_seq());

            sender.udt_send(packet);

            // 重启该包的定时器
            startPacketTimer(packetIndex);
        }
    }

    // 启动单个包的定时器
    private void startPacketTimer(int packetIndex) {
        int actualIndex = getIndex(packetIndex);
        SenderWindowElem elem = window[actualIndex];

        if (!elem.isAcked()) {
            // 使用包独立的定时器
            UDT_Timer packetTimer = new UDT_Timer();
            int delay = 1000; // 1秒超时
            int period = 1000;

            packetTimer.schedule(new SelectiveRetransmitTask(sender, this, packetIndex), delay, period);

            // 将定时器与包关联（需要在SenderWindowElem中添加timer字段）
            // elem.setTimer(packetTimer);
        }
    }

    public void sendPacket() {
        if (isEmpty() || isAllSent()) {
            return;
        }

        int currentIndex = getIndex(nextToSendIndex);
        TCP_PACKET packetToSend = window[currentIndex].getTcpPacket();

        // 为每个新发送的包启动独立定时器
        startPacketTimer(nextToSendIndex);

        nextToSendIndex++;
        sender.udt_send(packetToSend);

        System.out.println("Sent packet with seq: " + packetToSend.getTcpH().getTh_seq());
    }

    // 修改ACK处理：选择性确认
    public void ackPacket(int ackSequence) {
        boolean windowMoved = false;

        // 选择性确认：只确认特定序列号的包
        for (int i = baseIndex; i < rearIndex; i++) {
            int currentIndex = getIndex(i);
            SenderWindowElem elem = window[currentIndex];
            TCP_PACKET packet = elem.getTcpPacket();

            // 精确匹配确认序列号
            if (packet.getTcpH().getTh_seq() == ackSequence && !elem.isAcked()) {
                elem.ackPacket();
                System.out.println("ACK received for seq: " + ackSequence);

                // 取消该包的定时器
                // if (elem.getTimer() != null) {
                //     elem.getTimer().cancel();
                // }

                windowMoved = true;
                break; // 选择性确认只处理一个包
            }
        }

        // 滑动窗口：移动baseIndex到第一个未确认的包
        while (baseIndex < rearIndex && window[getIndex(baseIndex)].isAcked()) {
            int currentIndex = getIndex(baseIndex);
            window[currentIndex].resetElem();
            baseIndex++;
            windowMoved = true;
        }

        if (windowMoved) {
            System.out.println("Window moved. Base index: " + baseIndex +
                    ", Next to send: " + nextToSendIndex + ", Rear index: " + rearIndex);
        }
    }

    // 添加序列号比较方法，处理序列号回绕
    private boolean seqLessThanOrEqual(int seq1, int seq2) {
        // 处理32位序列号回绕
        return (seq1 <= seq2 && seq2 - seq1 < 0x7FFFFFFF) ||
                (seq1 > seq2 && seq1 - seq2 > 0x7FFFFFFF);
    }

    // 添加窗口状态查询方法
    public int getWindowUsed() {
        return rearIndex - baseIndex;
    }

    public int getWindowAvailable() {
        return windowSize - getWindowUsed();
    }
}