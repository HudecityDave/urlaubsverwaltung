package org.synyx.urlaubsverwaltung.core.sicknote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


/**
 * Repository for {@link SickNoteComment} entities.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public interface SickNoteCommentDAO extends JpaRepository<SickNoteComment, Integer> {

    @Query("select x from SickNoteComment x where x.sickNote = ?1")
    List<SickNoteComment> getCommentsBySickNote(SickNote sickNote);
}
