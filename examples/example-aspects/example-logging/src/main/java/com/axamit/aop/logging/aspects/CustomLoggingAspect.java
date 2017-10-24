package com.axamit.aop.logging.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: integrate http://aspects.jcabi.com/
@Aspect
public class CustomLoggingAspect {

    public CustomLoggingAspect() {
    }

    @Pointcut("execution(* *..PrivateClass+.*(..)) && !osgiLifecycleMethods()")
    public void methodsInDemoPackage() {
    }

    @Pointcut("execution(* *.activate(..)) || execution(* *.deactivate(..)) || execution(* *.modified(..))")
    public void osgiLifecycleMethods() {
    }

    @Around("methodsInDemoPackage()")
    public Object aroundMethodsInDemoPackage(ProceedingJoinPoint joinPoint) throws Throwable {

        Logger logger = LoggerFactory.getLogger("com.axamit.aop.target");

        // Demonstrates how to get bundle context
        try {
            BundleContext ctx = FrameworkUtil.getBundle(CustomLoggingAspect.class).getBundleContext();

            if(ctx != null) {
                logger.info("Aspect has BundleContext {}", ctx.getBundle().getSymbolicName());
            }

        } catch (Exception e) {
            logger.error("Cannot get Bundle Context", e);
        }

        // Here you can write your custom logging code for the invocation
        logger.info("Around advice invoked before {}", joinPoint.getSignature().getName());

        Object result = joinPoint.proceed(); // calling the original method

        logger.info("Around advice invoked after {}", joinPoint.getSignature().getName());

        return result;

    }

}
