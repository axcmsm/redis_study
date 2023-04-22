package com.axcmsm.service.impl;

import com.axcmsm.dto.Result;
import com.axcmsm.entity.SeckillVoucher;
import com.axcmsm.service.ISeckillVoucherService;
import com.axcmsm.utils.RedisIdWorker;
import com.axcmsm.utils.SimpleRedisLock;
import com.axcmsm.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.VoucherOrder;
import com.axcmsm.mapper.VoucherOrderMapper;
import com.axcmsm.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.Executors.*;


@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {


    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    public static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        //类加载时，读取配置
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //堵塞队列
    private BlockingQueue<VoucherOrder> orderTask = new ArrayBlockingQueue<>(1024 * 1024);
    //线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = newSingleThreadExecutor();

    //执行任务
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTask.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }

        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            Long userId = voucherOrder.getUserId();
            //分布式锁:基于redisson实现
            RLock lock = redissonClient.getLock("lock:order:" + userId);


            //获取锁
            boolean isLock = lock.tryLock();
            //判断释放获取锁成功
            if (!isLock) {
                //理论上是不可能出现锁的问题的,因为上面已经校验过,可下单后才会将订单放入队列中
                log.error("不允许重复下单");
                return;
            }

            try {
                 proxy.createVoucher(voucherOrder);
            } finally {
                //释放锁
                lock.unlock();
            }
        }
    }

    //初始化之前调用
    @PostConstruct
    public void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long execute = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),//空集合
                voucherId.toString(),
                userId.toString()
        );
        //2. 判断是否为0 (不为0,没有下单资格)
        int r = execute.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //3. 为0,将下单信息保存到堵塞队列
        long orderKey = redisIdWorker.nextId("order");
        // TODO: 2023/4/22 异步下单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderKey);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //保存订单
        orderTask.add(voucherOrder);
        //获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();


        //4. 返回id
        return Result.ok(orderKey);
    }


/*
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 查询优惠卷
        SeckillVoucher byId = seckillVoucherService.getById(voucherId);

        //2. 判断是否可以开始
        if (byId.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (byId.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        //3。 判断库存是否充足
        if ((byId.getStock() < 1)) {
            return Result.fail("库存不足");
        }
        Long user_id = UserHolder.getUser().getId();


        //分布式锁，使用redis的setnx实现(自定义实现)
        //SimpleRedisLock lock = new SimpleRedisLock("order:" + user_id, stringRedisTemplate);

        //分布式锁:基于redisson实现
        RLock lock = redissonClient.getLock("lock:order:" + user_id);

        //获取锁
        boolean isLock = lock.tryLock();

        //判断释放获取锁成功
        if (!isLock) {
            //获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单");
        }

        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucher(voucherId, user_id);
        } finally {
            //释放锁
            lock.unlock();
        }
    }
*/

    @Override
    @Transactional
    public Result createVoucher(Long voucherId, Long user_id) {

        //TODO 改进优化，一人一单功能 (需要加悲观锁）
        int count = query().eq("user_id", user_id).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("该用户已经购买过一次了");
        }


        //4. TODO  扣减库存 (乐观锁,改进）
        boolean success = seckillVoucherService.update()
                // set stock=stock-1
                .setSql("stock = stock - 1 ")
                // where voucher_id=voucher_id and stock > 0
                .eq("voucher_id", voucherId).eq("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }


        //5.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long order_id = redisIdWorker.nextId("order");
        voucherOrder.setId(order_id);//全局唯一id
        voucherOrder.setUserId(user_id);//用户
        voucherOrder.setVoucherId(voucherId);//代金卷
        save(voucherOrder);

        return Result.ok(order_id);
    }

    @Transactional
    @Override
    public void createVoucher(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //TODO 改进优化，一人一单功能 (需要加悲观锁）
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("该用户已经购买过一次了");
            return;
        }


        //4. TODO  扣减库存 (乐观锁,改进）
        boolean success = seckillVoucherService.update()
                // set stock=stock-1
                .setSql("stock = stock - 1 ")
                // where voucher_id=voucher_id and stock > 0
                .eq("voucher_id", voucherOrder.getVoucherId()).eq("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足");
            return;
        }


        //5.创建订单
        save(voucherOrder);

    }
}
