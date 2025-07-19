package devon;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

public class ObjectFactoryBuilderTest {

	public static void main(String[] args) {
		try {
            // 커스텀 ObjectFactoryBuilder 구현
            ObjectFactoryBuilder ofb = new CustomObjectFactoryBuilder();
            
            // NamingManager에 ObjectFactoryBuilder 설정
            // 이는 ObjectFactory가 동적으로 생성되는 방식을 변경함
            NamingManager.setObjectFactoryBuilder(ofb);
            
            // 참조 객체 생성
            RefAddr addr = new StringRefAddr("className", "com.example.MyClass");
            Reference ref = new Reference("com.example.MyClass", addr, 
                    "com.example.factory.MyObjectFactory", null);
            
            // 참조를 객체로 변환
            Object obj = NamingManager.getObjectInstance(ref, new CompositeName("myObject"), 
                    null, new Hashtable<>());
            
            System.out.println("변환된 객체: " + obj);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
	/**
     * 커스텀 ObjectFactoryBuilder 구현
     */
    static class CustomObjectFactoryBuilder implements ObjectFactoryBuilder {
        @Override
        public ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment) throws NamingException {
            System.out.println("ObjectFactory 생성 중...");
            
            if (obj instanceof Reference) {
                Reference ref = (Reference) obj;
                String className = ref.getClassName();
                
                // 클래스 이름에 따라 다른 ObjectFactory 반환
                if (className.contains("MyClass")) {
                    return new MyObjectFactory();
                }
            }
            
            // 기본 ObjectFactory 반환
            return new DefaultObjectFactory();
        }
    }
    
    /**
     * 커스텀 ObjectFactory 구현
     */
    static class MyObjectFactory implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) 
                throws Exception {
            System.out.println("MyObjectFactory에서 객체 생성 중: " + obj);
            
            if (obj instanceof Reference) {
                Reference ref = (Reference) obj;
                RefAddr addr = ref.get("className");
                if (addr != null) {
                    String className = (String) addr.getContent();
                    System.out.println("클래스 이름: " + className);
                    
                    // 여기서는 예제를 위해 간단한 문자열을 반환
                    return "MyClass의 인스턴스";
                }
            }
            
            return null;
        }
    }
    
    /**
     * 기본 ObjectFactory 구현
     */
    static class DefaultObjectFactory implements ObjectFactory {
        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) 
                throws Exception {
            System.out.println("DefaultObjectFactory에서 객체 생성 중: " + obj);
            return "기본 객체";
        }
    }
}
