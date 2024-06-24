package com.github.marschall.stringdeduplicationheapstatistics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    File heapDump = new File("src/test/resources/sample.hprof");
    assumeTrue(heapDump.exists(), "heap dump exists");
    StringDeduplicationHeapStatistics heapStatistics = this.parser.parse(heapDump);
    assertNotNull(heapStatistics);
    System.out.println("string deduplicated: " + heapStatistics.stringsDeduplicated());
    System.out.println("bytes saved: " + heapStatistics.bytesSaved());
  }

}
