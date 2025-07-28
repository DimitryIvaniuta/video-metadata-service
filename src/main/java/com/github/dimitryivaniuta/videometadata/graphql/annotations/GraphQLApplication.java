package com.github.dimitryivaniuta.videometadata.graphql.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphQLApplication {
    String namespace() default "";
}
