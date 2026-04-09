package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SensorStationCreateMapper implements DTOMapper<SensorStation, SensorStationCreateDTO> {

    private RoomMonitoringRepository monitoringRepository;

    @Autowired
    public SensorStationCreateMapper(RoomMonitoringRepository repository) {
        this.monitoringRepository = repository;
    }

    @Override
    public SensorStationCreateDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SensorStation mapFrom(SensorStationCreateDTO dto) {
        SensorStation station = new SensorStation();
        station.setName(dto.name());
        station.setStatus(DeviceStatus.OFFLINE);
        station.setLastHeartBeat(null); // never connected yet
        station.setRoomMonitoring(monitoringRepository.getReferenceById(dto.roomId()));
        return station;
    }
}
