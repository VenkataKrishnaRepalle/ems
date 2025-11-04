package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.SalaryDao;
import com.learning.emsmybatisliquibase.entity.Salary;
import com.learning.emsmybatisliquibase.entity.audit.SalaryAudit;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.mapper.SalaryMapper;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private final EmployeeService employeeService;

    private final SalaryMapper salaryMapper;

    private final SalaryDao salaryDao;

    /*
     * @param type can be "total" or "fixed"
     * @param salary amount
     * @return Map<String, Object> containing the breakdown of the salary
     */
    @Override
    public Salary process(String type, String salary, String joiningBonus) {
        Salary result;

        try {
            var total = BigDecimal.ZERO;
            var fixed = BigDecimal.ZERO;
            var variable = BigDecimal.ZERO;

            if (type.equals("total")) {
                total = new BigDecimal(salary);
                fixed = total.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
                variable = total.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            } else if (type.equals("fixed")) {
                fixed = new BigDecimal(salary).setScale(2, RoundingMode.HALF_UP);
                variable = fixed.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                total = fixed.add(variable).setScale(2, RoundingMode.HALF_UP);
            }

            var basic = fixed.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
            var pf = fixed.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
            var hra = fixed.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            var flex = fixed.multiply(new BigDecimal("0.28")).setScale(2, RoundingMode.HALF_UP);
            /*
            Basic - 40%
            PF - 12%
            HRA - 20%
            Flex - 28%
             */
            result = Salary.builder()
                    .total(total)
                    .fixed(fixed)
                    .joiningBonus(joiningBonus == null || joiningBonus.isEmpty() ? BigDecimal.ZERO : new BigDecimal(joiningBonus))
                    .variable(variable)
                    .basic(basic)
                    .pf(pf)
                    .hra(hra)
                    .flex(flex)
                    .build();
        } catch (NumberFormatException e) {
            throw new InvalidInputException("ERROR_PROCESSING_SALARY", "Invalid to process salary: " + salary);
        }
        return result;
    }

    @Override
    @Transactional
    public Salary insert(Salary salary) {
        employeeService.getById(salary.getEmployeeUuid());
        salary.setCreatedBy(getAuthentication());
        salary.setCreatedTime(Instant.now());
        salary.setUpdatedBy(getAuthentication());
        salary.setUpdatedTime(Instant.now());

        var salaryAudit = salaryMapper.salaryToSalaryAudit(salary);
        insertSalary(salary);
        insertAuditSalary(salaryAudit);

        return salary;
    }

    private void insertSalary(Salary salary) {
        try {
            if (0 == salaryDao.insert(salary)) {
                throw new IntegrityException("SALARY_INSERT_FAILED", "Failed to insert salary");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("SALARY_INSERT_FAILED", exception.getCause().getMessage());
        }
    }

    private void insertAuditSalary(SalaryAudit salaryAudit) {
        try {
            if (0 == salaryDao.insertAudit(salaryAudit)) {
                throw new IntegrityException("SALARY_AUDIT_INSERT_FAILED", "Failed to insert salary audit");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("SALARY_AUDIT_INSERT_FAILED", exception.getCause().getMessage());
        }
    }

    @Override
    public Map<String, Object> get(UUID employeeUuid) {
        var salary = salaryDao.get(employeeUuid);
        var audits = salaryDao.getAudit(employeeUuid)
                .stream()
                .sorted((a, b) -> b.getUpdatedTime().compareTo(a.getUpdatedTime()))
                .toList();

        return Map.of(
                "currentSalary", salary,
                "salaryHistory", audits
        );
    }

    @Override
    @Transactional
    public Salary update(UUID employeeUuid, String percentage) {
        employeeService.getById(employeeUuid);
        var oldSalary = salaryDao.get(employeeUuid);
        var newPercentage = new BigDecimal(percentage);
        var percentageMultiplier = newPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        var increase = oldSalary.getTotal().multiply(percentageMultiplier);
        var newTotal = oldSalary.getTotal().add(increase)
                .setScale(2, RoundingMode.HALF_UP);
        var newSalary = process("total", String.valueOf(newTotal), "");
        newSalary.setEmployeeUuid(employeeUuid);
        newSalary.setUpdatedBy(getAuthentication());
        newSalary.setUpdatedTime(Instant.now());
        updateSalary(newSalary);

        var auditSalary = salaryMapper.salaryToSalaryAudit(newSalary);
        insertAuditSalary(auditSalary);

        return newSalary;
    }

    private void updateSalary(Salary salary) {
        try {
            if (0 == salaryDao.update(salary)) {
                throw new IntegrityException("SALARY_UPDATE_FAILED", "Failed to update salary");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("SALARY_UPDATE_FAILED", exception.getCause().getMessage());
        }
    }

    private UUID getAuthentication() {
        return UUID.fromString(SecurityContextHolder.getContext()
                .getAuthentication()
                .getName());
    }
}
