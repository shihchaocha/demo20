package org.example.demo19.frontend.client;

import org.example.demo19.frontend.model.Organization;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "organization-service")
public interface OrganizationClient {

    @GetMapping("/api/organizations")
    List<Organization> findAll();
}
