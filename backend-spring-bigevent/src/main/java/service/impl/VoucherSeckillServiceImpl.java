package service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import model.entity.VoucherSeckill;
import mapper.VoucherSeckillMapper;
import service.VoucherSeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀优惠券服务实现类 - 实现秒杀优惠券相关业务逻辑
 */
@Service
@Slf4j
public class VoucherSeckillServiceImpl extends ServiceImpl<VoucherSeckillMapper, VoucherSeckill> implements VoucherSeckillService {
}