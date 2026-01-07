package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

public class SenderSlidingWindow {
    private final LinkedBlockingDeque<SenderWindowElem> window;
    private int cwnd = 1;
    private double dCwnd = 1.0;
    private int ssthresh = 16;
    private UDT_Timer timer;
    private final TCP_Sender sender;
    private final int delay = 3000;
    private final int period = 3000;
    private int lastAck = -1;
    private int lastAckCount = 0;


    public static class GBN_RetransTask extends TimerTask {
        private final SenderSlidingWindow window;

        public GBN_RetransTask(SenderSlidingWindow window) {
            this.window = window;
        }

        @Override
        public void run() {
            window.sendWindow();
        }
    }

    public SenderSlidingWindow(TCP_Sender sender) {
        this.sender = sender;
        this.window = new LinkedBlockingDeque<>();
        this.timer = new UDT_Timer();
    }

    // 拥塞后重传
//    private void sendWindow() {
//        ssthresh = Math.max(cwnd / 2, 2);
//        cwnd = 1;
//        // 重传窗口左沿的包
//        SenderWindowElem senderWindowElem = window.peekFirst(); // 取出窗口左沿的包
//        if (senderWindowElem != null) {
//            sender.udt_send(senderWindowElem.getTcpPacket());
//        }
//    }

    private void sendWindow() {
        for (SenderWindowElem elem : window) {
            if (!elem.isAcked()) {
                sender.udt_send(elem.getTcpPacket());
            }
        }
    }

    public void resetTimer() {
        try {
            timer.cancel();
            timer = new UDT_Timer();
            if (!window.isEmpty()) {
                timer.schedule(new GBN_RetransTask(this), delay, period);
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCwndFull() {
        return window.size() >= cwnd;
    }

    // 快重传时重发期待数据包
    private void resendPacket(int ack) {
        int expectedAck = ack + 100;
        for (SenderWindowElem elem : window) {
            if (elem.getTcpPacket().getTcpH().getTh_seq() == expectedAck) {
                sender.udt_send(elem.getTcpPacket());
                break;
            }
        }
    }

    public void pushPacket(TCP_PACKET packet) {
        // 如果窗口为空，代表第一个包
        if (window.isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), delay, period); // 设置一个重传任务
        }
        window.addLast(new SenderWindowElem(packet, SenderFlag.NOT_ACKED.ordinal()));
        sender.udt_send(packet);
    }

    // 收ACK
    public void ackPacket(int ack) {
        for (SenderWindowElem elem : window) {
            if (elem.getTcpPacket().getTcpH().getTh_seq() <= ack) { // 移除已经确认报文
                elem.ackPacket();
                window.remove(elem);
                if (cwnd < ssthresh) { // 慢开始
                    cwnd++;
                    dCwnd = cwnd;
                }
                // 拥塞避免
                if (cwnd >= ssthresh) {
                    dCwnd += (double) 1 / cwnd;
                    cwnd = (int) dCwnd;
                }

            } else {
                break;
            }
        }
        // 滑动窗口后重新对窗口左沿设置计时器
        resetTimer();

        // 收到重复ACK
        if (ack == lastAck) {
            lastAckCount++;
        } else { // 没收到重复ACK
            lastAck = ack;
            lastAckCount = 1;
        }
        // 快重传
        if (lastAckCount == 4) {
            ssthresh = cwnd / 2;
            cwnd = 1;
            dCwnd = cwnd;
            resendPacket(ack);
        }
    }
}