package com.n26.atrposki;
/*
 * @author aleksandartrposki@gmail.com
 * @since 05.05.18
 *
 *
 */

import com.n26.atrposki.utils.testableAtomics.AtomicLongWrapper;
import com.n26.atrposki.utils.testableAtomics.IAtomicLong;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    IAtomicLong atomicLongWrapper(){
        return new AtomicLongWrapper();
    }

}
