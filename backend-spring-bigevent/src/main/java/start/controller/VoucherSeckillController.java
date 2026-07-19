package start.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.ThreadLocalContext.ThreadLocalContextHolder;
import common.ThreadLocalContext.ThreadLocalParam;
import common.constant.JwtConstant;
import common.result.Result;
import jakarta.annotation.Resource;
import model.dto.VoucherDTO;
import model.entity.Voucher;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.VoucherOrderService;
import service.VoucherSeckillService;
import service.VoucherService;
import service.id.RedisID;
import service.lock.RedisLock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    public synchronized Result payVoucher(@RequestBody VoucherOrder voucherOrder) {
        VoucherSeckill voucherSeckill = voucherSeckillService.getById(voucherOrder.getVoucherId());
        if(voucherSeckill == null){
            return Result.error("不存在");
        }
        if (LocalDateTime.now().isBefore(voucherSeckill.getBeginTime()) || LocalDateTime.now().isAfter(voucherSeckill.getEndTime())){
            return Result.error("不在规定时间段");
        }
        if(voucherSeckill.getStock() <= 0){
            return Result.error("结束了");
        }
        //并发
        voucherOrder.setId(redisID.createId("redisson:order"));
        Long userId = ThreadLocalParam.getUserId();
        voucherOrder.setUserId(userId);
        RLock redisLock = redissonClient.getLock("redisson:voucherSeckill:"+userId);
        boolean start = false;
        try {
            start = redisLock.tryLock(10,10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!start){
            throw new RuntimeException("不要重复");
        }
        try {
            Result result = payVoucherSuccess(voucherOrder.getVoucherId());
            if (result.getCode() != 200){
                return Result.error("error");
            }
            voucherOrderService.save(voucherOrder);
        }finally {
            redisLock.unlock();
        }
        return Result.success("payVoucherSuccess");
    }
    @Transactional(rollbackFor = Exception.class)
    public Result payVoucherSuccess(Long voucherId) {
        Map<String,Object> claims = ThreadLocalContextHolder.get();
        String currentId = claims.get(JwtConstant.ID).toString();
        Long userId = Long.parseLong(currentId);
        //Atomicity
        boolean success = voucherSeckillService.lambdaUpdate()
                .eq(VoucherSeckill::getVoucherId, voucherId)
                .gt(VoucherSeckill::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        // 必须检查库存扣减是否成功，失败则返回错误，不创建订单
        if (!success) {
            throw new RuntimeException("库存不够");
        }
        Long count = voucherOrderService.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId,userId)
                .eq(VoucherOrder::getVoucherId,voucherId));
        if (count > 0){
            throw new RuntimeException("一人一单");
        }
        return Result.success(voucherId);
    }

}

