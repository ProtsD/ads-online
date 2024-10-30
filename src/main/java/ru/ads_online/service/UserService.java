package ru.ads_online.service;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.user.NewPassword;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;

public interface UserService {
    void setPassword(Authentication authentication, NewPassword newPassword);

    User getData(Authentication authentication);

    UpdateUser updateData(Authentication authentication, UpdateUser updateUser);

    void updateImage(Authentication authentication, MultipartFile image);
}