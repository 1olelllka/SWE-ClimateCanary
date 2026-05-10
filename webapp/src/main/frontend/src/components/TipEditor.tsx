import React from 'react';
import { InputTextarea } from 'primereact/inputtextarea';

interface TipEditorProps {
    value: string;
    onChange: (value: string) => void;
}

export const TipEditor: React.FC<TipEditorProps> = ({
                                                        value,
                                                        onChange
                                                    }) => {
    return (
        <InputTextarea
            value={value}
            onChange={event => onChange(event.target.value)}
            rows={5}
            autoResize={false}
            className="tip-management-textarea"
        />
    );
};