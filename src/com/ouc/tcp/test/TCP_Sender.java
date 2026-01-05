/***************************2.1: ACK/NACK
 **************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.TCP_PACKET;

enum WindowFlag {
    NOT_FULL, FULL
}

public class TCP_Sender extends TCP_Sender_ADT {

    private TCP_PACKET tcpPack;    // 待发送的TCP数据报
    private volatile int flag = WindowFlag.NOT_FULL.ordinal(); // 使用volatile保证可见性
    // 修正构造函数参数，与SenderSlidingWindow定义保持一致
    private final SenderSlidingWindow window = new SenderSlidingWindow(8, this);

    /*构造函数*/
    public TCP_Sender() {
        super();    // 调用超类构造函数
        super.initTCP_Sender(this);        // 初始化TCP发送端
    }

    @Override
    // 可靠发送（应用层调用）：封装应用层数据，产生TCP数据报
    public void rdt_send(int dataIndex, int[] appData) {

        // 1. 检查窗口是否已满
        if (window.isFull()) {
            flag = WindowFlag.FULL.ordinal();
        }

        // 2. 等待窗口滑动 (忙等待)
        // 注意：在实际运行中，如果是单线程模拟，忙等待可能会导致死锁，
        // 这里假设recv在另一个线程运行。
        while (flag == WindowFlag.FULL.ordinal()) {
            Thread.onSpinWait();
        }

        // 3. 生成TCP数据报
        tcpH.setTh_seq(dataIndex * appData.length + 1); // 包序号设置为字节流号
        tcpS.setData(appData);
        tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);

        // 更新校验和
        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
        tcpPack.setTcpH(tcpH);

        // 4. 将包放入窗口并发送
        try {
            window.pushPacket(tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        window.sendPacket();

        // 5. 发送后立即检查窗口是否已满，为下一次rdt_send调用做准备
        if (window.isFull()) {
            flag = WindowFlag.FULL.ordinal();
        }
    }

    @Override
    // 不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
    public void udt_send(TCP_PACKET stcpPack) {
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
        //System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
        //发送数据报
        client.send(stcpPack);
    }

    @Override
    // 处理确认逻辑
    public void waitACK() {
        // 循环检查确认号队列中是否有新收到的ACK
        if (!ackQueue.isEmpty()) {
            // 从ACK队列取出ACK
            int currentAck = ackQueue.poll();

            // 处理接收到的ACK，这可能会触发窗口滑动
            window.ackPacket(currentAck);

            // 修正：窗口滑动后，如果不再是满状态，更新flag以通知rdt_send继续发送
            if (!window.isFull()) {
                flag = WindowFlag.NOT_FULL.ordinal();
            }
        }
    }

    @Override
    // 接收到ACK报文：检查校验和，将确认号插入ack队列；NACK的确认号为－1；不需要修改
    public void recv(TCP_PACKET recvPack) {
        System.out.println("Receive ACK Number： " + recvPack.getTcpH().getTh_ack());
        ackQueue.add(recvPack.getTcpH().getTh_ack());
        System.out.println();

        //处理ACK报文
        waitACK();
    }
}