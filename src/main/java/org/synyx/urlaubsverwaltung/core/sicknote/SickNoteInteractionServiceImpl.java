package org.synyx.urlaubsverwaltung.core.sicknote;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.application.service.CommentService;
import org.synyx.urlaubsverwaltung.core.application.service.SignService;
import org.synyx.urlaubsverwaltung.core.mail.MailService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.settings.CalendarSettings;
import org.synyx.urlaubsverwaltung.core.settings.SettingsService;
import org.synyx.urlaubsverwaltung.core.sicknote.comment.SickNoteCommentService;
import org.synyx.urlaubsverwaltung.core.sicknote.comment.SickNoteStatus;
import org.synyx.urlaubsverwaltung.core.sync.CalendarSyncService;
import org.synyx.urlaubsverwaltung.core.sync.absence.Absence;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceMapping;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceMappingService;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceTimeConfiguration;
import org.synyx.urlaubsverwaltung.core.sync.absence.AbsenceType;

import java.util.Optional;


/**
 * Implementation for {@link org.synyx.urlaubsverwaltung.core.sicknote.SickNoteInteractionService}.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Service
@Transactional
public class SickNoteInteractionServiceImpl implements SickNoteInteractionService {

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;

    private final SickNoteService sickNoteService;
    private final SickNoteCommentService sickNoteCommentService;
    private final ApplicationService applicationService;
    private final CommentService applicationCommentService;
    private final SignService signService;
    private final MailService mailService;
    private final CalendarSyncService calendarSyncService;
    private final AbsenceMappingService absenceMappingService;
    private final SettingsService settingsService;

    @Autowired
    public SickNoteInteractionServiceImpl(SickNoteService sickNoteService,
        SickNoteCommentService sickNoteCommentService, ApplicationService applicationService,
        CommentService applicationCommentService, SignService signService, MailService mailService,
        CalendarSyncService calendarSyncService, AbsenceMappingService absenceMappingService,
        SettingsService settingsService) {

        this.sickNoteService = sickNoteService;
        this.sickNoteCommentService = sickNoteCommentService;
        this.applicationService = applicationService;
        this.applicationCommentService = applicationCommentService;
        this.signService = signService;
        this.mailService = mailService;
        this.calendarSyncService = calendarSyncService;
        this.absenceMappingService = absenceMappingService;
        this.settingsService = settingsService;
    }

    @Override
    public SickNote create(SickNote sickNote, Person creator) {

        sickNote.setActive(ACTIVE);
        sickNote.setLastEdited(DateMidnight.now());

        sickNoteService.save(sickNote);
        sickNoteCommentService.create(sickNote, SickNoteStatus.CREATED, Optional.<String>empty(), creator);

        Optional<String> eventId = calendarSyncService.addAbsence(new Absence(sickNote));

        if (eventId.isPresent()) {
            absenceMappingService.create(sickNote, eventId.get());
        }

        return sickNote;
    }


    @Override
    public SickNote update(SickNote sickNote, Person editor) {

        sickNote.setActive(ACTIVE);
        sickNote.setLastEdited(DateMidnight.now());

        sickNoteService.save(sickNote);
        sickNoteCommentService.create(sickNote, SickNoteStatus.EDITED, Optional.<String>empty(), editor);

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(sickNote.getId(),
                AbsenceType.SICKNOTE);

        if (absenceMapping.isPresent()) {
            calendarSyncService.update(new Absence(sickNote), absenceMapping.get().getEventId());
        }

        return sickNote;
    }


    @Override
    public SickNote convert(SickNote sickNote, Application application, Person converter) {

        // create an application for leave that is allowed directly
        application.setApplier(converter);
        application.setStatus(ApplicationStatus.ALLOWED);

        signService.signApplicationByBoss(application, converter);
        applicationService.save(application);
        applicationCommentService.create(application, ApplicationStatus.ALLOWED, Optional.<String>empty(), converter);
        mailService.sendSickNoteConvertedToVacationNotification(application);

        // make sick note inactive
        sickNote.setActive(INACTIVE);
        sickNote.setLastEdited(DateMidnight.now());

        sickNoteService.save(sickNote);
        sickNoteCommentService.create(sickNote, SickNoteStatus.CONVERTED_TO_VACATION, Optional.<String>empty(),
            converter);

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(sickNote.getId(),
                AbsenceType.SICKNOTE);

        if (absenceMapping.isPresent()) {
            String eventId = absenceMapping.get().getEventId();
            CalendarSettings calendarSettings = settingsService.getSettings().getCalendarSettings();

            calendarSyncService.update(new Absence(application, new AbsenceTimeConfiguration(calendarSettings)),
                eventId);
            absenceMappingService.delete(absenceMapping.get());
            absenceMappingService.create(application, eventId);
        }

        return sickNote;
    }


    @Override
    public SickNote cancel(SickNote sickNote, Person canceller) {

        sickNote.setActive(INACTIVE);
        sickNote.setLastEdited(DateMidnight.now());

        sickNoteService.save(sickNote);
        sickNoteCommentService.create(sickNote, SickNoteStatus.CANCELLED, Optional.<String>empty(), canceller);

        Optional<AbsenceMapping> absenceMapping = absenceMappingService.getAbsenceByIdAndType(sickNote.getId(),
                AbsenceType.SICKNOTE);

        if (absenceMapping.isPresent()) {
            calendarSyncService.deleteAbsence(absenceMapping.get().getEventId());
            absenceMappingService.delete(absenceMapping.get());
        }

        return sickNote;
    }
}
