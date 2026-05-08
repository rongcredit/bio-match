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
import com.rongcredit.flowx.utils.commands.AbstractCommand;

import lombok.extern.slf4j.Slf4j;

/**
 * 蛋白匹配控制台命令实现。
 * <p>
 * 本命令负责组织整个批量匹配分析流程，包括运行参数解析、DNA 序列数据加载、蛋白序列标准化处理、
 * 环状 RNA 场景下的匹配计算以及分析结果输出。该类是控制台模块与底层匹配算法之间的主要衔接入口。
 * </p>
 */
@Component("com.rongcredit.bio.match.console.command")
@Slf4j
public class MatchCommand extends AbstractCommand {

    /**
     * 用于识别并移除形如 `(+57.02)` 的修饰标记。
     */
    private static final String pattern1 = "\\(\\+\\d+(\\.\\d+)?\\)";

    /**
     * 在不保留方括号内容时，用于移除方括号包裹片段及点号的正则表达式。
     */
    private static final String pattern2 = "(\\[(\\w|-)+\\]|\\.)";

    /**
     * 在保留方括号内部字符时，用于去除括号符号及点号的正则表达式。
     */
    private static final String pattern3 = "(\\[|\\]|\\.)";

    /**
     * 命令运行所依赖的默认配置对象。
     */
    private final BioMatchConfig bioMatchConfig;

    /**
     * 构造命令实例。
     *
     * @param bioMatchConfig 配置对象
     */
    public MatchCommand(BioMatchConfig bioMatchConfig) {
        this.bioMatchConfig = bioMatchConfig;
    }

    /**
     * 返回命令关键字。
     *
     * @return 固定命令名称 `match`
     */
    @Override
    public String getCommand() {
        return "match";
    }

    /**
     * 校验目标文件路径是否存在且具备可读性。
     *
     * @param filePath 文件路径
     * @return 可读文件对象；若路径为空、文件不存在或不可读，则返回 {@code null}
     */
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

    /**
     * 执行匹配分析命令。
     * <p>
     * 方法执行流程包括：识别任务开关、解析输入输出参数、加载 DNA 与蛋白数据、调用
     * {@link CircProteinMatcher} 完成并行匹配，以及将命中结果写出至日志与结果文件。
     * 该过程构成控制台模式下的完整分析主流程。
     * </p>
     *
     * @param args 启动参数
     */
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

        boolean boundary = args.getOptionNames().contains("boundary");
        if (!boundary) {
            boundary = bioMatchConfig.isBoundary();
        }
        log.info("Check Boundary: {}", boundary);
        boolean include = args.getOptionNames().contains("include");
        if (!include) {
            include = bioMatchConfig.isInclude();
        }
        log.info("Include Breaks: {}", include);

        int left = getInt(args, "left", bioMatchConfig.getLeft());
        log.info("Left offset: {}", left);

        int right = getInt(args, "right", bioMatchConfig.getRight());
        log.info("Right offset: {}", right);

        // 加载 DNA 序列数据。
        MemoryHashSetRNAProvider dnaData;
        try {
            dnaData = loadDna(dnaFile);
        } catch (Throwable t) {
            log.error("Load dna file faild", t);
            return;
        }
        // 加载并标准化蛋白序列数据。
        Map<String, String> protinData;
        try {
            protinData = loadProtein(proteinFile, include);
        } catch (Throwable t) {
            log.error("Load protein file faild", t);
            return;
        }
        AtomicInteger total = new AtomicInteger(protinData.size());
        AtomicInteger finished = new AtomicInteger(0);
        final CircProteinMatcher matcher = new CircProteinMatcher(boundary);
        try (final BufferedWriter writer = (outputPath == null ? null
                : new BufferedWriter(new FileWriter(new File(outputPath), Charset.forName("UTF-8"))))) {
            ReentrantLock outputLock = new ReentrantLock();
            protinData.entrySet().parallelStream().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                List<MatchResult> results = matcher.match(dnaData, value, left, right);
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
                    // if (writer != null) {
                    // outputLock.lock();
                    // try {
                    // try {
                    // writer.append(String.format("%s did not matched", protein));
                    // writer.newLine();
                    // } catch (IOException e) {
                    // e.printStackTrace();
                    // }
                    // } finally {
                    // outputLock.unlock();
                    // }
                    // }
                    // log.info("{} did not matched", protein);
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

    /**
     * 从 DNA Excel 文件中装载核酸序列数据。
     * <p>
     * 本方法默认读取首列数据，并按照“以 `&gt;` 开头的行为序列标识、其后若干行为序列正文”的规则
     * 对输入内容进行组装，从而形成后续匹配分析所需的完整序列集合。
     * </p>
     *
     * @param dnaFile DNA 输入文件
     * @return 基于内存的核酸序列提供者
     * @throws Throwable 当文件读取或解析过程发生异常时抛出
     */
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

    /**
     * 从蛋白 Excel 文件中装载并标准化蛋白序列。
     * <p>
     * 本方法提取蛋白名称与原始蛋白序列字段，并根据配置移除修饰标记、方括号及点号等辅助符号，
     * 从而生成可直接用于匹配分析的标准化蛋白片段映射表。
     * </p>
     *
     * @param proteinFile 蛋白输入文件
     * @param include 是否保留方括号中的字符内容
     * @return 标准化后的蛋白映射表，键为“蛋白名称:原始蛋白串”
     * @throws Throwable 当文件读取或解析过程发生异常时抛出
     */
    protected Map<String, String> loadProtein(File proteinFile, boolean include) throws Throwable {
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
                    String normalized = protein.replaceAll(pattern1, "");
                    if (include) {
                        normalized = normalized.replaceAll(pattern3, "");
                    } else {
                        normalized = normalized.replaceAll(pattern2, "");
                    }
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

    /**
     * 本地调试入口。
     *
     * @param args 启动参数列表
     */
    public static void main(String[] args) {
        System.out.println(">hsa_circ_00001".matches("(A|T|C|G)+"));
        System.out.println("[R].HIADLAGNSEVILPVPAFNVINGGSHAGNK.[L]".replaceAll(pattern2, ""));
    }
}
