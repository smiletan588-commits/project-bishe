package com.smartpm.common.utils;

import com.smartpm.entity.User;

public class UserHolder {

    private static final ThreadLocal<User> USER = new ThreadLocal<>();

    public static void set(User user) {
        USER.set(user);
    }

    public static User get() {
        return USER.get();
    }

    public static Long getUserId() {
        User user = get();
        return user != null ? user.getId() : null;
    }

    public static void remove() {
        USER.remove();
    }
}
