package ru.ads_online.pojo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "image")
@Data
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Lob
    @Column(name = "image", nullable = false)
    private byte[] image;
}