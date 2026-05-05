package at.qe.skeleton.services;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingSeederService {

    private final BuildingRepository buildingRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;

    @Transactional
    public void seed() {
        log.info("Running building Seeder...");
        if (buildingRepository.existsByName("Headquarters")) {
            log.info("Building 'Headquarters' already exists. Aborting...");
            return;
        }

        Building hq = buildingRepository.save(
                Building.builder()
                        .name("Headquarters")
                        .address("Innrain 52, 6020 Innsbruck")
                        .build()
        );
        log.info("Saved new building 'Headquarters'");

        List<String> deptNames = List.of(
                "Engineering", "Marketing", "Human Resources", "Finance", "Operations"
        );

        for (String deptName : deptNames) {
            Department dept = departmentRepository.save(
                    Department.builder()
                            .name(deptName)
                            .building(hq)
                            .build()
            );
            log.info("Saved new department '{}'", deptName);

            String prefix = deptName.substring(0, 3).toUpperCase();
            for (int i = 1; i <= 3; i++) {
                RoomType type = (i == 3) ? RoomType.SHARED : RoomType.OFFICE;
                Room room = roomRepository.save(
                        Room.builder()
                                .roomNumber(prefix + "-10" + i)
                                .roomType(type)
                                .isActive(true)
                                .defaultPeopleCnt(i == 3 ? 10 : 4)
                                .department(dept)
                                .build()
                );
                monitoringRepository.save(RoomMonitoring
                        .builder()
                        .roomId(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .polLimit(PollutionLimit.builder().build())
                        .tempLimit(TemperatureLimit.builder().build())
                        .humLimit(HumidityLimit.builder().build())
                        .build());
                log.info("Saved new room {}.", prefix + "-10" + i);
            }
        }
    }
}
