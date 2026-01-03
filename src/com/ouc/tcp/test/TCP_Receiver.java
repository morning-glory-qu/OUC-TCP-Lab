/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TCP_Receiver extends TCP_Receiver_ADT {
    private TCP_PACKET ackPack;    //回复的ACK报文段
    private ReceiverSlidingWindow window = new ReceiverSlidingWindow(16);
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

            //  将数据包交给窗口缓冲区处理，并获取处理结果
            // 该方法会判断包是有序的、重复的、失序的，还是新的窗口基点
            int bufferResult = window.bufferPacket(recvPack);
            System.out.println("bufferResult: " + bufferResult);

            // 决定是否发送ACK。对于有序包、重复包或作为基点的包，都需要发送ACK。
            if (bufferResult == AckFlag.ORDERED.ordinal() ||
                    bufferResult == AckFlag.DUPLICATE.ordinal() ||
                    bufferResult == AckFlag.IS_BASE.ordinal()) {

                // ACK号设置为接收到的数据包的序号
                tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                reply(ackPack);
            }

            // 处理数据交付：如果新到的包正好是接收窗口的基点（IS_BASE），说明可能有连续的有序数据可以交付了
            if (bufferResult == AckFlag.IS_BASE.ordinal()) {
                TCP_PACKET packet = window.getPacketToDeliver();
                // 循环从窗口中提取所有已按序到达、可以交付给上层应用的数据包
                while (packet != null) {
                    dataQueue.add(packet.getTcpS().getData());
                    packet = window.getPacketToDeliver();
                }
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
		/*
		 	0.信道无差错
			1.只出错
			2.只丢包
			3.只延迟
			4.出错 / 丢包
			5.出错 / 延迟
			6.丢包 / 延迟
			7.出错 / 丢包 / 延迟
		 */
        tcpH.setTh_eflag((byte) 7);

        //发送数据报
        client.send(replyPack);
    }

}
