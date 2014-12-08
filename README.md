# Dependency Injection of next Generation (Ding) for Java 8

- no dependencies except Java 8
- singleton and thread scope is currently supported but more might be added in the future
- implementation is based on lambda syntax
- no bytecode modification
- almost no reflection, just a little bit for better error messages
- beans are registered with Java code only (no XML configuration)
- namespace support for bean names: multiple libraries can provide beans and avoid name conflicts
- non library application code can ignore namespaces

## Examples of Usage

Register a bean:

    dingManager.addSingletonBean("hello", () -> {
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
The bean is created during the first call to get() and cached until it is eventually replaced by another bean. Please
do not store the return value of helloBean.get() for a longer time since it would no longer be managed by the
dingManager. Keeping it in a local variable for a short interval would be okay

Replace an existing bean:

    final String who = "World!";
    dingManager.addSingletonBean("hello", () -> "Hello " + who, String.class)
    System.out.println(helloBean.get().toString());
The old bean will be removed but the new bean will be created with the first call to get(). We have changed the type
CharSequence to String. That works because String is a subtype of CharSequence. But we can still only use the methods
of CharSequence since we have retrieved the bean wrapper with type CharSequence. That means that e.g.
helloBean.get().startsWith(...) is invalid.

The rationale for replacing a bean is that a library might provide some default beans and your application wants to
replace some of them. Or the test code wants to provide some mock implementations.

A class can be registered the same way either through the constructor or a factory method:

    dingManager.addSingletonBean("myService", MyServiceImplementation::new, MyServiceInterface.class);
    dingManager.addSingletonBean("myService", MyServiceFactory::createService, MyServiceInterface.class);

You can use namespaces to avoid name clashes with other libraries. URLs are suitable as namespace values:

    dingManager.addSingletonBean(dingName("https://github.com/torstenwerner/ding", "myService"),
        MyServiceImplementation::new, MyServiceInterface.class);

Constructor injections looks like this but beware that you won't get any updates as the bean is referenced directly:

    final Supplier<String> dependency = dingManager.getBean("string", String.class);
    dingManager.addSingletonBean("stringBuilder", () -> new StringBuilder(dependency.get()), StringBuilder.class);
This is similar to storing the bean instead of the Supplier in the classes field.

Sometimes it might be useful to force the initialization of all beans for debugging purposed instead of waiting for the
lazy initialization:

    dingManager.initializeSingletons();

Real dependency injection is possible, too:

    dingManager.addSingletonBean("string", () -> "Hello", String.class);
    dingManager.addSingletonBean("stringBuilder", StringBuilder::new, StringBuilder.class,
        new DingDependency<>("string", StringBuilder::append, String.class));
The dependency "string" will be injected using the BiConsumer StringBuilder::append after the bean "stringBuilder" has
been created. Any number of DingDependency can be added to the parameter list of the addSingletonBean() method.

Global singleton beans are not the only kind of beans that can be used. Just use the method addThreadBean() to create
beans with thread scope. Every thread will get its own instance of the bean:

    dingManager.addThreadBean("myService", MyServiceImplementation::new, MyServiceInterface.class);

Such a bean cannot be as easily replaced as singleton beans. Adding a new bean with the same name won't influence
existing threads. Existing threads will continue to use the old bean but new threads will fetch the new bean. And it
won't be possible to change the scope when a bean with the same name is updated. Every bean definition keeps its initial
scope for the full runtime of the JVM.
