package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;


// TCP发送方的滑动窗口实现。

public class SenderSlidingWindow {
    private final int windowSize;
    private final SenderWindowElem[] window;
    private int baseIndex; // 指向窗口中 最早发送 的未确认数据包
    private int nextToSendIndex; // 指向下一个待发送的数据包
    private int rearIndex; // 窗口中最后一个数据包的位置

    public SenderSlidingWindow(int windowSize) {
        this.windowSize = windowSize;
        this.window = new SenderWindowElem[windowSize];
        // 初始化窗口中的每个元素
        for (int i = 0; i < windowSize; i++) {
            this.window[i] = new SenderWindowElem();
        }
        this.baseIndex = 0;
        this.nextToSendIndex = 0;
        this.rearIndex = 0;
    }

    // 根据序列号计算其在窗口数组中的实际索引（取模运算）。
    private int getIndex(int sequence) {
        return sequence % windowSize;
    }

    // 检查发送窗口是否已满（无法再添加新的数据包）。
    public boolean isFull() {
        return rearIndex - baseIndex == windowSize;
    }

    // 检查发送窗口是否为空（没有任何数据包，包括已发送和未发送）。
    public boolean isEmpty() {
        return baseIndex == rearIndex;
    }

    // 检查窗口内所有数据包是否都已发送（但未必已确认）。
    public boolean isAllSent() {
        return nextToSendIndex == rearIndex;
    }

    // 将一个新的数据包加入发送窗口（通常标记为未确认状态）。
    public void pushPacket(TCP_PACKET packet) {
        if (isFull()) {
            throw new IllegalStateException("Cannot push packet. Sender window is full.");
        }
        int currentIndex = getIndex(rearIndex);
        window[currentIndex].setElem(packet, SenderFlag.NOT_ACKED.ordinal());
        rearIndex++;
    }

     // 发送下一个待发送的数据包，并启动其重传定时器。
     // 如果窗口为空或所有包已发送，则方法直接返回。
    public void sendPacket(TCP_Sender sender, Client client, int delay, int period) {
        if (isEmpty() || isAllSent()) {
            return; // 无包可发
        }

        int currentIndex = getIndex(nextToSendIndex);
        TCP_PACKET packetToSend = window[currentIndex].getTcpPacket();

        // 创建并启动重传定时器
        UDT_RetransTask retransmitTask = new UDT_RetransTask(client, packetToSend);
        window[currentIndex].scheduleTask(retransmitTask, delay, period);
        nextToSendIndex++;
        sender.udt_send(packetToSend);
    }


     // 处理接收到的确认包（ACK）。
     //找到对应序列号且未确认的数据包，将其标记为已确认，并尝试向前滑动窗口。
    public void ackPacket(int ackSequence) {
        // 遍历当前窗口，寻找匹配的序列号
        for (int i = baseIndex; i != rearIndex; i++) {
            int currentIndex = getIndex(i);
            SenderWindowElem elem = window[currentIndex];
            TCP_PACKET packet = elem.getTcpPacket();

            // 找到匹配且未确认的包
            if (packet.getTcpH().getTh_seq() == ackSequence && !elem.isAcked()) {
                elem.ackPacket(); // 标记为已确认
                break;
            }
        }
        // 从base开始，连续确认的包都可以移出窗口
        while (baseIndex < rearIndex && window[getIndex(baseIndex)].isAcked()) {
            int currentIndex = getIndex(baseIndex);
            window[currentIndex].resetElem(); // 重置该窗口单元
            baseIndex++; // 窗口基索引前移
        }
    }
}