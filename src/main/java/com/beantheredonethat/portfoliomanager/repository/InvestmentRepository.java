package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Investment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class InvestmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvestmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Investment> investmentRowMapper = (rs, rowNum) -> {
        Investment i = new Investment();
        i.setInvestmentId(rs.getInt("investment_id"));
        i.setPortfolioId(rs.getInt("portfolio_id"));
        i.setSymbol(rs.getString("symbol"));
        i.setSchemeCode(rs.getString("scheme_code"));
        i.setCompanyName(rs.getString("company_name"));
        i.setExchange(rs.getString("exchange"));
        i.setCurrency(rs.getString("currency"));
        i.setAssetType(rs.getString("asset_type"));
        i.setCustomAssetType(rs.getString("custom_asset_type"));
        i.setQuantity(rs.getBigDecimal("quantity"));
        i.setInvestedAmount(rs.getBigDecimal("invested_amount"));
        i.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        i.setCurrentPrice(rs.getBigDecimal("current_price"));
        i.setCurrentValue(rs.getBigDecimal("current_value"));
        i.setProfitLoss(rs.getBigDecimal("profit_loss"));
        Date d = rs.getDate("purchase_date");
        i.setPurchaseDate(d == null ? null : d.toLocalDate());
        return i;
    };

    public Investment insertInvestment(Investment investment) {
        String sql = "INSERT INTO Investment (portfolio_id, symbol, scheme_code, company_name,exchange, currency, asset_type, custom_asset_type, quantity, invested_amount, purchase_price, current_price, current_value, profit_loss, purchase_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, investment.getPortfolioId());
            ps.setString(2, investment.getSymbol());
            ps.setString(3, investment.getSchemeCode());
            ps.setString(4, investment.getCompanyName());
              ps.setString(5, investment.getExchange());
            ps.setString(6, investment.getCurrency());
            ps.setString(7, investment.getAssetType());
            ps.setString(8, investment.getCustomAssetType());
            ps.setBigDecimal(9, investment.getQuantity());
            ps.setBigDecimal(10, investment.getInvestedAmount());
            ps.setBigDecimal(11, investment.getPurchasePrice());
            ps.setBigDecimal(12, investment.getCurrentPrice());
            ps.setBigDecimal(13, investment.getCurrentValue());
            ps.setBigDecimal(14, investment.getProfitLoss());
            if (investment.getPurchaseDate() != null) {
                ps.setDate(15, Date.valueOf(investment.getPurchaseDate()));
            } else {
                ps.setDate(15, null);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            investment.setInvestmentId(key.intValue());
        }
        return investment;
    }

    public Optional<Investment> findInvestmentById(Integer investmentId) {
        List<Investment> results = jdbcTemplate.query(
                "SELECT * FROM Investment WHERE investment_id = ?", investmentRowMapper, investmentId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Investment> findInvestmentByIdAndCustomerId(Integer investmentId, Integer customerId) {
        String sql = "SELECT i.* FROM Investment i " +
                "JOIN Portfolio p ON i.portfolio_id = p.portfolio_id " +
                "WHERE i.investment_id = ? AND p.customer_id = ?";
        List<Investment> results = jdbcTemplate.query(sql, investmentRowMapper, investmentId, customerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Investment> findAllInvestments() {
        return jdbcTemplate.query("SELECT * FROM Investment", investmentRowMapper);
    }

    public List<Investment> findByPortfolioId(Integer portfolioId) {
        return jdbcTemplate.query("SELECT * FROM Investment WHERE portfolio_id = ?", investmentRowMapper, portfolioId);
    }

    public List<Investment> findByPortfolioIdAndCustomerId(Integer portfolioId, Integer customerId) {
        String sql = "SELECT i.* FROM Investment i " +
                "JOIN Portfolio p ON i.portfolio_id = p.portfolio_id " +
                "WHERE i.portfolio_id = ? AND p.customer_id = ?";
        return jdbcTemplate.query(sql, investmentRowMapper, portfolioId, customerId);
    }

    public List<Investment> findByCustomerId(Integer customerId) {
        String sql = "SELECT i.* FROM Investment i " +
                "JOIN Portfolio p ON i.portfolio_id = p.portfolio_id " +
                "WHERE p.customer_id = ?";
        return jdbcTemplate.query(sql, investmentRowMapper, customerId);
    }

    public int countByCustomerId(Integer customerId) {
        String sql = "SELECT COUNT(*) FROM Investment i " +
                "JOIN Portfolio p ON i.portfolio_id = p.portfolio_id " +
                "WHERE p.customer_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, customerId);
        return count == null ? 0 : count;
    }

    public void updateInvestment(Investment investment) {
        String sql = "UPDATE Investment SET quantity = ?, purchase_price = ?, purchase_date = ?, custom_asset_type = ?, invested_amount = ?, scheme_code = ? WHERE investment_id = ?";
        jdbcTemplate.update(sql,
                investment.getQuantity(),
                investment.getPurchasePrice(),
                investment.getPurchaseDate() == null ? null : Date.valueOf(investment.getPurchaseDate()),
                investment.getCustomAssetType(),
                investment.getInvestedAmount(),
                investment.getSchemeCode(),
                investment.getInvestmentId());
    }

    public void updateMarketValues(Integer investmentId, java.math.BigDecimal currentPrice, java.math.BigDecimal currentValue, java.math.BigDecimal profitLoss) {
        String sql = "UPDATE Investment SET current_price = ?, current_value = ?, profit_loss = ? WHERE investment_id = ?";
        jdbcTemplate.update(sql, currentPrice, currentValue, profitLoss, investmentId);
    }

    public void deleteInvestment(Integer investmentId) {
        jdbcTemplate.update("DELETE FROM Investment WHERE investment_id = ?", investmentId);
    }

    public boolean portfolioExists(Integer portfolioId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Portfolio WHERE portfolio_id = ?", Integer.class, portfolioId);
        return count != null && count > 0;
    }
}

