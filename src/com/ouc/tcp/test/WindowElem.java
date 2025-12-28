package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

public class WindowElem {
    protected  TCP_PACKET tcpPacket;
    // 通用状态标记，可通过位运算表示多种状态
    /*
    0: 初始的空闲状态
     */
    protected  int flag;

    public WindowElem() {
        tcpPacket = null;
        flag = 0;
    }
    public WindowElem(TCP_PACKET tcpPacket, int flag) {
        this.tcpPacket = tcpPacket;
        this.flag = flag;
    }

    public TCP_PACKET getTcpPacket() {
        return tcpPacket;
    }

    public void setElem(TCP_PACKET tcpPacket, int flag) {
        this.tcpPacket = tcpPacket;
        this.flag = flag;
    }

    public void resetElem(){
        tcpPacket = null;
        flag = 0;
    }

}
