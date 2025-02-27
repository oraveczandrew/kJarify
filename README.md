[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# KJarify

A performant, multithreaded DEX to Java bytecode translator, written in kotlin.\
It has a similar capabilities as the original EnJarify written in Python ([source here](https://github.com/Storyyeller/enjarify)).

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

On a 8c/16t 7820X CPU 

| DEX size | EnJarify | KJarify |
|----------|----------|---------|
| ~16.6MB  | 1min 5s  | 5.6s    |
| ~94.6MB  | 8 min 9s | 37s     |