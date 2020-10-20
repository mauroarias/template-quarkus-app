package org.mauro.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

@ApplicationScoped
public class ObjectMapperConfig {

	private final static ObjectMapper mapper = getMapper();

	@Produces
	public ObjectMapper objectMapper() {
		return mapper;
	}

	public static ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		mapper.setSerializationInclusion(NON_NULL);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}
