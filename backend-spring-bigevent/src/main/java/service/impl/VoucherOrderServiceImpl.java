package service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import model.entity.VoucherOrder;
import model.entity.VoucherSeckill;
import mapper.VoucherOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.VoucherOrderService;
import service.VoucherSeckillService;

/**
 * 优惠券订单服务实现类 - 实现优惠券订单相关业务逻辑
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Autowired
    private VoucherSeckillService voucherSeckillService;

    /**
     * 支付成功处理逻辑
     * 1. 校验一人一单（放在前面，避免重复下单扣减库存）
     * 2. 扣减库存
     * 3. 保存订单
     * 
     * 使用 @Transactional 确保数据库操作的原子性
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void paySuccess(VoucherOrder voucherOrder) {
        // 一人一单检查（优先检查，避免重复下单扣减库存）
        Long count = this.count(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, voucherOrder.getUserId())
                .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId()));
        if (count > 0) {
            throw new RuntimeException("一人一单");
        }

        // 扣库存（乐观锁方式）
        boolean success = voucherSeckillService.lambdaUpdate()
                .eq(VoucherSeckill::getVoucherId, voucherOrder.getVoucherId())
                .gt(VoucherSeckill::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (!success) {
            throw new RuntimeException("库存不够");
        }

        // 保存订单
        this.save(voucherOrder);
    }
}