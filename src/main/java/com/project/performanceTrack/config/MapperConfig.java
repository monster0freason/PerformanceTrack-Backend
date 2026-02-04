package com.project.performanceTrack.config;

import com.project.performanceTrack.dto.FeedbackRequest;
import com.project.performanceTrack.dto.FeedbackResponseDTO;
import com.project.performanceTrack.entity.Feedback;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // 1. SET MATCHING STRATEGY TO STRICT
        // This stops ModelMapper from guessing. It will only map if names match exactly.
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // 2. Map Request to Entity (Skip the ID)
        mapper.typeMap(FeedbackRequest.class, Feedback.class).addMappings(m -> {
            m.skip(Feedback::setFeedbackId);
        });

        // 3. Map Entity to ResponseDTO (Flattening)
        mapper.typeMap(Feedback.class, FeedbackResponseDTO.class).addMappings(m -> {
            // Use 'src' to avoid null pointer issues during configuration
            m.map(src -> src.getGivenByUser().getName(), FeedbackResponseDTO::setGiverName);
            m.map(src -> src.getGivenByUser().getUserId(), FeedbackResponseDTO::setGiverId);
            m.map(src -> src.getGoal().getTitle(), FeedbackResponseDTO::setGoalTitle);
            m.map(src -> src.getGoal().getGoalId(), FeedbackResponseDTO::setGoalId);
        });

        return mapper;
    }
}