import React from 'react';
import { Dropdown } from 'primereact/dropdown';

import {
    getOptionKey,
    TipConditionOption,
    tipConditionOptions
} from '../utilities/tipManagementUtils';

interface TipConditionSelectProps {
    selectedOptionKey: string;
    onConditionChange: (option: TipConditionOption) => void;
}

export const TipConditionSelect: React.FC<TipConditionSelectProps> = ({
                                                                          selectedOptionKey,
                                                                          onConditionChange
                                                                      }) => {
    const dropdownOptions = tipConditionOptions.map(option => ({
        ...option,
        key: getOptionKey(option)
    }));

    return (
        <div className="tip-management-condition-row">
            <label className="tip-management-condition-label">
                Condition
            </label>

            <Dropdown
                value={selectedOptionKey}
                options={dropdownOptions}
                optionLabel="label"
                optionValue="key"
                onChange={event => {
                    const selectedOption = tipConditionOptions.find(
                        option => getOptionKey(option) === event.value
                    );

                    if (selectedOption) {
                        onConditionChange(selectedOption);
                    }
                }}
                className="tip-management-condition-dropdown"
            />
        </div>
    );
};