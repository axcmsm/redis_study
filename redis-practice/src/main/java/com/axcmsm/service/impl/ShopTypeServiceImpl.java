package com.axcmsm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.ShopType;
import com.axcmsm.mapper.ShopTypeMapper;
import com.axcmsm.service.IShopTypeService;
import org.springframework.stereotype.Service;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

}
