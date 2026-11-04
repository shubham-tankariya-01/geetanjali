package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.response.GarmentLatestMeasurement;
import com.geetanjali.backend.dto.response.MeasurementPrefill;
import com.geetanjali.backend.entity.Customer;
import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.entity.GarmentField;
import com.geetanjali.backend.entity.Measurement;
import com.geetanjali.backend.entity.MeasurementValue;
import com.geetanjali.backend.exception.ResourceNotFoundException;
import com.geetanjali.backend.repository.CustomerRepository;
import com.geetanjali.backend.repository.GarmentFieldRepository;
import com.geetanjali.backend.repository.GarmentRepository;
import com.geetanjali.backend.repository.MeasurementRepository;
import com.geetanjali.backend.repository.MeasurementValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// MeasurementService: The core measurement engine that manages, snapshots, and reuses tailor measurements.
// In a traditional database or naive app, people might simply "update" an existing customer's measurement row.
// In tailor shops, that causes a serious bug: updating old measurements corrupts past orders and historical receipts!
// Therefore, Geetanjali uses an immutable snapshot architecture:
// 1. If measurements haven't changed (compare as numbers: 38.0 equals 38.00), reuse the existing snapshot to avoid duplicate rows.
// 2. If any number changed, freeze a NEW snapshot with a fresh timestamp.
// 3. For fast order entry, prefill the newest snapshot's numbers so tailors don't re-type identical measurements.
//
// In Express, this corresponds to `services/measurementService.js`.
//
// @Service = Tells Spring to instantiate this class as a managed business service bean.
@Service
// @Transactional(readOnly = true) = Default to read-only database operations for optimal performance.
@Transactional(readOnly = true)
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final MeasurementValueRepository measurementValueRepository;
    private final GarmentRepository garmentRepository;
    private final GarmentFieldRepository garmentFieldRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    // Constructor injection: Spring automatically injects repository beans and the IST Clock bean.
    // Inputs:
    //   measurementRepository (MeasurementRepository)
    //   measurementValueRepository (MeasurementValueRepository)
    //   garmentRepository (GarmentRepository)
    //   garmentFieldRepository (GarmentFieldRepository)
    //   customerRepository (CustomerRepository)
    //   clock (Clock) - always Asia/Kolkata timezone, never system local clock
    public MeasurementService(
            MeasurementRepository measurementRepository,
            MeasurementValueRepository measurementValueRepository,
            GarmentRepository garmentRepository,
            GarmentFieldRepository garmentFieldRepository,
            CustomerRepository customerRepository,
            Clock clock) {
        this.measurementRepository = measurementRepository;
        this.measurementValueRepository = measurementValueRepository;
        this.garmentRepository = garmentRepository;
        this.garmentFieldRepository = garmentFieldRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    // =========================================================================
    // JOB 1: PREFILL
    // =========================================================================

    // Given a customer ID and a garment ID, returns the newest measurement snapshot's values keyed by field ID, plus its date.
    // Returns Optional.empty() if no snapshot exists yet for this customer and garment.
    // Used when opening an order entry screen to pre-fill numbers from the customer's previous order.
    //
    // Inputs: customerId (Long) - customer primary key, garmentId (Long) - garment primary key.
    // Output: Optional<MeasurementPrefill> - prefill DTO containing values map and date, or empty.
    public Optional<MeasurementPrefill> getPrefill(Long customerId, Long garmentId) {
        // Step 1: Find the newest snapshot for this customer and garment
        Optional<Measurement> newestOpt = measurementRepository
                .findFirstByCustomerIdAndGarmentIdOrderByCreatedAtDesc(customerId, garmentId);

        if (newestOpt.isEmpty()) {
            return Optional.empty();
        }

        // Step 2: Load the values for this snapshot
        Measurement newest = newestOpt.get();
        List<MeasurementValue> valuesList = measurementValueRepository.findByMeasurementId(newest.getId());

        // Step 3: Put numbers into a map keyed by field ID
        Map<Long, BigDecimal> valuesMap = new LinkedHashMap<>();
        for (MeasurementValue mv : valuesList) {
            valuesMap.put(mv.getField().getId(), mv.getValue());
        }

        // Step 4: Convert UTC Instant to Asia/Kolkata LocalDate for display
        LocalDate snapshotDate = newest.getCreatedAt().atZone(clock.getZone()).toLocalDate();

        return Optional.of(new MeasurementPrefill(
                newest.getId(),
                Collections.unmodifiableMap(valuesMap),
                newest.getCreatedAt(),
                snapshotDate
        ));
    }

    // Convenience overload for getPrefill using Customer and Garment entities.
    // Inputs: customer (Customer), garment (Garment).
    // Output: Optional<MeasurementPrefill> - prefill DTO or empty.
    public Optional<MeasurementPrefill> getPrefill(Customer customer, Garment garment) {
        if (customer == null || garment == null) {
            return Optional.empty();
        }
        return getPrefill(customer.getId(), garment.getId());
    }

    // =========================================================================
    // JOB 2: SAVE SNAPSHOT (Smart Deduplicating Snapshot Logic)
    // =========================================================================

    // Saves a measurement snapshot for a customer and garment from submitted numbers.
    // 1. Compares submitted numbers with the newest snapshot.
    // 2. If every number is identical (compared as numbers with compareTo, so 38.0 equals 38.00):
    //    returns the existing snapshot without creating any new rows in PostgreSQL!
    // 3. Otherwise: creates a new Measurement row with a fresh timestamp and one MeasurementValue
    //    row per non-blank (non-null) number.
    //
    // Inputs:
    //   customer (Customer) - customer entity
    //   garment (Garment) - garment entity
    //   submittedValues (Map<Long, BigDecimal>) - map of field ID to numeric value (null values are ignored/blank)
    // Output: Measurement - the persisted or reused measurement snapshot.
    //
    // @Transactional = Runs in a read-write database transaction. If any insert fails, all changes roll back.
    @Transactional
    public Measurement saveSnapshot(Customer customer, Garment garment, Map<Long, BigDecimal> submittedValues) {
        // Step 1: Filter submitted values to retain only non-null numbers (ignore blanks)
        Map<Long, BigDecimal> cleanSubmitted = new LinkedHashMap<>();
        if (submittedValues != null) {
            for (Map.Entry<Long, BigDecimal> entry : submittedValues.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    cleanSubmitted.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Step 2: Query the newest existing snapshot for this customer and garment
        Optional<Measurement> newestOpt = measurementRepository
                .findFirstByCustomerIdAndGarmentIdOrderByCreatedAtDesc(customer.getId(), garment.getId());

        // Step 3: If an existing snapshot is found, check if every number is identical
        if (newestOpt.isPresent()) {
            Measurement existingMeasurement = newestOpt.get();
            List<MeasurementValue> existingValues = measurementValueRepository.findByMeasurementId(existingMeasurement.getId());

            // Check if every number matches
            if (areValuesIdentical(existingValues, cleanSubmitted)) {
                // Every number is identical! Return the existing snapshot to avoid duplicate rows.
                return existingMeasurement;
            }
        }

        // Step 4: Numbers are different or no previous snapshot exists. Create a brand new snapshot!
        Instant now = clock.instant();
        Measurement newMeasurement = new Measurement(customer, garment, now);
        Measurement savedMeasurement = measurementRepository.save(newMeasurement);

        // Step 5: Insert one measurement_value row per non-blank number
        for (Map.Entry<Long, BigDecimal> entry : cleanSubmitted.entrySet()) {
            Long fieldId = entry.getKey();
            BigDecimal numericValue = entry.getValue();

            // Fetch the GarmentField definition (e.g. Chest, Waist)
            GarmentField field = garmentFieldRepository.findById(fieldId)
                    .orElseThrow(() -> new ResourceNotFoundException("GarmentField not found with id: " + fieldId));

            // Create MeasurementValue entity. The constructor automatically configures the composite key:
            // new MeasurementValueId(savedMeasurement.getId(), field.getId())
            MeasurementValue mv = new MeasurementValue(savedMeasurement, field, numericValue);
            measurementValueRepository.save(mv);
        }

        return savedMeasurement;
    }

    // Convenience overload of saveSnapshot taking customerId and garmentId primary keys.
    // Inputs: customerId (Long), garmentId (Long), submittedValues (Map<Long, BigDecimal>).
    // Output: Measurement entity.
    //
    // @Transactional = Ensures atomic database transaction.
    @Transactional
    public Measurement saveSnapshot(Long customerId, Long garmentId, Map<Long, BigDecimal> submittedValues) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Garment not found with id: " + garmentId));

        return saveSnapshot(customer, garment, submittedValues);
    }

    // Helper method to compare existing snapshot values with submitted values.
    // Returns true if and only if:
    // 1. Both sets have the exact same count of non-blank fields.
    // 2. Every field in the submitted map exists in the existing snapshot.
    // 3. Every numerical value is numerically equal using BigDecimal.compareTo() == 0.
    //
    // WHY compareTo() instead of equals():
    // In Java, BigDecimal("38.0").equals(BigDecimal("38.00")) returns FALSE because equals checks scale!
    // But BigDecimal("38.0").compareTo(BigDecimal("38.00")) == 0 returns TRUE because it compares mathematical value.
    // In a tailor shop, 38, 38.0, and 38.00 inches are the exact same physical measurement!
    //
    // Inputs: existingValues (List<MeasurementValue>), submitted (Map<Long, BigDecimal>).
    // Output: boolean - true if identical, false otherwise.
    private boolean areValuesIdentical(List<MeasurementValue> existingValues, Map<Long, BigDecimal> submitted) {
        // If the number of fields differs, they cannot be identical
        if (existingValues.size() != submitted.size()) {
            return false;
        }

        // Build a lookup map of existing values: fieldId -> numeric value
        Map<Long, BigDecimal> existingMap = new HashMap<>();
        for (MeasurementValue ev : existingValues) {
            existingMap.put(ev.getField().getId(), ev.getValue());
        }

        // Verify each submitted field against the existing map
        for (Map.Entry<Long, BigDecimal> entry : submitted.entrySet()) {
            Long fieldId = entry.getKey();
            BigDecimal submittedVal = entry.getValue();

            BigDecimal existingVal = existingMap.get(fieldId);
            if (existingVal == null) {
                // Submitted a field that wasn't in the existing snapshot
                return false;
            }

            // Compare numerical values ignoring scale (e.g. 38.0 vs 38.00)
            if (submittedVal.compareTo(existingVal) != 0) {
                return false; // Numbers differ
            }
        }

        return true;
    }

    // =========================================================================
    // JOB 3: LATEST PER GARMENT
    // =========================================================================

    // For the customer detail page: loops through all active garments in display order
    // and retrieves the newest measurement snapshot and values for each garment.
    //
    // Inputs: customerId (Long) - customer primary key.
    // Output: List<GarmentLatestMeasurement> - one entry per active garment, preserving sort order.
    public List<GarmentLatestMeasurement> getLatestPerGarment(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        return getLatestPerGarment(customer);
    }

    // Convenience overload for getLatestPerGarment taking a Customer entity.
    // Inputs: customer (Customer).
    // Output: List<GarmentLatestMeasurement>.
    public List<GarmentLatestMeasurement> getLatestPerGarment(Customer customer) {
        // Step 1: Query all active garments ordered by sort order (e.g. Shirt, Pant, Kurta, Blouse)
        List<Garment> activeGarments = garmentRepository.findByActiveTrueOrderBySortOrderAsc();

        List<GarmentLatestMeasurement> result = new ArrayList<>();

        // Step 2: For each active garment, find the newest snapshot for this customer
        for (Garment garment : activeGarments) {
            Optional<Measurement> newestOpt = measurementRepository
                    .findFirstByCustomerIdAndGarmentIdOrderByCreatedAtDesc(customer.getId(), garment.getId());

            if (newestOpt.isPresent()) {
                Measurement measurement = newestOpt.get();
                List<MeasurementValue> values = measurementValueRepository.findByMeasurementId(measurement.getId());
                result.add(new GarmentLatestMeasurement(garment, measurement, values));
            } else {
                // No measurement recorded yet for this garment
                result.add(new GarmentLatestMeasurement(garment, null, List.of()));
            }
        }

        return result;
    }

    // Convenience method: Retrieves just the latest Measurement entity for a customer and garment.
    // Inputs: customerId (Long), garmentId (Long).
    // Output: Optional<Measurement> - latest snapshot or empty.
    public Optional<Measurement> getLatestMeasurement(Long customerId, Long garmentId) {
        return measurementRepository.findFirstByCustomerIdAndGarmentIdOrderByCreatedAtDesc(customerId, garmentId);
    }
}
