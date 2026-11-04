package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.response.GarmentLatestMeasurement;
import com.geetanjali.backend.dto.response.MeasurementPrefill;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.MeasurementValue;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import com.geetanjali.backend.repository.MeasurementRepository;
import com.geetanjali.backend.repository.MeasurementValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// MeasurementEngineIntegrationTest: Real Spring Boot integration test running against PostgreSQL.
// Verifies the "Done when" criteria from Step 3:
// "a small test or temporary runner shows that saving the same numbers twice creates one snapshot,
// and changing one number creates a second."
@SpringBootTest
@Transactional
class MeasurementEngineIntegrationTest {

    @Autowired
    private MeasurementService measurementService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private GarmentFieldRepository garmentFieldRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private MeasurementValueRepository measurementValueRepository;

    @Test
    @DisplayName("Step 3 Core Test: saving same numbers twice creates one snapshot, and changing one number creates a second")
    void testSnapshotDeduplicationAndCreationLifecycle() {
        // Step 1: Arrange test customer and Shirt garment from seeded data
        Customer customer = customerRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> customerRepository.save(new Customer("Test Customer", "9999888877")));

        Garment shirt = garmentRepository.findByName("Shirt")
                .orElseThrow(() -> new IllegalStateException("Seeded garment Shirt must exist"));

        List<GarmentField> fields = garmentFieldRepository
                .findByGarmentIdAndActiveTrueOrderBySortOrderAsc(shirt.getId());
        assertTrue(fields.size() >= 2, "Shirt should have at least 2 fields from V2 seed");

        GarmentField field1 = fields.get(0); // e.g. Length
        GarmentField field2 = fields.get(1); // e.g. Chest

        int initialMeasurementCount = measurementRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId()).size();

        // Step 2: First save of numbers (Length = 30.0, Chest = 38.0)
        Map<Long, BigDecimal> initialNumbers = new LinkedHashMap<>();
        initialNumbers.put(field1.getId(), new BigDecimal("30.0"));
        initialNumbers.put(field2.getId(), new BigDecimal("38.0"));

        Measurement snapshot1 = measurementService.saveSnapshot(customer, shirt, initialNumbers);
        assertNotNull(snapshot1);
        assertNotNull(snapshot1.getId());

        // Verify composite key values in DB
        List<MeasurementValue> savedValues = measurementValueRepository.findByMeasurementId(snapshot1.getId());
        assertEquals(2, savedValues.size(), "Should have saved 2 measurement_value rows");
        for (MeasurementValue mv : savedValues) {
            assertEquals(snapshot1.getId(), mv.getId().getMeasurementId());
            assertNotNull(mv.getId().getFieldId());
        }

        // Step 3: Second save of the EXACT same numbers with different scale (30.00 and 38.00)
        Map<Long, BigDecimal> duplicateNumbers = new LinkedHashMap<>();
        duplicateNumbers.put(field1.getId(), new BigDecimal("30.00"));
        duplicateNumbers.put(field2.getId(), new BigDecimal("38.00"));

        Measurement snapshot2 = measurementService.saveSnapshot(customer, shirt, duplicateNumbers);

        // Verification of Step 3 Goal: MUST reuse snapshot 1 without creating a new row!
        assertEquals(snapshot1.getId(), snapshot2.getId(),
                "Saving the exact same numbers twice must return the existing snapshot ID!");

        int countAfterDuplicate = measurementRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId()).size();
        assertEquals(initialMeasurementCount + 1, countAfterDuplicate,
                "Snapshot count must remain 1 after saving duplicate numbers!");

        // Step 4: Third save with ONE number changed (Chest changed from 38.0 to 39.5)
        Map<Long, BigDecimal> modifiedNumbers = new LinkedHashMap<>();
        modifiedNumbers.put(field1.getId(), new BigDecimal("30.0"));
        modifiedNumbers.put(field2.getId(), new BigDecimal("39.5"));

        Measurement snapshot3 = measurementService.saveSnapshot(customer, shirt, modifiedNumbers);

        // Verification of Step 3 Goal: MUST create a second snapshot!
        assertNotEquals(snapshot1.getId(), snapshot3.getId(),
                "Changing one number must create a second snapshot with a new ID!");

        int countAfterModification = measurementRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId()).size();
        assertEquals(initialMeasurementCount + 2, countAfterModification,
                "Snapshot count must now be 2 after saving modified numbers!");

        // Step 5: Test Job 1 Prefill
        Optional<MeasurementPrefill> prefillOpt = measurementService.getPrefill(customer.getId(), shirt.getId());
        assertTrue(prefillOpt.isPresent(), "Prefill must find newest snapshot");
        MeasurementPrefill prefill = prefillOpt.get();
        assertEquals(snapshot3.getId(), prefill.measurementId(), "Prefill must return newest snapshot ID");
        // Values in newest snapshot must match the modified numbers
        assertEquals(0, new BigDecimal("39.5").compareTo(prefill.values().get(field2.getId())),
                "Prefill must return the updated Chest measurement 39.5");

        // Step 6: Test Job 3 Latest per garment
        List<GarmentLatestMeasurement> latestPerGarment = measurementService.getLatestPerGarment(customer);
        assertFalse(latestPerGarment.isEmpty(), "Must list active garments");

        // Shirt must have measurement, snapshot3
        GarmentLatestMeasurement shirtLatest = latestPerGarment.stream()
                .filter(glm -> glm.garment().getId().equals(shirt.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(shirtLatest.hasMeasurement());
        assertEquals(snapshot3.getId(), shirtLatest.measurement().getId());
    }
}
