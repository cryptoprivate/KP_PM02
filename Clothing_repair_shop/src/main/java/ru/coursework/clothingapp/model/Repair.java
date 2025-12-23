package ru.coursework.clothingapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repairs")
public class Repair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "defect_id", nullable = false)
    private Defect defect;

    @Column(name = "operation_name", nullable = false)
    private String operationName;

    @ManyToOne
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @Column(name = "material")
    private String material;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "photo_before")
    private String photoBefore;

    @Column(name = "photo_after")
    private String photoAfter;

    public Repair() {
    }

    public Repair(Item item, Defect defect, String operationName, Master master,
                  String material, BigDecimal price) {
        this.item = item;
        this.defect = defect;
        this.operationName = operationName;
        this.master = master;
        this.material = material;
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Defect getDefect() {
        return defect;
    }

    public void setDefect(Defect defect) {
        this.defect = defect;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Master getMaster() {
        return master;
    }

    public void setMaster(Master master) {
        this.master = master;
    }


    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDate completedAt) {
        this.completedAt = completedAt;
    }

    public String getPhotoBefore() {
        return photoBefore;
    }

    public void setPhotoBefore(String photoBefore) {
        this.photoBefore = photoBefore;
    }

    public String getPhotoAfter() {
        return photoAfter;
    }

    public void setPhotoAfter(String photoAfter) {
        this.photoAfter = photoAfter;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    @Override
    public String toString() {
        return operationName;
    }
}

