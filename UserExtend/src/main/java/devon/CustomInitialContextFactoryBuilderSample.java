package devon;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

public class CustomInitialContextFactoryBuilderSample {

    public static void main(String[] args) {
        try {
            // CustomInitialContextFactoryBuilder 설정
            setupInitialContextFactoryBuilder();
            
            // 데이터소스 등록
            registerDataSource();
            
            // 데이터소스 조회 및 사용
            useDataSource();
            
            System.out.println("샘플 실행 완료!");
        } catch (Exception e) {
            System.err.println("샘플 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * InitialContextFactoryBuilder 구현체를 설정합니다.
     */
    private static void setupInitialContextFactoryBuilder() throws NamingException {
        // NamingManager에 InitialContextFactoryBuilder 설정
        // 이미 설정되었는지 확인 (한 번만 설정할 수 있음)
        if (!NamingManager.hasInitialContextFactoryBuilder()) {
            NamingManager.setInitialContextFactoryBuilder(new CustomInitialContextFactoryBuilder());
            System.out.println("CustomInitialContextFactoryBuilder가 성공적으로 설정되었습니다.");
        }
    }
    
    /**
     * JNDI에 데이터소스를 등록합니다.
     */
    private static void registerDataSource() throws NamingException {
        // InitialContext 생성
        InitialContext ctx = new InitialContext();
        
        // H2 데이터소스 생성 (예제용 인메모리 DB)
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");
        
        // JNDI에 데이터소스 바인딩
        ctx.bind("java:comp/env/jdbc/TestDB", h2DataSource);
        System.out.println("데이터소스가 성공적으로 등록되었습니다: java:comp/env/jdbc/TestDB");
        
        ctx.close();
    }
    
    /**
     * JNDI에서 데이터소스를 조회하여 사용합니다.
     */
    private static void useDataSource() throws Exception {
        // InitialContext 생성
        InitialContext ctx = new InitialContext();
        
        // JNDI에서 데이터소스 조회
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/TestDB");
        System.out.println("데이터소스를 성공적으로 조회했습니다: " + ds);
        
        // 데이터소스에서 커넥션 획득 및 테스트
        try (var connection = ds.getConnection()) {
            System.out.println("데이터베이스 연결 성공: " + connection);
            
            // 테이블 생성
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100))");
                statement.execute("INSERT INTO users VALUES (1, '홍길동')");
                System.out.println("테이블 생성 및 데이터 삽입 완료");
                
                // 데이터 조회
                var rs = statement.executeQuery("SELECT id, name FROM users");
                while (rs.next()) {
                    System.out.println("사용자 ID: " + rs.getInt("id") + ", 이름: " + rs.getString("name"));
                }
            }
        }
        
        ctx.close();
    }
}

/**
 * 커스텀 InitialContextFactoryBuilder 구현체
 */
class CustomInitialContextFactoryBuilder implements InitialContextFactoryBuilder {
    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        return new CustomInitialContextFactory();
    }
}

/**
 * 커스텀 InitialContextFactory 구현체
 */
class CustomInitialContextFactory implements InitialContextFactory {
    // 객체를 저장할 내부 저장소
    private static final ConcurrentHashMap<String, Object> BINDINGS = new ConcurrentHashMap<>();
    
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new CustomInitialContext(BINDINGS);
    }
}

/**
 * 커스텀 InitialContext 구현체
 */
class CustomInitialContext extends InitialContext {
    private final ConcurrentHashMap<String, Object> bindings;
    
    public CustomInitialContext(ConcurrentHashMap<String, Object> bindings) throws NamingException {
        super(false); // 기본 InitialContext 생성자를 호출하지 않음
        this.bindings = bindings;
    }
    
    @Override
    public void bind(String name, Object obj) throws NamingException {
        bindings.put(name, obj);
    }
    
    @Override
    public Object lookup(String name) throws NamingException {
        Object result = bindings.get(name);
        if (result == null) {
            throw new NamingException("이름을 찾을 수 없습니다: " + name);
        }
        return result;
    }
    
    @Override
    public void rebind(String name, Object obj) throws NamingException {
        bindings.put(name, obj);
    }
    
    @Override
    public void unbind(String name) throws NamingException {
        bindings.remove(name);
    }
    
    // 필요한 다른 메서드도 구현할 수 있습니다.
}