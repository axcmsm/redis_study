package com.axcmsm.utils;

/**
 * ClassName: com.axcmsm.utils.ILock
 * 微信公众号：代码飞快
 * Description:
 * 分布式锁设计
 * @author 须贺
 * @version 2023/4/22
 */
public interface ILock {
    /**
     * 尝试去获取锁
     * @param timeoutSec
     * @return
     */
    boolean tryLock(long timeoutSec);
    /**
     * 释放锁
     */
    void unlock();
}
