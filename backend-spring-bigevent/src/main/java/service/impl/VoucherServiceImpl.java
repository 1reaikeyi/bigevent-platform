package service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import model.entity.Voucher;
import mapper.VoucherMapper;
import service.VoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券服务实现类 - 实现优惠券相关业务逻辑
 */
@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {
}