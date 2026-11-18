package ru.ukhanov.spvb.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
}