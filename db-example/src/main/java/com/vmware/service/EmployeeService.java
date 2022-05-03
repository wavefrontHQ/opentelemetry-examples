package com.vmware.service;

import com.vmware.model.Employee;
import com.vmware.repository.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
@Service
public class EmployeeService {

    @Autowired
    EmployeeRepo employeeRepo;

    public void save(final Employee employee) {
        employeeRepo.save(employee);
    }

    public Employee findById(final String id) {
        return employeeRepo.findById(Integer.valueOf(id)).get();
    }

    public Map<String, Employee> findAll() {
        Map<String, Employee> employeeMap = new HashMap<>();
        employeeRepo.findAll().forEach(employee -> employeeMap.put(String.valueOf(employee.getId()), employee));
        return employeeMap;
    }

    public void delete(String id) {
        employeeRepo.deleteById(Integer.valueOf(id));
    }
}
