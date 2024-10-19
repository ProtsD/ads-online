package ru.ads_online.pojo.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Component
@Data
@Accessors(chain = true)
public class User {
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private String image;
}
