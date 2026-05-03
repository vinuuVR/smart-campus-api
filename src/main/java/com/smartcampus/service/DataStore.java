package com.smartcampus.service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

public class DataStore {

    // Singletons – single instance for the whole application
    private static final DataStore INSTANCE = new DataStore();

    // Thread‑safe collections
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    // We'll store sensor room associations later; for now just a count map
    private final ConcurrentHashMap<String, Integer> roomSensorCount = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // --- Room operations ---
    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
        roomSensorCount.put(room.getId(), 0); // initially no sensors
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // Sensor operations
    public Collection<Sensor> getAllSensors() {
        return sensors.values();
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), new CopyOnWriteArrayList<>());
        // Automatically increment the sensor count for the room
        if (sensor.getRoomId() != null) {
            roomSensorCount.merge(sensor.getRoomId(), 1, Integer::sum);
        }
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    // Filtering by type – returns sensors matching the given type (case-insensitive)
    public Collection<Sensor> getSensorsByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .toList();
    }

    // For sensor association – will be used in Part 3 and Part 2's delete check
    public int getSensorCount(String roomId) {
        return roomSensorCount.getOrDefault(roomId, 0);
    }

    public void incrementSensorCount(String roomId) {
        roomSensorCount.merge(roomId, 1, Integer::sum);
    }

    public void decrementSensorCount(String roomId) {
        roomSensorCount.computeIfPresent(roomId, (k, v) -> v > 0 ? v - 1 : 0);
    }

    // Returns the list of readings for a sensor (returns empty list if none)
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.getOrDefault(sensorId, List.of());
    }

// Adds a new reading and updates the parent sensor's currentValue
    public void addReading(SensorReading reading) {
        readings.compute(reading.getSensorId(), (id, list) -> {
            List<SensorReading> newList = list == null ? new CopyOnWriteArrayList<>() : list;
            newList.add(reading);
            return newList;
        });

        // Side effect: update the parent sensor's currentValue
        Sensor sensor = sensors.get(reading.getSensorId());
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }
}
