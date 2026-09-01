package com.logistics.rca.domain;

public record CauseStat(Cause cause, int count, double shareOfProblems, String evidenceNote) {
}
