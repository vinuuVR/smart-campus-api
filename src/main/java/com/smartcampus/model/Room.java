package com.smartcampus.model;

public class Room {
    private String id;
    private String name;
    private String building;
    private String floor;

    public Room() {}  // needed for JSON deserialization

    public Room(String id, String name, String building, String floor) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.floor = floor;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
}