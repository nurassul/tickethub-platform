package dev.project.statistic.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatisticRepository {

    private final JdbcTemplate jdbcTemplate;


    public void incrementBookings() {
        var res = jdbcTemplate.update("""
                UPDATE statistics
                SET total_bookings = total_bookings + 1
                WHERE id = 1
                """
        );

        if (res != 1) {
            throw new IllegalStateException("Updated row must be only 1");
        }
    }

    public void incrementTickets(int count) {
        var res = jdbcTemplate.update("""
                UPDATE statistics
                SET total_tickets = total_tickets + ?
                WHERE id = 1
                """, count
        );

        if (res != 1) {
            throw new IllegalStateException("Updated row must be only 1");
        }
    }

    public StatisticsSnapshot getStatistics() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT total_bookings, total_tickets
                        FROM statistics 
                        WHERE id = 1
                        """, (rs, rowNum) -> new StatisticsSnapshot(
                        rs.getLong("total_bookings"),
                        rs.getLong("total_tickets")
                )
        );

    }

    public record StatisticsSnapshot(
            long totalBookings,
            long totalTickets
    ) {
    }

}
