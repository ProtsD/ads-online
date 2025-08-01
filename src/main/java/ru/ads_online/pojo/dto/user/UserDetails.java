package ru.ads_online.pojo.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserDetails {
    private String username;
    private String password;
    private Role role;
}