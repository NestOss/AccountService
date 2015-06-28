package com.nestos.accountservice.aspect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Class for collect AccountService methods invocation statistic.
 *
 * @author Roman Osipov.
 */
@Aspect
@ManagedResource(objectName = "accountService:name=StatisticHandler")
@Order(Ordered.HIGHEST_PRECEDENCE) // Invoke advice before cache handler.
public class StatisticHandler implements InitializingBean, DisposableBean {

    //-------------------Logger---------------------------------------------------
    private final static Logger logger = Logger.getLogger(StatisticHandler.class.getName());
    private final static Logger processingLogger = Logger.getLogger("com.nestos.processing");

    //-------------------NestedClasses--------------------------------------------
    // Executes once per second.
    private class ProcessStatistics implements Runnable {

        @Override
        public void run() {
            synchronized (lock) {
                for (Map.Entry<String, AtomicLong> entry : invocationCounterHashMap.entrySet()) {
                    String key = entry.getKey();
                    long value = entry.getValue().get();
                    Pair<Long, Long> pair = invocationsLastSecHashMap.get(key);
                    Long perSecCounter;
                    if (pair == null) {
                        pair = Pair.of(value, value);
                        perSecCounter = value;
                    } else {
                        perSecCounter = value - pair.getLeft();
                        pair = Pair.of(value, perSecCounter);
                    }
                    invocationsLastSecHashMap.put(key, pair);
                    if (enableLogging) {
                        logger.info(
                                String.format(
                                        "%s: total %d invocations, last second %d invocations.",
                                        key, value, perSecCounter));
                    }
                }
            }
        }
    }
    //-------------------Constants------------------------------------------------
    //-------------------Fields---------------------------------------------------
    // Maps method name to method invocation counter.
    private final ConcurrentHashMap<String, AtomicLong> invocationCounterHashMap
            = new ConcurrentHashMap<>();
    // Maps method name to pair of long values.
    // first value - method invocation counter for pred second,
    // second value - method number invocations during last second
    private final Map<String, Pair<Long, Long>> invocationsLastSecHashMap = new HashMap<>();
    private ScheduledExecutorService statisticExecutorService;
    private final Object lock = new Object();
    private volatile boolean enableLogging;

    //-------------------Constructors---------------------------------------------
    //-------------------Getters and setters--------------------------------------
    //-------------------Bean lifecycle methods----------------------------------
    @Override
    public void afterPropertiesSet() throws Exception {
        statisticExecutorService = Executors.newSingleThreadScheduledExecutor();
        // Schedule process statistic once per second.
        statisticExecutorService.scheduleAtFixedRate(
                new ProcessStatistics(), 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() throws Exception {
        if (statisticExecutorService != null) {
            statisticExecutorService.shutdown();
            // Because ProcessStatistics is shot task, this method returns near immediately. 
            statisticExecutorService.awaitTermination(1, TimeUnit.DAYS);
        }
    }

    //-------------------Methods--------------------------------------------------
    @Around("execution(* com.nestos.accountservice.jpa.AccountServiceImpl.*(..))")
    public Object proceedAroundMethodInvocation(ProceedingJoinPoint jp) throws Throwable {
        String methodName = jp.getSignature().getName();
        AtomicLong counter = invocationCounterHashMap.get(methodName);
        if (counter == null) {
            // Because first counter increment - relative rare operation,
            // this synchronized block has no perfomance impact.
            // Protects against map modification after putIfAbsent and before get. 
            synchronized (lock) {
                // prevent increment lose for first increment parallel processing  
                invocationCounterHashMap.putIfAbsent(methodName, new AtomicLong());
                counter = invocationCounterHashMap.get(methodName);
            }
        }
        counter.addAndGet(1);
        Object result = jp.proceed();
        if (enableLogging) {
            processingLogger.info(jp.getSignature().getName()
                    + " " + Arrays.deepToString(jp.getArgs()));
        }
        return result;
    }

    /**
     * Return target method invocation counter.
     *
     * @param methodName target method name.
     * @return count of method invocations.
     */
    @ManagedOperation(description = "Returns target method invocation counter.")
    public Long getMethodInvocationCount(String methodName) {
        AtomicLong counter = invocationCounterHashMap.get(methodName);
        return (counter == null) ? 0 : counter.get();
    }

    /**
     * Return count of target method invocations for last second.
     *
     * @param methodName target method name.
     * @return count of method invocations for last second.
     */
    @ManagedOperation(description = "Return count of target method invocations for last second.")
    public Long getMethodInvocationRate(String methodName) {
        Pair<Long, Long> pair = invocationsLastSecHashMap.get(methodName);
        return (pair == null) ? 0 : pair.getRight();
    }

    /**
     * Reset invocation counter for target method.
     *
     * @param methodName target method name.
     */
    @ManagedOperation(description = "Reset invocation counter for target method.")
    public void resetMethodInvocationCounter(String methodName) {
        synchronized (lock) {
            invocationCounterHashMap.remove(methodName);
            invocationsLastSecHashMap.remove(methodName);
        }
    }

    /**
     * Reset all statistics.
     */
    @ManagedOperation(description = "Reset all statistics.")
    public void reset() {
        synchronized (lock) {
            invocationCounterHashMap.clear();
            invocationsLastSecHashMap.clear();
        }
    }

    /**
     * Enable statistic logging.
     */
    @ManagedOperation(description = "Turn logging on.")
    public void logOn() {
        enableLogging = true;
    }

    /**
     * Disable statistic logging.
     */
    @ManagedOperation(description = "Turn logging off.")
    public void logOff() {
        enableLogging = false;
    }
}
