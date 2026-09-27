package com.example.jobflow.service;

import com.example.jobflow.model.JobEvent;
import com.example.jobflow.repository.JobEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobEventService {

    private final JobEventRepository jobEventRepository;

    public void log(Long jobId, String eventType, String detail) {
        JobEvent event = new JobEvent();
        event.setJobId(jobId);
        event.setEventType(eventType);
        event.setDetail(detail);
        jobEventRepository.save(event);
    }
}