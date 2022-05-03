package com.vmware.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.constant.ApplicationConstant;
import com.vmware.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */

@Component
public class Consumer {
    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @KafkaListener(groupId = ApplicationConstant.GROUP_ID, topics = ApplicationConstant.TOPIC_NAME, containerFactory = ApplicationConstant.KAFKA_LISTENER_CONTAINER_FACTORY)
    public void receivedMessage(Employee employee) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(employee);
        LOG.info("Employee info received from kafka: " + jsonString);
    }
}
