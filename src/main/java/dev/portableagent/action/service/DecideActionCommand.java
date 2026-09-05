package dev.portableagent.action.service;

import dev.portableagent.action.model.ActionDecision;

public record DecideActionCommand(ActionDecision decision, String payloadHash) {}
