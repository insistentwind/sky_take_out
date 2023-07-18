package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前加入购物车中的商品是否已经存在了
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果已经存在了，那么就只需要把数量加1
        if(list != null && list.size() > 0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);//update shoppingCart set number = #{number} where id = ?
            shoppingCartMapper.updateNumberByid(cart);
        }
        //否则需要插入一条新的数据
        else{
            //判断是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();

            if(dishId != null){
                //本次添加到购物车的是菜品
                Dish byId = dishMapper.getById(dishId);
                shoppingCart.setName(byId.getName());
                shoppingCart.setImage(byId.getImage());
                shoppingCart.setAmount(byId.getPrice());
            }
            else{
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal byId = setmealMapper.getById(setmealId);
                shoppingCart.setName(byId.getName());
                shoppingCart.setImage(byId.getImage());
                shoppingCart.setAmount(byId.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    /**
     * 查看购物袋
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前的用户id
        Long currentId = BaseContext.getCurrentId();
        ShoppingCart build = ShoppingCart.builder()
                .userId(currentId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(build);
        return list;
    }

    /**
     * 清空购物车信息
     */
    @Override
    public void deleteAll() {
        //获取当前的用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteAll(currentId);
    }

    /**
     * 清除购物车中单个商品
     * @param shoppingCartDTO
     */
    @Override
    public void deleteOne(ShoppingCartDTO shoppingCartDTO) {
            //说明是菜品
            //现在看数量问题
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

            Long currentId = BaseContext.getCurrentId();
            shoppingCart.setUserId(currentId);
            List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
            if(list.size() > 0 && list != null){
                //说明查到了
                ShoppingCart cart = list.get(0);
                if(cart.getNumber()> 1){
                    //此时数量大于1，只需要把菜品数量减一更新到数据库中就可以了
                    cart.setNumber(cart.getNumber() - 1);
                    shoppingCartMapper.updateNumberByid(cart);
                }
                else{
                    //否则，说明此时数量等于1,需要删除其数据库中信息
                    shoppingCartMapper.deleteOne(cart);
                }
            }
        }
    }
