package other.jdk;

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class PersonIndexExample {
    private static final int PERSON_COUNT = 1000;
    private static final int NAME_SIZE = 32;

    private static final GroupLayout PERSON_LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(NAME_SIZE, ValueLayout.JAVA_BYTE).withName("name"),
        ValueLayout.JAVA_INT.withName("age"),
        MemoryLayout.paddingLayout(4L),
        ValueLayout.JAVA_DOUBLE.withName("height")
    );
    
    static String[] names = {"Alice", "Bob", "Charlie", "David", "Eva", "Frank", "Grace", "Helen", "Ian", "Jane"};
    static int[] ages =     {25,      30,    22,       28,      24,   35,     27,     31,     29,    26};
    static double[] heights = {165.2, 180.5, 175.0, 172.8, 160.3, 185.4, 168.6, 170.2, 174.4, 169.0};


    private static final SequenceLayout PEOPLE_LAYOUT = MemoryLayout.sequenceLayout(PERSON_COUNT, PERSON_LAYOUT);

    public static void main(String[] args) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment people = arena.allocate(PEOPLE_LAYOUT); //총 사이즈 메모리 할당.

            // 핸들 준비
//            VarHandle nameHandle = PERSON_LAYOUT.varHandle(
//                MemoryLayout.PathElement.groupElement("name"),
//                MemoryLayout.PathElement.sequenceElement(0)
//            );
            
            VarHandle ageHandle = PERSON_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("age"));
            VarHandle heightHandle = PERSON_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("height"));

            // 값 입력 (예: 123번째 사람)
            long structSize = PERSON_LAYOUT.byteSize();
            
            for (int index = 0; index < PERSON_COUNT; index++) {
            	MemorySegment person = people.asSlice(index * PERSON_LAYOUT.byteSize(), PERSON_LAYOUT.byteSize());
            	MemorySegment nameSeg = person.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
            	
                String name = names[index%10]+"_"+index;
                byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
                for (int t = 0; t < nameBytes.length && t < NAME_SIZE; t++) {
                	//nameSeg.asByteBuffer().put(name.getBytes(StandardCharsets.UTF_8));
                	nameSeg.set(ValueLayout.JAVA_BYTE, (long) t, nameBytes[t]);
                }
                ageHandle.set(person, 0L, ages[index%10]);
                heightHandle.set(person, 0L, heights[index%10]);
            }
            
            
            for (int index = 0; index < PERSON_COUNT; index++) {
            	MemorySegment person = people.asSlice(index * PERSON_LAYOUT.byteSize(), PERSON_LAYOUT.byteSize());
            	MemorySegment nameSeg = person.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
//              byte[] nameBuf = new byte[NAME_SIZE];
//              for (int i = 0; i < NAME_SIZE; i++) {
//                 // nameBuf[i] = (byte) nameHandle.get(person, (long) i);
//              	nameBuf[i] = (byte) nameSeg.get(ValueLayout.JAVA_BYTE, i) ;      
//                  //nameBuf[i] = new String(nameSeg.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim();
//              }
//              String readName = new String(nameBuf, StandardCharsets.UTF_8).trim();
            	 String  readName = new String(nameSeg.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim();
                 int readAge = (int) ageHandle.get(person, 0L);
                 double readHeight = (double) heightHandle.get(person, 0L);

                 System.out.printf("Index %d - Name: %s, Age: %d, Height: %.2f\n", index, readName, readAge, readHeight);
            }
            
            
            //MemorySegment person = people.asSlice(index * structSize, structSize);
//            String name = "Lee Jun";
//            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
//            for (int i = 0; i < nameBytes.length && i < NAME_SIZE; i++) {
//            	//nameSeg.asByteBuffer().put(name.getBytes(StandardCharsets.UTF_8));
//            	nameSeg.set(ValueLayout.JAVA_BYTE, (long) i, nameBytes[i]);
//            }
//            ageHandle.set(person, 0L, 15);
//            heightHandle.set(person, 0L, 179.2);
            
            //MemorySegment person = segment.asSlice(i * PERSON_LAYOUT.byteSize(), PERSON_LAYOUT.byteSize());
            // 값 읽기
//            byte[] nameBuf = new byte[NAME_SIZE];
//            for (int i = 0; i < NAME_SIZE; i++) {
//               // nameBuf[i] = (byte) nameHandle.get(person, (long) i);
//            	nameBuf[i] = (byte) nameSeg.get(ValueLayout.JAVA_BYTE, i) ;      
//                //nameBuf[i] = new String(nameSeg.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim();
//            }
//            String readName = new String(nameBuf, StandardCharsets.UTF_8).trim();
            
//            String  readName = new String(nameSeg.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim();
//            
//            System.out.println("readName:"+readName);
//            int readAge = (int) ageHandle.get(person, 0L);
//            double readHeight = (double) heightHandle.get(person, 0L);
//
//            System.out.printf("Index %d - Name: %s, Age: %d, Height: %.2f\n",
//                index, readName, readAge, readHeight);
        }
    }
}
