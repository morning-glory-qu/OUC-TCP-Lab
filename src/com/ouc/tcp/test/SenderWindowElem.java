package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;


enum SenderFlag {
    NOT_ACKED,
    ACKED
}

/**
 * TCP发送方窗口元素实现
 * 管理数据包状态和重传机制
 */
public class SenderWindowElem extends WindowElem {

    public SenderWindowElem() {
        super();
    }
    public SenderWindowElem(TCP_PACKET tcpPacket, int flag) {
        super();
        this.tcpPacket = tcpPacket;
        this.flag = flag;
    }

    /** 返回包是否被确认 */
    public boolean isAcked() {
        return this.flag == SenderFlag.ACKED.ordinal();
    }

    /** 处理包确认：更新状态并停止计时器 */
    public void ackPacket() {
        this.flag = SenderFlag.ACKED.ordinal();
    }
}