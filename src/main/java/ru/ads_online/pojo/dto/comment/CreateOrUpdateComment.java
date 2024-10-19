package ru.ads_online.pojo.dto.comment;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrUpdateComment {
    @Size(min = 8, max = 64)
    private String text;
}
