import React, { useEffect, useMemo, useState } from 'react';
import globalAxios from 'axios';
import { Button } from 'primereact/button';

import { TipConditionSelect } from './TipConditionSelect';
import { TipEditor } from './TipEditor';

import {
    buildCreateDTO,
    getOptionKey,
    getTipKey,
    TipConditionOption,
    TipDTO,
    tipConditionOptions
} from '../utilities/tipManagementUtils';

export const TipManagementForm: React.FC = () => {
    const [selectedOption, setSelectedOption] = useState<TipConditionOption>(
        tipConditionOptions[0]
    );

    const [tips, setTips] = useState<TipDTO[]>([]);
    const [messagesByCondition, setMessagesByCondition] =
        useState<Record<string, string>>({});

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    const selectedOptionKey = getOptionKey(selectedOption);

    const selectedTip = useMemo(() => {
        return tips.find(tip => getTipKey(tip) === selectedOptionKey);
    }, [tips, selectedOptionKey]);

    const currentMessage =
        messagesByCondition[selectedOptionKey] ?? selectedOption.defaultMessage;

    useEffect(() => {
        setLoading(true);
        setError(null);

        globalAxios.get<TipDTO[]>('/api/tips')
            .then(response => {
                const loadedTips = Array.isArray(response.data)
                    ? response.data
                    : [];

                setTips(loadedTips);

                const nextMessages: Record<string, string> = {};

                tipConditionOptions.forEach(option => {
                    nextMessages[getOptionKey(option)] = option.defaultMessage;
                });

                loadedTips.forEach(tip => {
                    nextMessages[getTipKey(tip)] = tip.message;
                });

                setMessagesByCondition(nextMessages);
            })
            .catch(error => {
                console.error('Could not load tips', error.response?.data || error);
                setError('Tips could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    const handleConditionChange = (option: TipConditionOption) => {
        setSelectedOption(option);
        setSuccess(false);
        setError(null);
    };

    const handleMessageChange = (message: string) => {
        setMessagesByCondition(previous => ({
            ...previous,
            [selectedOptionKey]: message
        }));

        setSuccess(false);
        setError(null);
    };

    const handleSave = () => {
        if (!currentMessage.trim()) {
            setError('Message must not be empty.');
            return;
        }

        setSaving(true);
        setError(null);
        setSuccess(false);

        const request = selectedTip
            ? globalAxios.patch<TipDTO>(
                `/api/tips/${selectedTip.id}`,
                { message: currentMessage }
            )
            : globalAxios.post<TipDTO>(
                '/api/tips',
                buildCreateDTO(selectedOption, currentMessage)
            );

        request
            .then(response => {
                const savedTip = response.data;

                setTips(previousTips => {
                    const exists = previousTips.some(tip => tip.id === savedTip.id);

                    if (exists) {
                        return previousTips.map(tip =>
                            tip.id === savedTip.id ? savedTip : tip
                        );
                    }

                    return [...previousTips, savedTip];
                });

                setMessagesByCondition(previous => ({
                    ...previous,
                    [getTipKey(savedTip)]: savedTip.message
                }));

                setSuccess(true);
            })
            .catch(error => {
                console.error('Could not save tip', error.response?.data || error);
                setError('Tip could not be saved.');
            })
            .finally(() => {
                setSaving(false);
            });
    };

    return (
        <section className="tip-management-card">
            <h2 className="tip-management-section-title">
                Tips
            </h2>

            <TipConditionSelect
                selectedOptionKey={selectedOptionKey}
                onConditionChange={handleConditionChange}
            />

            <TipEditor
                value={currentMessage}
                onChange={handleMessageChange}
            />

            <Button
                label={saving ? 'Saving...' : 'Save Tips'}
                className="tip-management-save-button"
                onClick={handleSave}
                disabled={loading || saving}
            />

            {loading && (
                <p className="tip-management-info-message">
                    Loading tips...
                </p>
            )}

            {error && (
                <p className="tip-management-error-message">
                    {error}
                </p>
            )}

            {success && (
                <p className="tip-management-success-message">
                    Tip saved successfully.
                </p>
            )}
        </section>
    );
};