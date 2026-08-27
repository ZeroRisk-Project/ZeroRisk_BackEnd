package com.zerorisk.project.global.security.oauth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public final class CookieSerializationUtils {

    private CookieSerializationUtils() {
    }

    public static String serialize(Object object) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(byteStream)) {
                out.writeObject(object);
            }
            return Base64.getUrlEncoder().encodeToString(byteStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("쿠키 직렬화 실패", e);
        }
    }

    public static <T> T deserialize(String cookieValue, Class<T> targetClass) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookieValue);
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return targetClass.cast(in.readObject());
            }
        } catch (Exception e) {
            throw new IllegalStateException("쿠키 역직렬화 실패", e);
        }
    }
}
