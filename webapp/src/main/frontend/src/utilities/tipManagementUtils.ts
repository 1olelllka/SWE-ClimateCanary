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
    {
        label: 'CO2 > Max',
        violationType: 'OVER',
        violatedSensor: 'AIR',
        violationStatus: 'RED',
        defaultMessage: 'Please open Windows for 5 Minutes.'
    },
    {
        label: 'CO2 slightly high',
        violationType: 'OVER',
        violatedSensor: 'AIR',
        violationStatus: 'YELLOW',
        defaultMessage: 'Please ventilate the room soon.'
    },
    {
        label: 'Temperature > Max',
        violationType: 'OVER',
        violatedSensor: 'TEMPERATURE',
        violationStatus: 'RED',
        defaultMessage: 'Please lower the room temperature or ventilate the room.'
    },
    {
        label: 'Temperature < Min',
        violationType: 'UNDER',
        violatedSensor: 'TEMPERATURE',
        violationStatus: 'RED',
        defaultMessage: 'Please increase the room temperature.'
    },
    {
        label: 'Humidity > Max',
        violationType: 'OVER',
        violatedSensor: 'HUMIDITY',
        violationStatus: 'RED',
        defaultMessage: 'Please ventilate the room to reduce humidity.'
    },
    {
        label: 'Humidity < Min',
        violationType: 'UNDER',
        violatedSensor: 'HUMIDITY',
        violationStatus: 'RED',
        defaultMessage: 'Please increase humidity or avoid excessive heating.'
    }
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