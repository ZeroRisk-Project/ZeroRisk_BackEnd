package com.zerorisk.project.domain.comment.service;

import com.zerorisk.project.domain.comment.dto.CommentCreateRequest;
import com.zerorisk.project.domain.comment.dto.CommentResponse;
import com.zerorisk.project.domain.comment.dto.CommentUpdateRequest;
import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.exception.CommentAccessDeniedException;
import com.zerorisk.project.global.exception.CommentNotFoundException;
import com.zerorisk.project.global.exception.PostNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserActivityLogger userActivityLogger;

    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CommentCreateRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findByIdAndIsDeletedFalse(request.parentId())
                    .orElseThrow(CommentNotFoundException::new);
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(request.content())
                .build();

        Comment savedComment = commentRepository.save(comment);
        userActivityLogger.log(userId, "COMMENT_CREATE", "게시글 #" + postId + "에 댓글 작성");

        return CommentResponse.from(savedComment, userId);
    }

    public List<CommentResponse> getComments(Long postId, Long viewerId) {
        List<Comment> comments = commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);

        Map<Long, CommentResponse> responseById = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        for (Comment comment : comments) {
            responseById.put(comment.getId(), CommentResponse.from(comment, viewerId));
        }

        for (Comment comment : comments) {
            CommentResponse response = responseById.get(comment.getId());

            if (comment.isReply()) {
                CommentResponse parentResponse = responseById.get(comment.getParent().getId());
                if (parentResponse != null) {
                    parentResponse.replies().add(response);
                }
            } else {
                roots.add(response);
            }
        }

        return roots;
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(CommentNotFoundException::new);

        if (!comment.isOwner(userId)) {
            throw new CommentAccessDeniedException();
        }

        comment.update(request.content());
        userActivityLogger.log(userId, "COMMENT_UPDATE", "댓글 #" + commentId + " 수정");

        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(CommentNotFoundException::new);

        if (!comment.isOwner(userId)) {
            throw new CommentAccessDeniedException();
        }

        comment.softDelete();
        userActivityLogger.log(userId, "COMMENT_DELETE", "댓글 #" + commentId + " 삭제");
    }
}