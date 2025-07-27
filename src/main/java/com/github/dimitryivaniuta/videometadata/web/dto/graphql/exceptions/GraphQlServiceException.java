package com.github.dimitryivaniuta.videometadata.web.dto.graphql.exceptions;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;

/**
 * Converts a service exception into a GraphQL error with a clear message.
 */
public class GraphQlServiceException extends RuntimeException implements GraphQLError {

    public GraphQlServiceException(String message) {
        super(message);
    }

    public GraphQlServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }
    @Override
    public ErrorType getErrorType() {
        return ErrorType.DataFetchingException;
    }
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
