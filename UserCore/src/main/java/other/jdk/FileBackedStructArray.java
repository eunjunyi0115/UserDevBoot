//package other.jdk;
//
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.lang.foreign.Arena;
//import java.lang.foreign.MemoryLayout;
//import java.lang.foreign.MemoryLayout.PathElement;
//import java.lang.foreign.MemorySegment;
//import java.lang.foreign.StructLayout;
//import java.lang.foreign.ValueLayout;
//import java.lang.invoke.VarHandle;
//import java.nio.channels.FileChannel;
//import java.nio.charset.StandardCharsets;
//
//public class FileBackedStructArray {
//    private static final int NAME_SIZE = 32;
//    private static final int PERSON_COUNT = 3;
//
//    private static final StructLayout PERSON_LAYOUT = MemoryLayout.structLayout(
//        MemoryLayout.sequenceLayout(NAME_SIZE, ValueLayout.JAVA_BYTE).withName("name"),
//        ValueLayout.JAVA_INT.withName("age"),
//        MemoryLayout.paddingLayout(4),
//        ValueLayout.JAVA_DOUBLE.withName("height")
//    );
//
//    private static final VarHandle ageHandle = PERSON_LAYOUT.varHandle(PathElement.groupElement("age"));
//    private static final VarHandle heightHandle = PERSON_LAYOUT.varHandle(PathElement.groupElement("height"));
//
//    public static void main(String[] args) throws IOException {
//        try (RandomAccessFile file = new RandomAccessFile("people.bin", "rw");
//             FileChannel channel = file.getChannel();
//             Arena arena = Arena.ofConfined()) {
//
//            long totalSize = PERSON_LAYOUT.byteSize() * PERSON_COUNT;
//            MemorySegment segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, totalSize, arena);
//
//            // 데이터 쓰기
//            for (int i = 0; i < PERSON_COUNT; i++) {
//                MemorySegment person = segment.asSlice(i * PERSON_LAYOUT.byteSize(), PERSON_LAYOUT.byteSize());
//                MemorySegment nameSeg = person.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
//
//                String name = "사람" + (i + 1);
//                int age = 20 + i;
//                double height = 160.0 + i * 5;
//
//                nameSeg.asByteBuffer().put(name.getBytes(StandardCharsets.UTF_8));
//                ageHandle.set(person, 0L, age);
//                heightHandle.set(person, 0L, height);
//            }
//
//            System.out.println("== 저장된 데이터 확인 ==");
//
//            // 데이터 읽기
//            for (int i = 0; i < PERSON_COUNT; i++) {
//                MemorySegment person = segment.asSlice(i * PERSON_LAYOUT.byteSize(), PERSON_LAYOUT.byteSize());
//                MemorySegment nameSeg = person.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
//                String name = new String(nameSeg.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim();
//                int age = (int) ageHandle.get(person, 0L);
//                double height = (double) heightHandle.get(person, 0L);
//                System.out.printf("%d. 이름: %s, 나이: %d, 키: %.1fcm%n", i + 1, name, age, height);
//            }
//        }
//    }
//}