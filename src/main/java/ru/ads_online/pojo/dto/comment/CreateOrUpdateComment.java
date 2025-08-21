package ru.ads_online.pojo.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateOrUpdateComment {
    @NotNull
    @Size(min = 8, max = 64)
    private String text;
}
