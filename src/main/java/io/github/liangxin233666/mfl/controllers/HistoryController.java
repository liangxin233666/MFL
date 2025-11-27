package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import io.github.liangxin233666.mfl.repositories.projections.HistorySimpleView;
import io.github.liangxin233666.mfl.services.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final UserRepository userRepository;

    public HistoryController(HistoryService historyService, UserRepository userRepository) {
        this.historyService = historyService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getHistory(
            @PageableDefault(size = 30) Pageable pageable, // 默认每页30
            @AuthenticationPrincipal UserDetails currentUserDetails) {

        Long userId = Long.valueOf(currentUserDetails.getUsername());
        User user = userRepository.getReferenceById(userId);

        Page<HistorySimpleView> history = historyService.getUserHistory(user, pageable);

        // 返回包含分页信息的完整结构
        // history.getContent() 里面的JSON将极其干净，没有不需要的字段
        return ResponseEntity.ok(history);
    }
}