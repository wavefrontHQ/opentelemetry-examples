package com.vmware.dao;

import com.vmware.model.Employee;

import java.util.Map;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public interface EmployeeRepo {
    void save(Employee employee);
    Employee findById(String id);
    Map<String, Employee> findAll();
    void delete(String id);
}
