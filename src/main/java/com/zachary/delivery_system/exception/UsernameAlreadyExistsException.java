package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {

    public UsernameAlreadyExistsException() {
        super(
                HttpStatus.CONFLICT,
                "USERNAME_ALREADY_EXISTS",
                "Username already exists"
        );
    }
}
