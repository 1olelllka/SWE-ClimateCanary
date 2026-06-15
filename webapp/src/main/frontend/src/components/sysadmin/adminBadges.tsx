import React from 'react';

export const mutedValue = (value = 'N/A') => (
    <span style={{ color: '#9e9e9e' }}>{value}</span>
);

export const statusBadge = (status?: string) => {
    const online = status === 'ONLINE';
    return (
        <span style={{
            background: online ? '#4caf50' : '#9e9e9e',
            color: 'white',
            padding: '1px 7px',
            borderRadius: '12px',
            fontSize: '0.72rem',
            fontWeight: 600,
        }}>
            {online ? 'Online' : status ? 'Offline' : 'N/A'}
        </span>
    );
};
