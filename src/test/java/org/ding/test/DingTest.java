package org.ding.test;

import org.ding.DingName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static org.ding.DingManager.dingManager;
import static org.ding.DingName.dingName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DingTest {
    @Before
    public void before() {
        dingManager.deleteAllBeans();
    }

    @Test
    public void testBasicUsage() throws Exception {
        dingManager.addSingletonBean("hello", () -> {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Hello");
            return stringBuilder;
        }, CharSequence.class);
        final Supplier<CharSequence> bean01 = dingManager.getBean("hello", CharSequence.class);
        assertThat(bean01.get(), notNullValue());
        assertThat(bean01.get().length(), is(5));
        assertThat(bean01.get().toString(), is("Hello"));

        dingManager.addSingletonBean("hello", () -> "World!", String.class);
        assertThat(bean01.get(), notNullValue());
        assertThat(bean01.get().length(), is(6));
        assertThat(bean01.get().toString(), is("World!"));

        final Supplier<String> bean02 = dingManager.getBean("hello", String.class);
        assertThat(bean02.get(), notNullValue());
        assertThat(bean02.get().length(), is(6));
        assertThat(bean02.get(), startsWith("Wo"));
    }

    private Long threadId;

    @Test
    public void testThreads() throws Exception {
        dingManager.addThreadBean("hello", () -> {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Hello");
            threadId = Thread.currentThread().getId();
            return stringBuilder;
        }, CharSequence.class);
        final Supplier<CharSequence> bean01 = dingManager.getBean("hello", CharSequence.class);
        assertThat(bean01.get(), notNullValue());
        assertThat(bean01.get().length(), is(5));
        assertThat(bean01.get().toString(), is("Hello"));
        assertThat(threadId, notNullValue());
        final Long oldThreadId = threadId;

        threadId = null;
        final Thread newThread = new Thread(bean01::get);
        newThread.start();
        newThread.join();
        assertThat(threadId, notNullValue());
        assertThat(threadId, not(equalTo(oldThreadId)));
    }

    @Test
    public void testWrongTypeDuringGet() throws Exception {
        dingManager.addSingletonBean("hello", () -> "World!", String.class);
        try {
            dingManager.getBean("hello", Integer.class);
            fail("missing exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("incompatible class for bean hello, bean class is class java.lang.String but got class java.lang.Integer"));
        }

    }

    @Test
    public void testTypeDuringAdd() throws Exception {
        dingManager.addSingletonBean("hello", () -> "World!", String.class);
        final Supplier<CharSequence> bean = dingManager.getBean("hello", String.class);
        bean.get();
        dingManager.addSingletonBean("hello", () -> 26, String.class);
        try {
            bean.get();
            fail("exception missing");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("incompatible class for bean hello, bean class is class java.lang.Integer but got class java.lang.String"));
        }
    }

    @Test
    public void testTypeDoubleAdd() throws Exception {
        dingManager.addSingletonBean("hello", () -> "Hello", String.class);
        final Supplier<CharSequence> bean = dingManager.getBean("hello", String.class);
        bean.get();
        try {
            dingManager.addSingletonBean("hello", () -> "World!", CharSequence.class);
            fail("missing exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("incompatible classes for bean hello, old: class java.lang.String, new: interface java.lang.CharSequence"));
        }
    }

    @Test
    public void testNameSpace() throws Exception {
        dingManager.addSingletonBean("hello", () -> "Hello", String.class);

        final DingName hello = dingName("http://ding.it.or.else", "hello");
        Assert.assertThat(hello.toString(), is("{http://ding.it.or.else}hello"));
        dingManager.addSingletonBean(hello, () -> "World!", String.class);

        assertThat(dingManager.getBean("hello", String.class).get(), is("Hello"));
        assertThat(dingManager.getBean(hello, String.class).get(), is("World!"));
    }

    @Test
    public void testRecursive() throws Exception {
        dingManager.addSingletonBean("secondBean", SecondBean::new, SecondBean.class);
        dingManager.addSingletonBean("firstBean", () -> new FirstBean("Hildegunst"), FirstBean.class);

        final Supplier<SecondBean> secondBean = dingManager.getBean("secondBean", SecondBean.class);
        assertThat(secondBean.get().getFirstBean().getSecondBean().getName(), is("Ernie"));
        assertThat(secondBean.get().getFirstBean().getName(), is("Hildegunst"));
    }

    @Test
    public void testInitialize01() throws Exception {
        FirstBean.initialized = false;
        dingManager.addSingletonBean("firstBean", () -> new FirstBean("Hildegunst"), FirstBean.class);
        final Supplier<FirstBean> firstBean = dingManager.getBean("firstBean", FirstBean.class);
        assertThat(FirstBean.initialized, is(false));
        try {
            firstBean.get();
            fail("missing exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("bean secondBean does not exist"));
        }
    }

    @Test
    public void testInitialize02() throws Exception {
        FirstBean.initialized = false;
        dingManager.addSingletonBean("firstBean", () -> new FirstBean("Hildegunst"), FirstBean.class);
        dingManager.addSingletonBean("secondBean", SecondBean::new, SecondBean.class);
        final Supplier<FirstBean> firstBean = dingManager.getBean("firstBean", FirstBean.class);
        assertThat(FirstBean.initialized, is(false));
        firstBean.get();
        assertThat(FirstBean.initialized, is(true));
    }
}
