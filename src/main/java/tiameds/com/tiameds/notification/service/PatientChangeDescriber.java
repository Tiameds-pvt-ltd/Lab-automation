package tiameds.com.tiameds.notification.service;

import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.VisitRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Turns "current patient" + "proposed patient" into a readable list of what REALLY changed.
 *
 * Follows the rules of UpdatePatientService: a null in the proposed values means "not changed", so it is never
 * reported. Values the frontend recomputes on every save are ignored on purpose: the age text (it moves by days
 * with the clock; a real age change shows up as a date of birth change), test order, and null vs empty lists.
 */
@Component
public class PatientChangeDescriber {

    /** @param field short machine name, e.g. "firstName"; @param text readable, e.g. "First name: 'A' → 'B'" */
    public record Change(String field, String text) {}

    private static final String EMPTY = "(empty)";

    private final VisitRepository visitRepository;
    private final TestRepository testRepository;

    public PatientChangeDescriber(VisitRepository visitRepository, TestRepository testRepository) {
        this.visitRepository = visitRepository;
        this.testRepository = testRepository;
    }

    public List<Change> describe(PatientDTO current, PatientDTO proposed) {
        List<Change> out = new ArrayList<>();
        text(out, "firstName", "First name", current.getFirstName(), proposed.getFirstName());
        text(out, "lastName", "Last name", current.getLastName(), proposed.getLastName());
        text(out, "email", "Email", current.getEmail(), proposed.getEmail());
        text(out, "phone", "Phone", current.getPhone(), proposed.getPhone());
        text(out, "address", "Address", current.getAddress(), proposed.getAddress());
        text(out, "city", "City", current.getCity(), proposed.getCity());
        text(out, "state", "State", current.getState(), proposed.getState());
        text(out, "zip", "Zip", current.getZip(), proposed.getZip());
        text(out, "dateOfBirth", "Date of birth", str(current.getDateOfBirth()), str(proposed.getDateOfBirth()));
        label(out, "gender", "Gender", current.getGender(), proposed.getGender());
        label(out, "bloodGroup", "Blood group", current.getBloodGroup(), proposed.getBloodGroup());

        if (proposed.getVisit() != null) {
            visit(out, proposed.getVisit());
        }
        return out;
    }

    // ------------------------------------------------------------------ visit

    private void visit(List<Change> out, VisitDTO proposed) {
        VisitDTO current = loadVisit(proposed.getVisitId());
        if (current == null) {
            List<Long> tests = proposed.getTestIds() != null ? proposed.getTestIds() : List.of();
            out.add(new Change("visit", proposed.getVisitId() == null
                    ? "New visit added" + (tests.isEmpty() ? "" : " with tests: " + names(tests))
                    : "Visit details changed"));
            return;
        }

        text(out, "visitDate", "Visit date", str(current.getVisitDate()), str(proposed.getVisitDate()));
        label(out, "visitType", "Visit type", current.getVisitType(), proposed.getVisitType());
        label(out, "visitStatus", "Visit status", current.getVisitStatus(), proposed.getVisitStatus());
        text(out, "visitDescription", "Visit description", current.getVisitDescription(), proposed.getVisitDescription());
        if (proposed.getDoctorId() != null && !Objects.equals(current.getDoctorId(), proposed.getDoctorId())) {
            out.add(new Change("doctor", "Doctor changed"));
        }

        if (proposed.getTestIds() != null) {
            Set<Long> before = new LinkedHashSet<>(orEmpty(current.getTestIds()));
            Set<Long> after = new LinkedHashSet<>(proposed.getTestIds());
            Set<Long> added = new LinkedHashSet<>(after);
            added.removeAll(before);
            Set<Long> removed = new LinkedHashSet<>(before);
            removed.removeAll(after);
            if (!added.isEmpty()) {
                out.add(new Change("testsAdded", "Tests added: " + names(added)));
            }
            if (!removed.isEmpty()) {
                out.add(new Change("testsRemoved", "Tests removed: " + names(removed)));
            }
        }
        if (proposed.getPackageIds() != null
                && !new HashSet<>(orEmpty(current.getPackageIds())).equals(new HashSet<>(proposed.getPackageIds()))) {
            out.add(new Change("packages", "Health packages changed"));
        }
        if (proposed.getInsuranceIds() != null
                && !new HashSet<>(orEmpty(current.getInsuranceIds())).equals(new HashSet<>(proposed.getInsuranceIds()))) {
            out.add(new Change("insurance", "Insurance changed"));
        }

        billing(out, current.getBilling(), proposed.getBilling());
        testDiscounts(out, current, proposed);
    }

