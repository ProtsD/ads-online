package ru.ads_online.pojo.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Login {
    @Size(min = 8, max = 16)
    private String username;
    @Size(min = 4, max = 32)
    private String password;
}
