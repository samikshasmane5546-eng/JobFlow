package com.example.jobflow.repository;

import com.example.jobflow.model.FailedJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedJobRepository extends JpaRepository<FailedJob, Long> {
}