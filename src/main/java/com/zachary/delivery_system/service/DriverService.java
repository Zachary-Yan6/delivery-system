package com.zachary.delivery_system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zachary.delivery_system.dto.Driver.CreateDriverRequest;
import com.zachary.delivery_system.entity.Driver;


/**
* @author 22091
* @description 针对表【drivers】的数据库操作Service
* @createDate 2026-08-23 11:14:55
*/
public interface DriverService extends IService<Driver> {
    Driver createDriver(CreateDriverRequest request);

}
