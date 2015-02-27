package org.synyx.urlaubsverwaltung.core.sicknote;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.domain.Comment;
import org.synyx.urlaubsverwaltung.core.application.domain.DayLength;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.application.service.CommentService;
import org.synyx.urlaubsverwaltung.core.application.service.SignService;
import org.synyx.urlaubsverwaltung.core.calendar.OwnCalendarService;
import org.synyx.urlaubsverwaltung.core.mail.MailService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.sicknote.comment.SickNoteComment;
import org.synyx.urlaubsverwaltung.core.sicknote.comment.SickNoteCommentDAO;
import org.synyx.urlaubsverwaltung.core.sicknote.comment.SickNoteStatus;
import org.synyx.urlaubsverwaltung.web.application.ApplicationForLeaveForm;
import org.synyx.urlaubsverwaltung.web.sicknote.SickNoteConvertForm;

import java.math.BigDecimal;

import java.util.List;


/**
 * Service for handling {@link SickNote}s.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Service
@Transactional
public class SickNoteService {

    @Value("${sicknote.sickPay.limit}")
    private int sickPayLimit;

    @Value("${sicknote.sickPay.notification}")
    private int sickPayNotificationTime;

    private SickNoteDAO sickNoteDAO;
    private SickNoteCommentDAO commentDAO;
    private OwnCalendarService calendarService;
    private ApplicationService applicationService;
    private SignService signService;
    private CommentService commentService;
    private MailService mailService;

    @Autowired
    public SickNoteService(SickNoteDAO sickNoteDAO, SickNoteCommentDAO commentDAO, OwnCalendarService calendarService,
        ApplicationService applicationService, SignService signService, CommentService commentService,
        MailService mailService) {

        this.sickNoteDAO = sickNoteDAO;
        this.commentDAO = commentDAO;
        this.calendarService = calendarService;
        this.applicationService = applicationService;
        this.signService = signService;
        this.commentService = commentService;
        this.mailService = mailService;
    }


    public SickNoteService() {

        /* needed by Spring */
    }

    private void save(SickNote sickNote) {

        sickNote.setLastEdited(DateMidnight.now());

        sickNoteDAO.save(sickNote);
    }


    public void touch(SickNote sickNote, SickNoteStatus status, Person loggedUser) {

        sickNote.setActive(true);
        setWorkDays(sickNote);
        save(sickNote);

        SickNoteComment comment = new SickNoteComment();
        addComment(sickNote.getId(), comment, status, loggedUser);
    }


    private void setWorkDays(SickNote sickNote) {

        BigDecimal workDays;

        DateMidnight startDate = sickNote.getStartDate();
        DateMidnight endDate = sickNote.getEndDate();

        if (startDate != null && endDate != null) {
            Person person = sickNote.getPerson();

            workDays = calendarService.getWorkDays(DayLength.FULL, startDate, endDate, person);
        } else {
            workDays = BigDecimal.ZERO;
        }

        sickNote.setWorkDays(workDays);
    }


    public void addComment(Integer sickNoteId, SickNoteComment comment, SickNoteStatus status, Person author) {

        SickNote sickNote = getById(sickNoteId);

        comment.setDate(DateMidnight.now());
        comment.setStatus(status);
        comment.setPerson(author);

        commentDAO.save(comment);

        sickNote.addComment(comment);

        save(sickNote);
    }


    public SickNote getById(Integer id) {

        return sickNoteDAO.findOne(id);
    }


    public List<SickNote> getByPersonAndPeriod(Person person, DateMidnight from, DateMidnight to) {

        return sickNoteDAO.findByPersonAndPeriod(person, from.toDate(), to.toDate());
    }


    public List<SickNote> getByPeriod(DateMidnight from, DateMidnight to) {

        return sickNoteDAO.findByPeriod(from.toDate(), to.toDate());
    }


    public void convertSickNoteToVacation(SickNoteConvertForm sickNoteConvertForm, SickNote sickNote,
        Person loggedUser) {

        Application applicationForLeave = sickNoteConvertForm.generateApplicationForLeave();
        applicationForLeave.setApplier(loggedUser);

        BigDecimal workDays = calendarService.getWorkDays(applicationForLeave.getHowLong(),
                applicationForLeave.getStartDate(), applicationForLeave.getEndDate(), applicationForLeave.getPerson());
        applicationForLeave.setDays(workDays);

        signService.signApplicationByUser(applicationForLeave, loggedUser);

        applicationService.save(applicationForLeave);

        commentService.saveComment(new Comment(), loggedUser, applicationForLeave);

        setSickNoteInactive(sickNote);

        save(sickNote);

        SickNoteComment sickNoteComment = new SickNoteComment();
        addComment(sickNote.getId(), sickNoteComment, SickNoteStatus.CONVERTED_TO_VACATION, loggedUser);

        mailService.sendSickNoteConvertedToVacationNotification(applicationForLeave);
    }


    public void cancel(SickNote sickNote, Person loggedUser) {

        setSickNoteInactive(sickNote);
        save(sickNote);

        SickNoteComment sickNoteComment = new SickNoteComment();
        addComment(sickNote.getId(), sickNoteComment, SickNoteStatus.CANCELLED, loggedUser);
    }


    void setSickNoteInactive(SickNote sickNote) {

        sickNote.setWorkDays(BigDecimal.ZERO);
        sickNote.setActive(false);
    }


    public List<SickNote> getSickNotesReachingEndOfSickPay() {

        DateMidnight endDate = DateMidnight.now().plusDays(sickPayNotificationTime);

        return sickNoteDAO.findSickNotesByMinimumLengthAndEndDate(sickPayLimit, endDate.toDate());
    }
}
