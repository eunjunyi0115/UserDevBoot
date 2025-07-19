package other.unit;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.h2.jdbcx.JdbcDataSource;

public class JndiDataContext {
	
	public static void main(String[] args) {
		if(!NamingManager.hasInitialContextFactoryBuilder()){
			try {
				NamingManager.setInitialContextFactoryBuilder(new UnitTestContextFactoryBuilder());
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
		
		try {
			InitialContext ctx = new InitialContext();
			// H2 데이터소스 생성 (예제용 인메모리 DB)
	        JdbcDataSource h2DataSource = new JdbcDataSource();
	        h2DataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
	        h2DataSource.setUser("sa");
	        h2DataSource.setPassword("");
	        
	        // JNDI에 데이터소스 바인딩
	        ctx.bind("java:comp/env/jdbc/TestDB", h2DataSource);
	        System.out.println("데이터소스가 성공적으로 등록되었습니다: java:comp/env/jdbc/TestDB");
	        
	        System.out.println("contextMap:"+contextMap);
	        ctx.close();
		} catch (NamingException e) {
			e.printStackTrace();
		}
        
		
	}
	
	private static Map<String,Object> contextMap = new ConcurrentHashMap<>();
	
	static class UnitTestContextFactoryBuilder implements InitialContextFactoryBuilder{
		@Override
		public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
			return new InitialContextFactory() {
				@Override
				public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
					return new UnitTestContext(contextMap, environment);
				}
			};
		}
	}
	
	static class UnitTestContext implements Context{
		Map<String,Object> contextMap = null;
		public UnitTestContext(Map<String,Object> contextMap,Hashtable<?, ?> environment) {
			this.contextMap = contextMap;
		}
		public Object lookup(Name name) throws NamingException {
			return null;
		}
		public Object lookup(String name) throws NamingException {
			return null;
		}
		public void bind(Name name, Object obj) throws NamingException {
			contextMap.put(name.toString(), obj);
		}
		public void bind(String name, Object obj) throws NamingException {
			contextMap.put(name, obj);
		}
		public void rebind(Name name, Object obj) throws NamingException {
			contextMap.put(name.toString(),obj);
		}
		public void rebind(String name, Object obj) throws NamingException {
			contextMap.put(name, obj);
		}
		public void unbind(Name name) throws NamingException {
			contextMap.remove(name.toString());
		}
		public void unbind(String name) throws NamingException {
			contextMap.remove(name);
		}
		public void rename(Name oldName, Name newName) throws NamingException {}
		public void rename(String oldName, String newName) throws NamingException {}
		public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
			return null;
		}
		public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
			return null;
		}
		public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
			return null;
		}
		public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
			return null;
		}
		public void destroySubcontext(Name name) throws NamingException {}
		public void destroySubcontext(String name) throws NamingException {}
		public Context createSubcontext(Name name) throws NamingException {return null;}
		public Context createSubcontext(String name) throws NamingException {return null;}
		public Object lookupLink(Name name) throws NamingException {return null;}
		public Object lookupLink(String name) throws NamingException {return null;}
		public NameParser getNameParser(Name name) throws NamingException {return null;}
		public NameParser getNameParser(String name) throws NamingException {return null;}
		public Name composeName(Name name, Name prefix) throws NamingException {return null;}
		public String composeName(String name, String prefix) throws NamingException {return null;}
		public Object addToEnvironment(String propName, Object propVal) throws NamingException {return null;}
		public Object removeFromEnvironment(String propName) throws NamingException {return null;}
		public Hashtable<?, ?> getEnvironment() throws NamingException {return null;}
		public void close() throws NamingException { contextMap.clear();}
		public String getNameInNamespace() throws NamingException {return null;}
	}
	
}
