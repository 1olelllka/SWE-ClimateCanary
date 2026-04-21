package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensorStationCreateMapper implements DTOMapper<SensorStation, SensorStationCreateDTO> {

    private final RoomMonitoringRepository repository;

    @Override
    public SensorStationCreateDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SensorStation mapFrom(SensorStationCreateDTO dto) {
        SensorStation station = new SensorStation();
        station.setName(dto.name());
        station.setStatus(DeviceStatus.OFFLINE);
        station.setRoomMonitoring(repository.findById(dto.roomId()).orElseThrow(() -> new NotFoundException("Room with id " + dto.roomId() + " was not found.")));
        station.setLastHeartBeat(null); // never connected yet
        return station;
    }
}
