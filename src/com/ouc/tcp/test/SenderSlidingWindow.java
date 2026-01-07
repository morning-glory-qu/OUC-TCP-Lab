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
        public GBN_RetransTask(SenderSlidingWindow window) { this.window = window; }
        @Override
        public void run() {
            window.sendWindow();
        }
    }

    public SenderSlidingWindow(UDT_Timer timer, TCP_Sender sender) {

    }

    private void sendWindow() {
    }

    public void resetTimer(){

    }

    public boolean isCwndFull() {

    }

    public boolean isEmpty() {
        return window.isEmpty();
    }


    public void sendPacket(){

    }

    public void resendPacket(){

    }
    public void pushPacket(TCP_PACKET packet) {

    }

    public void ackPacket(TCP_PACKET packet) {

    }
}