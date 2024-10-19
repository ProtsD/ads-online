package ru.ads_online.pojo.dto.ad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateOrUpdateAd {
    @Size(min = 4, max = 32)
    private String title;
    @Min(0)
    @Max(10000000)
    private Integer price;
    @Size(min = 8, max = 64)
    private String description;
}
