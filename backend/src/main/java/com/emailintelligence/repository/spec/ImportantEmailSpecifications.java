package com.emailintelligence.repository.spec;

import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.PriorityLevel;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the dashboard search query dynamically - only the predicates for
 * filters that were actually provided get added to the query. This avoids
 * the "? IS NULL OR column = ?" pattern, which caused repeated Postgres
 * parameter-type-inference errors (enum columns needing casts, null
 * parameters being misread as bytea, etc). Adding predicates conditionally
 * sidesteps all of that: Postgres always sees concretely-typed parameters.
 */
public class ImportantEmailSpecifications {

    private ImportantEmailSpecifications() {
    }

    public static Specification<ImportantEmail> withFilters(
            EmailCategory category,
            PriorityLevel priority,
            String company,
            String search,
            Boolean isRead
    ) {
        return Specification
                .where(hasCategory(category))
                .and(hasPriority(priority))
                .and(companyContains(company))
                .and(textSearch(search))
                .and(hasReadStatus(isRead));
    }

    private static Specification<ImportantEmail> hasReadStatus(Boolean isRead) {
        if (isRead == null) return null;
        return (root, query, cb) -> cb.equal(root.get("isRead"), isRead);
    }

    private static Specification<ImportantEmail> hasCategory(EmailCategory category) {
        if (category == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private static Specification<ImportantEmail> hasPriority(PriorityLevel priority) {
        if (priority == null) return null;
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    private static Specification<ImportantEmail> companyContains(String company) {
        if (company == null || company.isBlank()) return null;
        String pattern = "%" + company.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("company")), pattern);
    }

    private static Specification<ImportantEmail> textSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("subject")), pattern),
                cb.like(cb.lower(root.get("summary")), pattern),
                cb.like(cb.lower(root.get("bodyText")), pattern),
                cb.like(cb.lower(root.get("senderName")), pattern),
                cb.like(cb.lower(root.get("company")), pattern)
        );
    }
}