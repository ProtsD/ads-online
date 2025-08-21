package ru.ads_online.pojo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Entity
@Table(name = "image")
@Data
@Accessors(chain = true)
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Lob
    @Column(name = "image", nullable = false)
    private byte[] image;
}