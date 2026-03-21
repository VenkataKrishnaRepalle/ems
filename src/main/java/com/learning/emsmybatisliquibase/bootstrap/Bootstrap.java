package com.learning.emsmybatisliquibase.bootstrap;

import com.learning.emsmybatisliquibase.dao.*;
import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import com.learning.emsmybatisliquibase.entity.*;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import com.learning.emsmybatisliquibase.entity.enums.JobTitleType;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.service.EmployeeRoleService;
import com.learning.emsmybatisliquibase.service.PeriodService;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.Calendar;

@Component
@RequiredArgsConstructor
public class Bootstrap implements CommandLineRunner {

    private final EmployeeDao employeeDao;

    private final EmployeeService employeeService;

    private final EmployeeRoleService employeeRoleService;

    private final PeriodDao periodDao;

    private final PeriodService periodService;

    private static final String ADMIN = "admin";

    private static final String TESCO = "Tesco";

    private static final String WALMART = "Walmart";

    @Override
    public void run(String... args) {
        if (periodDao.getByStatus(PeriodStatus.STARTED) == null) {
            var cycle = periodService.createPeriod(Calendar.getInstance().get(Calendar.YEAR));
            periodService.updateStatus(cycle.getUuid(), PeriodStatus.STARTED);
        }
        if (employeeDao.count() < 6) {
            var employee = AddEmployeeDto.builder()
                    .firstName(ADMIN)
                    .lastName(ADMIN)
                    .email("admin@gmail.com")
                    .phoneNumber("1234567890")
                    .dateOfBirth(Date.valueOf("2000-01-01").toLocalDate())
                    .joiningDate(Date.valueOf("2022-01-01").toLocalDate())
                    .gender(Gender.MALE)
                    .departmentName(TESCO)
                    .isManager("T")
                    .jobTitle(JobTitleType.CEO.toString())
                    .password("Admin@123")
                    .confirmPassword("Admin@123")
                    .build();
            var employeeResponse = employeeService.add(employee);
            employeeRoleService.add(EmployeeRole.builder()
                    .employeeUuid(employeeResponse.getUuid())
                    .role(RoleType.ADMIN)
                    .build());

            var employee1 = AddEmployeeDto.builder()
                    .firstName("venky")
                    .lastName("repalle")
                    .email("rvkrishna13052001@gmail.com")
                    .phoneNumber("9059712824")
                    .dateOfBirth(Date.valueOf("2001-05-13").toLocalDate())
                    .joiningDate(Date.valueOf("2018-07-04").toLocalDate())
                    .gender(Gender.MALE)
                    .isManager("T")
                    .departmentName(TESCO)
                    .jobTitle(JobTitleType.PROJECT_MANAGER.toString())
                    .password("Venky@123")
                    .confirmPassword("Venky@123")
                    .build();
            var employee1Response = employeeService.add(employee1);

            var employee2 = AddEmployeeDto.builder()
                    .firstName("chandu")
                    .lastName("raya")
                    .email("chandu.raya@gmail.com")
                    .phoneNumber("6305177093")
                    .dateOfBirth(Date.valueOf("2000-11-01").toLocalDate())
                    .joiningDate(Date.valueOf("2019-07-04").toLocalDate())
                    .gender(Gender.MALE)
                    .isManager("F")
                    .managerUuid(employee1Response.getUuid())
                    .departmentName(TESCO)
                    .jobTitle(JobTitleType.TECHNICAL_LEAD.toString())
                    .password("Chandu@123")
                    .confirmPassword("Chandu@123")
                    .build();
            employeeService.add(employee2);

            var employee3 = AddEmployeeDto.builder()
                    .firstName("sujith")
                    .lastName("bikki")
                    .email("sujith.bikki@gmail.com")
                    .phoneNumber("1234567890")
                    .dateOfBirth(Date.valueOf("2000-05-14").toLocalDate())
                    .joiningDate(Date.valueOf("2020-07-04").toLocalDate())
                    .gender(Gender.MALE)
                    .isManager("F")
                    .managerUuid(employee1Response.getUuid())
                    .departmentName(TESCO)
                    .jobTitle(JobTitleType.SENIOR_SOFTWARE_ENGINEER.toString())
                    .password("Sujith@123")
                    .confirmPassword("Sujith@123")
                    .build();
            employeeService.add(employee3);


            var employee4 = AddEmployeeDto.builder()
                    .firstName("veeranji")
                    .lastName("katari")
                    .email("veeranji.katari@gmail.com")
                    .phoneNumber("9490903106")
                    .dateOfBirth(Date.valueOf("2000-11-18").toLocalDate())
                    .joiningDate(Date.valueOf("2021-07-04").toLocalDate())
                    .gender(Gender.MALE)
                    .isManager("T")
                    .managerUuid(employee1Response.getUuid())
                    .departmentName(WALMART)
                    .jobTitle(JobTitleType.PROJECT_MANAGER.toString())
                    .password("Veeranji@123")
                    .confirmPassword("Veeranji@123")
                    .build();
            var employee4Response = employeeService.add(employee4);

            var employee5 = AddEmployeeDto.builder()
                    .firstName("lakshman")
                    .lastName("jampani")
                    .email("lakshman.jampani@gmail.com")
                    .phoneNumber("6303537771")
                    .dateOfBirth(Date.valueOf("2001-02-28").toLocalDate())
                    .joiningDate(Date.valueOf("2022-07-04").toLocalDate())
                    .gender(Gender.MALE)
                    .isManager("F")
                    .managerUuid(employee4Response.getUuid())
                    .departmentName(WALMART)
                    .jobTitle(JobTitleType.SOFTWARE_ENGINEER.toString())
                    .password("Lakshman@123")
                    .confirmPassword("Lakshman@123")
                    .build();
            employeeService.add(employee5);
        }
    }
}
