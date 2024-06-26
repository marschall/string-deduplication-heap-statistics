#!/bin/bash
set -u
${JAVA_HOME}/bin/java -cp lib/eclipse-collections-api-11.1.0.jar:lib/eclipse-collections-11.1.0.jar:lib/org-graalvm-visualvm-lib-jfluid-heap-2.1.8.jar:lib/string-deduplication-heap-statistics-1.0.0.jar \
  -Xmx256m \
  -XX:+UseSerialGC \
  -Djava.awt.headless=true \
  -XX:+UseStringDeduplication \
  -XX:-EnableDynamicAgentLoading \
  com.github.marschall.stringdeduplicationheapstatistics.HeapParser $@