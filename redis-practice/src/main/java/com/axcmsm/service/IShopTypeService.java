package com.axcmsm.service;

import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.ShopType;


public interface IShopTypeService extends IService<ShopType> {

    Result queryList();
}
