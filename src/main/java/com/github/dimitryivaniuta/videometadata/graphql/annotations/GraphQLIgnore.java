package com.github.dimitryivaniuta.videometadata.graphql.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GraphQLIgnore {
}