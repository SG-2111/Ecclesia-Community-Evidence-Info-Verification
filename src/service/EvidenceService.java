// service/EvidenceService.java
package service;

import exception.EntityNotFoundException;
import exception.ValidationException;
import model.Evidence;

import java.util.*;

public class EvidenceService {
    private Map<Integer, Evidence> evidenceMap = new HashMap<>();
    private int nextId = 1;

    public Evidence addEvidence(Evidence evidence) throws ValidationException {
        if (evidence.getTitle() == null || evidence.getTitle().trim().isEmpty()) {
            throw new ValidationException("Evidence title is required");
        }
        evidence.setId(nextId++);
        evidenceMap.put(evidence.getId(), evidence);
        return evidence;
    }

    public Evidence getEvidenceById(int id) throws EntityNotFoundException {
        Evidence e = evidenceMap.get(id);
        if (e == null) throw new EntityNotFoundException("Evidence not found");
        return e;
    }

    public List<Evidence> getEvidenceByClaimId(int claimId) {
        List<Evidence> result = new ArrayList<>();
        for (Evidence e : evidenceMap.values()) {
            if (e.getClaimId() == claimId) {
                result.add(e);
            }
        }
        return result;
    }
}