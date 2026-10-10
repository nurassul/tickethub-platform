package dev.project.statistic.api.service;


import dev.project.statistic.repository.ProcessedEventRepository;
import dev.project.statistic.repository.StatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final String CONSUMER_NAME = "statistic-event-v1";

    private final ProcessedEventRepository processedEventRepository;
    private final StatisticRepository statisticRepository;


    @Transactional
    public boolean addBooking(UUID eventId) {
        if (!processedEventRepository.tryRegister(CONSUMER_NAME, eventId)) {
            return false;
        }

        statisticRepository.incrementBookings();
        return true;
    }

    @Transactional
    public boolean addTickets(UUID eventId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Ticket count must be positive");
        }

        if (!processedEventRepository.tryRegister(CONSUMER_NAME, eventId)) {
            return false;
        }

        statisticRepository.incrementTickets(count);
        return true;
    }

    public StatisticRepository.StatisticsSnapshot getStatistics() {
        return statisticRepository.getStatistics();
    }



}
