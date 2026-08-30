package com.zachary.delivery_system.config;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev") // @Profile("dev") means this class runs only when your app starts in the dev profile.
// It prevents this demo account from being accidentally created in production.
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean dispatcherExists = appUserService.lambdaQuery()
                .eq(AppUser::getUsername, "dispatcher")
                .exists();

        if (dispatcherExists) {
            return;
        }

        AppUser dispatcher = new AppUser();
        dispatcher.setUsername("dispatcher");
        dispatcher.setPasswordHash(
                passwordEncoder.encode("dispatcher12345")
        );
        dispatcher.setRole("DISPATCHER");

        appUserService.save(dispatcher);
    }
}