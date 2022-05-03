package com.vmware.producer;

import com.vmware.constant.ApplicationConstant;
import com.vmware.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */

@RestController
@RequestMapping(value = "/employee")
public class Producer {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping
    public String save(@RequestBody final Employee employee) {
        LOG.info("Sending the new employee info to kafka.");
        try {
            kafkaTemplate.send(ApplicationConstant.TOPIC_NAME, employee);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Successfully added. Employee with id= " + employee.getId();
    }
}
