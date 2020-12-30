package com.easydicm.scpdb.mapper;

import com.easydicm.scpdb.entities.Patient;
import org.apache.ibatis.annotations.*;
import org.mybatis.scripting.freemarker.FreeMarkerLanguageDriver;

/**
 * @author dhz
 */
@Mapper
public interface IDbPatientMapper {

    /***
     * 数据插入
     * @param record
     */
    @Lang(FreeMarkerLanguageDriver.class)
    @Insert("sql/insert.ftl")
    void  insert(@Param("p") Patient record);

}