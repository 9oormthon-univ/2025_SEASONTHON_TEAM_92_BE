package org.example.seasontonebackend.mission.domain.entity;

public enum MissionCategory {
    NOISE("방음/소음"),
    WATER_PRESSURE("수압"),
    PARKING("주차"),
    HEATING("난방"),
    OTHER("기타");

    private final String description;

    MissionCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}