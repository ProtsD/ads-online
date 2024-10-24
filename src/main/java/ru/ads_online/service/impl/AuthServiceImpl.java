package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.ads_online.pojo.dto.user.Register;
import ru.ads_online.security.DatabaseUserDetailsService;
import ru.ads_online.service.AuthService;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final DatabaseUserDetailsService manager;
    private final PasswordEncoder encoder;

    @Override
    public boolean login(String userName, String password) {
        if (!manager.userExists(userName)) {
            return false;
        }
        UserDetails userFromDb = manager.loadUserByUsername(userName);
        return encoder.matches(password, userFromDb.getPassword());
    }

    @Override
    public boolean register(Register register) {
        if (manager.userExists(register.getUsername())) {
            return false;
        }
        manager.createUser(register);
        return true;
    }
}
