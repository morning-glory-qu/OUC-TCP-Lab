package com.ouc.tcp.test;

/**
 * TCP 接收方窗口元素
 * 用于缓存接收到的数据包（支持乱序）
 */

enum ReceiverFlag {
    WAIT, BUFFERED
    // WAIT: 等待接收或已经确认(最初状态或者最终状态)
    // BUFFERED: 已经接收但还未确认(中间状态)
}

public class ReceiverWindowElem extends WindowElem {

    public ReceiverWindowElem() {
        super();
    }

    public boolean isBuffered() {
        return flag == ReceiverFlag.BUFFERED.ordinal();
    }
}