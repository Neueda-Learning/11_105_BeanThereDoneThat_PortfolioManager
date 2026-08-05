package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateTransactionRequest;
import com.beantheredonethat.portfoliomanager.dto.TransactionResponse;
import com.beantheredonethat.portfoliomanager.entity.Investment;
import com.beantheredonethat.portfoliomanager.entity.InvestmentTransaction;
import com.beantheredonethat.portfoliomanager.exception.InvestmentNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.TransactionNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.InvestmentRepository;
import com.beantheredonethat.portfoliomanager.repository.InvestmentTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentTransactionService {

    private final InvestmentTransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;

    public InvestmentTransactionService(
            InvestmentTransactionRepository transactionRepository,
            InvestmentRepository investmentRepository) {

        this.transactionRepository = transactionRepository;
        this.investmentRepository = investmentRepository;
    }

    public TransactionResponse createTransaction(
            CreateTransactionRequest request) {

        Investment investment = investmentRepository
                .findInvestmentById(request.getInvestmentId())
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found with ID: "
                                        + request.getInvestmentId()));

        BigDecimal transactionAmount =
                request.getQuantity()
                        .multiply(request.getTransactionPrice())
                        .setScale(2, RoundingMode.HALF_UP);

        String type = request.getTransactionType().toUpperCase();

        if ("BUY".equals(type)) {

            investment.setQuantity(
                    investment.getQuantity()
                            .add(request.getQuantity()));

        } else if ("SELL".equals(type)) {

            if (investment.getQuantity()
                    .compareTo(request.getQuantity()) < 0) {

                throw new RuntimeException(
                        "Insufficient quantity available");
            }

            investment.setQuantity(
                    investment.getQuantity()
                            .subtract(request.getQuantity()));

        } else {

            throw new RuntimeException(
                    "Transaction type must be BUY or SELL");
        }

        if (investment.getPurchasePrice() != null) {

            investment.setInvestedAmount(
                    investment.getQuantity()
                            .multiply(investment.getPurchasePrice())
                            .setScale(2, RoundingMode.HALF_UP));
        }

        investmentRepository.updateInvestment(investment);

        InvestmentTransaction transaction =
                new InvestmentTransaction();

        transaction.setInvestmentId(
                request.getInvestmentId());

        transaction.setTransactionDate(
                request.getTransactionDate());

        transaction.setTransactionType(
                type);

        transaction.setQuantity(
                request.getQuantity());

        transaction.setTransactionPrice(
                request.getTransactionPrice());

        transaction.setTransactionAmount(
                transactionAmount);

        InvestmentTransaction saved =
                transactionRepository.insertTransaction(
                        transaction);

        saved.setSymbol(investment.getSymbol());
        saved.setCompanyName(investment.getCompanyName());
        saved.setAssetType(investment.getAssetType());

        return new TransactionResponse(saved);
    }

    public TransactionResponse getTransactionById(
            Integer id) {

        InvestmentTransaction transaction =
                transactionRepository.findById(id)
                        .orElseThrow(() ->
                                new TransactionNotFoundException(
                                        "Transaction not found with ID: "
                                                + id));

        return new TransactionResponse(transaction);
    }

    public List<TransactionResponse> getAllTransactions() {

        return transactionRepository
                .findAllTransactions()
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsByInvestment(
            Integer investmentId) {

        investmentRepository.findInvestmentById(investmentId)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found with ID: "
                                        + investmentId));

        return transactionRepository
                .findByInvestmentId(investmentId)
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public void deleteTransaction(Integer id) {

        transactionRepository.findById(id)
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                "Transaction not found with ID: "
                                        + id));

        transactionRepository.deleteTransaction(id);
    }
}