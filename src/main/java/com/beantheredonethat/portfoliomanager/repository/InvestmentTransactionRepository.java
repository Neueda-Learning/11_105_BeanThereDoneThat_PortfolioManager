package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
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
public class InvestmentTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvestmentTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<InvestmentTransaction> transactionRowMapper =
            (rs, rowNum) -> {

                InvestmentTransaction transaction =
                        new InvestmentTransaction();

                transaction.setTransactionId(
                        rs.getInt("transaction_id"));

                transaction.setInvestmentId(
                        rs.getInt("investment_id"));

                transaction.setSymbol(
                        rs.getString("symbol"));

                transaction.setCompanyName(
                        rs.getString("company_name"));

                transaction.setAssetType(
                        rs.getString("asset_type"));

                Date date = rs.getDate("transaction_date");

                transaction.setTransactionDate(
                        date == null ? null : date.toLocalDate());

                transaction.setTransactionType(
                        rs.getString("transaction_type"));

                transaction.setQuantity(
                        rs.getBigDecimal("quantity"));

                transaction.setTransactionPrice(
                        rs.getBigDecimal("transaction_price"));

                transaction.setTransactionAmount(
                        rs.getBigDecimal("transaction_amount"));

                return transaction;
            };

    public InvestmentTransaction insertTransaction(
            InvestmentTransaction transaction) {

        String sql =
                "INSERT INTO Investment_Transaction " +
                        "(investment_id, transaction_date, transaction_type, quantity, transaction_price, transaction_amount) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {

            PreparedStatement ps =
                    con.prepareStatement(
                            sql,
                            Statement.RETURN_GENERATED_KEYS);

            ps.setInt(
                    1,
                    transaction.getInvestmentId());

            ps.setDate(
                    2,
                    Date.valueOf(
                            transaction.getTransactionDate()));

            ps.setString(
                    3,
                    transaction.getTransactionType());

            ps.setBigDecimal(
                    4,
                    transaction.getQuantity());

            ps.setBigDecimal(
                    5,
                    transaction.getTransactionPrice());

            ps.setBigDecimal(
                    6,
                    transaction.getTransactionAmount());

            return ps;

        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key != null) {
            transaction.setTransactionId(
                    key.intValue());
        }

        return transaction;
    }

    public Optional<InvestmentTransaction> findById(
            Integer transactionId) {

        String sql =
                "SELECT t.*, i.symbol, i.company_name, i.asset_type " +
                        "FROM Investment_Transaction t " +
                        "JOIN Investment i ON t.investment_id = i.investment_id " +
                        "WHERE t.transaction_id = ?";

        List<InvestmentTransaction> results =
                jdbcTemplate.query(
                        sql,
                        transactionRowMapper,
                        transactionId);

        return results.isEmpty()
                ? Optional.empty()
                : Optional.of(results.get(0));
    }

    public List<InvestmentTransaction> findAllTransactions() {

        String sql =
                "SELECT t.*, i.symbol, i.company_name, i.asset_type " +
                        "FROM Investment_Transaction t " +
                        "JOIN Investment i ON t.investment_id = i.investment_id";

        return jdbcTemplate.query(
                sql,
                transactionRowMapper);
    }

    public List<InvestmentTransaction> findByInvestmentId(
            Integer investmentId) {

        String sql =
                "SELECT t.*, i.symbol, i.company_name, i.asset_type " +
                        "FROM Investment_Transaction t " +
                        "JOIN Investment i ON t.investment_id = i.investment_id " +
                        "WHERE t.investment_id = ?";

        return jdbcTemplate.query(
                sql,
                transactionRowMapper,
                investmentId);
    }

    public void deleteTransaction(
            Integer transactionId) {

        jdbcTemplate.update(
                "DELETE FROM Investment_Transaction WHERE transaction_id = ?",
                transactionId);
    }
}