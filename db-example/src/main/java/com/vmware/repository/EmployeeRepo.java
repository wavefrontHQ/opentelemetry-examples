package com.vmware.repository;

import com.vmware.model.Employee;
import org.springframework.data.repository.CrudRepository;

import java.util.Map;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public interface EmployeeRepo extends CrudRepository<Employee, Integer> {
}
