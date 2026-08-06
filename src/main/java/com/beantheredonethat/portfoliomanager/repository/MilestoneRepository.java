package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Milestone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class MilestoneRepository {

    private final JdbcTemplate jdbcTemplate;

    public MilestoneRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Milestone> milestoneRowMapper = (rs, rowNum) -> {
        Milestone milestone = new Milestone();
        milestone.setMilestoneId(rs.getInt("milestone_id"));
        milestone.setCustomerId(rs.getInt("customer_id"));
        milestone.setItem(rs.getString("item"));
        milestone.setPrice(rs.getBigDecimal("price"));
        milestone.setImageUrl(rs.getString("image_url"));
        milestone.setDisplayOrder(rs.getInt("display_order"));
        return milestone;
    };

    public Milestone save(Milestone milestone) {
        if (milestone.getMilestoneId() == null) {
            String sql = "INSERT INTO Milestone (customer_id, item, price, image_url, display_order) VALUES (?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, milestone.getCustomerId());
                ps.setString(2, milestone.getItem());
                ps.setBigDecimal(3, milestone.getPrice());
                ps.setString(4, milestone.getImageUrl());
                ps.setInt(5, milestone.getDisplayOrder());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                milestone.setMilestoneId(key.intValue());
            }
            return milestone;
        }

        String sql = "UPDATE Milestone SET item = ?, price = ?, image_url = ?, display_order = ? WHERE milestone_id = ? AND customer_id = ?";
        jdbcTemplate.update(sql,
                milestone.getItem(),
                milestone.getPrice(),
            milestone.getImageUrl(),
            milestone.getDisplayOrder(),
                milestone.getMilestoneId(),
                milestone.getCustomerId());

        return milestone;
    }

    public List<Milestone> findByCustomerId(Integer customerId) {
        String sql = "SELECT * FROM Milestone WHERE customer_id = ? ORDER BY display_order ASC, milestone_id ASC";
        return jdbcTemplate.query(sql, milestoneRowMapper, customerId);
    }

    public Optional<Milestone> findByIdAndCustomerId(Integer milestoneId, Integer customerId) {
        String sql = "SELECT * FROM Milestone WHERE milestone_id = ? AND customer_id = ?";
        List<Milestone> milestones = jdbcTemplate.query(sql, milestoneRowMapper, milestoneId, customerId);
        if (milestones.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(milestones.get(0));
    }

    public int getNextDisplayOrder(Integer customerId) {
        String sql = "SELECT COALESCE(MAX(display_order), 0) + 1 FROM Milestone WHERE customer_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, customerId);
        return next == null ? 1 : next;
    }

    public void updateDisplayOrder(Integer customerId, List<Integer> milestoneIdsInOrder) {
        if (milestoneIdsInOrder == null || milestoneIdsInOrder.isEmpty()) {
            return;
        }

        String sql = "UPDATE Milestone SET display_order = ? WHERE milestone_id = ? AND customer_id = ?";
        for (int i = 0; i < milestoneIdsInOrder.size(); i += 1) {
            jdbcTemplate.update(sql, i + 1, milestoneIdsInOrder.get(i), customerId);
        }
    }

    public void reorderSequentially(Integer customerId) {
        List<Milestone> milestones = findByCustomerId(customerId);
        if (milestones.isEmpty()) {
            return;
        }

        List<Integer> ids = milestones.stream().map(Milestone::getMilestoneId).toList();
        updateDisplayOrder(customerId, ids);
    }

    public void deleteByIdAndCustomerId(Integer milestoneId, Integer customerId) {
        String sql = "DELETE FROM Milestone WHERE milestone_id = ? AND customer_id = ?";
        jdbcTemplate.update(sql, milestoneId, customerId);
    }

    public java.math.BigDecimal getTotalProfitByCustomerId(Integer customerId) {
        String sql = "SELECT COALESCE(SUM(i.profit_loss), 0) " +
                "FROM Investment i " +
                "JOIN Portfolio p ON i.portfolio_id = p.portfolio_id " +
                "WHERE p.customer_id = ?";

        java.math.BigDecimal total = jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, customerId);
        return total == null ? java.math.BigDecimal.ZERO : total;
    }
}
