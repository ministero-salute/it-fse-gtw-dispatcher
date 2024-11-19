package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class BenchmarkCFG {
    
     /** 
     *  CDA attachment name.
     */
	@Value("${benchmark.enable}")
	private boolean benchmarkEnable;
}