package org.mauro;

import org.mauro.model.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class Repository {

    private final Map<UUID, Session> sessions = new HashMap<>();


    public void save(final Session session) {
        sessions.put(session.getId(), session);
    }

    public Session get(final UUID id) {
        final Session session = sessions.get(id);
        if (session == null) {
            throw new NotFoundException(    "session not found");
        }
        return session;
    }

    public void delete(final UUID id) {
        sessions.remove(get(id));
    }
}
