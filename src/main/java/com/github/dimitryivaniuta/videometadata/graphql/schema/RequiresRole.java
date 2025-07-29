package com.github.dimitryivaniuta.videometadata.graphql.schema;

import java.lang.annotation.*;

/** Declares that the GraphQL operation requires the caller to have ANY of the listed roles. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String[] value();
}