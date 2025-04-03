package com.rongcredit.bio.match.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "bio-match")
@Component
public class BioMatchConfig {

	private boolean boundary = false;
	private int circLoop = 4;
	private String dnaFile;
	private String proteinFile;
	private String outputFile = "./output.txt";
}
