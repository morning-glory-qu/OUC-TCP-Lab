/***************************2.1: ACK/NACK*****************
 ***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;    // 回复的ACK报文段
    // 修改为 ReceiverWindow，对应 TCP/SR 阶段的接收窗口
    private final ReceiverSlidingWindow window = new ReceiverSlidingWindow(8);
    private UDT_Timer timer = new UDT_Timer();

    /* 构造函数 */
    public TCP_Receiver() {
        super();    // 调用超类构造函数
        super.initTCP_Receiver(this);    // 初始化TCP接收端
    }

    @Override
    // 接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        // 1. 检查校验码
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            int bufferResult;
            try {
                // 将收到的包放入窗口缓存（注意使用 clone，防止引用冲突）
                bufferResult = window.bufferPacket(recvPack.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }

            // 2. 如果收到的是 Base（期望的有序包）
            if (bufferResult == AckFlag.IS_BASE.ordinal()) {
                TCP_PACKET packet = window.getPacketToDeliver();
                // 循环取出窗口中连续的所有包
                while (packet != null) {
                    dataQueue.add(packet.getTcpS().getData()); // 交付数据

                    // 更新 ACK 号为当前已交付的最后一个包的 Seq
                    tcpH.setTh_ack(packet.getTcpH().getTh_seq());
                    // 构造 ACK 报文
                    ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                    tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

                    packet = window.getPacketToDeliver();
                }

                // 3. 延迟确认机制：重置定时器
                if (timer != null) {
                    timer.cancel();
                    timer = new UDT_Timer();
                    timer.schedule(
                            new TimerTask() {
                                public void run() {
                                    reply(ackPack);    // 500ms 后回复最新的累积 ACK
                                }
                            }, 500
                    );
                }
            }
            // 4. 如果收到的是重复包或乱序包（且不是刚才处理掉的有序包）
            // 注意：如果 bufferResult 是 ORDERED，说明是乱序存入，不立即回ACK，等待Base补齐
            else if (bufferResult != AckFlag.ORDERED.ordinal()) {
                // 如果是重复包（DUPLICATE）等情况，立即重发当前的 ackPack
                reply(ackPack);
            }
        } else {
            // 校验和错误，直接丢弃
            System.out.println("Checksum Error, Packet Dropped.");
        }

        // 交付数据
        if (!dataQueue.isEmpty()) {
            deliver_data();
        }
    }

    @Override
    // 交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fw, true))) {
            //循环检查data队列中是否有新交付数据
            while (!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();
                //将数据写入文件
                for (int datum : data) {
                    writer.write(datum + "\n");
                }
                writer.flush();        // 清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);

        //发送数据报
        client.send(replyPack);
    }
}