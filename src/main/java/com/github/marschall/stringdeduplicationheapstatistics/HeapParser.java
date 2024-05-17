package com.github.marschall.stringdeduplicationheapstatistics;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongIntMaps;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapFactory;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;

public final class HeapParser {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("usage: heapDumpPath");
      System.exit(1);
    }
    String heapDumpPath = args[0];
    HeapParser parser = new HeapParser();
    StringDeduplicationHeapStatistics statistics = parser.parse(new File(heapDumpPath));
    System.out.println("string deduplicated: " + statistics.stringsDeduplicated());
    System.out.println("bytes saved: " + statistics.bytesSaved());
  }

  public StringDeduplicationHeapStatistics parse(File heapDump) throws IOException {
    Heap heap = HeapFactory.createHeap(heapDump);
    JavaClass stringClass = heap.getJavaClassByName(String.class.getName());
    if (stringClass == null) {
      throw new IllegalStateException("String class not found");
    }
    StringDeduplicationHeapStatisticsBuilder builder = new StringDeduplicationHeapStatisticsBuilder();
    Iterator<Instance> allInstances = heap.getAllInstancesIterator();
    while (allInstances.hasNext()) {
      Instance instance = allInstances.next();
      if (instance.getJavaClass() == stringClass) {
        if (instance.getValueOfField("value") instanceof PrimitiveArrayInstance array) {
          long instanceId = array.getInstanceId();
          long sizeInBytes = array.getSize();
          builder.addByteArray(instanceId, sizeInBytes);
        } else {
          throw new IllegalStateException("String#value is not an array");
        }
      }
    }
    return builder.build();
  }

  static final class StringDeduplicationHeapStatisticsBuilder {


    // instanceId -> count
    private final MutableLongBag byteArrayInstanceIds;
    // instanceId -> compressed size
    private final MutableLongIntMap objectSizes;

    StringDeduplicationHeapStatisticsBuilder() {
      this.byteArrayInstanceIds = LongBags.mutable.empty();
      this.objectSizes = LongIntMaps.mutable.empty();
    }

    void addByteArray(long instanceId, long sizeInBytes) {
      byteArrayInstanceIds.add(instanceId);
      objectSizes.put(instanceId, compress(sizeInBytes));
    }

    private long getObjectSize(long instanceId) {
      return deCompress(objectSizes.get(instanceId));
    }

    private static int compress(long objectSizeInBytes) {
      if ((objectSizeInBytes % 8) != 0) {
        throw new IllegalStateException("not 8 byte aligned");
      }
      // we know object sizes are 8 byte aligned
      // a byte array can have Integer.MAX_VALUE elements, with an object header
      // the total size can be more than Integer.MAX_VALUE bytes
      // we could also use "unsigned int" see Integer#toUnsignedLong
      return Math.toIntExact(objectSizeInBytes / 8L);
    }

    private static long deCompress(int compressedObjectSize) {
      return compressedObjectSize * 8L;
    }

    StringDeduplicationHeapStatistics build() {

      class Accumulator implements LongIntProcedure {

        long stringsDeduplicated;
        long bytesSaved;

        @Override
        public void value(long instanceId, int count) {
          if (count > 1) {
            // 2 Strings with the save backing byte array -> only one was deduplicated
            long deduplicatedCount = count - 1L;
            stringsDeduplicated += deduplicatedCount;
            long decompressedSize = getObjectSize(instanceId);
            bytesSaved += decompressedSize * deduplicatedCount;
          }
        }
      }
      Accumulator accumulator = new Accumulator();
      // we could do MutableLongBag deduplicatedArrayIds = byteArrayInstanceIds.selectByOccurrences(i -> i > 1);
      // but save ourselves the allocation here
      byteArrayInstanceIds.forEachWithOccurrences(accumulator);
      return new StringDeduplicationHeapStatistics(accumulator.stringsDeduplicated, accumulator.bytesSaved);
    }

  }

  //  private void parseMat(File file) {
  //    
  //  }
  //  
  //  private ISnapshot openSnapshot(File heapDumpFile) throws SnapshotException {
  //    VoidProgressListener listener = new VoidProgressListener();
  //    return SnapshotFactory.openSnapshot(heapDumpFile, listener);
  //  }

}
