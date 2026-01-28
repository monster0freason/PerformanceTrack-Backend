package com.project.performanceTrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;



@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId; // Unique identifier for the generated report file


    @Column(nullable = false, length = 50)
    private String scope;


    @Column(columnDefinition = "TEXT")
    private String metrics;


    @Column(length = 20)
    private String format;


    @ManyToOne
    @JoinColumn(name = "generated_by")
    private User generatedBy;


    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;


    @Column(name = "file_path", length = 500)
    private String filePath;
}