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
    private UDT_Timer timer;
    private TCP_Sender sender;
    private int delay;
    private int period;


    public SenderSlidingWindow(int windowSize, TCP_Sender sender, int delay, int period) {
        this.sender = sender;
        this.windowSize = windowSize;
        this.window = new SenderWindowElem[windowSize];
        for (int i = 0; i < windowSize; i++) {
            this.window[i] = new SenderWindowElem();
        }
        this.baseIndex = 0;
        this.nextToSendIndex = 0;
        this.rearIndex = 0;
        this.timer = new UDT_Timer();
        this.delay = delay;
        this.period = period;
    }

    public static class GBNRetransmitTask extends TimerTask {
        private TCP_Sender sender;
        private SenderSlidingWindow window;

        public GBNRetransmitTask(TCP_Sender sender, SenderSlidingWindow window) {
            this.sender = sender;
            this.window = window;
        }

        public void run() {
            // 超时时重传所有已发送但未确认的包
            window.sendWindow();
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

    private boolean atBase() {
        return nextToSendIndex == baseIndex;
    }

    public void pushPacket(TCP_PACKET packet) {
        if (isFull()) {
            throw new IllegalStateException("Cannot push packet. Sender window is full.");
        }
        int currentIndex = getIndex(rearIndex);
        window[currentIndex].setElem(packet, SenderFlag.NOT_ACKED.ordinal());
        rearIndex++;
    }

    public void sendWindow() {
        nextToSendIndex = baseIndex;
        while (nextToSendIndex < rearIndex) {
            sendPacket();
        }
    }

    // 重置定时器
    public void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new UDT_Timer();
        if (baseIndex < nextToSendIndex) { // 有未确认的包才启动定时器
            timer.schedule(new GBNRetransmitTask(sender, this), delay, period);
        }
    }

    public void sendPacket() {
        if (isEmpty() || isAllSent()) {
            return;
        }

        int currentIndex = getIndex(nextToSendIndex);
        TCP_PACKET packetToSend = window[currentIndex].getTcpPacket();

        // 如果是第一个未确认的包，启动定时器
        if (atBase()) {
            resetTimer();
        }

        nextToSendIndex++;
        sender.udt_send(packetToSend);
    }

    // 修改ACK处理逻辑：采用累积确认
    public void ackPacket(int ackSequence) {
        // GBN累积确认：确认n号包意味着n及之前的所有包都已正确接收
        for (int i = baseIndex; i != rearIndex; i++) {
            int currentIndex = getIndex(i);
            SenderWindowElem elem = window[currentIndex];
            TCP_PACKET packet = elem.getTcpPacket();

            // 累积确认：确认序号为ackSequence的包，意味着所有序号小于等于ackSequence的包都已正确接收
            if (packet.getTcpH().getTh_seq() <= ackSequence && !elem.isAcked()) {
                elem.ackPacket();
                elem.resetElem();
                baseIndex++;
                resetTimer();
            }
        }
    }
}