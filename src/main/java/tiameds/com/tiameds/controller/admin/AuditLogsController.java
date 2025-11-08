package tiameds.com.tiameds.controller.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.repository.AuditLogsRepository;

import java.util.List;

@RestController
@RequestMapping("/admin/audit")
public class AuditLogsController {

    private final AuditLogsRepository repository;

    public AuditLogsController(AuditLogsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<LabAuditLogs>> recent(@RequestParam(defaultValue = "50") int limit) {
        if (limit <= 0 || limit > 500) limit = 50;
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<LabAuditLogs> logs = repository.findAll(pageable).getContent();
        return ResponseEntity.ok(logs);
    }
}





