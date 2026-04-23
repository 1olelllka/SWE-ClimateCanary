package at.qe.skeleton.services;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingSeederService {

    private final BuildingRepository buildingRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final UserxRepository userxRepository;

    @Transactional
    public void seed() {
        if (buildingRepository.existsByName("Headquarters")) {
            return;
        }

        Building hq = buildingRepository.save(
                Building.builder()
                        .name("Headquarters")
                        .address("Innrain 52, 6020 Innsbruck")
                        .build()
        );

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
                monitoringRepository.save(RoomMonitoring.builder().roomId(room.getId()).roomNumber(room.getRoomNumber()).build());
                // Assign demo employee to first Engineering office for dashboard demo
                if ("Engineering".equals(deptName) && i == 1) {
                    userxRepository.findFirstByUsername("employee").ifPresentOrElse(
                            user -> {
                                System.out.println("✅ Employee found: " + user.getUsername());
                                System.out.println("➡ Assigning room: " + room.getRoomNumber());

                                user.setMyRoom(room);
                                userxRepository.save(user);
                            },
                            () -> {
                                System.out.println("❌ Employee NOT FOUND during seeding");
                            }
                    );
                }
            }
        }
    }
}
