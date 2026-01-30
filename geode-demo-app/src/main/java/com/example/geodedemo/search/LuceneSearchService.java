package com.example.geodedemo.search;

import com.example.geodedemo.entity.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.lucene.LuceneIndex;
import org.apache.geode.cache.lucene.LuceneQuery;
import org.apache.geode.cache.lucene.LuceneQueryException;
import org.apache.geode.cache.lucene.LuceneService;
import org.apache.geode.cache.lucene.LuceneServiceProvider;
import org.apache.geode.cache.lucene.PageableLuceneQueryResults;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Lucene full-text search service for Customer data.
 * Provides fast text search capabilities on customer names and emails.
 *
 * Note: Lucene indexes must be created on the server before data is inserted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LuceneSearchService {

    private static final String INDEX_NAME = "customerIndex";
    private static final String REGION_NAME = "Customers";

    private final GemFireCache cache;
    private LuceneService luceneService;

    @PostConstruct
    public void init() {
        try {
            luceneService = LuceneServiceProvider.get(cache);
            log.info("Lucene search service initialized");

            // Note: Index creation must be done on the server side before region is created
            // This is typically done via gfsh:
            // gfsh> create lucene index --name=customerIndex --region=/Customers --field=name,email

        } catch (Exception e) {
            log.warn("Could not initialize Lucene service: {}", e.getMessage());
        }
    }

    /**
     * Search customers by name using Lucene full-text search.
     *
     * @param queryString Search query (supports wildcards: *, ?)
     * @return List of matching customers
     */
    public List<Customer> searchByName(String queryString) {
        return executeSearch("name", queryString);
    }

    /**
     * Search customers by email using Lucene full-text search.
     *
     * @param queryString Search query
     * @return List of matching customers
     */
    public List<Customer> searchByEmail(String queryString) {
        return executeSearch("email", queryString);
    }

    /**
     * Search customers across all indexed fields.
     *
     * @param queryString Search query
     * @return List of matching customers
     */
    public List<Customer> searchAll(String queryString) {
        List<Customer> results = new ArrayList<>();
        results.addAll(searchByName(queryString));
        results.addAll(searchByEmail(queryString));

        // Remove duplicates
        return results.stream()
            .collect(Collectors.toMap(Customer::getCustomerId, c -> c, (a, b) -> a))
            .values()
            .stream()
            .collect(Collectors.toList());
    }

    /**
     * Execute Lucene search on specified field.
     */
    private List<Customer> executeSearch(String field, String queryString) {
        List<Customer> results = new ArrayList<>();

        if (luceneService == null) {
            log.warn("Lucene service not available, falling back to region scan");
            return fallbackSearch(field, queryString);
        }

        try {
            LuceneQuery<String, Customer> query = luceneService.createLuceneQueryFactory()
                .setLimit(100)
                .create(INDEX_NAME, REGION_NAME, queryString, field);

            PageableLuceneQueryResults<String, Customer> pageResults = query.findPages();

            while (pageResults.hasNext()) {
                pageResults.next().forEach(entry -> {
                    if (entry.getValue() != null) {
                        results.add(entry.getValue());
                    }
                });
            }

            log.info("Lucene search '{}' on field '{}' returned {} results",
                queryString, field, results.size());

        } catch (LuceneQueryException e) {
            log.error("Lucene query error: {}", e.getMessage());
            return fallbackSearch(field, queryString);
        }

        return results;
    }

    /**
     * Fallback search when Lucene is not available.
     * Performs a simple region scan with string matching.
     */
    private List<Customer> fallbackSearch(String field, String queryString) {
        List<Customer> results = new ArrayList<>();
        Region<String, Customer> region = cache.getRegion(REGION_NAME);

        if (region == null) {
            log.warn("Region {} not found", REGION_NAME);
            return results;
        }

        String searchLower = queryString.toLowerCase().replace("*", "").replace("?", "");

        for (Customer customer : region.values()) {
            if (customer == null) continue;

            String fieldValue = null;
            if ("name".equals(field) && customer.getName() != null) {
                fieldValue = customer.getName().toLowerCase();
            } else if ("email".equals(field) && customer.getEmail() != null) {
                fieldValue = customer.getEmail().toLowerCase();
            }

            if (fieldValue != null && fieldValue.contains(searchLower)) {
                results.add(customer);
            }
        }

        log.info("Fallback search '{}' on field '{}' returned {} results",
            queryString, field, results.size());

        return results;
    }

    /**
     * Get Lucene index information.
     */
    public Map<String, Object> getIndexInfo() {
        if (luceneService == null) {
            return Map.of(
                "status", "unavailable",
                "message", "Lucene service not initialized"
            );
        }

        try {
            Collection<LuceneIndex> indexes = luceneService.getAllIndexes();
            List<Map<String, Object>> indexList = new ArrayList<>();

            for (LuceneIndex index : indexes) {
                indexList.add(Map.of(
                    "name", index.getName(),
                    "regionPath", index.getRegionPath(),
                    "fields", List.of(index.getFieldNames())
                ));
            }

            return Map.of(
                "status", "available",
                "indexes", indexList
            );

        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "message", e.getMessage()
            );
        }
    }
}
