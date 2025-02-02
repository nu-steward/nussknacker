import React, { useCallback } from "react";
import { createSelector } from "reselect";
import { useSelector } from "react-redux";
import CommentContent from "./CommentContent";
import Date from "../common/Date";
import { variables } from "../../stylesheets/variables";
import { ProcessCommentsList, RemoveButton } from "./StyledComment";
import { getFeatureSettings, getLoggedUser } from "../../reducers/selectors/settings";
import { ListSeparator } from "../common/ListSeparator";

const getComments = (state) => state.processActivity?.comments || [];
const getCommentSettings = createSelector(getFeatureSettings, (f) => f.commentSettings || {});

interface CommentsListProps {
    deleteComment: (comment) => void;
}

export default function CommentsList({ deleteComment }: CommentsListProps) {
    const loggedUser = useSelector(getLoggedUser);
    const comments = useSelector(getComments);
    const commentSettings = useSelector(getCommentSettings);

    const isLastComment = useCallback((index) => index + 1 === comments.length, [comments.length]);

    return (
        <ProcessCommentsList>
            {comments.map((comment, index) => (
                <div key={comment.id}>
                    <div style={{ width: "100%" }}>
                        <Date date={comment.createDate} />
                        <span style={{ color: variables.commentHeaderColor }}>{`| v${comment.processVersionId} | ${comment.user}`}</span>
                        {comment.user != loggedUser.id ? null : (
                            <RemoveButton className="glyphicon glyphicon-remove" onClick={() => deleteComment(comment)} />
                        )}
                    </div>
                    <CommentContent content={comment.content} commentSettings={commentSettings} />
                    {!isLastComment(index) && <ListSeparator />}
                </div>
            ))}
        </ProcessCommentsList>
    );
}
