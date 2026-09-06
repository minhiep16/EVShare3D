package com.example.evshare.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ownership_groups")
public class OwnershipGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    @Column(name = "formation_date", nullable = false)
    private LocalDate formationDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public OwnershipGroup() {
    }

    public OwnershipGroup(Long id, String groupName, Vehicle vehicle, LocalDate formationDate, Boolean isActive) {
        this.id = id;
        this.groupName = groupName;
        this.vehicle = vehicle;
        this.formationDate = formationDate;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LocalDate getFormationDate() {
        return formationDate;
    }

    public void setFormationDate(LocalDate formationDate) {
        this.formationDate = formationDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
