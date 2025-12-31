package com.ouc.tcp.test;

/**
 * TCP 接收方窗口元素
 * 用于缓存接收到的数据包（支持乱序）
 */

enum ReceiverFlag {
    NOT_RECEIVED,
    RECEIVED
}

public class ReceiverWindowElem extends WindowElem {

    public ReceiverWindowElem() {
        super();
        this.flag = ReceiverFlag.NOT_RECEIVED.ordinal();
    }

    /** 是否已经接收 */
    public boolean isReceived() {
        return this.flag == ReceiverFlag.RECEIVED.ordinal();
    }

    /** 接收并缓存数据包 */
    public void receivePacket() {
        this.flag = ReceiverFlag.RECEIVED.ordinal();
    }

    /** 重置窗口元素（窗口右移） */
    public void resetElem() {
        super.resetElem();
    }
}
