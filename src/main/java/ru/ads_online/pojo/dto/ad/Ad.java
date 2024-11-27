package ru.ads_online.pojo.dto.ad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Ad {
    private int author;
    private String image;
    private int id;
    @Min(0)
    @Max(10000000)
    private int price;
    @Size(min = 4, max = 32)
    private String title;
}
