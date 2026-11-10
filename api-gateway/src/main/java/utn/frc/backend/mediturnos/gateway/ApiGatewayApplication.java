package utn.frc.backend.mediturnos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal de Spring Cloud Gateway para MediTurnos API.
 * Ejecuta en entorno reactivo no bloqueante (Project Reactor + WebFlux / Netty).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
