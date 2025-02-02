import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { importFiles } from "../../../../actions/nk/importExport";
import { CapabilitiesToolbarButton } from "../../../toolbarComponents/CapabilitiesToolbarButton";
import { getProcessId } from "../../../../reducers/selectors/graph";
import { useTranslation } from "react-i18next";
import Icon from "../../../../assets/img/toolbarButtons/import.svg";
import { ToolbarButtonProps } from "../../types";

type Props = ToolbarButtonProps;

function ImportButton(props: Props) {
    const { disabled } = props;
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const processId = useSelector(getProcessId);

    return (
        <CapabilitiesToolbarButton
            write
            name={t("panels.actions.process-import.button", "import")}
            icon={<Icon />}
            disabled={disabled}
            onDrop={(files) => dispatch(importFiles(processId, files))}
        />
    );
}

export default ImportButton;
