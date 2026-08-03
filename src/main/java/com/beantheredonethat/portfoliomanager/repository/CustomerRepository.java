package com.beantheredonethat.portfoliomanager.repository;

import com.beantheredonethat.portfoliomanager.entity.Customer;
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
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Customer> customerRowMapper = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("customer_id"));
        c.setCustomerName(rs.getString("customer_name"));
        c.setUsername(rs.getString("username"));
        c.setPasswordHash(rs.getString("password_hash"));
        c.setEmail(rs.getString("email"));
        c.setPhoneNumber(rs.getString("phone_number"));
        return c;
    };

    public Customer save(Customer customer) {
        if (customer.getCustomerId() == null) {
            String sql = "INSERT INTO Customer (customer_name, username, password_hash, email, phone_number) VALUES (?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, customer.getCustomerName());
                ps.setString(2, customer.getUsername());
                ps.setString(3, customer.getPasswordHash());
                ps.setString(4, customer.getEmail());
                ps.setString(5, customer.getPhoneNumber());
                return ps;
            }, keyHolder);
            customer.setCustomerId(keyHolder.getKey().intValue());
        } else {
            String sql = "UPDATE Customer SET customer_name = ?, username = ?, password_hash = ?, email = ?, phone_number = ? WHERE customer_id = ?";
            jdbcTemplate.update(sql,
                    customer.getCustomerName(),
                    customer.getUsername(),
                    customer.getPasswordHash(),
                    customer.getEmail(),
                    customer.getPhoneNumber(),
                    customer.getCustomerId());
        }
        return customer;
    }

    public Optional<Customer> findById(Integer id) {
        List<Customer> results = jdbcTemplate.query(
                "SELECT * FROM Customer WHERE customer_id = ?", customerRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Customer> findAll() {
        return jdbcTemplate.query("SELECT * FROM Customer", customerRowMapper);
    }

    public Optional<Customer> findByUsername(String username) {
        List<Customer> results = jdbcTemplate.query(
                "SELECT * FROM Customer WHERE username = ?", customerRowMapper, username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Customer> findByEmail(String email) {
        List<Customer> results = jdbcTemplate.query(
                "SELECT * FROM Customer WHERE email = ?", customerRowMapper, email);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Customer WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Customer WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsById(Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Customer WHERE customer_id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void deleteById(Integer id) {
        jdbcTemplate.update("DELETE FROM Customer WHERE customer_id = ?", id);
    }
}