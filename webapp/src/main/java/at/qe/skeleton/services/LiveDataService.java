package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.ClimateStats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveDataService {

    private final SimpMessagingTemplate messagingTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    public void pushActiveWarning(UUID roomId, WarningDTO warning) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/active-warnings/" + roomId.toString(), mapper.writeValueAsString(warning));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void resolveActiveWarning(UUID roomId, WarningDTO warning) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/resolve-warnings/" + roomId.toString(), mapper.writeValueAsString(warning));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void pushLiveClimateData(UUID roomId, ClimateDataPointDTO stats) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            messagingTemplate.convertAndSend("/topic/climate-data/"+roomId.toString(), mapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
