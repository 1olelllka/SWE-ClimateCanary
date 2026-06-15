import { RaspberryDTO } from '../../generated-skeleton-api';

export interface RaspberryRoomRef {
    roomId?: string;
    roomName?: string;
}

export type RaspberryDTOReal = Omit<RaspberryDTO, 'roomId' | 'roomNumber'> & {
    room?: RaspberryRoomRef;
};

export interface UserRoleSummary {
    id: string;
    name: string;
}

export interface UserRoomSummary {
    id: string;
    departmentName: string;
    roomNumber: string;
}

export interface FullUser {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: UserRoleSummary[];
    myRoom: UserRoomSummary | null;
}

export interface SelectOption {
    label: string;
    value: string;
}

export interface ConfirmDeleteState {
    message: string;
    onConfirm: () => void;
}
