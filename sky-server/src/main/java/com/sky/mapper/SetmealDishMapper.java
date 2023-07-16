package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    //@Select("select setmeal_id from setmeal_dish where")
    List<Long> getSetmealIdsBydIds(List<Long> dishIds);

    /**
     * 把与插入的套餐有关的菜品关联在数据库表中(批量保存)
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 删除套餐
     * @param id
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void delete(Long id);
}
