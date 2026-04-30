//package at.qe.skeleton.tests.services;
//
//import at.qe.skeleton.dtos.*;
//import at.qe.skeleton.exceptions.NotFoundException;
//import at.qe.skeleton.mappers.AggregatedStatsMapper;
//import at.qe.skeleton.mappers.ClimateDataPointMapper;
//import at.qe.skeleton.mappers.LimitMapper;
//import at.qe.skeleton.model.*;
//import at.qe.skeleton.repositories.*;
//import at.qe.skeleton.services.impl.ClimateStatsServiceImpl;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.*;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("ClimateStatsService")
//class ClimateStatsServiceUnitTests {
//
//    @Mock ClimateStatsRepository    climateStatsRepository;
//    @Mock AggregatedStatsRepository aggregatedStatsRepository;
//    @Mock RoomMonitoringRepository  roomMonitoringRepository;
//
//
//    @Spy ClimateDataPointMapper climateMapper    = new ClimateDataPointMapper();
//    @Spy AggregatedStatsMapper  aggregatedMapper = new AggregatedStatsMapper();
//    @Spy LimitMapper            limitMapper      = new LimitMapper();
//
//    @InjectMocks ClimateStatsServiceImpl service;
//
//
//
//    private final UUID          roomId = UUID.randomUUID();
//    private final LocalDateTime base   = LocalDateTime.of(2024, 6, 15, 12, 0);
//    private final LocalDate     today  = LocalDate.of(2024, 6, 15);
//
//    private RoomMonitoring room() {
//        return RoomMonitoring.builder().roomId(roomId).roomNumber("A101").build();
//    }
//
//    private ClimateStats stats(LocalDateTime date, double temp, double hum, double poll) {
//        return ClimateStats.builder()
//                .date(date).tempVal(temp).humVal(hum).pollVal(poll)
//                .roomMonitoring(room()).build();
//    }
//
//
//    @Nested
//    @DisplayName("getCurrentClimate")
//    class GetCurrentClimate {
//
//        @Test
//        @DisplayName("returns mapped DTO for the latest entry")
//        void returnsLatestMapped() {
//            ClimateStats entity = stats(base, 22, 55, 400);
//            when(climateStatsRepository
//                    .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
//                    .thenReturn(Optional.of(entity));
//
//            ClimateDataPointDTO result = service.getCurrentClimate(roomId);
//
//            assertThat(result.timestamp()).isEqualTo(base);
//            assertThat(result.temperature()).isEqualTo(22);
//            assertThat(result.humidity()).isEqualTo(55);
//            assertThat(result.airQuality()).isEqualTo(400);
//        }
//
//        @Test
//        @DisplayName("throws NotFoundException when no data exists")
//        void throwsWhenEmpty() {
//            when(climateStatsRepository
//                    .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
//                    .thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.getCurrentClimate(roomId))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(roomId.toString());
//        }
//    }
//
//
//
//    @Nested
//    @DisplayName("saveMeasurementBatch")
//    class SaveMeasurementBatch {
//
//        @Test
//        @DisplayName("saves a ClimateStats entity with correct values from batch")
//        void savesCorrectEntity() {
//            MeasurementBatchDTO batch = new MeasurementBatchDTO(
//                    roomId, base,
//                    List.of(
//                            new ReadingDTO(MeasurementType.TEMPERATURE, 22.5),
//                            new ReadingDTO(MeasurementType.HUMIDITY,    55.0),
//                            new ReadingDTO(MeasurementType.CO2,         400.0)
//                    ));
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(room()));
//            when(climateStatsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            service.saveMeasurementBatch(batch);
//
//            ArgumentCaptor<ClimateStats> captor = ArgumentCaptor.forClass(ClimateStats.class);
//            verify(climateStatsRepository).save(captor.capture());
//            ClimateStats saved = captor.getValue();
//
//            assertThat(saved.getTempVal()).isEqualTo(22.5);
//            assertThat(saved.getHumVal()).isEqualTo(55.0);
//            assertThat(saved.getPollVal()).isEqualTo(400.0);
//            assertThat(saved.getDate()).isEqualTo(base);
//            assertThat(saved.getRoomMonitoring().getRoomId()).isEqualTo(roomId);
//        }
//
//        @Test
//        @DisplayName("last reading of each type wins when duplicates are present")
//        void lastReadingWins() {
//            MeasurementBatchDTO batch = new MeasurementBatchDTO(
//                    roomId, base,
//                    List.of(
//                            new ReadingDTO(MeasurementType.TEMPERATURE, 20.0),
//                            new ReadingDTO(MeasurementType.TEMPERATURE, 25.0), // wins
//                            new ReadingDTO(MeasurementType.HUMIDITY,    50.0),
//                            new ReadingDTO(MeasurementType.CO2,         300.0)
//                    ));
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(room()));
//            when(climateStatsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            service.saveMeasurementBatch(batch);
//
//            ArgumentCaptor<ClimateStats> captor = ArgumentCaptor.forClass(ClimateStats.class);
//            verify(climateStatsRepository).save(captor.capture());
//            assertThat(captor.getValue().getTempVal()).isEqualTo(25.0);
//        }
//
//        @Test
//        @DisplayName("missing reading type defaults to 0.0")
//        void missingReadingDefaultsToZero() {
//            MeasurementBatchDTO batch = new MeasurementBatchDTO(
//                    roomId, base,
//                    List.of(new ReadingDTO(MeasurementType.TEMPERATURE, 22.0))
//            );
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(room()));
//            when(climateStatsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            service.saveMeasurementBatch(batch);
//
//            ArgumentCaptor<ClimateStats> captor = ArgumentCaptor.forClass(ClimateStats.class);
//            verify(climateStatsRepository).save(captor.capture());
//            assertThat(captor.getValue().getHumVal()).isZero();
//            assertThat(captor.getValue().getPollVal()).isZero();
//        }
//
//        @Test
//        @DisplayName("throws NotFoundException when room does not exist")
//        void throwsWhenRoomNotFound() {
//            MeasurementBatchDTO batch = new MeasurementBatchDTO(
//                    roomId, base,
//                    List.of(new ReadingDTO(MeasurementType.TEMPERATURE, 22.0))
//            );
//            when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.saveMeasurementBatch(batch))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(roomId.toString());
//
//            verify(climateStatsRepository, never()).save(any());
//        }
//    }
//
//
//
//    @Nested
//    @DisplayName("getOvertime")
//    class GetOvertime {
//
//        @Test
//        @DisplayName("returns entries within date and time window")
//        void filtersCorrectly() {
//            LocalDate  start     = today;
//            LocalDate  end       = today;
//            LocalTime  startTime = LocalTime.of(8, 0);
//            LocalTime  endTime   = LocalTime.of(18, 0);
//
//            List<ClimateStats> raw = List.of(
//                    stats(today.atTime(7,  0), 18, 45, 250), // before startTime — excluded
//                    stats(today.atTime(10, 0), 20, 50, 300), // in window
//                    stats(today.atTime(14, 0), 21, 52, 320), // in window
//                    stats(today.atTime(20, 0), 22, 55, 400)  // after endTime — excluded
//            );
//
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    roomId,
//                    start.atTime(startTime),
//                    end.atTime(endTime)))
//                    .thenReturn(raw);
//
//            List<ClimateDataPointDTO> result =
//                    service.getOvertime(roomId, start, end, startTime, endTime);
//
//            assertThat(result).hasSize(2)
//                    .extracting(ClimateDataPointDTO::temperature)
//                    .containsExactly(20.0, 21.0);
//        }
//
//        @Test
//        @DisplayName("uses MIDNIGHT and LocalTime.MAX when times are null")
//        void nullTimesDefaultToFullDay() {
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    roomId,
//                    today.atStartOfDay(),
//                    today.atTime(LocalTime.MAX)))
//                    .thenReturn(List.of());
//
//            List<ClimateDataPointDTO> result =
//                    service.getOvertime(roomId, today, today, null, null);
//
//            assertThat(result).isEmpty();
//        }
//
//        @Test
//        @DisplayName("returns empty list when repository returns no data")
//        void returnsEmptyWhenNoData() {
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    any(), any(), any()))
//                    .thenReturn(List.of());
//
//            assertThat(service.getOvertime(roomId, today, today, null, null)).isEmpty();
//        }
//    }
//
//
//    @Nested
//    @DisplayName("getClimateHistoryFull")
//    class GetClimateHistoryFull {
//
//        @Test
//        @DisplayName("groups by hour when granularity is HOUR and timeframe is not MONTH")
//        void groupsByHourForWeek() {
//            List<ClimateStats> raw = List.of(
//                    stats(today.atTime(10,  0), 20, 50, 300),
//                    stats(today.atTime(10, 30), 22, 54, 320), // same hour
//                    stats(today.atTime(11,  0), 24, 58, 340)
//            );
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    eq(roomId), any(), any()))
//                    .thenReturn(raw);
//
//            List<AggregatedDataPointDTO> result =
//                    service.getClimateHistoryFull(roomId, "WEEK", "HOUR");
//
//            // 10:00 and 10:30 collapse into one hourly bucket
//            assertThat(result).hasSize(2);
//            assertThat(result.get(0).avgTemperature()).isEqualTo(21.0); // (20+22)/2
//        }
//
//        @Test
//        @DisplayName("groups by day when granularity is DAY")
//        void groupsByDayForWeek() {
//            List<ClimateStats> raw = List.of(
//                    stats(today.minusDays(1).atTime(10, 0), 18, 45, 250),
//                    stats(today.minusDays(1).atTime(14, 0), 20, 50, 300), // same day
//                    stats(today.atTime(10, 0),              22, 55, 400)
//            );
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    eq(roomId), any(), any()))
//                    .thenReturn(raw);
//
//            List<AggregatedDataPointDTO> result =
//                    service.getClimateHistoryFull(roomId, "WEEK", "DAY");
//
//            assertThat(result).hasSize(2);
//            assertThat(result.get(0).avgTemperature()).isEqualTo(19.0); // (18+20)/2
//        }
//
//        @Test
//        @DisplayName("uses DAY grouping for MONTH timeframe even if granularity is HOUR")
//        void monthTimeframeAlwaysUsesDay() {
//            List<ClimateStats> raw = List.of(
//                    stats(today.atTime(10,  0), 20, 50, 300),
//                    stats(today.atTime(10, 30), 22, 54, 320)  // same day
//            );
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    eq(roomId), any(), any()))
//                    .thenReturn(raw);
//
//            List<AggregatedDataPointDTO> result =
//                    service.getClimateHistoryFull(roomId, "MONTH", "HOUR");
//
//            // forced to DAY grouping — both entries collapse into one day bucket
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).avgTemperature()).isEqualTo(21.0);
//        }
//
//        @Test
//        @DisplayName("throws IllegalArgumentException for unknown timeframe")
//        void throwsForInvalidTimeframe() {
//            assertThatThrownBy(() -> service.getClimateHistoryFull(roomId, "YEAR", "DAY"))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("YEAR");
//        }
//
//        @Test
//        @DisplayName("returns empty list when no data exists")
//        void returnsEmptyWhenNoData() {
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    any(), any(), any()))
//                    .thenReturn(List.of());
//
//            assertThat(service.getClimateHistoryFull(roomId, "DAY", "HOUR")).isEmpty();
//        }
//    }
//
//
//    @Nested
//    @DisplayName("getClimateHistoryReduced")
//    class GetClimateHistoryReduced {
//
//        @Test
//        @DisplayName("returns aggregated stats when pre-aggregated data exists")
//        void usesAggregatedData() {
//            AggregatedStats agg = AggregatedStats.builder()
//                    .roomId(roomId).date(today).granularity(Granularity.DAILY)
//                    .avgTemp(21).avgHumidity(53).avgCO2(350)
//                    .build();
//            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
//                    eq(roomId), any(), any(), eq(Granularity.DAILY)))
//                    .thenReturn(List.of(agg));
//
//            List<AggregatedDataPointDTO> result =
//                    service.getClimateHistoryReduced(roomId, "WEEK");
//
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).avgTemperature()).isEqualTo(21.0f);
//            verify(climateStatsRepository, never())
//                    .findByRoomMonitoring_RoomIdAndDateBetween(any(), any(), any());
//        }
//
//        @Test
//        @DisplayName("falls back to raw grouping when no aggregated data exists")
//        void fallsBackToRaw() {
//            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
//                    eq(roomId), any(), any(), eq(Granularity.DAILY)))
//                    .thenReturn(List.of());
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    eq(roomId), any(), any()))
//                    .thenReturn(List.of(stats(today.atTime(10, 0), 20, 50, 300)));
//
//            List<AggregatedDataPointDTO> result =
//                    service.getClimateHistoryReduced(roomId, "WEEK");
//
//            assertThat(result).hasSize(1);
//            verify(climateStatsRepository)
//                    .findByRoomMonitoring_RoomIdAndDateBetween(any(), any(), any());
//        }
//
//        @Test
//        @DisplayName("throws IllegalArgumentException for unknown timeframe")
//        void throwsForInvalidTimeframe() {
//            assertThatThrownBy(() -> service.getClimateHistoryReduced(roomId, "DECADE"))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("DECADE");
//        }
//    }
//
//
//    @Nested
//    @DisplayName("getLimits")
//    class GetLimits {
//
//        private RoomMonitoring roomWithLimits() {
//            return RoomMonitoring.builder()
//                    .roomId(roomId)
//                    .tempLimit(TemperatureLimit.builder().minVal(18f).maxVal(26f).build())
//                    .humLimit(HumidityLimit.builder().minVal(30f).maxVal(70f).build())
//                    .polLimit(PollutionLimit.builder().maxVal(800f).build())
//                    .build();
//        }
//
//        @Test
//        @DisplayName("returns LimitDTO with correct values when room exists")
//        void returnsLimitsForKnownRoom() {
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(roomWithLimits()));
//
//            LimitDTO result = service.getLimits(roomId);
//
//            assertThat(result.roomId()).isEqualTo(roomId);
//            assertThat(result.tempMin()).isEqualTo(18f);
//            assertThat(result.tempMax()).isEqualTo(26f);
//            assertThat(result.humMin()).isEqualTo(30f);
//            assertThat(result.humMax()).isEqualTo(70f);
//            assertThat(result.co2Max()).isEqualTo(800f);
//        }
//
//        @Test
//        @DisplayName("returns defaults when room has no limits configured")
//        void returnsDefaultsWhenLimitsNull() {
//            RoomMonitoring noLimits = RoomMonitoring.builder().roomId(roomId).build();
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(noLimits));
//
//            LimitDTO result = service.getLimits(roomId);
//
//            assertThat(result.tempMin()).isEqualTo(18f);
//            assertThat(result.tempMax()).isEqualTo(26f);
//            assertThat(result.humMin()).isEqualTo(30f);
//            assertThat(result.humMax()).isEqualTo(70f);
//            assertThat(result.co2Max()).isEqualTo(800f);
//        }
//
//        @Test
//        @DisplayName("throws NotFoundException when room does not exist")
//        void throwsWhenRoomNotFound() {
//            when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.getLimits(roomId))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(roomId.toString());
//        }
//    }
//}
