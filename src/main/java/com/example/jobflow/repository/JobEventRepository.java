package com.example.jobflow.repository;

import com.example.jobflow.model.JobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobEventRepository extends JpaRepository<JobEvent, Long> {

    List<JobEvent> findByJobIdOrderByCreatedAtAsc(Long jobId);
}