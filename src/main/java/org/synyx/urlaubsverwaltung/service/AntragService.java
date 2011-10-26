package org.synyx.urlaubsverwaltung.service;

import java.util.List;

import org.synyx.urlaubsverwaltung.domain.Antrag;
import org.synyx.urlaubsverwaltung.domain.Person;
import org.synyx.urlaubsverwaltung.domain.State;

/**
 * 
 * use this service to access to the request-data (who, how many days, ...)
 * 
 * @author johannes
 * 
 */
public interface AntragService {

	/**
	 * use this to save or edit an existing request
	 * 
	 * @param antrag
	 *            the new version of the request
	 */
	void save(Antrag antrag);

	/**
	 * use this to set a request to approved (only boss)
	 * 
	 * @param antrag
	 *            the request to be edited
	 */
	void approve(Antrag antrag);

	/**
	 * use this to set a request to declined (only boss)
	 * 
	 * @param antrag
	 *            the request to be edited
	 */
	void decline(Antrag antrag);

	/**
	 * use this to get a certain request if you know its id
	 * 
	 * @param id
	 *            the id of the request
	 * @return returns the request as an Antrag-object
	 */
	Antrag getRequestById(Integer id);

	/**
	 * use this to get all requests of a certain person
	 * 
	 * @param person
	 *            the person you want to get the requests of
	 * @return returns all requests of a person as a list of Antrag-objects
	 */
	List<Antrag> getAllRequestsForPerson(Person person);

	/**
	 * use this to get all requests of the system
	 * 
	 * @return returns all requests of the system as a list of antrag-objects
	 */
	List<Antrag> getAllRequests();

	/**
	 * use this to get all requests of a certain state(like 'waiting')
	 * 
	 * @return returns all requests of a state as a list of antrag-objects
	 */
	List<Antrag> getAllRequestsByState(State state);

}