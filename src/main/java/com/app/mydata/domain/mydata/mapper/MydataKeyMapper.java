package com.app.mydata.domain.mydata.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MydataKeyMapper {

    int existsByCiHash (@Param("ciHash") String ciHash);

    void insertKey(@Param("ciHash") String ciHash);
}