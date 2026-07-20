package start.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import common.constant.JwtConstant;
import common.result.Result;
import common.ThreadLocalContext.ThreadLocalContextHolder;
import lombok.extern.slf4j.Slf4j;
import model.dto.VoucherDTO;
import model.entity.Voucher;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import org.springframework.aop.framework.AopContext;
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
import service.lock.RedisLock;
import service.id.RedisID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * 使用redisLock
 */
@RestController
@RequestMapping("/voucher")
@Slf4j
public class VoucherController {
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private VoucherSeckillService voucherSeckillService;
    @Autowired
    private VoucherOrderService voucherOrderService;
    @Autowired
    private RedisID redisID;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/create")
    public Result createVoucher(@RequestBody VoucherDTO voucherDTO) {
        Voucher voucher = BeanUtil.toBean(voucherDTO, Voucher.class);
        voucherService.save(voucher);
        List<VoucherSeckill> voucherSeckillList = voucherDTO.getVoucherSeckillList().stream()
                .map((VoucherSeckill voucherSeckill) -> {return VoucherSeckill.builder()
                        .stock(voucherSeckill.getStock())
                        .beginTime(voucherSeckill.getBeginTime())
                        .endTime(voucherSeckill.getEndTime())
                        .voucherId(voucherDTO.getId())
                        .build();})
                .toList();
        for (int i = 0; i < voucherSeckillList.size(); i++) {
            stringRedisTemplate.opsForValue().set("voucherSeckill:stock:"+voucher.getId(),
                    voucherSeckillList.get(i).getStock().toString());
        }
        voucherSeckillService.saveBatch(voucherSeckillList);
        return Result.success("createVoucher");
    }

    @PostMapping("/pay")
    public Result payVoucher(@RequestBody VoucherOrder voucherOrder) {
        VoucherSeckill voucherSeckill = voucherSeckillService.voucherSeckillValid(voucherOrder.getVoucherId());
        if (voucherSeckill == null) {
            return Result.error("失败");
        }
        Map<String, Object> claims = ThreadLocalContextHolder.get();
        Long userId = Long.parseLong(claims.get(JwtConstant.ID).toString());
        // 获取锁
        RedisLock redisLock = new RedisLock(stringRedisTemplate, "voucherSeckill:" + userId);
        boolean locked = redisLock.getLocked(10);
        if (!locked) {
            throw new RuntimeException("不要重复");
        }
        try {
            voucherOrder.setId(redisID.createId("order"));
            voucherOrder.setUserId(userId);
            return ((VoucherController) AopContext.currentProxy()).paySuccess(voucherOrder);
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
        return Result.success("payVoucherSuccess");
    }

}
