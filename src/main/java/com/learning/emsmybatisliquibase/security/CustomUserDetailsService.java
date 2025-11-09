package com.learning.emsmybatisliquibase.security;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeeRoleDao;
import com.learning.emsmybatisliquibase.dao.PasswordDao;
import com.learning.emsmybatisliquibase.entity.enums.PasswordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeDao employeeDao;

    private final PasswordDao passwordDao;

    private final EmployeeRoleDao employeeRoleDao;

    private static final String ROLE_PREFIX = "ROLE_";

    private static final String MANAGER = "MANAGER";

    private static final String EMPLOYEE = "EMPLOYEE";


    @Override
    public UserDetails loadUserByUsername(String uuid) throws UsernameNotFoundException {
        var employee = employeeDao.get(UUID.fromString(uuid));
        if (employee == null) {
            throw new UsernameNotFoundException("User not found with uuid: " + uuid);
        }

        var password = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(),
                PasswordStatus.ACTIVE).getFirst();
        var employeeRoles = employeeRoleDao.getByEmployeeUuid(employee.getUuid());

        Set<GrantedAuthority> authorities = employeeRoles.stream().map(role ->
                new SimpleGrantedAuthority(ROLE_PREFIX + role.getRole().toString())).collect(Collectors.toSet());
        if (employee.getIsManager().equals(Boolean.TRUE)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + MANAGER));
        }
        authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + EMPLOYEE));
        return new org.springframework.security.core.userdetails.User(
                uuid,
                password.getPassword(),
                authorities
        );
    }

}
