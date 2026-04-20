package at.qe.skeleton.services;

import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RaspberryService {
    Page<RaspberryPi> getAllRaspberries(Pageable pageable);

    RaspberryPi getSpecificRaspberry(UUID id);

    RaspberryPi createNewRaspberry(RaspberryPi raspberryPi);

    RaspberryPi updateRaspberryById(UUID id, RaspberryPi raspberryPi);

    void deleteRaspberry(UUID id);

    int getOccupancyFromRedis(UUID id);

    PiConfigDTO getConfigForRaspberry(UUID id);

    void retryConnection(UUID id);

    RaspberryPi addNewRoom(UUID raspberryId, UUID roomId);

    RaspberryPi removeRoomFromRaspberry(UUID raspberryId, UUID roomId);
}