package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

/*
    滑动窗口元素类：用于管理TCP报文包和其状态标志
    该类封装了TCP报文包和对应的状态标志，用于实现滑动窗口协议中的数据包跟踪管理。通过标志位的位运算可以表示多种状态（如已发送、已确认、超时重传等）。
 */
public class WindowElem {
    protected TCP_PACKET tcpPacket;
    // 通用状态标记，可通过位运算表示多种状态
    /*
    0: 初始的空闲状态
     */
    protected int flag; // flag

    // 无参构造：创建空窗口元素
    public WindowElem() {
        tcpPacket = null;
        flag = 0; // 窗口元素状态
    }

    public WindowElem(TCP_PACKET tcpPacket, int flag) {
        this.tcpPacket = tcpPacket;
        this.flag = flag; // 窗口元素状态
    }

    // 获取TCP报文包
    public TCP_PACKET getTcpPacket() {
        return tcpPacket;
    }

    // 设置窗口元素内容
    public void setElem(TCP_PACKET tcpPacket, int flag) {
        this.tcpPacket = tcpPacket;
        this.flag = flag;
    }

    // 重置窗口元素回到初始状态。
    public void resetElem() {
        tcpPacket = null;
        flag = 0;
    }

}