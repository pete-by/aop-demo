package com.axamit.aop.osgi.weaver;

import com.axamit.aop.osgi.weaver.utils.*;
import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class Weaver implements WeavingHook, WovenClassListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Weaver.class);

    private static final String AXAMIT_AOP_PACKAGE = "com.axamit.aop.osgi;";
    private static final String AXAMIT_AOP_BUNDLE = "com.axamit.aop.core";
    private static final String CLASSPATH_SEPARATOR = ";";

    /**
     * target bundles to weave, TODO: move to settings in BundleContext
     */
    private String includedBundles = "com.axamit.aop.example-target;";
    /**
     *  required during weaving process, probably we can add "com.axamit.logging.osgi" to this Weaver's bundle import as it is generic
     *  or alternatively we can include it as a dependency of a discovered aspects bundle.
     */
    private String requiredBundles = "com.axamit.logging.osgi;org.apache.servicemix.bundles.aspectj";
    /**
     * List of bundles that contain, or may contain aspects. NB: provide the aspects bundle with correct symbolic name!
     * TODO: should be moved to the configuration
     */
    private String aspectBundles = "com.axamit.aop.example-logging;";

    /**
     * Packages that contain classes to be excluded from weaving
     */
    private String[] excludedPackages;

    /**
     * Packages that contain classes to be included from weaving
     */
    private String[] includedPackages;

    private List<URL> aspectURLs = new ArrayList<>(); // stores class URLs of discovered Aspects
    private List<Long> aspectBundleIds = new ArrayList<>(); // stores discovered aspect bundle ids
    private List<Long> requiredBundleIds = new ArrayList<>(); // stores required bundle ids (for weaving classloader)

    private AspectsLocator aspectsLocator;

    public Weaver(BundleContext context) {

        aspectsLocator = new AspectsLocator();

        scanBundles(context);

        String includedPackagesProperty = "com.axamit.aop.target;";
        String exludedPackagesProperty = AXAMIT_AOP_PACKAGE + "org.osgi;org.apache.felix;aj.org;org.aspectj";
        excludedPackages = exludedPackagesProperty.split(CLASSPATH_SEPARATOR);
        includedPackages = includedPackagesProperty.split(CLASSPATH_SEPARATOR);
    }

    private void scanBundles(BundleContext context) {

        BundleStartLevel thisBundleStartLevel = context.getBundle().adapt(BundleStartLevel.class);
        final int startLevel = thisBundleStartLevel.getStartLevel();

        Bundle[] bundles = context.getBundles();

        for (Bundle bundle : bundles) {

            // Bundles scanning is resource consuming, so lets only weave the bundles we are interested in
            // NB: This is commented out because we do weaving when weave method is called by the Framework, not up front
            // This may change later if we decide to perform weaving upfront
            /*
            if (isIncluded(bundle)) {
                scanBundle(bundle);
            }
            */

            // Find bundle with Aspects and scan the classpath to get them
            if(isAspectsBundle(bundle)) {

                if(bundle.getState() != Bundle.ACTIVE) {

                    LOGGER.warn("Aspects bundle {} is not active", bundle.getSymbolicName());

                    BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
                    if( startLevel <= bundleStartLevel.getStartLevel() ) {
                        LOGGER.info("Aspects bundle start level is higher or the same as AOP bundle.\n Forcing bundle {} to start immediately",
                                bundle.getSymbolicName());
                    }

                    try {
                        bundle.start();

                        int maxIterations = 1000;
                        while(bundle.getState() != Bundle.ACTIVE) {
                            if(bundle.getState() != Bundle.STARTING || maxIterations <= 0) {
                                throw new BundleException("Problem starting bundle " + bundle.getSymbolicName());
                            }
                            maxIterations--;
                        }

                        LOGGER.info("Aspects bundle {} is now active", bundle.getSymbolicName());

                    } catch (BundleException e) {
                         LOGGER.error("Cannot start aspects bundle {}", bundle.getSymbolicName(), e);
                    }

                }

                BundleUtils.listEntries(bundle);
                List<URL> foundAspects = Arrays.asList(aspectsLocator.findAspectClasses(bundle));
                if(!foundAspects.isEmpty()) {
                    aspectURLs.addAll(foundAspects);
                    aspectBundleIds.add(bundle.getBundleId());
                }

            } else if (isRequiredBundle(bundle)){
                requiredBundleIds.add(bundle.getBundleId());
            }
        }
    }

    private boolean isIncluded(Bundle bundle) {
        return includedBundles.contains(bundle.getSymbolicName())
                && !AXAMIT_AOP_BUNDLE.equals(bundle.getSymbolicName());
    }

    private boolean isAspectsBundle(Bundle bundle) {
        return aspectBundles.contains( bundle.getSymbolicName() )
                && !AXAMIT_AOP_BUNDLE.equals(bundle.getSymbolicName());
    }

    private boolean isRequiredBundle(Bundle bundle) {
        return requiredBundles.contains( bundle.getSymbolicName() );
    }

    /**
     * Just a dumping method to list resources (classpath) and entries (locally bundled)
     * @param bundle to scan
     */
    private void scanBundle(Bundle bundle) {
        BundleUtils.listResources(bundle);
        BundleUtils.listEntries(bundle);
    }

    @Override
    public void weave(WovenClass wovenClass) {

        final String className = wovenClass.getClassName();

        // this current Bundle
        Bundle weaverBundle = FrameworkUtil.getBundle(Weaver.class);
        BundleContext bundleContext = weaverBundle.getBundleContext();

        if (shouldWeave(wovenClass)) { // check if this class should be weaved

            LOGGER.info("Candidate for weaving {}", className);

            final BundleWiring bundleWiring = wovenClass.getBundleWiring();
            if(bundleWiring != null) { // bundle is not in Active state

                final Bundle bundle = bundleWiring.getBundle();
                final ClassLoader bundleClassLoader =  bundleWiring.getClassLoader();

                String classResource = ClassUtils.classNameToPath(className); // get class' resource path
                URL targetUrl = bundleClassLoader.getResource( classResource );
                final List<URL> classUrls = new ArrayList<>();
                classUrls.add(targetUrl);

                // build list of earlier discovered aspect bundles to form a composite classpath
                List<Bundle> aspectBundles = new ArrayList<>();
                for(Long aspectBundleId : aspectBundleIds) {
                    Bundle aspectBundle = bundleContext.getBundle(aspectBundleId);
                    if(aspectBundle != null) {
                        aspectBundles.add(aspectBundle);
                    }
                }

                List<Bundle> requiredBundles = new ArrayList<>();
                for(Long requiredBundleId : requiredBundleIds) {
                    Bundle requiredBundle = bundleContext.getBundle(requiredBundleId);
                    if(requiredBundle != null) {
                        requiredBundles.add(requiredBundle);
                    }
                }

                requiredBundles.addAll(aspectBundles); // combine required + aspect bundles

                try {
                    OsgiWeavingClassLoader weavingClassLoader = new OsgiWeavingClassLoader(classUrls, aspectURLs, bundle, requiredBundles);
                    ClassLoaderWeavingAdaptor weavingAdaptor = new OsgiClassLoaderWeavingAdaptor();
                    weavingAdaptor.initialize(bundleWiring.getClassLoader(), new OsgiWeavingContext(weavingClassLoader));

                    byte[] originalClassBytes = wovenClass.getBytes(); // original, not-modified bytecode
                    byte[] wovenClassBytes = weavingAdaptor.weaveClass( // enhanced class' bytecode
                            className,
                            originalClassBytes);

                    if (wovenClassBytes != null && wovenClassBytes.length > 0 // unknown error during weaving
                            && !Arrays.equals(originalClassBytes, wovenClassBytes)) { // no weaving happened, e.g. because there are no matching pointcuts

                        wovenClass.setBytes(wovenClassBytes); // update class bytecode
                        List<String> imports = wovenClass.getDynamicImports();

                        // add dynamic AspectJ imports or other dependencies
                        // TODO: load from resource file or from aspectj bundle resources
                        imports.add("aj.org.objectweb.asm");
                        imports.add("aj.org.objectweb.asm.signature");
                        imports.add("org.aspectj.runtime");
                        imports.add("org.aspectj.apache.bcel");
                        imports.add("org.aspectj.apache.bcel.classfile");
                        imports.add("org.aspectj.apache.bcel.classfile.annotation");
                        imports.add("org.aspectj.apache.bcel.generic");
                        imports.add("org.aspectj.apache.bcel.util");
                        imports.add("org.aspectj.asm");
                        imports.add("org.aspectj.asm.internal");
                        imports.add("org.aspectj.bridge");
                        imports.add("org.aspectj.bridge.context");
                        imports.add("org.aspectj.internal.lang.annotation");
                        imports.add("org.aspectj.internal.lang.reflect");
                        imports.add("org.aspectj.lang");
                        imports.add("org.aspectj.lang.annotation");
                        imports.add("org.aspectj.lang.annotation.control");
                        imports.add("org.aspectj.lang.internal.lang");
                        imports.add("org.aspectj.lang.reflect");
                        imports.add("org.aspectj.runtime");
                        imports.add("org.aspectj.runtime.internal");
                        imports.add("org.aspectj.runtime.internal.cflowstack");
                        imports.add("org.aspectj.runtime.reflect");
                        imports.add("org.aspectj.util");
                        imports.add("org.aspectj.weaver");
                        imports.add("org.aspectj.weaver.ast");
                        imports.add("org.aspectj.weaver.bcel");
                        imports.add("org.aspectj.weaver.bcel.asm");
                        imports.add("org.aspectj.weaver.loadtime");
                        imports.add("org.aspectj.weaver.loadtime.definition");
                        imports.add("org.aspectj.weaver.ltw");
                        imports.add("org.aspectj.weaver.model");
                        imports.add("org.aspectj.weaver.patterns");
                        imports.add("org.aspectj.weaver.reflect");
                        imports.add("org.aspectj.weaver.tools");
                        imports.add("org.aspectj.weaver.tools.cache");

                        // Add required Aspect imports
                        for(URL aspectURL : aspectURLs) {
                            String aspectClassResource = aspectURL.getPath().substring(1); // trim leading /
                            String aspectClassName = ClassUtils.pathToClassName(aspectClassResource);
                            String aspectPackage = getPackage(aspectClassName);
                            imports.add( aspectPackage );
                        }

                        // register processed class in classloader's cache for future retrieval
                        weavingClassLoader.acceptClass(className, originalClassBytes, wovenClassBytes);
                    }

                    wovenClasses.put(className, new ArrayList<>());
                    for(Map.Entry<String, byte[]> generatedClass : weavingClassLoader.getGeneratedClasses().entrySet()) {

                        String name = generatedClass.getKey();
                        if(!name.equals(className)) { // only newly generated classes
                            byte[] classBytes = generatedClass.getValue();
                            WovenClassInfo wovenClassInfo = new WovenClassInfo(bundle.getBundleId(),
                                    name,
                                    classBytes);

                            wovenClasses.get(className).add(wovenClassInfo);
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }

            }

        } else {
            LOGGER.trace("Class {} has not been woven", className);
        }

    }

    private static class WovenClassInfo {

        private long bundleId;

        private String name;

        private byte[] bytes;

        public WovenClassInfo(long bundleId, String name, byte[] bytes) {
            this.bundleId = bundleId;
            this.name = name;
            this.bytes = bytes;
        }

        public long getBundleId() {
            return bundleId;
        }

        public String getName() {
            return name;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }

    private Map<String, List<WovenClassInfo>> wovenClasses = Collections.synchronizedMap( new HashMap<>() );

    public boolean shouldWeave(final WovenClass wovenClass) {
        LOGGER.trace("Checking exclusion/inclusion for weaving {}", wovenClass.getClassName());
        boolean result = !isClassExcluded(wovenClass);
        if(!result) {
            LOGGER.trace("Class {} was excluded from weaving", wovenClass.getClassName());
        }
        return result;
    }

    private boolean isClassExcluded(WovenClass wovenClass) {
        final String className = wovenClass.getClassName();

        // We should not weave this bundle's classes, so they should be always excluded
        // NB: we have to check the package without using other classes (e.g. ClassUtils), as it may throw ClassNotFoundException
        if(wovenClass.getClassName().contains(AXAMIT_AOP_PACKAGE)) {
            LOGGER.info("Cannot weave classes from Axamit AOP Bundle, class: {}", className);
            return false;
        }

        String classPackage = getPackage(className);
        boolean result = !isIncluded(classPackage);

        if(!result) {
            LOGGER.trace("Class is excluded by package exclusion rules {}", className);
        }

        return result;
    }

    public static String getPackage(String className) {
        int lastDotIndex = className.lastIndexOf(".");
        String classPackage = className.substring(0,
                lastDotIndex > 0
                        ? lastDotIndex
                        : className.length());
        return classPackage;
    }

    /**
     * Checks if package is excluded from weaving explicitly
     * @param pckg test package
     * @return {@code true}, if package is excluded
     */
    private boolean isPackageExcluded(String pckg) {
        for(String excludedPackage : excludedPackages) {
             if(!"".equals(excludedPackage) && pckg.contains(excludedPackage)) {
                 return true;
             }
        }
        return false;
    }

    /**
     * Checks if package is included for weaving exlicitly or implicitly.
     * Explicit inclusion rules have priority over exclusion rules.
     * @param pckg test package
     * @return {@code true}, if package is included
     */
    private boolean isIncluded(String pckg) {

        if(includedPackages.length > 0) { // if included packages are not empty

            // then package must be listed explicitly
            for (String includedPackage : includedPackages) {
                if (!"".equals(pckg) && pckg.contains(includedPackage)) {
                    return true;
                }
            }

            return false; // not found among white listed packages
        }

        // if white-listed packages are empty, check if the package is excluded explicitly
        return !isPackageExcluded(pckg);
    }

    /**
     * Must be created in advance, otherwise won't be available in modified method.
     */
    private final ClassLoaderHelper classLoaderHelper = new ClassLoaderHelper();

    @Override
    public void modified(WovenClass wovenClass) {

        LOGGER.info("Class {} is modified, state: {}", wovenClass.getClassName(), wovenClass.getState());

        // We should define closure classes only when woven class is defined and dynamic imports are added to the bundle
        if(wovenClass.getState() != WovenClass.DEFINED) {
            return;
        }

        // Add generated closure classes $AjcClosure{N} (missing in original bundle classloader) to the target bundle
        List<WovenClassInfo> wovenClassInfoList = wovenClasses.get(wovenClass.getClassName());
        if(wovenClassInfoList != null) {

            // This Bundle
            final Bundle weaverBundle = FrameworkUtil.getBundle(this.getClass());
            final BundleContext bundleContext = weaverBundle.getBundleContext();

            for(WovenClassInfo wovenClassInfo : wovenClassInfoList) {
                if (wovenClassInfo != null) {

                    long targetBundleId = wovenClassInfo.getBundleId();
                    Bundle targetBundle = bundleContext.getBundle(targetBundleId);
                    BundleWiring bundleWiring = targetBundle.adapt(BundleWiring.class);
                    if(bundleWiring != null) {
                        classLoaderHelper.defineClass(bundleWiring.getClassLoader(), wovenClassInfo.getName(), wovenClassInfo.getBytes(), null);
                    } else {
                        LOGGER.debug("BundleWiring for {} is not available", targetBundle.getSymbolicName());
                    }
                }
            }
        }

        // cleanup
        wovenClasses.remove(wovenClass.getClassName());
    }

}
