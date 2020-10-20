package org.mauro.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;

@Data
@RegisterForReflection
public class Session extends SessionRequest {

    private UUID id;
    private String country;
    private Language language;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private ZonedDateTime validUntil;

    public static Session fromRequest(final UUID id, final SessionRequest request) {
        final ZonedDateTime now = ZonedDateTime.now(UTC);
        final Session session = new Session();
        session.setId(id);
        session.setCountry(request.getCountry());
        session.setLanguage(request.getLanguage());
        session.setCreatedDate(now);
        session.setUpdatedDate(now);
        session.setValidUntil(now.plusMinutes(5));
        return session;
    }
}
