package start.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.ThreadLocalContext.ThreadLocalParam;
import common.result.Result;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.VoucherSeckillService;
import service.id.RedisID;

import java.util.concurrent.TimeUnit;

/**
 * 使用redisson,RedissonClient
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
//    @Resource(name = "redissonClient")
    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("/pay")
    public Result payVoucher(@RequestBody VoucherOrder voucherOrder) {
        VoucherSeckill voucherSeckill = voucherSeckillService.voucherSeckillValid(voucherOrder.getVoucherId());
        if (voucherSeckill == null) {
            return Result.error("失败");
        }
        Long userId = ThreadLocalParam.getUserId();
        RLock redisLock = redissonClient.getLock("redisson:voucherSeckill:" + userId);
        boolean locked = false;
        try {
            // 尝试获取锁，等待10秒，持有10秒
            locked = redisLock.tryLock(10, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!locked) {
            throw new RuntimeException("不要重复");
        }
        try {
            voucherOrder.setId(redisID.createId("order"));
            voucherOrder.setUserId(userId);
            return ((VoucherSeckillController) AopContext.currentProxy()).paySuccess(voucherOrder);
        } finally {
            redisLock.unlock();
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public Result paySuccess(VoucherOrder voucherOrder) {
        // 一人一单检查
        Long count = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, voucherOrder.getUserId())
                .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId()));
        if (count > 0) {
            throw new RuntimeException("一人一单");
        }
        // 扣库存
        boolean success = voucherSeckillService.lambdaUpdate()
                .eq(VoucherSeckill::getVoucherId, voucherOrder.getVoucherId())
                .gt(VoucherSeckill::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (!success) {
            throw new RuntimeException("库存不够");
        }
        // 保存订单
        voucherOrderService.save(voucherOrder);
        return Result.success("paySuccess");
    }

}

