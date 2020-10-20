package org.mauro;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.mauro.model.Session;
import org.mauro.model.SessionRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/session")
@ApplicationScoped
@Tag(name = "session", description = "session API controller")
public class Controller {

	private final Service service;

	@Inject
	Controller(final Service service) {
		this.service = service;
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@Operation(summary = "New user session", description = "Create and persist a new user session")
	@APIResponses({@APIResponse(responseCode = "200", description = "session created"),
				   @APIResponse(name = "400", responseCode = "400", description = "Bad request, something was wrong in the request."),
				   @APIResponse(name = "500", responseCode = "500", description = "Internal service error") })
	public Session createSession(final SessionRequest request) {
		return service.createSession(request);
	}

	@GET
	@Path("/{id}")
	@Produces(APPLICATION_JSON)
	@Operation(summary = "get a session", description = "Get an existing user session")
	@APIResponses({@APIResponse(responseCode = "200", description = "session returned", ref = "Session"),
 				   @APIResponse(name = "400", responseCode = "400", description = "Bad request, something was wrong in the request."),
				   @APIResponse(name = "404", responseCode = "404", description = "Resource not found"),
				   @APIResponse(name = "500", responseCode = "500", description = "Internal service error") })
	public Session getSession(@PathParam("id") String id) {
		return service.getSession(convertId(id));
	}

	@DELETE
	@Path("/{id}")
	@Operation(summary = "Delete a session", description = "Delete an existing user session")
	@APIResponses({@APIResponse(responseCode = "200", description = "session deleted"),
				   @APIResponse(name = "400", responseCode = "400", description = "Bad request, something was wrong in the request."),
				   @APIResponse(name = "404", responseCode = "404", description = "Resource not found"),
				   @APIResponse(name = "500", responseCode = "500", description = "Internal service error") })
	public void deleteSession(@PathParam("id") String id) {
		service.deleteSession(convertId(id));
	}

	@PATCH
	@Path("/{id}/refresh")
	@Produces(APPLICATION_JSON)
	@Operation(summary = "New user session", description = "Create and persist a new user session")
	@APIResponses({@APIResponse(responseCode = "200", description = "session created"),
				   @APIResponse(name = "400", responseCode = "400", description = "Bad request, something was wrong in the request."),
				   @APIResponse(name = "404", responseCode = "404", description = "Resource not found"),
				   @APIResponse(name = "500", responseCode = "500", description = "Internal service error") })
	public Session refreshSession(@PathParam("id") String id) {
		return service.update(convertId(id));
	}

	@GET
	@Path("/{id}/is-valid")
	@Produces(APPLICATION_JSON)
	@Operation(summary = "Check if a session is valid", description = "Check if a session is valid")
	@APIResponses({@APIResponse(responseCode = "200", description = "valid session returned"),
				   @APIResponse(name = "400", responseCode = "400", description = "Bad request, something was wrong in the request."),
				   @APIResponse(name = "404", responseCode = "404", description = "Resource not found"),
				   @APIResponse(name = "500", responseCode = "500", description = "Internal service error") })
	public Boolean isValidSession(@PathParam("id") String id) {
		return service.isValid(convertId(id));
	}

	private UUID convertId(final String id) {
		try {
			return UUID.fromString(id);
		} catch (Exception ex) {
			throw new BadRequestException("invalid UUID");
		}
	}
}
