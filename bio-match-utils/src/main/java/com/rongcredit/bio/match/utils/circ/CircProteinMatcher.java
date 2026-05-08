package com.rongcredit.bio.match.utils.circ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.rongcredit.bio.match.utils.MatchResult;
import com.rongcredit.bio.match.utils.ProteinMatcher;
import com.rongcredit.bio.match.utils.ProteinTranslator;
import com.rongcredit.bio.match.utils.RNAProvider;
import com.rongcredit.bio.match.utils.TranslateResult;

/**
 * 面向环状 RNA 场景的蛋白匹配实现。
 * <p>
 * 本实现针对环状 RNA 的结构特征开展蛋白片段检索分析：首先将输入核酸序列转换为适合跨边界分析的候选蛋白集合，
 * 随后在不同阅读框翻译结果中定位目标蛋白片段，并结合边界约束判定其是否构成有效的跨环化位点命中结果。
 * </p>
 */
public class CircProteinMatcher implements ProteinMatcher {

    /**
     * 序列翻译缓存，用于避免同一核酸序列的重复翻译开销。
     */
    private Map<String, CircProtein> sequence2ProteinMapCache = new HashMap<>();

    /**
     * 核酸序列蛋白翻译器。
     */
    private final ProteinTranslator proteinTranslator = new ProteinTranslator();

    /**
     * 缓存写入锁，用于并发场景下保证翻译缓存的一致性。
     */
    private ReentrantLock cacheLock = new ReentrantLock();

    /**
     * 是否启用环化边界校验。
     */
    private final boolean checkBoundary;

    /**
     * 构造默认匹配器，默认启用环化边界校验。
     */
    public CircProteinMatcher() {
        this.checkBoundary = true;
    }

    /**
     * 构造匹配器实例。
     *
     * @param checkBoundary 是否启用环化边界校验
     */
    public CircProteinMatcher(boolean checkBoundary) {
        this.checkBoundary = checkBoundary;
    }

    /**
     * 将输入核酸序列转换为环状 RNA 分析对象。
     * <p>
     * 本方法通过将原始序列与自身拼接，模拟环状 RNA 跨边界翻译的计算环境，并分别在三个阅读框下生成候选蛋白序列。
     * 同时，方法会记录用于边界判定的关键位置，为后续跨边界匹配提供依据。
     * </p>
     *
     * @param sequence 输入核酸序列
     * @return 环状 RNA 蛋白信息对象
     */
    private CircProtein toCirc(final String sequence) {
        List<Integer> boundarys = new ArrayList<>();
        int boundary = sequence.length() / 3;
        boundarys.add(boundary);

        List<String> proteins = new ArrayList<>();
        for (int beginIndex = 0; beginIndex < 3; beginIndex++) {
            String subSeq = sequence.substring(beginIndex) + sequence;
            TranslateResult translateResult = proteinTranslator.translate(subSeq);
            String protein = translateResult.getProtein();
            proteins.add(protein);
        }
        return new CircProtein(proteins, boundarys);
    }

