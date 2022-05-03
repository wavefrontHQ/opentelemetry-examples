package com.vmware.controller;

import com.vmware.model.Employee;
import com.vmware.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
@RestController
@RequestMapping(value = "/employee")
public class EmployeeController {
    private static final Logger LOG = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    EmployeeService employeeService;

    @PostMapping
    public String save(@RequestBody final Employee employee) {
        LOG.info("Saving the new employee to the db.");
        employeeService.save(employee);
        return "Successfully added. Employee with id= " + employee.getId();
    }

    @GetMapping("/getall")
    public Map<String, Employee> findAll() {
        LOG.info("Fetching all employees from the db.");
        final Map<String, Employee> employeeMap = employeeService.findAll();
        return employeeMap;
    }

    @GetMapping("/get/{id}")
    public Employee findById(@PathVariable("id") final String id) {
        LOG.info("Fetching employee with id= " + id);
        return employeeService.findById(id);
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Employee> delete(@PathVariable("id") final String id) {
        LOG.info("Deleting employee with id= " + id);
        employeeService.delete(id);
        return findAll();
    }
}
