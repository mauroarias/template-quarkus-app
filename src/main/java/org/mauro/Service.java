package org.mauro;

import org.mauro.model.Session;
import org.mauro.model.SessionRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;

@ApplicationScoped
public class Service {

	private final Repository repository;

	@Inject
	public Service(final Repository repository) {
		this.repository = repository;
	}

	public Session createSession(final SessionRequest request) {
		final UUID id = UUID.randomUUID();
		final Session session = Session.fromRequest(id, request);
		repository.save(session);
		return session;
	}

	public Session getSession(final UUID id) {
		return repository.get(id);
	}

	public void deleteSession(final UUID id) {
		repository.delete(id);
	}

	public Session update(final UUID id) {
		final Session session = repository.get(id);
		if(isValid(session.getValidUntil())) {
			session.setValidUntil(ZonedDateTime.now(UTC).plusMinutes(5));
			repository.save(session);
			return session;
		} else {
			throw new BadRequestException("Session is expired");
		}
	}

	public boolean isValid(final UUID id) {
		return isValid(repository.get(id).getValidUntil());
	}

	private boolean isValid(final ZonedDateTime date) {
		return date.isAfter(ZonedDateTime.now(UTC));
	}
}
