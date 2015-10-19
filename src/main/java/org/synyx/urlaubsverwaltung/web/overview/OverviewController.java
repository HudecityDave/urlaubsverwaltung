package org.synyx.urlaubsverwaltung.web.overview;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.apache.log4j.Logger;

import org.joda.time.DateMidnight;
import org.joda.time.chrono.GregorianChronology;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.synyx.urlaubsverwaltung.core.account.domain.Account;
import org.synyx.urlaubsverwaltung.core.account.service.AccountService;
import org.synyx.urlaubsverwaltung.core.account.service.VacationDaysService;
import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.calendar.WorkDaysService;
import org.synyx.urlaubsverwaltung.core.overtime.OvertimeService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.PersonService;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNoteService;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNoteType;
import org.synyx.urlaubsverwaltung.core.util.DateUtil;
import org.synyx.urlaubsverwaltung.security.SessionService;
import org.synyx.urlaubsverwaltung.web.ControllerConstants;
import org.synyx.urlaubsverwaltung.web.application.ApplicationForLeave;
import org.synyx.urlaubsverwaltung.web.person.PersonConstants;
import org.synyx.urlaubsverwaltung.web.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.web.sicknote.ExtendedSickNote;
import org.synyx.urlaubsverwaltung.web.statistics.UsedDaysOverview;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;


