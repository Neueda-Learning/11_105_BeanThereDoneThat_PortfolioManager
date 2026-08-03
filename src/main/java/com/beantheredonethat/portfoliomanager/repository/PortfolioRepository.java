package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Portfolio;
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
public class PortfolioRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Portfolio> portfolioRowMapper = (rs, rowNum) -> {
        Portfolio p = new Portfolio();
        p.setPortfolioId(rs.getInt("portfolio_id"));
        p.setCustomerId(rs.getInt("customer_id"));
        p.setPortfolioName(rs.getString("portfolio_name"));
        return p;
    };

    public Portfolio save(Portfolio portfolio) {
        if (portfolio.getPortfolioId() == null) {
            String sql = "INSERT INTO Portfolio (customer_id, portfolio_name) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, portfolio.getCustomerId());
                ps.setString(2, portfolio.getPortfolioName());
                return ps;
            }, keyHolder);
            portfolio.setPortfolioId(keyHolder.getKey().intValue());
        } else {
            String sql = "UPDATE Portfolio SET customer_id = ?, portfolio_name = ? WHERE portfolio_id = ?";
            jdbcTemplate.update(sql,
                    portfolio.getCustomerId(),
                    portfolio.getPortfolioName(),
                    portfolio.getPortfolioId());
        }
        return portfolio;
    }

    public Optional<Portfolio> findById(Integer id) {
        List<Portfolio> results = jdbcTemplate.query(
                "SELECT * FROM Portfolio WHERE portfolio_id = ?", portfolioRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Portfolio> findAll() {
        return jdbcTemplate.query("SELECT * FROM Portfolio", portfolioRowMapper);
    }

    public List<Portfolio> findByCustomerId(Integer customerId) {
        return jdbcTemplate.query(
                "SELECT * FROM Portfolio WHERE customer_id = ?", portfolioRowMapper, customerId);
    }

    public void deleteById(Integer id) {
        jdbcTemplate.update("DELETE FROM Portfolio WHERE portfolio_id = ?", id);
    }
}
