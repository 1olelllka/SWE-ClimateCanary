export type ViolationType = 'OVER' | 'UNDER';
export type ViolatedSensor = 'TEMPERATURE' | 'AIR' | 'HUMIDITY';
export type WarningStatus = 'GREEN' | 'YELLOW' | 'RED';

export interface TipDTO {
    id: string;
    violationStatus: WarningStatus;
    violationType: ViolationType;
    violatedSensor: ViolatedSensor;
    message: string;
}

export interface TipCreateDTO {
    violationType: ViolationType;
    violatedSensor: ViolatedSensor;
    violationStatus: WarningStatus;
    message: string;
}

export interface TipPatchDTO {
    message: string;
}

export interface TipConditionOption {
    label: string;
    violationType: ViolationType;
    violatedSensor: ViolatedSensor;
    violationStatus: WarningStatus;
    defaultMessage: string;
}

export const tipConditionOptions: TipConditionOption[] = [
    // CO₂ too high
    { label: 'CO₂ slightly elevated', violationType: 'OVER', violatedSensor: 'AIR', violationStatus: 'GREEN',  defaultMessage: 'Open a window briefly.' },
    { label: 'CO₂ moderately high',   violationType: 'OVER', violatedSensor: 'AIR', violationStatus: 'YELLOW', defaultMessage: 'Open windows to ventilate.' },
    { label: 'CO₂ critically high',   violationType: 'OVER', violatedSensor: 'AIR', violationStatus: 'RED',    defaultMessage: 'Open all windows immediately.' },

    // Temperature too high
    { label: 'Temperature slightly high', violationType: 'OVER', violatedSensor: 'TEMPERATURE', violationStatus: 'GREEN',  defaultMessage: 'Consider opening a window.' },
    { label: 'Temperature moderately high', violationType: 'OVER', violatedSensor: 'TEMPERATURE', violationStatus: 'YELLOW', defaultMessage: 'Open windows.' },
    { label: 'Temperature critically high', violationType: 'OVER', violatedSensor: 'TEMPERATURE', violationStatus: 'RED',    defaultMessage: 'Activate air conditioning.' },

    // Temperature too low
    { label: 'Temperature slightly low',   violationType: 'UNDER', violatedSensor: 'TEMPERATURE', violationStatus: 'GREEN',  defaultMessage: 'Consider closing windows.' },
    { label: 'Temperature moderately low', violationType: 'UNDER', violatedSensor: 'TEMPERATURE', violationStatus: 'YELLOW', defaultMessage: 'Close windows, increase heating.' },
    { label: 'Temperature critically low', violationType: 'UNDER', violatedSensor: 'TEMPERATURE', violationStatus: 'RED',    defaultMessage: 'Turn on heating immediately.' },

    // Humidity too high
    { label: 'Humidity slightly high',   violationType: 'OVER', violatedSensor: 'HUMIDITY', violationStatus: 'GREEN',  defaultMessage: 'Improve air circulation.' },
    { label: 'Humidity moderately high', violationType: 'OVER', violatedSensor: 'HUMIDITY', violationStatus: 'YELLOW', defaultMessage: 'Use a dehumidifier or open windows.' },
    { label: 'Humidity critically high', violationType: 'OVER', violatedSensor: 'HUMIDITY', violationStatus: 'RED',    defaultMessage: 'Ventilate the room.' },

    // Humidity too low
    { label: 'Humidity slightly low',   violationType: 'UNDER', violatedSensor: 'HUMIDITY', violationStatus: 'GREEN',  defaultMessage: 'Consider using a humidifier.' },
    { label: 'Humidity moderately low', violationType: 'UNDER', violatedSensor: 'HUMIDITY', violationStatus: 'YELLOW', defaultMessage: 'Use a humidifier.' },
    { label: 'Humidity critically low', violationType: 'UNDER', violatedSensor: 'HUMIDITY', violationStatus: 'RED',    defaultMessage: 'Use a humidifier immediately.' },
];

export const getTipConditionKey = (
    violationType: ViolationType,
    violatedSensor: ViolatedSensor,
    violationStatus: WarningStatus
): string => {
    return `${violationStatus}:${violatedSensor}:${violationType}`;
};

export const getOptionKey = (option: TipConditionOption): string => {
    return getTipConditionKey(
        option.violationType,
        option.violatedSensor,
        option.violationStatus
    );
};

export const getTipKey = (tip: TipDTO): string => {
    return getTipConditionKey(
        tip.violationType,
        tip.violatedSensor,
        tip.violationStatus
    );
};

export const buildCreateDTO = (
    option: TipConditionOption,
    message: string
): TipCreateDTO => {
    return {
        violationType: option.violationType,
        violatedSensor: option.violatedSensor,
        violationStatus: option.violationStatus,
        message
    };
};