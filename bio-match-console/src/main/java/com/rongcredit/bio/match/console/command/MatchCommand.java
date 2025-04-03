package com.rongcredit.bio.match.console.command;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.rongcredit.bio.match.console.command.data.DNAData;
import com.rongcredit.bio.match.console.command.data.ProteinData;
import com.rongcredit.bio.match.utils.MatchResult;
import com.rongcredit.bio.match.utils.MemoryHashSetRNAProvider;
import com.rongcredit.bio.match.utils.circ.CircProteinMatcher;
import com.rongcredit.commons.utils.commands.AbstractCommand;

import lombok.extern.slf4j.Slf4j;

@Component("com.rongcredit.bio.match.console.command")
@Slf4j
public class MatchCommand extends AbstractCommand {

	private String pattern1 = "\\(\\+\\d+(\\.\\d+)?\\)";
	private String pattern2 = "(\\[(\\w|-)+\\]|\\.)";

	public MatchCommand() {
	}

	@Override
	public String getCommand() {
		return "match";
	}

	protected File assertFileExists(String filePath) {
		if (filePath == null || filePath.isBlank()) {
			return null;
		}
		File file = new File(filePath);
		if (file.exists() && file.canRead()) {
			return file;
		}
		return null;
	}

	@Override
	public void execute(ApplicationArguments args) {
		final boolean match = args.getOptionNames().contains("match");
		if (!match) {
			return;
		}
		String filePath = getString(args, "dna", null);
		File dnaFile = assertFileExists(filePath);
		if (dnaFile == null) {
			log.error("The dna file does not specified or the file can not read: {}", filePath);
			return;
		}
		log.info("The dna file is: {}", filePath);
		filePath = getString(args, "protein", null);
		File proteinFile = assertFileExists(filePath);
		if (proteinFile == null) {
			log.error("The protein file does not specified or the file can not read: {}", filePath);
			return;
		}
		log.info("The protein file is: {}", filePath);
		// Load the dna file data
		MemoryHashSetRNAProvider dnaData;
		try {
			dnaData = loadDna(dnaFile);
		} catch (Throwable t) {
			log.error("Load dna file faild", t);
			return;
		}
		// Load the protein file data
		Map<String, String> proteinData;
		try {
			proteinData = loadProtein(proteinFile);
		} catch (Throwable t) {
			log.error("Load protein file faild", t);
			return;
		}
		AtomicInteger total = new AtomicInteger(proteinData.size());
		AtomicInteger finished = new AtomicInteger(0);
		final CircProteinMatcher matcher = new CircProteinMatcher();
		proteinData.entrySet().parallelStream().forEach(proteinEntry -> {
			String sequence = proteinEntry.getValue();
			List<MatchResult> results = matcher.match(dnaData, sequence);
			if (results != null) {
				for (MatchResult result : results) {
					log.info("{} matched to {} at {}", sequence, result.getDnaKey(), result.getIndex());
				}
			}
			finished.incrementAndGet();
			if (finished.get() % 100 == 0) {
				log.info("remains: {}", total.get() - finished.get());
			}
		});
		log.info("finished");
	}

	protected MemoryHashSetRNAProvider loadDna(File dnaFile) throws Throwable {
		final MemoryHashSetRNAProvider data = new MemoryHashSetRNAProvider();
		try (FileInputStream inputStream = new FileInputStream(dnaFile)) {
			String[] temp = new String[2];
			EasyExcel.read(inputStream, DNAData.class, new ReadListener<DNAData>() {
				@Override
				public void invoke(DNAData item, AnalysisContext context) {
					if (item == null) {
						return;
					}
					Integer rowIndex = context.readRowHolder().getRowIndex();
					if (rowIndex == null) {
						return;
					}
					String dna = item.getDNA();
					if (dna == null || dna.isBlank()) {
						return;
					}
					dna = dna.trim();
					if (rowIndex % 2 == 0) {
						temp[0] = dna;
					} else {
						temp[1] = dna;
						data.put(temp[0], temp[1]);
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					log.info("load {} dna from {} records", data.size(), context.readRowHolder().getRowIndex());
				}
			}).sheet().doRead();
		}
		return data;
	}

	protected Map<String, String> loadProtein(File proteinFile) throws Throwable {
		final Map<String, String> data = new LinkedHashMap<>();
		try (FileInputStream inputStream = new FileInputStream(proteinFile)) {
			EasyExcel.read(inputStream, ProteinData.class, new ReadListener<ProteinData>() {
				@Override
				public void invoke(ProteinData item, AnalysisContext context) {
					if (item == null) {
						return;
					}
					String protein = item.getProtein();
					if (protein == null || protein.isBlank()) {
						return;
					}
					protein = protein.trim();
					String normalized = protein.replaceAll(pattern1, "").replaceAll(pattern2, "");
//					if (data.containsKey(protein)) {
//						log.warn("duplicated: {}", protein);
//					}
					data.put(protein, normalized);
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					log.info("load {} proteins from {} records", data.size(), context.readRowHolder().getRowIndex());
				}
			}).sheet().doRead();
		}
		return data;
	}

	public static void main(String[] args) {
		System.out.println("[R].GIAAQPLYAGYCNHENM.[-]".replaceAll("(\\[(\\w|-)+\\]|\\.)", ""));
	}
}
