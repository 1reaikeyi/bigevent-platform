package service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import model.entity.VoucherOrder;
import mapper.VoucherOrderMapper;
import service.VoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券订单服务实现类 - 实现优惠券订单相关业务逻辑
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {
}