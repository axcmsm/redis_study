package com.axcmsm.service;

import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.VoucherOrder;
import org.springframework.transaction.annotation.Transactional;


public interface IVoucherOrderService extends IService<VoucherOrder> {


    Result seckillVoucher(Long voucherId);

    Result createVoucher(Long voucherId, Long user_id);

    void createVoucher(VoucherOrder voucherOrder);
}
