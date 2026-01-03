package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

/**
 * TCP 接收方滑动窗口（适用于不带拥塞控制的TCP）
 * 核心功能：
 * 1. 接收并缓存数据包（支持乱序到达）
 * 2. 选择性确认机制
 * 3. 按序将数据包交付给上层应用
 */

enum AckFlag {
    ORDERED, DUPLICATE, UNORDERED, IS_BASE, NEW_BUFFERED
    // ORDERED: 接收到的包是按序的
    // DUPLICATE: 接收到的包是重复的
    // UNORDERED: 接收到的包提前送达，乱序但已缓存
    // IS_BASE: 接收到的包是基序号的包，开始交付数据
    // NEW_BUFFERED: 新包被成功缓存（选择性确认用）
}

public class ReceiverSlidingWindow {
    private final int windowSize;
    private final ReceiverWindowElem[] window;
    private int baseSeq; // 期望收到的下一个数据包的seq
    private int lastContinuousSeq; // 最后一个连续的数据包序号

    public ReceiverSlidingWindow(int windowSize) {
        this.windowSize = windowSize;
        this.window = new ReceiverWindowElem[windowSize];
        // 初始化窗口中的每个元素
        for (int i = 0; i < windowSize; i++) {
            this.window[i] = new ReceiverWindowElem();
        }
        this.baseSeq = 0;
        this.lastContinuousSeq = -1; // 初始为-1，表示没有连续序列
    }

    // 根据序列号计算其在窗口数组中的实际索引
    private int getIndex(int sequence) {
        return sequence % windowSize;
    }

    // 处理接收到的数据包，支持选择性确认和乱序缓存
    public int bufferPacket(TCP_PACKET packet) {
        int packetDataLength = packet.getTcpS().getData().length;
        int seq = (packet.getTcpH().getTh_seq() - 1) / packetDataLength;

        // 检查序列号是否在当前接收窗口内 [baseSeq, baseSeq + windowSize - 1]
        if (seq >= baseSeq + windowSize) {
            return AckFlag.UNORDERED.ordinal();
        }
        if (seq < baseSeq) {
            return AckFlag.DUPLICATE.ordinal();
        }

        // 检查是否已经缓存过该包（避免重复处理）
        int currentIndex = getIndex(seq);
        if (window[currentIndex].isBuffered() || window[currentIndex].isDelivered()) {
            return AckFlag.DUPLICATE.ordinal();
        }

        // 缓存数据包
        window[currentIndex].setElem(packet, ReceiverFlag.BUFFERED.ordinal());
        window[currentIndex].bufferPacket(); // 标记为已缓存
        window[currentIndex].setReceiveTimestamp(System.currentTimeMillis());

        // 判断包是否按序到达
        if (seq == baseSeq) {
            // 更新连续序列号
            updateLastContinuousSeq();
            return AckFlag.IS_BASE.ordinal();
        } else {
            // 乱序包，但已成功缓存
            window[currentIndex].setInOrder(false);
            return AckFlag.NEW_BUFFERED.ordinal();
        }
    }

    // 更新最后一个连续的数据包序号
    private void updateLastContinuousSeq() {
        int currentSeq = baseSeq;
        while (currentSeq < baseSeq + windowSize) {
            int index = getIndex(currentSeq);
            if (window[index].isBuffered() || window[index].isDelivered()) {
                lastContinuousSeq = currentSeq;
                currentSeq++;
            } else {
                break;
            }
        }
    }

    // 获取当前可以按序交付的数据包（支持连续交付）
    public TCP_PACKET getPacketToDeliver() {
        int currentIndex = getIndex(baseSeq);
        ReceiverWindowElem currentElem = window[currentIndex];

        if (!currentElem.isBuffered()) {
            return null;
        }

        // 获取数据包并标记为已交付
        TCP_PACKET packetToDeliver = currentElem.getTcpPacket();
        currentElem.deliverPacket(); // 标记为已交付
        currentElem.resetElem();

        baseSeq++; // 滑动窗口基序号

        // 检查是否有更多连续包可以交付
        return packetToDeliver;
    }

    // 获取需要选择性确认的包序列号（用于SACK机制）
    public int[] getSelectiveAckSequences() {
        int[] ackSequences = new int[windowSize];
        int count = 0;

        for (int i = baseSeq; i < baseSeq + windowSize; i++) {
            int index = getIndex(i);
            if (window[index].isBuffered() && !window[index].isDelivered()) {
                ackSequences[count++] = i;
            }
        }

        // 返回实际长度的数组
        int[] result = new int[count];
        System.arraycopy(ackSequences, 0, result, 0, count);
        return result;
    }

    // 获取接收窗口状态信息（用于调试和监控）
    public String getWindowStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Window BaseSeq: ").append(baseSeq)
                .append(", LastContinuousSeq: ").append(lastContinuousSeq)
                .append(", AvailableSpace: ").append(getAvailableSpace());
        return status.toString();
    }

    // 获取当前接收窗口的可用空间
    public int getAvailableSpace() {
        int bufferedCount = 0;
        for (int i = 0; i < windowSize; i++) {
            if (window[i].isBuffered() || window[i].isDelivered()) {
                bufferedCount++;
            }
        }
        return windowSize - bufferedCount;
    }

    // 获取基序号（期望的下一个序列号）
    public int getBaseSeq() {
        return baseSeq;
    }

    // 获取最后一个连续序列号（用于选择性确认）
    public int getLastContinuousSeq() {
        return lastContinuousSeq;
    }

    // 检查特定序列号是否已缓存
    public boolean isPacketBuffered(int seq) {
        if (seq < baseSeq || seq >= baseSeq + windowSize) {
            return false;
        }
        int index = getIndex(seq);
        return window[index].isBuffered();
    }

    // 获取窗口中使用率（用于性能监控）
    public double getWindowUsage() {
        int used = 0;
        for (int i = 0; i < windowSize; i++) {
            if (window[i].isBuffered() || window[i].isDelivered()) {
                used++;
            }
        }
        return (double) used / windowSize;
    }
}