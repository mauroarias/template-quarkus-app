package org.mauro.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Data
@RegisterForReflection
@Tag(description = "Session")
public class SessionRequest {
	private String country;
	private Language language;
}
