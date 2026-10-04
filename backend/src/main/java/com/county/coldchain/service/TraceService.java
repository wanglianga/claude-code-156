package com.county.coldchain.service;

import com.county.coldchain.domain.DomainEnums.Stage;
import com.county.coldchain.domain.entity.TraceEvent;
import com.county.coldchain.repo.TraceEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 追溯链事件记录：每个业务动作都在这里落一个节点。 */
@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceEventRepository traceEventRepository;

    @Transactional
    public TraceEvent log(Long batchId, Long lotId, Long customerId, Stage stage,
                          String title, String detail, String operator) {
        TraceEvent e = new TraceEvent();
        e.setBatchId(batchId);
        e.setLotId(lotId);
        e.setCustomerId(customerId);
        e.setStage(stage);
        e.setTitle(title);
        e.setDetail(detail);
        e.setOperator(operator);
        e.setEventAt(LocalDateTime.now());
        return traceEventRepository.save(e);
    }

    @Transactional
    public TraceEvent log(Long batchId, Stage stage, String title, String detail, String operator) {
        return log(batchId, null, null, stage, title, detail, operator);
    }
}
