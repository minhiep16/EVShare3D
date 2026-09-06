package com.example.evshare.entity;

import com.example.evshare.entity.enums.UsageSessionStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "usage_sessions")
@EntityListeners(AuditingEntityListener.class)
public class UsageSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "start_odometer", nullable = false, precision = 10, scale = 2)
    private BigDecimal startOdometer;

    @Column(name = "end_odometer", precision = 10, scale = 2)
    private BigDecimal endOdometer;

    @Column(name = "start_battery", nullable = false)
    private Integer startBattery;

    @Column(name = "end_battery")
    private Integer endBattery;

    @CreatedDate
    @Column(name = "check_in_time", nullable = false, updatable = false)
    private Instant checkInTime = Instant.now();

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private UsageSessionStatus status = UsageSessionStatus.ACTIVE;

    public UsageSession() {
    }

    public UsageSession(Long id, Booking booking, BigDecimal startOdometer, BigDecimal endOdometer, Integer startBattery, Integer endBattery, Instant checkInTime, Instant checkOutTime, UsageSessionStatus status) {
        this.id = id;
        this.booking = booking;
        this.startOdometer = startOdometer;
        this.endOdometer = endOdometer;
        this.startBattery = startBattery;
        this.endBattery = endBattery;
        this.checkInTime = checkInTime != null ? checkInTime : Instant.now();
        this.checkOutTime = checkOutTime;
        this.status = status != null ? status : UsageSessionStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public BigDecimal getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(BigDecimal startOdometer) {
        this.startOdometer = startOdometer;
    }

    public BigDecimal getEndOdometer() {
        return endOdometer;
    }

    public void setEndOdometer(BigDecimal endOdometer) {
        this.endOdometer = endOdometer;
    }

    public Integer getStartBattery() {
        return startBattery;
    }

    public void setStartBattery(Integer startBattery) {
        this.startBattery = startBattery;
    }

    public Integer getEndBattery() {
        return endBattery;
    }

    public void setEndBattery(Integer endBattery) {
        this.endBattery = endBattery;
    }

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public UsageSessionStatus getStatus() {
        return status;
    }

    public void setStatus(UsageSessionStatus status) {
        this.status = status;
    }
}
