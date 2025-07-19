//package other;
//
//import java.lang.invoke.MethodHandles;
//import java.util.concurrent.Callable;
//
//import net.bytebuddy.ByteBuddy;
//import net.bytebuddy.implementation.MethodDelegation;
//import net.bytebuddy.implementation.bind.annotation.SuperCall;
//import net.bytebuddy.matcher.ElementMatchers;
//
//public class ByteBuddyTest {
//	
//	 public static class Service {
//	    public void sayHello(String name) {
//	        System.out.println("Original Hello: " + name);
//	    }
//	}
//
//	 //인터셉터는 무조건 static 메소드하나만 있을수 잇다.
//	 public static class Interceptor {
//	    public static void intercept(String name) {
//	        System.out.println("Intercepted Hello: " + name);
//	    }
//	}
//	 
//	 public class SuperInterceptor {
//		    public static void intercept(@SuperCall Callable<Void> original) throws Exception {
//		    	
//		    	
//		        System.out.println("Before original" + original.getClass());
//		        original.call();  // 원래 메서드 실행
//		        System.out.println("After original");
//		    }
//		}
//	
//	public static void main(String[] args) throws Throwable {
//		MethodHandles.Lookup lookup = MethodHandles.lookup();
//		
//	    Class<? extends Service> dynamicType = new ByteBuddy()
//                .subclass(Service.class)
//               // .name("com.example.HelloByteBuddy")
//                .method(ElementMatchers.named("sayHello"))
//                //.intercept(FixedValue.value("Hello ByteBuddy!"))
//                .intercept(MethodDelegation.to(SuperInterceptor.class))
//                .make()
//               // .load(ByteBuddyTest.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                //.load(lookup.getClass().getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup))
//                .load(Service.class.getClassLoader())
//                .getLoaded();
//	    
//	    Service obj = dynamicType.getDeclaredConstructor().newInstance();
//	    obj.sayHello("World");
//        //System.out.println(obj.sayHello("World")); // 출력: Hello ByteBuddy!
//	}
//}
