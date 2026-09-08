package com.example.evshare.dto.response;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    private Long id;
    private Long vehicleId;
    private String vehicleModel;
    private String vehicleLicensePlate;
    private Long userId;
    private String userName;
    private String userEmail;
    private Instant startTime;
    private Instant endTime;
    private Instant bufferedEndTime;
    private BookingStatus status;
    private BigDecimal estimatedCost;
    private Instant createdAt;

    public BookingResponse() {
    }

    public BookingResponse(Long id, Long vehicleId, String vehicleModel, String vehicleLicensePlate,
                           Long userId, String userName, String userEmail,
                           Instant startTime, Instant endTime, Instant bufferedEndTime,
                           BookingStatus status, BigDecimal estimatedCost, Instant createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.vehicleModel = vehicleModel;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bufferedEndTime = bufferedEndTime;
        this.status = status;
        this.estimatedCost = estimatedCost;
        this.createdAt = createdAt;
    }

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) {
            return null;
        }
        Instant bufferedEnd = booking.getEndTime() != null
                ? booking.getEndTime().plus(30, ChronoUnit.MINUTES)
                : null;

        return Builder.builder()
                .id(booking.getId())
                .vehicleId(booking.getVehicle() != null ? booking.getVehicle().getId() : null)
                .vehicleModel(booking.getVehicle() != null ? booking.getVehicle().getModelName() : null)
                .vehicleLicensePlate(booking.getVehicle() != null ? booking.getVehicle().getLicensePlate() : null)
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .userName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .userEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .bufferedEndTime(bufferedEnd)
                .status(booking.getStatus())
                .estimatedCost(booking.getEstimatedCost())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }

    public void setVehicleLicensePlate(String vehicleLicensePlate) {
        this.vehicleLicensePlate = vehicleLicensePlate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getBufferedEndTime() {
        return bufferedEndTime;
    }

    public void setBufferedEndTime(Instant bufferedEndTime) {
        this.bufferedEndTime = bufferedEndTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private Long id;
        private Long vehicleId;
        private String vehicleModel;
        private String vehicleLicensePlate;
        private Long userId;
        private String userName;
        private String userEmail;
        private Instant startTime;
        private Instant endTime;
        private Instant bufferedEndTime;
        private BookingStatus status;
        private BigDecimal estimatedCost;
        private Instant createdAt;

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder vehicleId(Long vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder vehicleModel(String vehicleModel) {
            this.vehicleModel = vehicleModel;
            return this;
        }

        public Builder vehicleLicensePlate(String vehicleLicensePlate) {
            this.vehicleLicensePlate = vehicleLicensePlate;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder bufferedEndTime(Instant bufferedEndTime) {
            this.bufferedEndTime = bufferedEndTime;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder estimatedCost(BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BookingResponse build() {
            return new BookingResponse(id, vehicleId, vehicleModel, vehicleLicensePlate,
                    userId, userName, userEmail, startTime, endTime, bufferedEndTime,
                    status, estimatedCost, createdAt);
        }
    }
}
