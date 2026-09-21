package dev.project.statistic.api.service;


import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatisticsService {

    private final AtomicInteger totalBookings = new AtomicInteger(0);
    private final AtomicInteger totalTickets = new AtomicInteger(0);


    public void addBooking() {
        totalBookings.incrementAndGet();
    }

    public void addTickets(int count) {
        totalTickets.addAndGet(count);
    }

    public int getTotalTickets() {
        return totalTickets.get();
    }

    public int getTotalBookings() {
        return totalBookings.get();
    }
}
