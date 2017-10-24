package com.axamit.aop.target.hidden.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivateClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateClass.class);

    PojoClass foo(PojoArg param0, int param2, double param3) {
        LOGGER.info("foo method has been called with title:" + param0.getText());
        return new PojoClass( "ECHO: " + param0.getText(), 10);
    }

}
