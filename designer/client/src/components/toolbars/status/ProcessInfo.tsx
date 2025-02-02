import i18next from "i18next";
import React, { memo } from "react";
import { connect } from "react-redux";
import { SwitchTransition } from "react-transition-group";
import { RootState } from "../../../reducers";
import { getFetchedProcessDetails, getProcessUnsavedNewName, isProcessRenamed } from "../../../reducers/selectors/graph";
import { getProcessState } from "../../../reducers/selectors/scenarioState";
import { getCustomActions } from "../../../reducers/selectors/settings";
import { UnknownRecord } from "../../../types/common";
import { CssFade } from "../../CssFade";
import ProcessStateIcon from "../../Process/ProcessStateIcon";
import { ToolbarWrapper } from "../../toolbarComponents/toolbarWrapper/ToolbarWrapper";
import { DefaultToolbarPanel, ToolbarPanelProps } from "../../toolbarComponents/DefaultToolbarPanel";
import { ToolbarButtons } from "../../toolbarComponents/toolbarButtons";
import { ActionButton } from "../../toolbarSettings/buttons";
import ProcessStateUtils from "../../Process/ProcessStateUtils";
import {
    PanelProcessInfo,
    PanelProcessInfoIcon,
    ProcessInfoDescription,
    ProcessInfoText,
    ProcessName,
    ProcessRename,
} from "./ProcessInfoComponents";

type State = UnknownRecord;

class ProcessInfo extends React.Component<ToolbarPanelProps & StateProps, State> {
    static defaultProps = {
        isStateLoaded: false,
    };

    render() {
        const { process, processState, customActions, isRenamePending, unsavedNewName } = this.props;
        const description = ProcessStateUtils.getStateDescription(process, processState);
        const transitionKey = ProcessStateUtils.getTransitionKey(process, processState);
        // TODO: better styling of process info toolbar in case of many custom actions
        return (
            <ToolbarWrapper title={i18next.t("panels.status.title", "Status")} id={this.props.id}>
                <SwitchTransition>
                    <CssFade key={transitionKey}>
                        <PanelProcessInfo>
                            <PanelProcessInfoIcon>
                                <ProcessStateIcon process={process} processState={processState} />
                            </PanelProcessInfoIcon>
                            <ProcessInfoText>
                                {isRenamePending ? (
                                    <ProcessRename title={process.name}>{unsavedNewName}*</ProcessRename>
                                ) : (
                                    <ProcessName>{process.name}</ProcessName>
                                )}
                                <ProcessInfoDescription>{description}</ProcessInfoDescription>
                            </ProcessInfoText>
                        </PanelProcessInfo>
                    </CssFade>
                </SwitchTransition>
                <ToolbarButtons variant={this.props.buttonsVariant}>
                    {this.props.children}
                    {
                        //TODO: to be replaced by toolbar config
                        customActions.map((action) => (
                            <ActionButton name={action.name} key={action.name} />
                        ))
                    }
                </ToolbarButtons>
            </ToolbarWrapper>
        );
    }
}

const mapState = (state: RootState) => ({
    process: getFetchedProcessDetails(state),
    isRenamePending: isProcessRenamed(state),
    unsavedNewName: getProcessUnsavedNewName(state),
    processState: getProcessState(state),
    customActions: getCustomActions(state),
});

type StateProps = ReturnType<typeof mapState>;

export default connect(mapState)(memo(ProcessInfo)) as typeof DefaultToolbarPanel;
