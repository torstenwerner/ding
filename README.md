# Dependency Injection of next Generation (Ding) for Java 8

- no dependencies except Java 8
- implementation is based on lambda syntax
- no bytecode modification
- no reflection: at least not yet but might be added in the future
- beans are registered with Java code only (no XML configuration)
- namespace support for bean names: multiple libraries can provide beans and avoid name conflicts
- non library application code can ignore namespaces
