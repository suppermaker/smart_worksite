package com.xd.smartworksite.qa.mapper;

import com.xd.smartworksite.qa.domain.QaMessage;
import com.xd.smartworksite.qa.domain.QaSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QaMapper {
    int insertSession(QaSession session);

    QaSession selectSessionById(@Param("sessionId") Long sessionId);

    List<QaSession> selectSessions(@Param("projectId") Long projectId,
                                   @Param("accessibleProjectIds") List<Long> accessibleProjectIds,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword);

    int updateSessionTitle(@Param("sessionId") Long sessionId,
                           @Param("title") String title,
                           @Param("updatedBy") Long updatedBy);

    int archiveSession(@Param("sessionId") Long sessionId,
                       @Param("updatedBy") Long updatedBy);

    int insertMessage(QaMessage message);

    int updateMessage(QaMessage message);

    QaMessage selectMessageById(@Param("messageId") Long messageId);

    List<QaMessage> selectMessagesBySessionId(@Param("sessionId") Long sessionId);

    int updateMessageFeedback(@Param("messageId") Long messageId,
                              @Param("feedbackJson") String feedbackJson,
                              @Param("updatedBy") Long updatedBy);
}
