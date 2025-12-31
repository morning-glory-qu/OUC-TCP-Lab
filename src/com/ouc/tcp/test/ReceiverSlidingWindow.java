package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

/**
 * TCP 接收方滑动窗口
 * 核心功能：
 * 1. 接收并缓存数据包（允许乱序到达）
 * 2. 判断数据包状态（按序、重复、乱序、基序号包）
 * 3. 按序将数据包交付给上层应用
 */

enum AckFlag {
    ORDERED, DUPLICATE, UNORDERED, IS_BASE
    // ORDERED: 接收到的包是按序的
    // DUPLICATE: 接收到的包是重复的
    // UNORDERED: 接收到的包提前送达，乱序
    // IS_BASE: 接收到的包是基序号的包，开始交付数据
}

public class ReceiverSlidingWindow {
    private final int windowSize;
    private final ReceiverWindowElem[] window;
    private int baseSeq; // 期望收到的下一个数据包的 seq

    public ReceiverSlidingWindow(int windowSize) {
        this.windowSize = windowSize;
        this.window = new ReceiverWindowElem[windowSize];
        // 初始化窗口中的每个元素
        for (int i = 0; i < windowSize; i++) {
            this.window[i] = new ReceiverWindowElem();
        }
        this.baseSeq = 0;
    }


     // 根据序列号计算其在窗口数组中的实际索引（取模运算，实现环形缓冲区）。
    private int getIndex(int sequence) {
        // 使用取模运算实现环形缓冲区，确保索引在 [0, windowSize-1] 范围内
        return sequence % windowSize;
    }


     //处理接收到的数据包，将其缓存在窗口的相应位置，并返回该包的状态，允许缓存乱序包
    public int bufferPacket(TCP_PACKET packet) {
        // 计算数据包的逻辑序列号
        int packetDataLength = packet.getTcpS().getData().length;
        int seq = (packet.getTcpH().getTh_seq() - 1) / packetDataLength;

        // 检查序列号是否在当前接收窗口内 [baseSeq, baseSeq + windowSize - 1]
        if (seq >= baseSeq + windowSize) {
            // 序列号超出窗口右边界，是后面的包，当前无法处理，应返回乱序状态
            return AckFlag.UNORDERED.ordinal();
        }
        if (seq < baseSeq) {
            // 序列号小于基序号，是重复包或过时包
            return AckFlag.DUPLICATE.ordinal();
        }

        // 序列号在窗口内，缓存数据包
        int currentIndex = getIndex(seq);
        window[currentIndex].setElem(packet, ReceiverFlag.BUFFERED.ordinal());

        // 判断是否是期望的基序号包
        if (seq == baseSeq) {
            return AckFlag.IS_BASE.ordinal();
        }
        // 是窗口内但非基序号的包（乱序但有效，已缓存）
        return AckFlag.ORDERED.ordinal();
    }

    /**
     * 获取当前可以按序交付给上层应用的数据包。
     * 该方法会检查基序号位置的数据包是否已缓存，如果已缓存则取出并交付，同时窗口基序号前移。
     */
    public TCP_PACKET getPacketToDeliver() {
        int currentIndex = getIndex(baseSeq);
        ReceiverWindowElem currentElem = window[currentIndex];

        // 检查基序号位置的数据包是否已缓存
        if (!currentElem.isBuffered()) {
            return null; // 基序号包尚未到达，无法进行连续交付
        }

        // 获取数据包，重置窗口单元状态，并滑动窗口基序号
        TCP_PACKET packetToDeliver = currentElem.getTcpPacket();
        currentElem.resetElem();
        baseSeq++; // 窗口基序号向前滑动一位

        return packetToDeliver;
    }

    /**
     * 获取当前的窗口基序号。
     * 这个值通常用于在ACK报文中告知发送方接收方期望的下一个序列号。
     */
    public int getBaseSeq() {
        return baseSeq;
    }

     // 检查接收窗口是否已满（即所有位置都已缓存数据包）。
    public boolean isFull() {
        // 检查从baseSeq到baseSeq+windowSize-1是否都有数据
        for (int i = baseSeq; i < baseSeq + windowSize; i++) {
            if (!window[getIndex(i)].isBuffered()) {
                return false;
            }
        }
        return true;
    }


    // 获取当前接收窗口的可用空间
    public int getAvailableSpace() {
        int bufferedCount = 0;
        for (int i = 0; i < windowSize; i++) {
            if (window[i].isBuffered()) {
                bufferedCount++;
            }
        }
        return windowSize - bufferedCount;
    }
}