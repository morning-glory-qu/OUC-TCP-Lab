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
    int sequence = 1;//用于记录当前待接收的包序号，也就是期望接受的下一个数据字节的起始序号(TCP的seq只记录数据部分第一个字节在原始字节流的位置)

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        //检查校验码，生成ACK
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            //生成ACK报文段（设置确认号）
            tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            //回复ACK报文段
            reply(ackPack);

            //将接收到的正确有序的数据插入data队列，准备交付
            if (recvPack.getTcpH().getTh_seq()==sequence) { // 当前接收到的数据包 如果和期望序号匹配
                dataQueue.add(recvPack.getTcpS().getData()); // 把数据部分加入到数据队列
                sequence += recvPack.getTcpS().getData().length; // 更新 sequence = sequence + 当前数据包长度
            }
        } else { // 其它的丢掉
            System.out.println("Receive Computed: " + CheckSum.computeChkSum(recvPack));
            System.out.println("Received Packet" + recvPack.getTcpH().getTh_sum());
            System.out.println("Problem: Packet Number: " + recvPack.getTcpH().getTh_seq() + " + InnerSeq:  " + sequence);
            tcpH.setTh_ack(sequence);
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            //回复ACK报文段
            reply(ackPack);
        }

        System.out.println();

        //交付数据（每20组数据交付一次）
        if (dataQueue.size() == 20)
            deliver_data();
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(fw, true));

            //循环检查data队列中是否有新交付数据
            while(!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                //将数据写入文件
                for(int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();		//清空输出缓存
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
        tcpH.setTh_eflag((byte) 1);    //eFlag=0，信道无错误

        //发送数据报
        client.send(replyPack);
    }

}
