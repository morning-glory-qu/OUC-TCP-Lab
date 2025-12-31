package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;


enum SenderFlag {
    NOT_ACKED,
    ACKED
}

/**
 * TCP发送方窗口元素实现
 * 管理数据包状态和重传机制
 */
public class SenderWindowElem extends WindowElem {
    private UDT_Timer timer; // 包重传计时器

    public SenderWindowElem() {
        super();
        this.timer = null;
    }

    /** 返回包是否被确认 */
    public boolean isAcked() {
        return this.flag == SenderFlag.ACKED.ordinal();
    }

    /** 设置重传任务 */
    public void scheduleTask(UDT_RetransTask task, int delay, int period) {
        this.timer = new UDT_Timer();
        this.timer.schedule(task, delay, period);
    }

    /** 处理包确认：更新状态并停止计时器 */
    public void ackPacket() {
        this.flag = SenderFlag.ACKED.ordinal();
        this.timer.cancel();
    }
}