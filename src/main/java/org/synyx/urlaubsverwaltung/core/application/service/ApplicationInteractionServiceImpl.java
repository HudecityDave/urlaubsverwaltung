package org.synyx.urlaubsverwaltung.core.application.service;

import org.apache.log4j.Logger;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.synyx.urlaubsverwaltung.core.account.service.AccountInteractionService;
import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.domain.Comment;
import org.synyx.urlaubsverwaltung.core.mail.MailService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.sync.providers.CalendarProviderService;
import org.synyx.urlaubsverwaltung.core.sync.absence.Absence;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceMapping;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceMappingService;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceTimeConfiguration;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceType;

import java.util.Optional;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Service
@Transactional
public class ApplicationInteractionServiceImpl implements ApplicationInteractionService {

    private static final Logger LOG = Logger.getLogger(ApplicationInteractionServiceImpl.class);

    private final ApplicationService applicationService;
    private final AccountInteractionService accountInteractionService;
    private final SignService signService;
    private final CommentService commentService;
    private final MailService mailService;
    private final CalendarProviderService calendarProviderService;
    private final AbsenceMappingService absenceMappingService;
    private final AbsenceTimeConfiguration absenceTimeConfiguration;

    @Autowired
    public ApplicationInteractionServiceImpl(ApplicationService applicationService, CommentService commentService,
        AccountInteractionService accountInteractionService, SignService signService, MailService mailService,
        CalendarProviderService calendarProviderService, AbsenceMappingService absenceMappingService,
        AbsenceTimeConfiguration absenceTimeConfiguration) {

        this.applicationService = applicationService;
        this.commentService = commentService;
        this.accountInteractionService = accountInteractionService;
        this.signService = signService;
        this.mailService = mailService;
        this.calendarProviderService = calendarProviderService;
        this.absenceMappingService = absenceMappingService;
        this.absenceTimeConfiguration = absenceTimeConfiguration;
    }

    @Override
    public Application apply(Application application, Person applier, Optional<String> comment) {

        Person person = application.getPerson();

        application.setStatus(ApplicationStatus.WAITING);
        application.setApplier(applier);
        application.setApplicationDate(DateMidnight.now());

        signService.signApplicationByUser(application, applier);

        applicationService.save(application);

        LOG.info("Created application for leave: " + application.toString());

        // COMMENT
        Comment createdComment = commentService.create(application, ApplicationStatus.WAITING, comment, applier);

        // EMAILS

        // person himself applies for leave
        if (person.equals(applier)) {
            // person gets a confirmation email with the data of the application for leave
            mailService.sendConfirmation(application, createdComment);
        }
        // someone else (normally the office) applies for leave on behalf of the person
        else {
            // person gets an email that someone else has applied for leave on behalf
            mailService.sendAppliedForLeaveByOfficeNotification(application, createdComment);
        }

        // bosses gets email that a new application for leave has been created
        mailService.sendNewApplicationNotification(application, createdComment);

        // update remaining vacation days (if there is already a holidays account for next year)
        accountInteractionService.updateRemainingVacationDays(application.getStartDate().getYear(), person);

        Optional<String> eventId = calendarProviderService.addAbsence(new Absence(application,
                    absenceTimeConfiguration));

        if (eventId.isPresent()) {
            absenceMappingService.create(application, eventId.get());
        }

        return application;
    }


    @Override
    public Application allow(Application application, Person boss, Optional<String> comment) {

        application.setStatus(ApplicationStatus.ALLOWED);
        application.setBoss(boss);
        application.setEditedDate(DateMidnight.now());

        signService.signApplicationByBoss(application, boss);

        applicationService.save(application);

        LOG.info("Allowed application for leave: " + application.toString());

        Comment createdComment = commentService.create(application, ApplicationStatus.ALLOWED, comment, boss);

        mailService.sendAllowedNotification(application, createdComment);

        if (application.getHolidayReplacement() != null) {
            mailService.notifyHolidayReplacement(application);
        }

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(application.getId(),
                AbsenceType.VACATION);

        if (absenceMapping.isPresent()) {
            calendarProviderService.updateAbsence(new Absence(application, absenceTimeConfiguration),
                    absenceMapping.get().getEventId());
        }

        return application;
    }


    @Override
    public Application reject(Application application, Person boss, Optional<String> comment) {

        application.setStatus(ApplicationStatus.REJECTED);
        application.setBoss(boss);
        application.setEditedDate(DateMidnight.now());

        signService.signApplicationByBoss(application, boss);

        applicationService.save(application);

        LOG.info("Rejected application for leave: " + application.toString());

        Comment createdComment = commentService.create(application, ApplicationStatus.REJECTED, comment, boss);

        mailService.sendRejectedNotification(application, createdComment);

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(application.getId(),
                AbsenceType.VACATION);

        if (absenceMapping.isPresent()) {
            calendarProviderService.deleteAbsence(absenceMapping.get().getEventId());
            absenceMappingService.delete(absenceMapping.get());
        }

        return application;
    }


    @Override
    public Application cancel(Application application, Person canceller, Optional<String> comment) {

        boolean cancellingAllowedApplication = application.hasStatus(ApplicationStatus.ALLOWED);

        application.setCanceller(canceller);
        application.setCancelDate(DateMidnight.now());

        if (cancellingAllowedApplication) {
            application.setStatus(ApplicationStatus.CANCELLED);
        } else {
            application.setStatus(ApplicationStatus.REVOKED);
        }

        applicationService.save(application);

        LOG.info("Cancelled application for leave: " + application);

        Comment createdComment = commentService.create(application, application.getStatus(), comment, canceller);

        if (cancellingAllowedApplication) {
            // if allowed application has been cancelled, office and bosses get an email
            mailService.sendCancelledNotification(application, false, createdComment);
        }

        Person person = application.getPerson();

        if (!person.equals(canceller)) {
            // if application has been cancelled for someone on behalf,
            // the person gets an email regardless of application status
            mailService.sendCancelledNotification(application, true, createdComment);
        }

        accountInteractionService.updateRemainingVacationDays(application.getStartDate().getYear(), person);

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(application.getId(),
                AbsenceType.VACATION);

        if (absenceMapping.isPresent()) {
            calendarProviderService.deleteAbsence(absenceMapping.get().getEventId());
            absenceMappingService.delete(absenceMapping.get());
        }

        return application;
    }
}
