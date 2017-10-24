package com.axamit.aop.osgi.weaver.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BundleUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleUtils.class);

    public static void listResources(Bundle bundle) {

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

         /*
         * A bundle in the INSTALLED or UNINSTALLED state does not have a current wiring, adapting such
         * a bundle returns {@code null}.
         */
        if (bundleWiring != null) {

            LOGGER.info("Listing resources for bundle {} ", bundle.getSymbolicName());

            Collection<String> resources = bundleWiring.listResources("/" /* can be a package folder */, "*.class",
                    BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);

            for (String resource : resources) {
                LOGGER.info("Resource {} ", resource);
            }
        } else {
            LOGGER.error("Bundle has not yet started {} ", bundle.getSymbolicName());
        }
    }

    public static void listEntries(Bundle bundle) {

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring != null) {

            LOGGER.info("Listing entries for bundle {} ", bundle.getSymbolicName());

            List<URL> entries = bundleWiring.findEntries("/", "*.*", BundleWiring.FINDENTRIES_RECURSE);

            for (URL entry : entries) {
                LOGGER.info("Entry {} ", entry);
            }

        } else {
            LOGGER.error("Bundle has not yet started {} ", bundle.getSymbolicName());
        }

    }

    public static List<ClassLoader> getBundleClassLoaders(List<Bundle> bundles) {
        List<ClassLoader> classLoaders = new ArrayList<>();
        for(Bundle bundle : bundles) {
            ClassLoader classLoader = getBundleClassLoader(bundle);
            if(classLoader != null) {
                classLoaders.add(classLoader);
            }
        }

        return classLoaders;
    }

    public static ClassLoader getBundleClassLoader(Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if(bundleWiring != null) {
            return bundleWiring.getClassLoader();
        } else {
            LOGGER.error("Bundle has not yet started {} ", bundle.getSymbolicName());
        }
        return null;
    }

}
