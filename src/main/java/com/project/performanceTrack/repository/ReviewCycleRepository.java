package com.project.performanceTrack.repository;

import com.project.performanceTrack.entity.ReviewCycle;
import com.project.performanceTrack.enums.ReviewCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Integer> {

        //finding review cycles by status
    List<ReviewCycle> findByStatus(ReviewCycleStatus status);

    //finding first active review cycle
    Optional<ReviewCycle> findFirstByStatusOrderByStartDateDesc(ReviewCycleStatus status);

}