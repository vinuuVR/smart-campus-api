package com.smartcampus.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import com.smartcampus.model.SensorReading;
import com.smartcampus.service.DataStore;
import com.smartcampus.model.Sensor;
import com.smartcampus.exception.SensorUnavailableException;

public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    // Constructor: the sub‑resource locator will pass the parent sensor's ID
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {
        List<SensorReading> readings = store.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        // Enforce state constraint: cannot add readings to a sensor in MAINTENANCE
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with ID " + sensorId + " does not exist.");
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is in MAINTENANCE and cannot accept readings.");
        }

        reading.setSensorId(sensorId);

        // Ensure the sensorId field is set to the parent sensor
        reading.setSensorId(sensorId);

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReading(reading);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(reading.getId())
                .build();
        return Response.created(location).entity(reading).build();
    }
}
