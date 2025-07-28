package com.github.dimitryivaniuta.videometadata.graphql.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphQLMutation {
    String value() default "";
    String description() default "";
}