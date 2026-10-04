[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# KJarify

A performant, multithreaded DEX to Java bytecode translator, written in kotlin.\
It has similar capabilities as the original EnJarify written in Python ([source here](https://github.com/Storyyeller/enjarify)).

## Usage

### Command line

    java -jar kJarify-fat.jar example.apk

### As a library

#### In Java

    void example(File input, File output) {
        KJarify.process(inputFile, outputFile, OptimizationOptions.PRETTY);
    }

#### In kotlin with coroutines

    suspend fun example(input: File, output: File) {
        KJarify.suspendProcess(input, output, OptimizationOptions.PRETTY)
    }

#### More advanced

##### Java

    List<byte[]> dexDataList = ...

    DexProcessor.ProcessCallBack callback = new DexProcessor.ProcessCallBack() {
        @Override
        public void onClassTranslated(@NotNull String unicodeName, @NotNull byte[] classData) {
            ... handle the translated byte codes
        }
    };

    DexProcessor processor = new DexProcessor(
            OptimizationOptions.ALL,
            Executors.newFixedThreadPool(16),
            callback
    );

    processor.process(dexDataList);

    // you can get the result without a callback too
    Map<String, byte[]> classDataList = processor.classes;

##### Kotlin

    val dexDataList: List<ByteArray> = ...

    val callback = object : DexProcessor.ProcessCallBack() {
        override suspend fun suspendOnClassTranslated(unicodeName: String, classData: ByteArray) {
            ... handle the translated byte codes
        }
    }

    val processor = DexProcessor(
        optimizationOptions = OptimizationOptions(
            ...
        ),
        coroutineDispatcher = Dispatchers.Default,
        callback = callback,
    )

    processor.suspendProcess(dexDataList)

    // you can get the result without a callback too
    val classDataList: Map<String, ByteArray> = processor.classes

### Speed

Recent measurements (8c/16t, OpenJDK 21, CPython 3.14, PRETTY optimization):

| Input     | DEX size | Classes | EnJarify | KJarify |
|-----------|----------|---------|----------|---------|
| small APK | ~18MB    | ~12k    | 56s      | ~1.0s   |
| small APK | ~10.5MB  | ~11k    | 1min 12s | ~1.1s   |
| large APK | ~359MB   | ~445k   | ~32min   | ~41s    |

Translation scales with physical CPU cores (worker count is tunable, see below).

### Profiling

A profiler test translates a bundled sample APK and reports wall time,
throughput, heap and GC stats:

    ./gradlew profiler

Options (all optional):

    ./gradlew profiler -Pinput=app-beta.apk -Pruns=3 -Pwarmup=1 -Pthreads=8 -Pheap=12g -Pjfr -Pgc=parallel

### Performance tips

- For large APKs (>100MB DEX), ParallelGC is ~20% faster than G1:
  `java -XX:+UseParallelGC -jar kJarify-fat.jar big.apk`
- Worker threads beyond the physical core count add little (hyperthreading
  does not help this workload); try matching the physical core count.