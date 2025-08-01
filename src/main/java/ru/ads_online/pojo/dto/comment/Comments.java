package ru.ads_online.pojo.dto.comment;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Comments {
    private int count;
    private List<Comment> results;
}
