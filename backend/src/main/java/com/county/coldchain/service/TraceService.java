package com.county.coldchain.service;

import com.county.coldchain.domain.Batch;
import com.county.coldchain.domain.Domain;
import com.county.coldchain.domain.TraceEvent;
import com.county.coldchain.repo.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 批次链事件流水：所有业务动作统一经此落链，保证监管追溯完整 */
@Service
public class TraceService {

    private final EventRepo eventRepo;
    private final BatchRepo batchRepo;

    public TraceService(EventRepo eventRepo, BatchRepo batchRepo) {
        this.eventRepo = eventRepo;
        this.batchRepo = batchRepo;
    }

    public TraceEvent record(Batch batch, Domain.EventType type, String detail, String operator) {
        int seq = batchRepo.maxSeq(batch.getId()) + 1;
        TraceEvent event = new TraceEvent(batch, seq, type, detail, operator, LocalDateTime.now());
        return eventRepo.save(event);
    }
}
