package com.smartcampus;

import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import com.smartcampus.resources.DiscoveryResource;
import com.smartcampus.resources.RoomResource;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.resources.SensorResource;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.resources.SensorReadingResource;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.filter.LoggingFilter;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(SensorResource.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorReadingResource.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);
        classes.add(LoggingFilter.class);
        return classes;
    }
}
