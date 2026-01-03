package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

/**
 * TCP 接收方窗口元素（适用于不带拥塞控制的TCP）
 * 用于缓存接收到的数据包，支持按序交付
 */

enum ReceiverFlag {
    WAIT, BUFFERED, DELIVERED
    // WAIT: 等待接收（初始状态）
    // BUFFERED: 已接收但未交付给应用层（缓存状态）
    // DELIVERED: 已按序交付给应用层（最终状态）
}

public class ReceiverWindowElem extends WindowElem {
    private long receiveTimestamp; // 数据包接收时间戳
    private boolean isInOrder;    // 是否按序到达

    public ReceiverWindowElem() {
        super();
        this.receiveTimestamp = System.currentTimeMillis();
        this.isInOrder = false;
    }

    public ReceiverWindowElem(TCP_PACKET packet, int flag) {
        super(packet, flag);
        this.receiveTimestamp = System.currentTimeMillis();
        this.isInOrder = false;
    }

    /** 检查数据包是否已缓存 */
    public boolean isBuffered() {
        return flag == ReceiverFlag.BUFFERED.ordinal();
    }

    /** 检查数据包是否已交付 */
    public boolean isDelivered() {
        return flag == ReceiverFlag.DELIVERED.ordinal();
    }

    /** 标记为已缓存 */
    public void bufferPacket() {
        this.flag = ReceiverFlag.BUFFERED.ordinal();
        this.receiveTimestamp = System.currentTimeMillis();
    }

    /** 标记为已交付 */
    public void deliverPacket() {
        this.flag = ReceiverFlag.DELIVERED.ordinal();
    }

    /** 设置按序到达状态 */
    public void setInOrder(boolean inOrder) {
        this.isInOrder = inOrder;
    }

    /** 检查是否按序到达 */
    public boolean isInOrder() {
        return this.isInOrder;
    }

    /** 获取接收时间戳 */
    public long getReceiveTimestamp() {
        return receiveTimestamp;
    }

    /** 计算数据包在接收窗口中的存活时间 */
    public long getBufferTime() {
        return System.currentTimeMillis() - receiveTimestamp;
    }
}