package com.smartcampus.model;

public class Sensor {
    private String id;
    private String type;          // e.g. "CO2", "occupancy", "lighting"
    private String roomId;
    private String status;        // e.g. "ACTIVE", "MAINTENANCE"
    private double currentValue;

    public Sensor() {}

    public Sensor(String id, String type, String roomId, String status, double currentValue) {
        this.id = id;
        this.type = type;
        this.roomId = roomId;
        this.status = status;
        this.currentValue = currentValue;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
}