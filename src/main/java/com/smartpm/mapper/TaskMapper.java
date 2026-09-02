package com.smartpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpm.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
