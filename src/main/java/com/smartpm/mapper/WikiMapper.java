package com.smartpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpm.entity.Wiki;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WikiMapper extends BaseMapper<Wiki> {
}
