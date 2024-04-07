package com.circle.utils;

/**
 * @Classname ILock
 * @Description 锁接口
 * @Author israein
 * @Date 2023-05-15 15:57
 */
public interface ILock {
    /**
     * 尝试获取锁
     * @Author israein
     * @date 15:58 2023/5/15
     * @param timeoutSec 锁持有的超时时间, 过期后自动释放
     * @return boolean true: 成功 false: 失败
     **/
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     * @Author israein
     * @date 15:59 2023/5/15
     **/
    void unLock();
}
