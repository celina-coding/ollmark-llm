package com.example.aiPoc.exceptions;

import java.util.ArrayList;
import java.util.List;

import com.example.aiPoc.models.ValidationError;

/**
 * Exception levée lors d'erreurs de validation de code
 */
public class CodeValidationException extends RuntimeException {
    
    private List<ValidationError> errors = new ArrayList<>();
    private String code;

    public CodeValidationException(String message) {
        super(message);
    }

    public CodeValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    public CodeValidationException(String message, String code, List<ValidationError> errors) {
        super(message);
        this.code = code;
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    @Override
    public String toString() {
        return "CodeValidationException{" +
                "message='" + getMessage() + '\'' +
                ", errorCount=" + getErrorCount() +
                '}';
    }
}