package com.example.short_video.service;

import com.example.short_video.dto.request.SendLiveCommentRequest;
import com.example.short_video.dto.request.StartLiveRequest;
import com.example.short_video.dto.response.LiveCommentMessage;
import com.example.short_video.dto.response.LiveStreamResponse;
import com.example.short_video.entity.User;

import java.util.List;

public interface LiveStreamService {
    LiveStreamResponse startLive(StartLiveRequest request, User user);
    LiveStreamResponse endLive(Long liveId, User user);
    List<LiveStreamResponse> getMyStreams(User user);
    LiveStreamResponse getLiveByUser(Long userId);
    List<LiveStreamResponse> getLiveFeed();
    LiveStreamResponse getLiveById(Long liveId, User currentUser);
    LiveCommentMessage sendComment(Long liveId, SendLiveCommentRequest request, User user);
    List<LiveCommentMessage> getComments(Long liveId);
    void incrementViewerCount(Long liveId);
    void decrementViewerCount(Long liveId);
}
