package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensorStationPatchMapper implements DTOMapper<SensorStation, SensorStationPatchDTO> {

    private final RoomMonitoringRepository repository;

    @Override
    public SensorStationPatchDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SensorStation mapFrom(SensorStationPatchDTO dto) {
        return SensorStation.builder()
                .name(dto.name())
                .status(dto.status())
                .lastHeartBeat(dto.lastHeartBeat())
                .roomMonitoring(repository.findById(dto.roomId()).orElse(null))
                .build();
    }
}
