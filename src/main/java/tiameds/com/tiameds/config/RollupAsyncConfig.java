package tiameds.com.tiameds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated small thread pool for daily_lab_stats rollup recomputation, kept
 * separate from any other async work in the app. See RollupRecomputeListener —
 * moving recomputeDay() off the request thread removes ~6 extra DB queries'
 * worth of latency from every visit/billing/report write.
 */
@Configuration
@EnableAsync
public class RollupAsyncConfig {

    @Bean(name = "rollupTaskExecutor")
    public Executor rollupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("rollup-recompute-");
        executor.initialize();
        return executor;
    }
}
