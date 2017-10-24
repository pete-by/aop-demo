package com.axamit.aop.osgi.weaver.utils;

public class ClassUtils {

    private static final String PATH_SEPARATOR = "/";
    private static final String PACKAGE_SEPARATOR = ".";
    private static final String CLASS_EXTENSION = ".class";

    public static String classNameToPath(String className) {
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR).concat(CLASS_EXTENSION);
    }

    public static String pathToClassName(String classResource) {
        return classResource.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR).replace(CLASS_EXTENSION, "");
    }

}
