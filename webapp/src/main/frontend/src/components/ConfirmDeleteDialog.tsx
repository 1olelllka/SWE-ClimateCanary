import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

interface ConfirmDeleteDialogProps {
    visible: boolean;
    message: string;
    onConfirm: () => void;
    onHide: () => void;
    loading?: boolean;
}

const ConfirmDeleteDialog: React.FC<ConfirmDeleteDialogProps> = ({
    visible,
    message,
    onConfirm,
    onHide,
    loading = false,
}) => (
    <Dialog
        header={
            <span style={{display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
                <i
                    className="pi pi-exclamation-triangle"
                    style={{color: '#ef4444', fontSize: '1.1rem'}}
                />
                {' '}
                Confirm Delete
            </span>
        }
        visible={visible}
        className="admin-form-dialog"
        style={{width: '400px'}}
        onHide={onHide}
        footer={
            <div className="admin-dialog-footer">
                <Button label="Cancel" severity="secondary" outlined onClick={onHide} disabled={loading} />
                <Button label="Delete" icon="pi pi-trash" severity="danger" loading={loading} onClick={onConfirm} />
            </div>
        }
        draggable={false}
    >
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem', padding: '0.5rem 0' }}>
            <i className="pi pi-exclamation-circle" style={{ color: '#ef4444', fontSize: '1.5rem', flexShrink: 0, marginTop: '0.1rem' }} />
            <p style={{ margin: 0, fontSize: '0.9rem', lineHeight: 1.5 }}>{message}</p>
        </div>
    </Dialog>
);

export default ConfirmDeleteDialog;
