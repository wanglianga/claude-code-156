package com.county.coldchain.web;

import com.county.coldchain.domain.entity.Category;
import com.county.coldchain.domain.entity.Customer;
import com.county.coldchain.domain.entity.Location;
import com.county.coldchain.domain.entity.Owner;
import com.county.coldchain.domain.entity.Zone;
import com.county.coldchain.repo.CategoryRepository;
import com.county.coldchain.repo.CustomerRepository;
import com.county.coldchain.repo.LocationRepository;
import com.county.coldchain.repo.ZoneRepository;
import com.county.coldchain.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "基础资料")
@RestController
@RequestMapping("/api/masters")
@RequiredArgsConstructor
public class MasterDataController {

    private final OwnerService ownerService;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final LocationRepository locationRepository;

    @Operation(summary = "货主列表")
    @GetMapping("/owners")
    public List<Owner> owners() { return ownerService.list(); }

    @Operation(summary = "新增/更新货主")
    @PostMapping("/owners")
    public Owner saveOwner(@RequestBody Owner owner) { return ownerService.save(owner); }

    @Operation(summary = "客户列表")
    @GetMapping("/customers")
    public List<Customer> customers() { return customerRepository.findAll(); }

    @Operation(summary = "新增/更新客户")
    @PostMapping("/customers")
    public Customer saveCustomer(@RequestBody Customer c) { return customerRepository.save(c); }

    @Operation(summary = "品类列表")
    @GetMapping("/categories")
    public List<Category> categories() { return categoryRepository.findAll(); }

    @Operation(summary = "新增/更新品类")
    @PostMapping("/categories")
    public Category saveCategory(@RequestBody Category c) { return categoryRepository.save(c); }

    @Operation(summary = "温区列表")
    @GetMapping("/zones")
    public List<Zone> zones() { return zoneRepository.findAll(); }

    @Operation(summary = "库位列表（可按温区）")
    @GetMapping("/locations")
    public List<Location> locations(Long zoneId) {
        return zoneId == null ? locationRepository.findAll() : locationRepository.findByZoneId(zoneId);
    }

    @Operation(summary = "新增/更新库位")
    @PostMapping("/locations")
    public Location saveLocation(@RequestBody Location l) {
        if (l.getOccupiedKg() == null) {
            l.setOccupiedKg(java.math.BigDecimal.ZERO);
        }
        return locationRepository.save(l);
    }
}
