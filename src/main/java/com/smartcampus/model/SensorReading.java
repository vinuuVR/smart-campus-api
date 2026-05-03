package com.smartcampus.model;

public class SensorReading {
    private String id;
    private String sensorId;
    private long timestamp;       // epoch millis
    private double value;

    public SensorReading() {}

    public SensorReading(String id, String sensorId, long timestamp, double value) {
        this.id = id;
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}