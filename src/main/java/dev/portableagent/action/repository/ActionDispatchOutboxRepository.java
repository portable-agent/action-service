package dev.portableagent.action.repository;

import dev.portableagent.action.domain.ActionDispatchOutbox;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionDispatchOutboxRepository extends JpaRepository<ActionDispatchOutbox, UUID> {
    List<ActionDispatchOutbox> findByDispatchedAtIsNullOrderByCreatedAtAsc(Limit limit);
}
