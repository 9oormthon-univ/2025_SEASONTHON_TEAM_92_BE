package org.example.seasontonebackend.diagnosis.domain;

public enum DiagnosisScore {
    ONE("1", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5);

    private final String value;
    private final int intValue;

    DiagnosisScore(String value, int intValue) {
        this.value = value;
        this.intValue = intValue;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public static DiagnosisScore fromValue(String value) {
        for (DiagnosisScore score : values()) {
            if (score.getValue().equals(value)) {
                return score;
            }
        }
        throw new IllegalArgumentException("Invalid score value: " + value);
    }
}