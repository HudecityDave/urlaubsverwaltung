package org.synyx.urlaubsverwaltung.web.statistics;

import org.synyx.urlaubsverwaltung.core.calendar.WorkDaysService;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNoteType;

import java.math.BigDecimal;

import java.util.List;


/**
 * Build a sick days statistic for a certain year and person.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class SickDaysOverview {

    private final SickDays sickDays;

    private final SickDays childSickDays;

    public SickDaysOverview(List<SickNote> sickNotes, WorkDaysService calendarService) {

        this.sickDays = new SickDays();
        this.childSickDays = new SickDays();

        for (SickNote sickNote : sickNotes) {
            if (!sickNote.isActive()) {
                continue;
            }

            if (sickNote.getType().equals(SickNoteType.SICK_NOTE_CHILD)) {
                this.childSickDays.addDays(SickDays.SickDayType.TOTAL, getTotalDays(sickNote, calendarService));

                if (sickNote.isAubPresent()) {
                    this.childSickDays.addDays(SickDays.SickDayType.WITH_AUB,
                        getDaysWithAUB(sickNote, calendarService));
                }
            } else {
                this.sickDays.addDays(SickDays.SickDayType.TOTAL, getTotalDays(sickNote, calendarService));

                if (sickNote.isAubPresent()) {
                    this.sickDays.addDays(SickDays.SickDayType.WITH_AUB, getDaysWithAUB(sickNote, calendarService));
                }
            }
        }
    }

    private BigDecimal getTotalDays(SickNote sickNote, WorkDaysService calendarService) {

        return calendarService.getWorkDays(sickNote.getDayLength(), sickNote.getStartDate(), sickNote.getEndDate(),
                sickNote.getPerson());
    }


    private BigDecimal getDaysWithAUB(SickNote sickNote, WorkDaysService calendarService) {

        return calendarService.getWorkDays(sickNote.getDayLength(), sickNote.getAubStartDate(),
                sickNote.getAubEndDate(), sickNote.getPerson());
    }


    public SickDays getSickDays() {

        return sickDays;
    }


    public SickDays getChildSickDays() {

        return childSickDays;
    }
}
