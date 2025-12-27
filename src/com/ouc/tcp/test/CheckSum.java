package com.ouc.tcp.test;

import java.util.zip.CRC32;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

@SuppressWarnings("ALL")
public class CheckSum {

	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
    CRC32 crc32 = new CRC32();
    // 获取TCP首部信息
    TCP_HEADER header = tcpPack.getTcpH();
    // 将首部中序列号和确认号添加到CRC32计算中
    crc32.update(header.getTh_seq());
    crc32.update(header.getTh_ack());
    // 数据部分
    int[] data = tcpPack.getTcpS().getData();
    for (int value : data) {
        crc32.update(value);
    }
    // 计算结果并返回
    return (short) crc32.getValue();
    }

}