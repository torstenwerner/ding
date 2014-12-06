# Dependency Injection of next Generation (Ding) for Java 8

- no dependencies except Java 8
- only singleton scope is currently supported but more is planned for the future
- implementation is based on lambda syntax
- no bytecode modification
- no reflection: at least not yet but might be added in the future
- beans are registered with Java code only (no XML configuration)
- namespace support for bean names: multiple libraries can provide beans and avoid name conflicts
- non library application code can ignore namespaces

## Examples of Usage

Register a bean:

    dingManager.addBean("hello", () -> {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Hello");
            return stringBuilder;
        }, CharSequence.class);
The bean is not created yet because it might itself depend on other beans. It is registered with class CharSequence in
this example. That means it can only retrieved as this type or a base type later.

Retrieve a bean wrapper e.g. for storing it in a classes field:

    private final CharSequence helloBean = dingManager.getBean("hello", CharSequence.class);
The bean is still not yet created. Don't forget that only the original type or one of its base types can be retrieved.

Use the bean:

    System.out.println(helloBean.get().length());
    System.out.println(helloBean.get().toString());
The bean is created during the first call to get() and cached until it is eventually replaced by another bean.

Replace an existing bean:

    final String who = "World!";
    dingManager.addBean("hello", () -> "Hello " + who, String.class)
    System.out.println(helloBean.get().toString());
The old bean will be removed but the new bean will be created with the first call to get(). We have changed the type
CharSequence to String. That works because String is a subtype of CharSequence. But we can still only use the methods
of CharSequence since we have retrieved the bean wrapper with type CharSequence. That means that e.g.
helloBean.get().startsWith(...) is invalid.

Registering a class is similar:

    dingManager.addBean("myservice", MyServiceImplementation::new, MyServiceInterface.class);

You can use namespaces to avoid name clashes with other libraries:

    dingManager.addBean(dingName("https://github.com/torstenwerner/ding", "myservice"),
        MyServiceImplementation::new, MyServiceInterface.class);
