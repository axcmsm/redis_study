package com.axcmsm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.dto.Result;
import com.axcmsm.entity.Voucher;


public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
