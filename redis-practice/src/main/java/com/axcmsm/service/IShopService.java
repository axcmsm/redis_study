package com.axcmsm.service;

import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.Shop;


public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);
}
