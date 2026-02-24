package com.shopimage.util;

import java.util.UUID;

public class IdUtil {
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}