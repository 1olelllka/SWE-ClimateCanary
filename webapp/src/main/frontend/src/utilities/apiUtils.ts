export const extractArrayResponse = <T,>(
    responseData: unknown,
    fallback: T[]
): T[] => {
    const maybePaged = responseData as { content?: unknown };

    if (Array.isArray(maybePaged?.content)) {
        return maybePaged.content as T[];
    }

    if (Array.isArray(responseData)) {
        return responseData as T[];
    }

    return fallback;
};

export const getApiErrorMessage = (
    error: unknown,
    fallbackMessage: string
): string => {
    const maybeAxiosError = error as {
        response?: {
            status?: number;
            data?: {
                detail?: string;
                message?: string;
            };
        };
    };

    return (
        maybeAxiosError.response?.data?.detail ||
        maybeAxiosError.response?.data?.message ||
        `${fallbackMessage} Status: ${maybeAxiosError.response?.status || 'unknown'}`
    );
};