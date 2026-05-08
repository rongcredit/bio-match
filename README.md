# bio-match

## 作者信息

- 刘圣林，[liushl123@126.com](mailto:liushl123@126.com)，怀化学院
- 欧键，[oujian@rongcredit.cn](mailto:oujian@rongcredit.cn)，深圳融克迪特科技有限公司

## 项目简介

`bio-match` 是一个基于 Maven 的多模块 Java 项目，用于生物序列相关的翻译与匹配分析，主要面向以下场景：

- 将 DNA/RNA 序列翻译为蛋白序列
- 针对环状 RNA 场景进行蛋白片段匹配
- 通过控制台命令批量读取 DNA 与蛋白数据文件并输出匹配结果

项目整体由一个控制台应用模块和一个基础算法工具模块组成。

## 作者与依赖说明

- `FlowX` 组件为深圳融克迪特科技有限公司开发的基于 Spring Boot 的后端框架，目前尚未开源。
- 本项目父工程依赖 `flowx`，工具模块依赖 `flowx-utils`，因此构建前需要在本地或私有仓库中具备相关依赖。

## 模块说明

### 1. bio-match-console

控制台应用模块，负责启动 Spring Boot 上下文并执行命令。

- 启动类：`com.rongcredit.bio.match.console.BioMatchApp`
- 扫描包路径：`com.rongcredit`
- 排除了数据库自动配置：`DataSourceAutoConfiguration`
- 主要命令实现：`com.rongcredit.bio.match.console.command.MatchCommand`

### 2. bio-match-utils

公共算法工具模块，负责序列翻译、匹配及结果封装。

核心类包括：

- `ProteinTranslator`：负责将核酸序列按密码子翻译为蛋白序列
- `ProteinMatcher`：蛋白匹配接口定义
- `CircProteinMatcher`：环状 RNA 蛋白匹配实现
- `RNAProvider`：RNA/DNA 序列提供接口
- `MemoryHashSetRNAProvider`：基于内存的序列提供实现
- `MatchResult`：匹配结果对象
- `TranslateResult`：翻译结果对象

## 项目结构

```text
bio-match/
├─ pom.xml                       父工程
├─ bio-match-console/            控制台应用模块
└─ bio-match-utils/              序列匹配工具模块
```

## 环境要求

- JDK 8 及以上
- Maven 3.x
- 可访问 `flowx` / `flowx-utils` 相关依赖

## 构建方式

在项目根目录执行：

```bash
mvn -T 1C clean install
```

该命令会同时构建两个模块，并将构建产物安装到本地 Maven 仓库。

如果只构建工具模块，可执行：

```bash
mvn -pl bio-match-utils clean install
```

## 运行方式

### 1. 使用 Spring Boot 方式启动

在项目根目录执行：

```bash
cd bio-match-console
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev --match"
```

### 2. 使用打包后的 Jar 启动

```bash
cd bio-match-console
mvn package
java -jar target/bio-match-console-1.0.1-SNAPSHOT.jar --match
```

## 核心命令说明

项目当前的核心控制台命令为 `match`。

只有在启动参数中包含 `--match` 时，匹配逻辑才会真正执行。

支持的主要参数如下：

- `--match`：执行匹配任务
- `--dna=文件路径`：指定 DNA 输入文件
- `--protein=文件路径`：指定蛋白输入文件
- `--output=文件路径`：指定输出结果文件
- `--boundary`：启用边界检测
- `--include`：保留方括号中的内容参与归一化处理
- `--left=数字`：左侧偏移量
- `--right=数字`：右侧偏移量

其中部分参数如果未显式传入，会从配置文件中读取默认值。

## 默认配置

控制台模块的默认配置文件位于：

- `bio-match-console/config/application.yml`

默认配置要点如下：

- `left: 1`
- `right: 1`
- `include: true`
- `boundary: true`
- `dnaFile: ./sample/CircRNA序列.xlsx`
- `proteinFile: ./sample/质谱检测到的肽段-peptide1.xlsx`
- `outputFile: ./output.txt`

## 输入数据格式

### 1. DNA 文件

DNA 数据通过 EasyExcel 读取，映射类为 `DNAData`。

- 读取第 1 列（索引 0）作为 DNA 文本
- 当单元格内容以 `>` 开头时，视为一条序列的标识头
- 后续行内容会被拼接为该标识对应的序列内容

也就是说，DNA 文件更接近一种按行展开的 FASTA 风格 Excel 数据。

### 2. 蛋白文件

蛋白数据通过 EasyExcel 读取，映射类为 `ProteinData`。

- 第 3 列（索引 2）读取蛋白序列
- 第 8 列（索引 7）读取蛋白名称

程序会对蛋白序列做归一化处理：

- 去掉类似 `(+57.02)` 这样的修饰标记
- 根据 `include` 参数决定是否移除方括号内容或仅移除方括号符号本身

最终以 “蛋白名称:原始蛋白串” 作为键，归一化后的蛋白串作为实际匹配内容。

## 匹配逻辑说明

匹配逻辑主要由 `CircProteinMatcher` 实现，处理流程大致如下：

1. 将输入核酸序列按 3 个不同阅读框进行翻译
2. 将原始序列与拼接序列结合，模拟环状 RNA 的翻译结果
3. 在翻译后的蛋白序列中查找目标蛋白片段
4. 根据 `left`、`right` 和 `boundary` 配置判断命中结果是否跨越边界
5. 输出命中的序列标识、目标蛋白、命中位置等信息

输出结果对象为 `MatchResult`，核心字段包括：

- `dnaKey`：DNA 序列标识
- `protein`：待匹配蛋白片段
- `targetProtein`：翻译后的目标蛋白序列
- `targetIndex`：目标蛋白序列索引
- `index`：匹配位置
- `boundary`：命中的边界位置

## 输出结果

当指定输出文件时，程序会将结果以 UTF-8 文本写入输出文件，每条结果一行，格式类似：

```text
蛋白名称:原始蛋白 matched to >hsa_circ_xxxx at 123
```

同时程序也会在日志中输出匹配信息与处理进度。

## 开发说明

- 控制台命令继承自 `AbstractCommand`，命令风格依赖 FlowX 提供的命令框架能力
- DNA 与蛋白文件均使用 EasyExcel 读取
- 匹配过程使用并行流处理，适合批量数据场景
- 输出写文件时使用锁控制，避免并行写入冲突

## 测试

可在模块目录下执行：

```bash
mvn test
```

如果本地缺少 `flowx` 相关依赖，测试与构建可能无法通过。

## 许可证

本项目采用 MIT License，详见根目录 `LICENSE` 文件。
