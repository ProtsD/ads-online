package ru.ads_online.mapper;

import org.springframework.stereotype.Component;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.pojo.dto.user.UserDetails;

@Component
public class UserMapper {
    public UserEntity toUserEntity(User user) {
        return new UserEntity()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setPhone(user.getPhone())
                .setRole(user.getRole())
                .setImage(user.getImage());
    }

    public User toUser(UserEntity userEntity) {
        return new User()
                .setId(userEntity.getId())
                .setUsername(userEntity.getUsername())
                .setFirstName(userEntity.getFirstName())
                .setLastName(userEntity.getLastName())
                .setPhone(userEntity.getPhone())
                .setRole(userEntity.getRole())
                .setImage(userEntity.getImage());
    }

    public UpdateUser toUpdateUser(UserEntity userEntity) {
        return new UpdateUser()
                .setFirstName(userEntity.getFirstName())
                .setLastName(userEntity.getLastName())
                .setPhone(userEntity.getPhone());
    }

    public UserDetails toUserDetails(UserEntity userEntity) {
        return new UserDetails()
                .setUsername(userEntity.getUsername())
                .setPassword(userEntity.getPassword())
                .setRole(userEntity.getRole());
    }
}
