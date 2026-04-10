package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RaspberryCreateDTO;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RaspberryCreateMapper implements DTOMapper<RaspberryPi, RaspberryCreateDTO> {

    private final RoomMonitoringRepository monitoringRepository;

    @Autowired
    public RaspberryCreateMapper(RoomMonitoringRepository monitoringRepository) {
        this.monitoringRepository = monitoringRepository;
    }

    @Override
    public RaspberryCreateDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RaspberryPi mapFrom(RaspberryCreateDTO dto) {
        RaspberryPi pi = new RaspberryPi();
        pi.setIp(dto.ipAddress());
        pi.setName(dto.name());
        pi.setViolationCounter(0);
        pi.setStatus(DeviceStatus.OFFLINE);
        pi.setFrequency(dto.frequency());
        pi.setLastHeartBeat(null);
        pi.setRoomMonitoring(monitoringRepository.getReferenceById(dto.roomId()));
        return pi;
    }
}