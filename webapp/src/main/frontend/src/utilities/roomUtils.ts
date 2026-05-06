export interface RoomDTO {
    id: string;
    roomNumber?: string | null;
    name?: string | null;
    departmentName?: string | null;
}

export const getRoomDisplayName = (
    room: RoomDTO | undefined,
    fallback: string
): string => {
    if (!room) {
        return fallback;
    }

    return (
        room.roomNumber ||
        room.name ||
        room.departmentName ||
        fallback
    );
};