//package at.qe.skeleton.tests.services;
//
//import at.qe.skeleton.dtos.*;
//import at.qe.skeleton.exceptions.NotFoundException;
//import at.qe.skeleton.mappers.AggregatedStatsMapper;
//import at.qe.skeleton.mappers.ClimateDataPointMapper;
//import at.qe.skeleton.mappers.LimitMapper;
//import at.qe.skeleton.model.*;
//import at.qe.skeleton.repositories.AggregatedStatsRepository;
//import at.qe.skeleton.repositories.ClimateStatsRepository;
//import at.qe.skeleton.repositories.RoomMonitoringRepository;
//import at.qe.skeleton.services.impl.ClimateStatsServiceImpl;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.OffsetDateTime;
//import java.time.ZoneId;
//import java.time.ZoneOffset;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
//    private final OffsetDateTime base   = OffsetDateTime.of(LocalDateTime.of(2024, 6, 15, 12, 0), ZoneOffset.UTC);
//    private final OffsetDateTime today  = OffsetDateTime.of(LocalDateTime.of(2024, 6, 15, 12, 0), ZoneOffset.UTC);
//
//    private RoomMonitoring room() {
//        return RoomMonitoring.builder().roomId(roomId).roomNumber("A101").build();
//    }
//
//    private ClimateStats stats(OffsetDateTime date, double temp, double hum, double poll) {
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
//            OffsetDateTime  start     = today;
//            OffsetDateTime  end       = today;
//            LocalTime  startTime = LocalTime.of(8, 0);
//            LocalTime  endTime   = LocalTime.of(18, 0);
//
//            List<ClimateStats> inWindow = List.of(
//                    stats(today.withHour(10), 20, 50, 300),
//                    stats(today.withHour(14), 21, 52, 320)
//            );
//
//            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
//                    roomId,
//                    start.toLocalDate().atTime(startTime).atZone(ZoneId.systemDefault()).toOffsetDateTime(),
//                    end.toLocalDate().atTime(endTime).atZone(ZoneId.systemDefault()).toOffsetDateTime()))
//                    .thenReturn(inWindow);
//
//            List<ClimateDataPointDTO> result =
//                    service.getOvertime(roomId, start.toLocalDate(), end.toLocalDate(), startTime, endTime);
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
//                    today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
//                    today.toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toOffsetDateTime()))
//                    .thenReturn(List.of());
//
//            List<ClimateDataPointDTO> result =
//                    service.getOvertime(roomId, today.toLocalDate(), today.withHour(23).withMinute(59).toLocalDate(), null, null);
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
//            assertThat(service.getOvertime(roomId, today.toLocalDate(), today.toLocalDate(), null, null)).isEmpty();
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
//                    stats(today.withHour(10), 20, 50, 300),
//                    stats(today.withHour(10).withMinute(30), 22, 54, 320), // same hour
//                    stats(today.withHour(11), 24, 58, 340)
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
//                    stats(today.minusDays(1).withHour(10), 18, 45, 250),
//                    stats(today.minusDays(1).withHour(14), 20, 50, 300), // same day
//                    stats(today.withHour(10),              22, 55, 400)
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
//                    stats(today.withHour(10), 20, 50, 300),
//                    stats(today.withHour(10).withMinute(30), 22, 54, 320)  // same day
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
//                    .roomId(roomId).date(today.toLocalDate()).granularity(Granularity.DAILY)
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
//                    .thenReturn(List.of(stats(today.withHour(10), 20, 50, 300)));
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
