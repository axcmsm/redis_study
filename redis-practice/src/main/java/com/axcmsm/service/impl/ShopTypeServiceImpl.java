package com.axcmsm.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.axcmsm.dto.Result;
import com.axcmsm.utils.RedisConstants;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.ShopType;
import com.axcmsm.mapper.ShopTypeMapper;
import com.axcmsm.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        //查询缓存
        String key = "cache:shop:list";
        String str = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(str)){
            List<ShopType> typeList = JSONUtil.toList(str, ShopType.class);
            return Result.ok(typeList);
        }
        //查询数据库,写入缓存
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList.isEmpty()){
            return Result.fail("商品种类列表为空");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(typeList),1, TimeUnit.DAYS);
        //返回
        return Result.ok(typeList);
    }
}
