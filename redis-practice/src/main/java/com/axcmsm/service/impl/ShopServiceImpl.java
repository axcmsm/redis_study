package com.axcmsm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.Shop;
import com.axcmsm.mapper.ShopMapper;
import com.axcmsm.service.IShopService;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

}
