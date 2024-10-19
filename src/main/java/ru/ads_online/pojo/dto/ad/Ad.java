package ru.ads_online.pojo.dto.ad;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Ad {
    private int author;
    private String image;
    private int pk;
    private int price;
    private String title;
}
