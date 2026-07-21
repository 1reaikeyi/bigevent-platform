package start.controller;

import common.ThreadLocalContext.ThreadLocalParam;
import common.result.Result;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.VoucherSeckillService;
import service.id.RedisID;

import java.util.concurrent.TimeUnit;

/**
 * 使用redisson分布式锁实现秒杀 - 同步版本
 * 支付成功逻辑已迁移至 VoucherOrderService.paySuccess()
 */
@RestController
@RequestMapping("/voucherSeckill")
public class VoucherSeckillController {
    @Autowired
    private VoucherSeckillService voucherSeckillService;
    @Autowired
    private VoucherOrderService voucherOrderService;
    @Autowired
    private RedisID redisID;
    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("/pay")
    public Result redisLock(@RequestBody VoucherOrder voucherOrder) {
        // 校验秒杀活动是否有效
        VoucherSeckill voucherSeckill = voucherSeckillService.voucherSeckillValid(voucherOrder.getVoucherId());
        if (voucherSeckill == null) {
            return Result.error("秒杀活动不存在或已结束");
        }

        // 获取当前用户ID
        Long userId = ThreadLocalParam.getUserId();

        // 使用 Redisson 分布式锁防止重复下单
        RLock redisLock = redissonClient.getLock("redisson:voucherSeckill:" + userId + ":" + voucherOrder.getVoucherId());
        boolean locked = false;
        try {
            // 尝试获取锁，等待10秒，持有10秒
            locked = redisLock.tryLock(10, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁失败");
        }

        if (!locked) {
            throw new RuntimeException("请勿重复下单");
        }

        try {
            // 设置订单基础信息
            voucherOrder.setId(redisID.createId("pay"));
            voucherOrder.setUserId(userId);
            voucherOrderService.paySuccess(voucherOrder);
            voucherOrder.setStatus(1L);

            return Result.success("paySuccess");
        } finally {
            // 确保锁被释放
            if (locked && redisLock.isHeldByCurrentThread()) {
                redisLock.unlock();
            }
        }
    }
}