    private void billing(List<Change> out, BillingDTO current, BillingDTO proposed) {
        if (proposed == null || current == null) {
            return;
        }
        money(out, "totalAmount", "Total amount", current.getTotalAmount(), proposed.getTotalAmount());
        money(out, "discount", "Discount", current.getDiscount(), proposed.getDiscount());
        money(out, "netAmount", "Net amount", current.getNetAmount(), proposed.getNetAmount());
        money(out, "gstRate", "GST rate", current.getGstRate(), proposed.getGstRate());
        money(out, "gstAmount", "GST amount", current.getGstAmount(), proposed.getGstAmount());
        money(out, "receivedAmount", "Received amount", current.getReceivedAmount(), proposed.getReceivedAmount());
        // Older bills store UNPAID, the screen sends DUE for the same state: not an edit
        if (proposed.getPaymentStatus() != null
                && !paymentStatusKey(current.getPaymentStatus()).equals(paymentStatusKey(proposed.getPaymentStatus()))) {
            text(out, "paymentStatus", "Payment status", current.getPaymentStatus(), proposed.getPaymentStatus());
        }
        label(out, "paymentMethod", "Payment method", current.getPaymentMethod(), proposed.getPaymentMethod());
        text(out, "discountReason", "Discount reason", current.getDiscountReason(), proposed.getDiscountReason());
    }

    private void testDiscounts(List<Change> out, VisitDTO current, VisitDTO proposed) {
        if (proposed.getListOfEachTestDiscount() == null || current.getListOfEachTestDiscount() == null) {
            return;
        }
        Map<Long, TestDiscountDTO> before = new HashMap<>();
        for (TestDiscountDTO d : current.getListOfEachTestDiscount()) {
            if (d != null && d.getTestId() != null) {
                before.put(d.getTestId(), d);
            }
        }
        for (TestDiscountDTO after : proposed.getListOfEachTestDiscount()) {
            TestDiscountDTO old = after != null && after.getTestId() != null ? before.get(after.getTestId()) : null;
            if (old == null) {
                continue; // a newly added test is already reported under "Tests added"
            }
            List<String> parts = new ArrayList<>();
            if (differs(old.getDiscountAmount(), after.getDiscountAmount())) {
                parts.add("amount " + num(old.getDiscountAmount()) + " → " + num(after.getDiscountAmount()));
            }
            if (differs(old.getDiscountPercent(), after.getDiscountPercent())) {
                parts.add("percent " + num(old.getDiscountPercent()) + " → " + num(after.getDiscountPercent()));
            }
            if (differs(old.getFinalPrice(), after.getFinalPrice())) {
                parts.add("final price " + num(old.getFinalPrice()) + " → " + num(after.getFinalPrice()));
            }
            if (!parts.isEmpty()) {
                out.add(new Change("testDiscount", "Discount on " + names(List.of(after.getTestId()))
                        + ": " + String.join(", ", parts)));
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private VisitDTO loadVisit(Long visitId) {
        if (visitId == null) {
            return null;
        }
        try {
            return visitRepository.findById(visitId).map(VisitDTO::new).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void text(List<Change> out, String field, String label, String current, String proposed) {
        if (proposed == null) {
            return; // null = not changed
        }
        String before = current == null ? "" : current.trim();
        String after = proposed.trim();
        if (!before.equals(after)) {
            out.add(new Change(field, label + ": '" + (before.isEmpty() ? EMPTY : before) + "' → '"
                    + (after.isEmpty() ? EMPTY : after) + "'"));
        }
    }

    /** Like text(), but upper/lower case alone ('male' vs 'Male') is not a change. */
    private void label(List<Change> out, String field, String label, String current, String proposed) {
        if (proposed == null) {
            return;
        }
        String before = current == null ? "" : current.trim();
        if (!before.equalsIgnoreCase(proposed.trim())) {
            text(out, field, label, current, proposed);
        }
    }

    private String paymentStatusKey(String status) {
        String key = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return key.equals("UNPAID") ? "DUE" : key;
    }

    private void money(List<Change> out, String field, String label, BigDecimal current, BigDecimal proposed) {
        if (proposed != null && differs(current, proposed)) {
            out.add(new Change(field, "Billing - " + label + ": " + num(current) + " → " + num(proposed)));
        }
    }

    /** Null proposed = not changed; null current counts as zero. */
    private boolean differs(BigDecimal current, BigDecimal proposed) {
        if (proposed == null) {
            return false;
        }
        BigDecimal before = current == null ? BigDecimal.ZERO : current;
        return before.compareTo(proposed) != 0;
    }

    private String num(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String names(Collection<Long> testIds) {
        Map<Long, String> byId = testRepository.findAllById(testIds).stream()
                .collect(Collectors.toMap(Test::getId, t -> t.getName() != null ? t.getName() : "#" + t.getId(),
                        (a, b) -> a));
        return testIds.stream().map(id -> byId.getOrDefault(id, "#" + id)).collect(Collectors.joining(", "));
    }
}
