package com.rongcredit.bio.match.console.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.rongcredit.bio.match.console.command.data.DNAData;
import com.rongcredit.bio.match.console.command.data.ProteinData;
import com.rongcredit.bio.match.console.config.BioMatchConfig;
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
	private final BioMatchConfig bioMatchConfig;

	public MatchCommand(BioMatchConfig bioMatchConfig) {
		this.bioMatchConfig = bioMatchConfig;
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
		String filePath = getString(args, "dna", bioMatchConfig.getDnaFile());
		File dnaFile = assertFileExists(filePath);
		if (dnaFile == null) {
			log.error("The dna file does not specified or the file can not read: {}", filePath);
			return;
		}
		log.info("The dna file is: {}", filePath);
		filePath = getString(args, "protein", bioMatchConfig.getProteinFile());
		File proteinFile = assertFileExists(filePath);
		if (proteinFile == null) {
			log.error("The protein file does not specified or the file can not read: {}", filePath);
			return;
		}
		log.info("The protein file is: {}", filePath);
		String outputPath = getString(args, "output", bioMatchConfig.getOutputFile());
		log.info("The output file is: {}", outputPath);

		int circLoop = getInt(args, "circ", bioMatchConfig.getCircLoop());
		log.info("The crec loop is: {}", circLoop);
		boolean boundary = args.getOptionNames().contains("boundary");
		if (!boundary) {
			boundary = bioMatchConfig.isBoundary();
		}
		log.info("Check Boundary: {}", boundary);

		// Load the dna file data
		MemoryHashSetRNAProvider dnaData;
		try {
			dnaData = loadDna(dnaFile);
		} catch (Throwable t) {
			log.error("Load dna file faild", t);
			return;
		}
		// Load the protein file data
		Map<String, String> protinData;
		try {
			protinData = loadProtein(proteinFile);
		} catch (Throwable t) {
			log.error("Load protein file faild", t);
			return;
		}
		AtomicInteger total = new AtomicInteger(protinData.size());
		AtomicInteger finished = new AtomicInteger(0);
		final CircProteinMatcher matcher = new CircProteinMatcher(circLoop, boundary);
		try (final BufferedWriter writer = (outputPath == null ? null
				: new BufferedWriter(new FileWriter(new File(outputPath), Charset.forName("UTF-8"))))) {
			ReentrantLock outputLock = new ReentrantLock();
			protinData.entrySet().parallelStream().forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				List<MatchResult> results = matcher.match(dnaData, value);
				if (results != null && !results.isEmpty()) {
					if (writer != null) {
						outputLock.lock();
						try {
							for (MatchResult result : results) {
								try {
									writer.append(String.format("%s matched to %s at %s", key, result.getDnaKey(),
											result.getIndex()));
									writer.newLine();
									writer.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
								log.info("{} matched to {} at {}", key, result.getDnaKey(), result.getIndex());
							}
						} finally {
							outputLock.unlock();
						}

					}
				} else {
//                    if (writer != null) {
//                        outputLock.lock();
//                        try {
//                            try {
//                                writer.append(String.format("%s did not matched", protein));
//                                writer.newLine();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        } finally {
//                            outputLock.unlock();
//                        }
//                    }
//                    log.info("{} did not matched", protein);
				}
				finished.incrementAndGet();
				if (finished.get() % 100 == 0) {
					log.info("remains: {}", total.get() - finished.get());
				}
			});
			log.info("finished");
		} catch (Throwable t) {
			log.error("match faild:", t);
		}
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
					if (dna.startsWith(">")) {
						if (temp[1] != null && !temp[1].isBlank()) {
							if (data.containsKey(temp[0])) {
								log.warn("duplicated dna:{}, {}", rowIndex, temp[0]);
							} else {
								data.put(temp[0], temp[1]);
							}
						}
						temp[0] = dna;
						temp[1] = "";
					} else {
						if (temp[1] != null && !temp[1].isBlank()) {
							log.warn("may be wrong at: {}, {}", rowIndex, temp[0]);
						}
						temp[1] += dna;
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
		final Map<String, String> data = new TreeMap<>();
		try (FileInputStream inputStream = new FileInputStream(proteinFile)) {
			EasyExcel.read(inputStream, ProteinData.class, new ReadListener<ProteinData>() {
				@Override
				public void invoke(ProteinData item, AnalysisContext context) {
					if (item == null) {
						return;
					}
					String proteinName = item.getProteinName();
					if (proteinName == null || proteinName.isBlank()) {
						return;
					}
					String protein = item.getProtein();
					if (protein == null || protein.isBlank()) {
						return;
					}
					protein = protein.trim();
					String normalized = protein.replaceAll(pattern1, "").replaceAll(pattern2, "");
					data.put(item.getProteinName() + ":" + protein, normalized);
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
		System.out.println(">hsa_circ_00001".matches("(A|T|C|G)+"));
		System.out.println("[R].HIADLAGNSEVILPVPAFNVINGGSHAGNK.[L]".replaceAll("(\\[(\\w|-)+\\]|\\.)", ""));
	}
}
