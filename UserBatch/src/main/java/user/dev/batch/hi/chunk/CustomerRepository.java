package user.dev.batch.hi.chunk;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 기본적인 CRUD 메서드 제공됨
}
