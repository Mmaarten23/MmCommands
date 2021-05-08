package com.mmaarten.mmcommands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MmCommandSignature {
    String name();
    MmCommandType type();
    String description();

    String[] aliases() default {};
    String permission() default "";
    String arguments() default "";
}
