
package com.unveil.service;

import com.unveil.entity.BadActor;
import com.unveil.repository.BadActorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {

    private final BadActorRepository repository;

    public SearchService(BadActorRepository badActorRepository) {
        this.repository = badActorRepository;
    }

    /**
     * Search by specific filter with pagination
     */
    public Page<BadActor> searchByFilter(String filter, String value, int page, int size) {
        // Validate filter
        String normalizedFilter = filter.toLowerCase().trim();
        if (!isValidFilter(normalizedFilter)) {
            throw new IllegalArgumentException("Unsupported filter: " + filter +
                    ". Supported filters: name, email, phone, company, actions, all");
        }

        // Validate value
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Search value cannot be empty");
        }

        // Create pageable object (sorted by creation date, newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Perform search based on filter
        return switch (normalizedFilter) {
            case "name" -> searchByName(value.trim(), pageable);
            case "email" -> searchByEmail(value.trim(), pageable);
            case "phone" -> searchByPhone(value.trim(), pageable);
            case "company" -> searchByCompany(value.trim(), pageable);
            case "action" -> searchByAction(value.trim(), pageable);
            case "all" -> searchByAll(value.trim(), pageable);
            default -> throw new IllegalArgumentException("Unsupported filter: " + filter);
        };
    }

    /**
     * Search by name (partial match, case insensitive)
     */
    private Page<BadActor> searchByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Search by email (exact match, case insensitive)
     */
    private Page<BadActor> searchByEmail(String email, Pageable pageable) {
        // Validate email format
        if (!isValidEmailFormat(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return repository.findByEmailIgnoreCase(email, pageable);
    }

    /**
     * Search by phone (exact match)
     */
    private Page<BadActor> searchByPhone(String phone, Pageable pageable) {
        return repository.findByPhone(phone, pageable);
    }

    /**
     * Search by company (partial match, case insensitive)
     */
    private Page<BadActor> searchByCompany(String company, Pageable pageable) {
        return repository.findByCompanyContainingIgnoreCase(company, pageable);
    }

    /**
     * Search by scam type (partial match, case insensitive)
     */
    private Page<BadActor> searchByAction(String actions, Pageable pageable) {
        return repository.findByActionsContainingIgnoreCase(actions, pageable);
    }

    /**
     * Search across all fields
     */
    private Page<BadActor> searchByAll(String searchTerm, Pageable pageable) {
        return repository.searchBadActors(searchTerm, pageable);
    }

    /**
     * Check if filter is valid
     */
    private boolean isValidFilter(String filter) {
        return filter.equals("name") ||
                filter.equals("email") ||
                filter.equals("phone") ||
                filter.equals("company") ||
                filter.equals("actions") ||
                filter.equals("all");
    }

    /**
     * Basic email format validation
     */
    private boolean isValidEmailFormat(String email) {
        return email.contains("@") &&
                email.contains(".") &&
                email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Get total count of bad actors in database
     */
    public long getTotalBadActorsCount() {
        return repository.count();
    }

    /**
     * Get a specific BadActor by ID
     */
    public Optional<BadActor> getBadActorById(Long id) {
        return repository.findById(id);
    }

    /**
     * Get all scam types for frontend filtering
     */
    public List<String> getAllActions() {
        return repository.findAllActions();
    }

    /**
     * Add a new BadActor (for future user reporting feature)
     */
    public BadActor addBadActor(BadActor BadActor) {
        // Basic validation
        if (BadActor.getName() == null && BadActor.getEmail() == null && BadActor.getPhone() == null) {
            throw new IllegalArgumentException("At least one of name, email, or phone must be provided");
        }

        return repository.save(BadActor);
    }
}