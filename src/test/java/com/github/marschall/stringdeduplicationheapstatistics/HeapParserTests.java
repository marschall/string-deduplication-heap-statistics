package com.github.marschall.stringdeduplicationheapstatistics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeapParserTests {
  
  private HeapParser parser;

  @BeforeEach
  void setUp() {
    this.parser = new HeapParser();
  }

  @Test
  void parseSample() throws IOException {
    StringDeduplicationHeapStatistics heapStatistics = this.parser.parse(new File("src/test/resources/sample.hprof"));
    assertNotNull(heapStatistics);
    System.out.println("string deduplicated: " + heapStatistics.stringsDeduplicated());
    System.out.println("bytes saved: " + heapStatistics.bytesSaved());
  }

}