    /**
     * 将核酸序列转换为可复用的环状翻译结果对象。
     *
     * @param sequence 输入核酸序列
     * @return 适用于环状匹配分析的翻译结果对象
     */
    private CircProtein translate(final String sequence) {
        if (sequence == null) {
            throw new IllegalArgumentException("The DNA/RNA sequence can not be null");
        }
        String normalizedSequence = sequence.toUpperCase();
        CircProtein protein = sequence2ProteinMapCache.get(normalizedSequence);
        if (protein != null) {
            return protein;
        }
        cacheLock.lock();
        try {
            if (protein == null) {
                protein = toCirc(normalizedSequence);
                sequence2ProteinMapCache.put(sequence, protein);
            }
            return protein;
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * 本地调试入口。
     *
     * @param args 启动参数列表
     */
    public static void main(String[] args) {
        CircProteinMatcher matcher = new CircProteinMatcher(true);
        final String rna = "GTCTGGGATAAAAGTGAAAGTGGTGATTGGCATTGTACTGCTAGCTGGAAGACACATAGTGGATCTGTATGGCGTGTGACATGGGCCCATCCTGAATTTGGGCAGGTTTTGGCTTCCTGTTCTTTTGACCGAACAGCTGCTGTATGGGAAGAAATAGTAGGAGAATCAAATGATAAACTGCGAGGACAGAGCCACTGGGTTAAAAGGACAACTCTGGTGGATAGCAGAACATCTGTTACTGATGTGAAGTTTGCTCCCAAGCACATGGGTCTTATGTTAGCAACCTGTTCCGCAGATGGTATAGTAAGAATCTATGAGGCACCAGATGTTATGAATCTCAGCCAGTGGTCTTTGCAGCATGAGATCTCATGTAAGCTAAGCTGTAGTTGTATTTCTTGGAACCCTTCAAGCTCTCGTGCTCATTCCCCCATGATCGCCGTAGGAAGTGATGACAGTAGCCCCAACGCAATGGCCAAGGTTCAGATTTTTGAATATAATGAAAACACCAG";
        CircProtein protein = matcher.translate(rna);
        for (String p : protein.getProteins()) {
            System.out.println(p);
        }

        System.out.println(Arrays.toString(protein.getBoundarys().toArray()));
        List<MatchResult> results1 = matcher.match(null, rna, "VQIFEYNENTR", 1, 1);
        if (results1 != null) {
            for (MatchResult results : results1) {
                System.out.println(results.toString());
            }
        }
        // List<MatchResult> results2 = matcher.match(null, rna, "SGFVRNRTF");
        // if (results2 != null) {
        // for (MatchResult results : results2) {
        // System.out.println(results.toString());
        // }
        // }
        // List<MatchResult> results2 = matcher.match(null, rna, "NATAASGFVRNRTF");
        // if (results2 != null) {
        // for (MatchResult results : results2) {
        // System.out.println(results.toString());
        // }
        // }
    }

    /**
     * 对单条核酸序列执行蛋白匹配分析。
     * <p>
     * 本方法首先获取目标序列对应的环状翻译结果，随后在不同阅读框下生成的蛋白序列中检索目标片段。
     * 当启用边界校验时，仅保留满足跨边界判定条件的匹配结果，以提高结果解释的一致性。
     * </p>
     *
     * @param key 序列标识
     * @param RNA 核酸序列内容
     * @param protein 待匹配蛋白片段
     * @param leftOffset 边界左偏移量
     * @param rightOffset 边界右偏移量
     * @return 匹配结果列表；若不存在符合条件的结果，则返回 {@code null}
     */
    private List<MatchResult> match(final String key, final String RNA, final String protein, final int leftOffset,
            final int rightOffset) {
        // 获取当前核酸序列对应的环状翻译结果。
//        boolean x = (RNA == null || RNA.isBlank() ? 0 : RNA.length() % 3) == 0 ? true : false;
        CircProtein circProtein = translate(RNA);
        List<String> targets = circProtein.getProteins();
        List<Integer> boundarys = circProtein.getBoundarys();
        // 逐个阅读框执行目标蛋白片段匹配。
        final int pl = protein.length();
        List<MatchResult> results = new ArrayList<>();
        int targetIndex = 0;
        int left = leftOffset + 1;
        int right = rightOffset;
        for (String target : targets) {

            Integer matchedBoundary = null;
            Integer matchedIndex = null;

            for (int fromIndex = 0; fromIndex < target.length(); fromIndex++) {
                int index = target.indexOf(protein, fromIndex);
                if (index == -1) {
                    break;
                }
                fromIndex = index + 1;
                // 根据配置判断当前命中是否满足边界条件。
                if (checkBoundary) {
                    for (Integer boundary : boundarys) {
                        if ((index <= boundary - left) && ((index + pl) >= (boundary + right))) {
                            matchedBoundary = boundary;
                            break;
                        }
                    }
                    if (matchedBoundary != null) {
                        matchedIndex = index;
                        break;
                    }
                } else {
                    matchedIndex = index;
                    break;
                }
            }

            MatchResult result = null;
            if (matchedIndex != null) {
                result = new MatchResult();
                result.setDnaKey(key);
                result.setTargetProtein(target);
                result.setTargetIndex(targetIndex);
                result.setIndex(matchedIndex);
                result.setBoundary(matchedBoundary);
                result.setProtein(protein);
                results.add(result);
            }
            targetIndex++;
        }
        return results.isEmpty() ? null : results;
    }

    /**
     * 在序列提供者中的全部核酸序列上并行执行匹配分析。
     *
     * @param provider 核酸序列提供者
     * @param protein 待匹配蛋白片段
     * @param leftOffset 边界左偏移量
     * @param rightOffset 边界右偏移量
     * @return 全部匹配结果；当输入参数不合法或不存在结果时，返回 {@code null} 或空列表
     */
    @Override
    public List<MatchResult> match(final RNAProvider provider, final String protein, final int leftOffset,
            final int rightOffset) {
        if (provider == null || protein == null || protein.isBlank()) {
            return null;
        }
        List<MatchResult> matchedSequences = new ArrayList<>();
        ReentrantLock writeLock = new ReentrantLock();
        provider.entrySet().parallelStream().forEach(entry -> {
            String id = entry.getKey();
            String sequence = entry.getValue();
            if (sequence == null || sequence.isBlank()) {
                return;
            }
            List<MatchResult> result = match(id, sequence, protein.toUpperCase(), leftOffset, rightOffset);
            if (result != null) {
                writeLock.lock();
                try {
                    matchedSequences.addAll(result);
                } finally {
                    writeLock.unlock();
                }
            }
        });
        return matchedSequences;
    }
}
