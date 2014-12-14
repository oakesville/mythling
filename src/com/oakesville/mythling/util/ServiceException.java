package com.oakesville.mythling.util;

import java.io.IOException;

public class ServiceException extends IOException {
    public ServiceException(String msg) {
        super(msg);
    }

    public ServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}