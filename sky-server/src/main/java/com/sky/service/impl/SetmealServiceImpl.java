package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //向套餐表中插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();
        //给菜品和套餐表中，把套餐的对应id赋值给菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.page(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 套餐批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                //如果等于1就说明套餐在起售中，不能被删除
                throw new DeletionNotAllowedException("套餐起售中，不能被删除");
            }
        }
        for (Long id : ids) {
            //删除和套餐相关的菜品id，在套餐和菜品关联表中。
            setmealDishMapper.delete(id);
            //删除套餐表中相关的数据
            setmealMapper.deleteById(id);
        }
    }

    /**
     * 套餐起售和停售
     * @param status
     */
    @Override
    public void setmealStatus(String status,String id) {
        setmealMapper.setmealStatus(status,id);
    }

    /**
     * 修改套餐
     * @param setmealVO
     */
    @Override
    public void updateSetmeal(SetmealVO setmealVO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealVO,setmeal);

        //修改套餐的数据，不包括包含的菜品list
        setmealMapper.updateSetmeal(setmeal);

        //修改套餐的菜品list
        //删除菜品list再添加新的
        setmealDishMapper.delete(setmeal.getId());
        List<SetmealDish> list = setmealVO.getSetmealDishes();
        if (list != null && list.size() > 0) {
            list.forEach(dish -> {
                dish.setSetmealId(setmealVO.getId());//把id赋给每个list
            });
        }
        setmealDishMapper.insertBatch(list);

    }

    /**
     * 根据id查询套餐内容
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        SetmealVO setmealVO = new SetmealVO();
        //根据id查询套餐内容，不包含套餐包括的菜品list
        Setmeal setmeal = new Setmeal();
        setmeal = setmealMapper.getById(id);

        //根据id查询菜品list
        List<SetmealDish> list = setmealMapper.getListById(id);

        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(list);

        return setmealVO;
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
