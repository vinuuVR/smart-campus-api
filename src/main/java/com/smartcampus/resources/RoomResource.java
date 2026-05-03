package com.smartcampus.resources;

import com.smartcampus.model.Room;
import com.smartcampus.service.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.*;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = store.getAllRooms();
        return Response.ok(allRooms).build();
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Auto‑generate an ID if not provided (you can also require it)
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(UUID.randomUUID().toString());
        }
        store.addRoom(room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found"))
                    .build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found"))
                    .build();
        }

        // Enforce business rule: cannot delete room with sensors
        if (store.getSensorCount(roomId) > 0) {
            throw new RoomNotEmptyException("Room " + roomId + " still has active sensors assigned.");
        }

        store.removeRoom(roomId);
        return Response.noContent().build();
    }
}