package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.DeviceStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service that pushes real-time events to WebSocket subscribers via STOMP topics.
 * All payloads are serialized to JSON using Jackson before being sent. Date/time
 * values are written in ISO-8601 format (timestamps-as-strings are disabled).
 *
 * <p>Topic layout:
 * <ul>
 *   <li>{@code /topic/active-warnings/{roomId}} — new warning for a room</li>
 *   <li>{@code /topic/resolve-warnings/{roomId}} — resolved warning for a room</li>
 *   <li>{@code /topic/active-warnings-department/{departmentId}} — new warning for a department</li>
 *   <li>{@code /topic/resolve-warnings-department/{departmentId}} — resolved warning for a department</li>
 *   <li>{@code /topic/climate-data/{roomId}} — live climate reading for a room</li>
 *   <li>{@code /topic/sensor-status/{sensorId}} — Arduino/sensor connection status</li>
 *   <li>{@code /topic/raspberry-status/{piId}} — Raspberry Pi connection status</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LiveDataService {

    private final SimpMessagingTemplate messagingTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Broadcasts a new active warning to all subscribers of the given room's warning topic.
     *
     * @param roomId  the UUID of the room the warning applies to
     * @param warning the warning payload to broadcast
     * @throws RuntimeException if JSON serialization fails
     */
    public void pushActiveWarning(UUID roomId, WarningDTO warning) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/active-warnings/" + roomId.toString(), mapper.writeValueAsString(warning));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcasts a new active warning to all subscribers of the given department's warning topic.
     *
     * @param departmentId the UUID of the department the warning applies to
     * @param dto          the warning payload to broadcast
     * @throws RuntimeException if JSON serialization fails
     */
    public void pushActiveWarningDepartment(UUID departmentId, WarningDTO dto) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/active-warnings-department/" + departmentId.toString(), mapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcasts a warning-resolved event to all subscribers of the given department's
     * resolve topic.
     *
     * @param departmentId the UUID of the department whose warning was resolved
     * @param dto          the resolved warning payload
     * @throws RuntimeException if JSON serialization fails
     */
    public void resolveActiveWarningDepartment(UUID departmentId, WarningDTO dto) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/resolve-warnings-department/" + departmentId.toString(), mapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcasts a warning-resolved event to all subscribers of the given room's
     * resolve topic.
     *
     * @param roomId  the UUID of the room whose warning was resolved
     * @param warning the resolved warning payload
     * @throws RuntimeException if JSON serialization fails
     */
    public void resolveActiveWarning(UUID roomId, WarningDTO warning) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/resolve-warnings/" + roomId.toString(), mapper.writeValueAsString(warning));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pushes a live climate data point to all subscribers of the given room's
     * climate-data topic.
     *
     * @param roomId the UUID of the room the reading belongs to
     * @param stats  the climate data payload
     * @throws RuntimeException if JSON serialization fails
     */
    public void pushLiveClimateData(UUID roomId, ClimateDataPointDTO stats) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/climate-data/" + roomId.toString(), mapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pushes the connection status of an Arduino/sensor station to its status topic.
     *
     * @param sensorStation the UUID of the sensor station
     * @param status        the new {@link DeviceStatus}
     * @throws RuntimeException if JSON serialization fails
     */
    public void pushConnectionStatusArduino(UUID sensorStation, DeviceStatus status) {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/sensor-status/" + sensorStation.toString(), mapper.writeValueAsString("%s".formatted(status.name())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pushes the connection status of a Raspberry Pi to its status topic.
     *
     * @param raspberry the UUID of the Raspberry Pi
     * @param status    the new {@link DeviceStatus}
     * @throws RuntimeException if JSON serialization fails
     */
    public void pushConnectionStatusRaspberry(UUID raspberry, DeviceStatus status) {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/raspberry-status/" + raspberry.toString(), mapper.writeValueAsString("%s".formatted(status.name())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}