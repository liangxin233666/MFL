package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.ReadingHistory;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.ReadingHistoryRepository;
import io.github.liangxin233666.mfl.repositories.projections.HistorySimpleView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class HistoryService {

    private final ReadingHistoryRepository historyRepository;

    public HistoryService(ReadingHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * 记录历史 (完全异步，不影响API响应速度)
     * 逻辑：如果存在就更新时间，如果不存在就插入。然后修剪多余记录。
     */
    @Async
    @Transactional
    public void recordHistoryAsync(User user, Article article) {
        // 1. 更新或插入
        ReadingHistory history = historyRepository.findByUserAndArticle(user, article)
                .orElseGet(() -> {
                    ReadingHistory h = new ReadingHistory();
                    h.setUser(user);
                    h.setArticle(article);
                    return h;
                });

        history.setViewedAt(OffsetDateTime.now());
        historyRepository.save(history);

        // 2. 修剪 (保留最新的30条)
        // 这个操作也在异步线程里，所以慢一点也没关系
        historyRepository.keepMostRecentRecords(user.getId(), 30);
    }

    /**
     * 获取历史列表
     * 直接返回投影视图
     */
    @Transactional(readOnly = true)
    public Page<HistorySimpleView> getUserHistory(User user, Pageable pageable) {
        return historyRepository.findByUserOrderByViewedAtDesc(user, pageable);
    }
}