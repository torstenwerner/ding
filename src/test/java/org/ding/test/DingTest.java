package org.ding.test;

import org.ding.DingName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static org.ding.DingManager.dingManager;
import static org.ding.DingName.dingName;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DingTest {
    @Before
    public void before() {
        dingManager.deleteAllBeans();
    }

    @Test
    public void testBasicUsage() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hello");
        dingManager.addBean("hello", () -> stringBuilder, CharSequence.class);
        final Supplier<CharSequence> bean01 = dingManager.getBean("hello", CharSequence.class);
        assertThat(bean01.get(), notNullValue());
        assertThat(bean01.get().length(), is(5));
        assertThat(bean01.get().toString(), is("Hello"));

        dingManager.addBean("hello", () -> "World!", String.class);
        assertThat(bean01.get(), notNullValue());
        assertThat(bean01.get().length(), is(6));
        assertThat(bean01.get().toString(), is("World!"));

        final Supplier<String> bean02 = dingManager.getBean("hello", String.class);
        assertThat(bean02.get(), notNullValue());
        assertThat(bean02.get().length(), is(6));
        assertThat(bean02.get(), startsWith("Wo"));
    }

    @Test
    public void testWrongTypeDuringGet() throws Exception {
        dingManager.addBean("hello", () -> "World!", String.class);
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
        dingManager.addBean("hello", () -> "World!", String.class);
        final Supplier<CharSequence> bean = dingManager.getBean("hello", String.class);
        bean.get();
        dingManager.addBean("hello", () -> 26, String.class);
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
        dingManager.addBean("hello", () -> "Hello", String.class);
        final Supplier<CharSequence> bean = dingManager.getBean("hello", String.class);
        bean.get();
        try {
            dingManager.addBean("hello", () -> "World!", CharSequence.class);
            fail("missing exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("incompatible classes for bean hello, old: class java.lang.String, new: interface java.lang.CharSequence"));
        }
    }

    @Test
    public void testNameSpace() throws Exception {
        dingManager.addBean("hello", () -> "Hello", String.class);

        final DingName hello = dingName("http://ding.it.or.else", "hello");
        Assert.assertThat(hello.toString(), is("{http://ding.it.or.else}hello"));
        dingManager.addBean(hello, () -> "World!", String.class);

        Assert.assertThat(dingManager.getBean("hello", String.class).get(), is("Hello"));
        Assert.assertThat(dingManager.getBean(hello, String.class).get(), is("World!"));
    }
}
