package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

public class SenderSlidingWindow {
    private LinkedBlockingDeque<SenderWindowElem> window;
    private int cwnd = 1;
    private double dCwnd = 1.0;
    private int ssthresh = 16;
    private UDT_Timer timer;
    private TCP_Sender sender;
    private final int delay = 1000;
    private final int period = 1000;
    private int lastAck = -1;
    private int lastAckCount = 0;
    private final int lastAckCountLimit = 3;


    public class GBN_RetransTask extends TimerTask {
        private final SenderSlidingWindow window;

        public GBN_RetransTask(SenderSlidingWindow window) {
            this.window = window;
        }

        @Override
        public void run() {
            window.sendWindow();
        }
    }

    public SenderSlidingWindow(UDT_Timer timer, TCP_Sender sender) {
        this.sender = sender;
        this.window = new LinkedBlockingDeque<>();
        this.timer = new UDT_Timer();
    }

    private void sendWindow() {

    }

    public void resetTimer() {
        timer.cancel();
        timer = new UDT_Timer();
        if (!isEmpty()) {
            timer.schedule(new GBN_RetransTask(this), delay, period);
        }
    }

    public boolean isCwndFull() {
        return window.size() >= cwnd;
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }


    public void sendPacket() {
        SenderWindowElem elem = window.poll();
        if (elem == null) {
            return;
        }
        if (!elem.isAcked()) {
            sender.udt_send(elem.getTcpPacket());
        }
        window.push(elem);
    }

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
        // 如果窗口为空，启动定时器
        if (isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), delay, period);
        }
        window.push(new SenderWindowElem(packet, SenderFlag.NOT_ACKED.ordinal()));
    }


    public void ackPacket(int ack) {
        for (SenderWindowElem elem : window) {
            if (elem.getTcpPacket().getTcpH().getTh_seq() <= ack) {
                elem.ackPacket();
                window.remove(elem);
                if (cwnd < ssthresh) {
                    cwnd++;
                    dCwnd = cwnd;
                }

            }
        }
        resetTimer();

        // 更新拥塞窗口
        if (cwnd >= ssthresh) {
            dCwnd += (double) 1 / cwnd;
            cwnd = (int) dCwnd;
        }

        if (ack == lastAck) {
            lastAckCount++;
        } else {
            lastAck = ack;
            lastAckCount = 1;
        }

        if (lastAckCount >= lastAckCountLimit) {
            ssthresh = cwnd / 2;
            cwnd = 1;
            dCwnd = (double) cwnd;
            resendPacket(ack);
        }

    }
}