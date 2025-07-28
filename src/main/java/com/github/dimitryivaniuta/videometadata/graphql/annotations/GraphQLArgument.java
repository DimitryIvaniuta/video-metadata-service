package com.github.dimitryivaniuta.videometadata.graphql.annotations;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphQLArgument {
    String value();
    String description() default "";
}