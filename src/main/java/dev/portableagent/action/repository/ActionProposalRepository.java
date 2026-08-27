package dev.portableagent.action.repository;

import dev.portableagent.action.domain.ActionProposal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionProposalRepository extends JpaRepository<ActionProposal, UUID> {
    Optional<ActionProposal> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<ActionProposal> findByIdAndTenantId(UUID id, UUID tenantId);
}
