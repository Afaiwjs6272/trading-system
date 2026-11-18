package ru.ukhanov.spvb.model.order.enums;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    /** Заявка в стакане, исполнений ещё не было. */
    NEW,
    /** Исполнена частично, остаток в стакане. */
    PARTIALLY_FILLED,
    /** Исполнена полностью. */
    FILLED,
    /** Снята пользователем (остаток не исполнен). */
    CANCELLED;
}