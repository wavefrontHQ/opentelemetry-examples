package com.vmware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.metrics.LongHistogram;

/**
 * @author Sumit Deo (deosu@vmware.com)
 */
public class App {
  private final static Logger logger = LoggerFactory.getLogger(App.class);
  public static void main(String[] args) throws InterruptedException {

    OtelService service = new OtelService();
    LongHistogram recorder = service.initHistogramMetricRecorder();

    logger.info("Recording metrics in iterations-");
    for (int i = 0; i < 10; i++) {
      logger.info("iteration# " + i);
      Thread.sleep(1000);
      recorder.record(i * 10);
    }
  }
}
