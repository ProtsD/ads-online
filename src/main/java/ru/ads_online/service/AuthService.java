package ru.ads_online.service;

import ru.ads_online.pojo.dto.user.Register;

public interface AuthService {
    boolean login(String userName, String password);

    boolean register(Register register);
}
