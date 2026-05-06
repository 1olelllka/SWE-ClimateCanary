import { LimitState } from '../components/RoomLimitSettings';

export interface LimitDTO {
    roomId?: string;
    tempMin?: number | null;
    tempMax?: number | null;
    humMin?: number | null;
    humMax?: number | null;
    co2Max?: number | null;
}

export const emptyLimits: LimitState = {
    temperatureMin: '',
    temperatureMax: '',
    humidityMin: '',
    humidityMax: '',
    airQualityMin: '',
    airQualityMax: ''
};

export const mapLimitDtoToState = (dto: LimitDTO | null): LimitState => {
    return {
        temperatureMin: getLimitValue(dto, 'tempMin'),
        temperatureMax: getLimitValue(dto, 'tempMax'),
        humidityMin: getLimitValue(dto, 'humMin'),
        humidityMax: getLimitValue(dto, 'humMax'),
        airQualityMin: '',
        airQualityMax: getLimitValue(dto, 'co2Max')
    };
};

export const mapLimitStateToDto = (
    roomId: string,
    limits: LimitState
): LimitDTO => {
    return {
        roomId,
        tempMin: parseOptionalNumber(limits.temperatureMin),
        tempMax: parseOptionalNumber(limits.temperatureMax),
        humMin: parseOptionalNumber(limits.humidityMin),
        humMax: parseOptionalNumber(limits.humidityMax),
        co2Max: parseOptionalNumber(limits.airQualityMax)
    };
};

const getLimitValue = (
    dto: LimitDTO | null,
    key: keyof LimitDTO
): string => {
    const value = dto?.[key];

    if (value === null || value === undefined) {
        return '';
    }

    return String(value);
};

export const parseOptionalNumber = (value: string): number | null => {
    const normalized = value.trim().replace(',', '.');

    if (!normalized || normalized === '-') {
        return null;
    }

    const parsed = Number(normalized);

    if (Number.isNaN(parsed)) {
        return null;
    }

    return parsed;
};