package com.zachary.delivery_system.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.service.AppUserService;
import com.zachary.delivery_system.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 22091
* @description 针对表【app_users】的数据库操作Service实现
* @createDate 2026-08-23 11:13:50
*/
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser>
    implements AppUserService {

}




