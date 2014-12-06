package org.ding.test;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;

import static org.ding.DingManager.dingManager;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class DingTest {
    @Test
    public void testSuccess() throws Exception {
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

        dingManager.addBean("hello", () -> 26, String.class);
        try {
            bean02.get();
            Assert.fail("exception missing");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("incompatible class, bean class is class java.lang.Integer but got class java.lang.String"));
        }
    }
}
