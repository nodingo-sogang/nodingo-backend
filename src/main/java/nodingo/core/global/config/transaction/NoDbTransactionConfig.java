package nodingo.core.global.config.transaction;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoDbTransactionConfig {

    @Bean(name = "noDbTransactionManager")
    public ResourcelessTransactionManager noDbTransactionManager() {
        return new ResourcelessTransactionManager();
    }
}