/**
 * Controller for the different ways of displaying the personal overview page.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Controller
public class OverviewController {

    private static final Logger LOG = Logger.getLogger(OverviewController.class);

    @Autowired
    private PersonService personService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private VacationDaysService vacationDaysService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private WorkDaysService calendarService;

    @Autowired
    private SickNoteService sickNoteService;

    @Autowired
    private OvertimeService overtimeService;

    @RequestMapping(value = "/overview", method = RequestMethod.GET)
    public String showOverview(
        @RequestParam(value = ControllerConstants.YEAR_ATTRIBUTE, required = false) String year) {

        Person user = sessionService.getSignedInUser();

        if (StringUtils.hasText(year)) {
            return "redirect:/web/staff/" + user.getId() + "/overview?year=" + year;
        }

        return "redirect:/web/staff/" + user.getId() + "/overview";
    }


    @RequestMapping(value = "/staff/{personId}/overview", method = RequestMethod.GET)
    public String showOverview(@PathVariable("personId") Integer personId,
        @RequestParam(value = ControllerConstants.YEAR_ATTRIBUTE, required = false) String year, Model model)
        throws UnknownPersonException, AccessDeniedException {

        Person person = personService.getPersonByID(personId).orElseThrow(() -> new UnknownPersonException(personId));
        Person signedInUser = sessionService.getSignedInUser();

        if (!sessionService.isSignedInUserAllowedToAccessPersonData(signedInUser, person)) {
            throw new AccessDeniedException(String.format(
                    "User '%s' has not the correct permissions to access the overview page of user '%s'",
                    signedInUser.getLoginName(), person.getLoginName()));
        }

        model.addAttribute(PersonConstants.PERSON_ATTRIBUTE, person);

        Integer yearToShow = parseYearParameter(year);
        prepareApplications(person, yearToShow, model);
        prepareHolidayAccounts(person, yearToShow, model);
        prepareSickNoteList(person, yearToShow, model);

        model.addAttribute(ControllerConstants.YEAR_ATTRIBUTE, DateMidnight.now().getYear());

        return "person/overview";
    }


    /**
     * Parses the year of the given String, if parsing fails, the current year is returned.
     *
     * @param  input
     *
     * @return  parsed Integer of the given String, if parsing failed the current year is returned
     */
    private Integer parseYearParameter(String input) {

        // default value for year is the current year
        Integer year = DateMidnight.now(GregorianChronology.getInstance()).getYear();

        if (StringUtils.hasText(input)) {
            try {
                year = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                LOG.warn("Tried to show overview for an invalid year entry: " + input, ex);
            }
        }

        return year;
    }


    private void prepareSickNoteList(Person person, int year, Model model) {

        List<SickNote> sickNotes = sickNoteService.getByPersonAndPeriod(person, DateUtil.getFirstDayOfYear(year),
                DateUtil.getLastDayOfYear(year));

        List<ExtendedSickNote> extendedSickNotes = FluentIterable.from(sickNotes).transform(input ->
                        new ExtendedSickNote(input, calendarService)).toSortedList((o1, o2) -> {
                // show latest sick notes at first
                return o2.getStartDate().compareTo(o1.getStartDate());
            });

        BigDecimal sickDays = BigDecimal.ZERO;
        BigDecimal sickDaysWithAUB = BigDecimal.ZERO;
        BigDecimal childSickDays = BigDecimal.ZERO;
        BigDecimal childSickDaysWithAUB = BigDecimal.ZERO;

        for (SickNote sickNote : sickNotes) {
            if (!sickNote.isActive()) {
                continue;
            }

            if (sickNote.getType().equals(SickNoteType.SICK_NOTE_CHILD)) {
                childSickDays = childSickDays.add(calendarService.getWorkDays(sickNote.getDayLength(),
                            sickNote.getStartDate(), sickNote.getEndDate(), person));

                if (sickNote.isAubPresent()) {
                    BigDecimal workDays = calendarService.getWorkDays(sickNote.getDayLength(),
                            sickNote.getAubStartDate(), sickNote.getAubEndDate(), person);
                    childSickDaysWithAUB = childSickDaysWithAUB.add(workDays);
                }
            } else {
                sickDays = sickDays.add(calendarService.getWorkDays(sickNote.getDayLength(), sickNote.getStartDate(),
                            sickNote.getEndDate(), person));

                if (sickNote.isAubPresent()) {
                    BigDecimal workDays = calendarService.getWorkDays(sickNote.getDayLength(),
                            sickNote.getAubStartDate(), sickNote.getAubEndDate(), person);
                    sickDaysWithAUB = sickDaysWithAUB.add(workDays);
                }
            }
        }

        model.addAttribute("sickDays", sickDays);
        model.addAttribute("sickDaysWithAUB", sickDaysWithAUB);
        model.addAttribute("childSickDays", childSickDays);
        model.addAttribute("childSickDaysWithAUB", childSickDaysWithAUB);
        model.addAttribute("sickNotes", extendedSickNotes);
    }


    private void prepareApplications(Person person, int year, Model model) {

        // get the person's applications for the given year
        List<Application> applications = FluentIterable.from(
                    applicationService.getApplicationsForACertainPeriodAndPerson(DateUtil.getFirstDayOfYear(year),
                        DateUtil.getLastDayOfYear(year), person))
            .filter(input ->
                    !input.hasStatus(ApplicationStatus.REVOKED))
            .toList();

        if (!applications.isEmpty()) {
            ImmutableList<ApplicationForLeave> applicationsForLeave = FluentIterable.from(applications)
                .transform(input ->
                            new ApplicationForLeave(input, calendarService))
                .toSortedList((o1, o2) -> {
                    // show latest applications at first
                    return o2.getStartDate().compareTo(o1.getStartDate());
                });

            model.addAttribute("applications", applicationsForLeave);

            UsedDaysOverview usedDaysOverview = new UsedDaysOverview(applications, year, calendarService);
            model.addAttribute("usedDaysOverview", usedDaysOverview);
        }

        model.addAttribute("overtimeTotal", overtimeService.getTotalOvertimeForPerson(person));

        // TODO: Subtract hours of applications for leave because of having overtime due from total overtime
        model.addAttribute("overtimeLeft", BigDecimal.ZERO);
    }


    private void prepareHolidayAccounts(Person person, int year, Model model) {

        // get person's holidays account and entitlement for the given year
        Optional<Account> account = accountService.getHolidaysAccount(year, person);

        if (account.isPresent()) {
            model.addAttribute("vacationDaysLeft", vacationDaysService.getVacationDaysLeft(account.get()));
            model.addAttribute("account", account.get());
            model.addAttribute(PersonConstants.BEFORE_APRIL_ATTRIBUTE, DateUtil.isBeforeApril(DateMidnight.now()));
        }
    }
}