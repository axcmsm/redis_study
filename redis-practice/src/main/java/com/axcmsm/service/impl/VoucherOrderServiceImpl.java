package com.axcmsm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.VoucherOrder;
import com.axcmsm.mapper.VoucherOrderMapper;
import com.axcmsm.service.IVoucherOrderService;
import org.springframework.stereotype.Service;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

}
