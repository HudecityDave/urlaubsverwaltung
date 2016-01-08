package org.synyx.urlaubsverwaltung.web.overtime;

import org.joda.time.DateMidnight;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;


/**
 * Validates overtime record.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Component
public class OvertimeValidator implements Validator {

    private static final int MAX_CHARS = 200;

    private static final String ERROR_MANDATORY = "error.entry.mandatory";
    private static final String ERROR_MAX_CHARS = "error.entry.tooManyChars";
    private static final String ERROR_INVALID_PERIOD = "error.entry.invalidPeriod";

    @Override
    public boolean supports(Class<?> clazz) {

        return OvertimeForm.class.equals(clazz);
    }


    @Override
    public void validate(Object target, Errors errors) {

        OvertimeForm overtimeForm = (OvertimeForm) target;

        validatePeriod(overtimeForm, errors);
        validateNumberOfHours(overtimeForm, errors);
        validateComment(overtimeForm, errors);
    }


    private void validatePeriod(OvertimeForm overtimeForm, Errors errors) {

        DateMidnight startDate = overtimeForm.getStartDate();
        DateMidnight endDate = overtimeForm.getEndDate();

        validateDateNotNull(startDate, "startDate", errors);
        validateDateNotNull(endDate, "endDate", errors);

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.rejectValue("endDate", ERROR_INVALID_PERIOD);
        }
    }


    private void validateDateNotNull(DateMidnight date, String field, Errors errors) {

        // may be that date field is null because of cast exception, than there is already a field error
        if (date == null && !errors.hasFieldErrors(field)) {
            errors.rejectValue(field, ERROR_MANDATORY);
        }
    }


    private void validateNumberOfHours(OvertimeForm overtimeForm, Errors errors) {

        BigDecimal numberOfHours = overtimeForm.getNumberOfHours();

        // may be that number of hours field is null because of cast exception, than there is already a field error
        if (numberOfHours == null && !errors.hasFieldErrors("numberOfHours")) {
            errors.rejectValue("numberOfHours", ERROR_MANDATORY);
        }
    }


    private void validateComment(OvertimeForm overtimeForm, Errors errors) {

        String comment = overtimeForm.getComment();

        if (StringUtils.hasText(comment) && comment.length() > MAX_CHARS) {
            errors.rejectValue("comment", ERROR_MAX_CHARS);
        }
    }
}
