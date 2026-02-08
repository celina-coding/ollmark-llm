package com.penpot.ai.core.domain;

import java.util.Arrays;

/**
 * Énumération des types de tâches supportés.
 * Extensible via le pattern Strategy pour les comportements spécifiques.
 */
public enum TaskType {
    EXECUTE_CODE("executeCode"),
    FETCH_STRUCTURE("fetchStructure"),
    MODIFY_SHAPE("modifyShape"),
    CREATE_ELEMENT("createElement");

    private final String taskName;

    TaskType(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public static TaskType fromString(String taskName) {
        return Arrays.stream(values())
            .filter(t -> t.taskName.equals(taskName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown task type: " + taskName));
    }
}