package io.github.victorleasilva.capo;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Ponto de entrada da aplicação do laboratório "Arquitetura Debaixo do Capô". */
@SpringBootApplication
public class CapoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapoApplication.class, args);
    }

    /** O relógio da aplicação, sempre injetado (00 §4): os testes podem trocá-lo por um relógio fixo. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
