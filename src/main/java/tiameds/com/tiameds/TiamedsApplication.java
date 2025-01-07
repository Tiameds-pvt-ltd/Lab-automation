package tiameds.com.tiameds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication
@PropertySource("classpath:application.yml")
public class TiamedsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiamedsApplication.class, args);
    }

}
