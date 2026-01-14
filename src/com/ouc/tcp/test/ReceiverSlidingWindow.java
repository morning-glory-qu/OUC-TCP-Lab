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
    ORDERED, DUPLICATE, UNORDERED, IS_BASE;
    // ORDERED: 接收到的包是按序的
    // DUPLICATE: 接收到的包是重复的
    // UNORDERED: 接收到的包提前送达，乱序
    // IS_BASE: 接收到的包是基序号的包，开始交付数据


    @Override
    public String toString() {
        return switch (this) {
            case ORDERED -> "ORDERED";
            case DUPLICATE -> "DUPLICATE";
            case UNORDERED -> "UNORDERED";
            case IS_BASE -> "IS_BASE";
        };
    }
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


    // 处理接收到的数据包，更新窗口状态
    //处理接收到的数据包，将其缓存在窗口的相应位置，并返回该包的状态，允许缓存乱序包


    //处理接收到的数据包，将其缓存在窗口的相应位置，并返回该包的状态，允许缓存乱序包
    public int bufferPacket(TCP_PACKET packet) {
        // 1. 计算当前收到的包的逻辑序列号
        int packetDataLength = packet.getTcpS().getData().length;
        int seq = (packet.getTcpH().getTh_seq() - 1) / packetDataLength;

        // 2. GBN 核心逻辑：只接受期望的那个序号 (baseSeq)
        if (seq == baseSeq) {
            // 是期望的包，存入（或直接交付上层），并准备接收下一个
            // 在 GBN 中，其实不需要 window 数组缓存乱序包，只需要存当前这一个
            window[getIndex(seq)].setElem(packet, ReceiverFlag.BUFFERED.ordinal());

            // 注意：GBN 的 baseSeq 增加通常在接收逻辑处理完后
            return AckFlag.IS_BASE.ordinal();
        }

        // 3. 如果收到的是已处理过的包 (seq < baseSeq)
        if (seq < baseSeq) {
            return AckFlag.DUPLICATE.ordinal();
        }

        // 4. 如果收到的是乱序的包 (seq > baseSeq)
        // GBN 直接丢弃乱序包，不进行缓存
        return AckFlag.UNORDERED.ordinal();
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


    //  获取当前的窗口基序号。
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
}