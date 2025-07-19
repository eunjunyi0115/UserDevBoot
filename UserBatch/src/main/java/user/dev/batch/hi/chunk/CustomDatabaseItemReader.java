package user.dev.batch.hi.chunk;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Component
@StepScope
@Slf4j
public class CustomDatabaseItemReader implements ItemReader<Customer> {

    private final CustomerRepository customerRepository;
    private Iterator<Customer> customerIterator;
    private boolean initialized = false;

    public CustomDatabaseItemReader(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer read() {
        if (!initialized) {
            List<Customer> customers = customerRepository.findAll();
            log.info("customers:{}",customers);
            customerIterator = customers.iterator();
            initialized = true;
        }

        return customerIterator.hasNext() ? customerIterator.next() : null;
    }
}