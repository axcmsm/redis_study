-- 优惠卷id
local voucherId=ARGV[1]
-- 用户id
local userId=ARGV[2]
-- 数据key
local stockKey='seckill:stock:' .. voucherId
local orderKey='seckill:order:' .. voucherId

-- 1. 判断库存是否充足 get stockKey
if(tonumber(redis.call('get',stockKey)) <= 0) then
    return 1
end

-- 2. 判断用户是否下单 sismember orderKey userId
if(tonumber(redis.call('sismember',orderKey,userId)) == 1) then
    -- 在set中存在,说明:重复下单
    return 2
end

-- 3. 扣库存 incrby stockKey -1
redis.call('incrby',stockKey,-1))
-- 4. 下单 sadd orderKey userId
redis.call('sadd',orderKey,userId))
-- 成功返回0
return 0


