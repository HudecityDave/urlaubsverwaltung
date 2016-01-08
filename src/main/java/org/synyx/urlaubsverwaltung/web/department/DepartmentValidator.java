package org.synyx.urlaubsverwaltung.web.department;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.synyx.urlaubsverwaltung.core.department.Department;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.Role;

import java.util.List;


/**
 * Validates the content of {@link Department}s.
 *
 * @author  Daniel Hammann - <hammann@synyx.de>
 */
@Component
public class DepartmentValidator implements Validator {

    private static final int MAX_CHARS_NAME = 50;
    private static final int MAX_CHARS_DESCRIPTION = 200;

    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_DESCRIPTION = "description";
    private static final String ATTRIBUTE_DEPARTMENT_HEADS = "departmentHeads";

    private static final String ERROR_REASON = "error.entry.mandatory";
    private static final String ERROR_LENGTH = "error.entry.tooManyChars";
    private static final String ERROR_DEPARTMENT_HEAD_NOT_ASSIGNED =
        "department.members.error.departmentHeadNotAssigned";
    private static final String ERROR_DEPARTMENT_HEAD_NO_ACCESS = "department.members.error.departmentHeadHasNoAccess";

    @Override
    public boolean supports(Class<?> clazz) {

        return Department.class.equals(clazz);
    }


    /**
     * Department name is mandatory Department name and description must have less than 200 characters.
     *
     * @param  target
     * @param  errors
     */
    @Override
    public void validate(Object target, Errors errors) {

        Department department = (Department) target;

        validateName(errors, department.getName());
        validateDescription(errors, department.getDescription());
        validateDepartmentHeads(errors, department.getMembers(), department.getDepartmentHeads());
    }


    private void validateName(Errors errors, String text) {

        boolean hasText = StringUtils.hasText(text);

        if (!hasText) {
            errors.rejectValue(ATTRIBUTE_NAME, ERROR_REASON);
        }

        if (hasText && text.length() > MAX_CHARS_NAME) {
            errors.rejectValue(ATTRIBUTE_NAME, ERROR_LENGTH);
        }
    }


    private void validateDescription(Errors errors, String description) {

        boolean hasText = StringUtils.hasText(description);

        if (hasText && description.length() > MAX_CHARS_DESCRIPTION) {
            errors.rejectValue(ATTRIBUTE_DESCRIPTION, ERROR_LENGTH);
        }
    }


    private void validateDepartmentHeads(Errors errors, List<Person> members, List<Person> departmentHeads) {

        if (departmentHeads != null) {
            for (Person departmentHead : departmentHeads) {
                if (members == null || (members != null && !members.contains(departmentHead))) {
                    errors.rejectValue(ATTRIBUTE_DEPARTMENT_HEADS, ERROR_DEPARTMENT_HEAD_NOT_ASSIGNED);
                }

                if (!departmentHead.hasRole(Role.DEPARTMENT_HEAD)) {
                    errors.rejectValue(ATTRIBUTE_DEPARTMENT_HEADS, ERROR_DEPARTMENT_HEAD_NO_ACCESS);
                }
            }
        }
    }
}
