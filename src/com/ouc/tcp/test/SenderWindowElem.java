package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;


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

    /** 返回包是否被确认 */
    public boolean isAcked() {
        return this.flag == SenderFlag.ACKED.ordinal();
    }

    /** 处理包确认：更新状态并停止计时器 */
    public void ackPacket() {
        this.flag = SenderFlag.ACKED.ordinal();
    }
}