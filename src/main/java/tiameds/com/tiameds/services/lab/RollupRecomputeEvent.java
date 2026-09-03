package tiameds.com.tiameds.services.lab;

import java.time.LocalDate;

/**
 * Published by write paths (VisitService, PatientService, BillingManagementService,
 * ReportService, UpdatePatientService) instead of calling DashboardRollupService
 * directly, so the actual recompute runs asynchronously after the write's own
 * transaction commits — see RollupRecomputeListener.
 */
public class RollupRecomputeEvent {

    private final Long labId;
    private final LocalDate date;

    public RollupRecomputeEvent(Long labId, LocalDate date) {
        this.labId = labId;
        this.date = date;
    }

    public Long getLabId() {
        return labId;
    }

    public LocalDate getDate() {
        return date;
    }
}
