package other.jdk;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class ArenaExample {
    public static void main(String[] args) {
        // 1. 기본 Arena 생성 - 구속된(confined) Arena는 단일 스레드에서만 사용 가능
        try (Arena arena = Arena.ofConfined()) {
            // 여러 메모리 세그먼트 할당
            MemorySegment segment1 = arena.allocate(100);
            MemorySegment segment2 = arena.allocate(200);
            
            System.out.println("segment1 크기: " + segment1.byteSize());
            System.out.println("segment2 크기: " + segment2.byteSize());
            
            // 데이터 조작
            segment1.fill((byte)1);  // 모든 바이트를 1로 채움
            segment2.fill((byte)2);  // 모든 바이트를 2로 채움
            
            // 첫 번째 바이트 확인
            System.out.println("segment1 첫 번째 바이트: " + segment1.get(ValueLayout.JAVA_BYTE, 0));
            System.out.println("segment2 첫 번째 바이트: " + segment2.get(ValueLayout.JAVA_BYTE, 0));
        } // arena가 닫히면 모든 메모리가 자동으로 해제됨
        
        // 2. 공유 Arena 생성 - 다중 스레드에서 사용 가능
        try (Arena sharedArena = Arena.ofShared()) {
            MemorySegment sharedSegment = sharedArena.allocate(1024);
            System.out.println("공유 세그먼트 크기: " + sharedSegment.byteSize());
            
            // 다른 스레드에서도 접근 가능
            Thread thread = new Thread(() -> {
                sharedSegment.set(ValueLayout.JAVA_INT, 0, 42);
                System.out.println("스레드에서 값 설정 완료");
            });
            
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println("메인 스레드에서 읽은 값: " + sharedSegment.get(ValueLayout.JAVA_INT, 0));
        } // sharedArena가 닫히면 모든 메모리가 자동으로 해제됨
        
        // 3. 자동 Arena - 스코프 기반 메모리 관리
//        try(Arena autoArena = Arena.ofAuto()){
//        	 MemorySegment autoSegment = autoArena.allocate(50);
//             System.out.println("자동 세그먼트 크기: " + autoSegment.byteSize());
//             autoArena.close();
//        }
        
        // 4. 글로벌 Arena - 프로그램 종료시까지 유지되는 메모리
        MemorySegment globalSegment = Arena.global().allocate(50);
        System.out.println("글로벌 세그먼트 크기: " + globalSegment.byteSize());
        // 프로그램이 종료될 때까지 유지됨
    }
}