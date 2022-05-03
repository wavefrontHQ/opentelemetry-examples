package com.vmware.service;

import com.vmware.dao.EmployeeRepo;
import com.vmware.model.Employee;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
@Service
public class EmployeeService implements EmployeeRepo {
    public static final String OUTBOUND_EXTERNAL_SERVICE_KEY = "_outboundExternalService";
    public static final String EXTERNAL_SERVICE_VAL = "redis";
    public static final String INBOUND_EXTERNAL_SERVICE_KEY = "_inboundExternalService";
    public static final String EXTERNAL_APPLICATION_KEY = "_externalApplication";
    public static final String EXTERNAL_APPLICATION_VAL = "redis-db-app";
    public static final String EXTERNAL_HOST_KEY = "_externalHost";
    public static final String EXTERNAL_COMPONENT_KEY = "_externalComponent";
    public static final String EXTERNAL_CATEGORY_KEY = "_externalCategory";
    public static final String EXTERNAL_HOST_VAL = "localhost";
    public static final String EXTERNAL_COMPONENT_VAL = "cache-db";
    public static final String EXTERNAL_CATEGORY_VAL = "database";
    private final String EMPLOYEE_CACHE = "EMPLOYEE_CACHE";

    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, Employee> hashOperations;

    @PostConstruct
    private void initializeHashOperations() {
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(final Employee employee) {
        //setSpanTags(OUTBOUND_EXTERNAL_SERVICE_KEY);
        hashOperations.put(EMPLOYEE_CACHE, employee.getId(), employee);
    }

    @Override
    public Employee findById(final String id) {
        //setSpanTags(INBOUND_EXTERNAL_SERVICE_KEY);
        return hashOperations.get(EMPLOYEE_CACHE, id);
    }

    @Override
    public Map<String, Employee> findAll() {
        //setSpanTags(INBOUND_EXTERNAL_SERVICE_KEY);
        return hashOperations.entries(EMPLOYEE_CACHE);
    }

    @Override
    public void delete(String id) {
        //setSpanTags(OUTBOUND_EXTERNAL_SERVICE_KEY);
        hashOperations.delete(EMPLOYEE_CACHE, id);
    }

    private void setSpanTags(String callType) {
        Span span = Span.current();
//        span.setAttribute(callType, EXTERNAL_SERVICE_VAL);
//        span.setAttribute(EXTERNAL_APPLICATION_KEY, EXTERNAL_APPLICATION_VAL);
//        span.setAttribute(EXTERNAL_HOST_KEY, EXTERNAL_HOST_VAL);
//        span.setAttribute(EXTERNAL_COMPONENT_KEY, EXTERNAL_COMPONENT_VAL);
//        span.setAttribute(EXTERNAL_CATEGORY_KEY, EXTERNAL_CATEGORY_VAL);
        //span.setAttribute("span.kind", "client");
        span.setAttribute("span.kind", "client");
        span.setAttribute("component", "java-jdbc");
        span.setAttribute("db.instance", "employeeDB");
        span.setAttribute("db.type", "postgresql");
    }
}
