package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

enum SenderFlag {
    NOT_ACKED, ACKED
}

/**
 * 适用于不带拥塞控制的TCP发送方窗口元素
 * 修改要点：简化重传策略，移除精细的包级计时器管理
 */
public class SenderWindowElem extends WindowElem {
    private UDT_Timer timer; // 包重传计时器
    private long sendTimestamp; // 发送时间戳
    private int retransmitCount; // 重传次数统计

    public SenderWindowElem() {
        super();
        this.timer = null;
        this.retransmitCount = 0;
        this.sendTimestamp = System.currentTimeMillis();
    }

    /** 返回包是否被确认 */
    public boolean isAcked() {
        return this.flag == SenderFlag.ACKED.ordinal();
    }

    /** 设置重传任务 - 修改为简化版本 */
    public void scheduleTask(UDT_RetransTask task, int delay, int period) {
        this.timer = new UDT_Timer();
        // 固定延迟重传，不采用指数退避等拥塞控制策略
        this.timer.schedule(task, delay, period);
        this.retransmitCount++;
        this.sendTimestamp = System.currentTimeMillis();
    }

    /** 处理包确认：更新状态并停止计时器 */
    public void ackPacket() {
        this.flag = SenderFlag.ACKED.ordinal();
        if (this.timer != null) {
            this.timer.cancel();
        }
    }

    /** 获取重传次数 - 用于简单监控 */
    public int getRetransmitCount() {
        return retransmitCount;
    }

    /** 获取发送时间戳 - 用于RTT估算 */
    public long getSendTimestamp() {
        return sendTimestamp;
    }

    // 设置定时器
    public void setTimer(UDT_Timer timer) {
        this.timer = timer;
    }

    // 获取定时器
    public UDT_Timer getTimer() {
        return timer;
    }

    /** 强制取消计时器 - 用于连接终止或重置 */
    public void cancelTimer() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
    }
}