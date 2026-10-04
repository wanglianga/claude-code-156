package com.county.coldchain.service;

import com.county.coldchain.domain.DomainEnums.AlertLevel;
import com.county.coldchain.domain.DomainEnums.AlertType;
import com.county.coldchain.domain.entity.Alert;
import com.county.coldchain.repo.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 风险告警：题目中十类异常场景的统一入口。 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional
    public Alert raise(AlertType type, AlertLevel level, Long batchId, Long zoneId,
                       String title, String detail, String raisedBy) {
        Alert a = new Alert();
        a.setType(type);
        a.setLevel(level);
        a.setBatchId(batchId);
        a.setZoneId(zoneId);
        a.setTitle(title);
        a.setDetail(detail);
        a.setRaisedBy(raisedBy);
        return alertRepository.save(a);
    }

    @Transactional
    public Alert resolve(Long id, String resolution) {
        Alert a = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + id));
        a.setResolved(true);
        a.setResolution(resolution);
        return alertRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<Alert> openAlerts() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Alert> batchAlerts(Long batchId) {
        return alertRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
    }
}
