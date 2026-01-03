/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

public class TCP_Receiver extends TCP_Receiver_ADT {
    private TCP_PACKET ackPack;    //回复的ACK报文段
    private ReceiverSlidingWindow window = new ReceiverSlidingWindow(16);
    private UDT_Timer timer = new UDT_Timer(); // 添加定时器用于延迟ACK

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    public void rdt_recv(TCP_PACKET recvPack) {
        int recvSeq = recvPack.getTcpH().getTh_seq();
        int[] recvData = recvPack.getTcpS().getData();

        // 检查校验和
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {

            // 将数据包交给窗口缓冲区处理，并获取处理结果
            int bufferResult = window.bufferPacket(recvPack);
            System.out.println("bufferResult: " + bufferResult);

            // 延迟ACK策略：只在特定条件下发送ACK
            if (bufferResult == AckFlag.IS_BASE.ordinal()) {
                // 处理基础包：交付数据并设置延迟ACK
                TCP_PACKET packet = window.getPacketToDeliver();
                while (packet != null) {
                    dataQueue.add(packet.getTcpS().getData());
                    packet = window.getPacketToDeliver();
                }

                // 设置ACK号
                tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));

                // 延迟发送ACK（500ms），等待可能到达的更多数据包
                if (timer != null) {
                    timer.cancel();
                    timer = new UDT_Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            reply(ackPack);
                        }
                    }, 500); // 延迟500毫秒[1](@ref)
                }

            } else if (bufferResult == AckFlag.ORDERED.ordinal() ||
                    bufferResult == AckFlag.DUPLICATE.ordinal()) {
                // 对于有序包或重复包，不立即回复ACK，等待延迟ACK机制处理
                // 这是延迟ACK策略的关键区别：不立即回复每个包[7](@ref)
            }

        } else {
            // 校验和错误的包：直接丢弃，不回复ACK。
            System.out.println("Checksum failed. Packet dropped, no ACK sent.");
        }

        System.out.println();

        // 交付数据
        if (!dataQueue.isEmpty()) {
            deliver_data();
        }
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
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