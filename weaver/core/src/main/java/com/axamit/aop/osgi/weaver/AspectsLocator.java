package com.axamit.aop.osgi.weaver;

import com.axamit.aop.osgi.weaver.utils.ClassUtils;
import org.aspectj.lang.annotation.Aspect;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This helper class allows autodiscovery of aspect classes in bundles. Please note, that only annotation defined aspects
 * (with @Aspect) can be discovered.
 */
public class AspectsLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AspectsLocator.class);

    public URL[] findAspectClasses(Bundle aspectsBundle) {

        BundleWiring bundleWiring = aspectsBundle.adapt(BundleWiring.class);
        if (bundleWiring != null) {

            Collection<String> classResources = bundleWiring.listResources("/", "*.class",
                    BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);

            List<URL> urls = new ArrayList<>();
            for (String classResource : classResources) {

                String className = ClassUtils.pathToClassName(classResource);

                try {
                    Class clazz = bundleWiring.getClassLoader().loadClass(className);
                    if (clazz.isAnnotationPresent(Aspect.class)) {

                        URL aspectUrl = bundleWiring.getClassLoader().getResource(classResource);
                        if (aspectUrl != null) {
                            urls.add(aspectUrl);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            return urls.toArray(new URL[]{});
        } else {
            LOGGER.error("Bundle with Aspects has not been yet started");
        }

        return new URL[] {};
    }

        /*
    FastClasspathScanner does not work :(
    public URL[] findAspectClasses2(Bundle aspectsBundle) {

        BundleWiring bundleWiring = aspectsBundle.adapt(BundleWiring.class);

        FastClasspathScanner fastClasspathScanner = new FastClasspathScanner(
                "-org.osgi",
                "-org.felix"
        )
                .setAnnotationVisibility(RetentionPolicy.RUNTIME)
                .addClassLoader(bundleWiring.getClassLoader());

        Class aspectAnnotationClass = null;
        try {
            aspectAnnotationClass = bundleWiring.getClassLoader().loadClass(Aspect.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        List<URL> urls = new ArrayList<>();
        fastClasspathScanner.matchClassesWithAnnotation(
                aspectAnnotationClass,
                clazz -> {

                    String classResource = ClassUtils.classNameToPath(clazz.getName());
                    URL aspectUrl = bundleWiring.getClassLoader().getResource(classResource);
                    if (aspectUrl != null) {
                        urls.add(aspectUrl);
                    }

                }).scan(1);

        return urls.toArray(new URL[]{});
    } */

}
