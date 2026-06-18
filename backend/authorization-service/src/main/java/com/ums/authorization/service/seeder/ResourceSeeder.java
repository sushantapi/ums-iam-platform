package com.ums.authorization.service.seeder;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Resource;
import com.ums.authorization.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourceSeeder {

    private final ResourceRepository resourceRepository;

  
    public void seed() {

        createResource("USER", "User Management");
        createResource("ROLE", "Role Management");
        createResource("ORGANIZATION", "Organization Management");
        createResource("PAYROLL", "Payroll Management");
        createResource("PROJECT", "Project Management");
        createResource("DEPARTMENT", "Department Management");
    }

    private void createResource(String code, String name) {

        if (!resourceRepository.existsByCodeIgnoreCase(code)) {

            resourceRepository.save(
                    Resource.builder()
                            .code(code)
                            .name(name)
                            .build()
            );
        }
    }
}