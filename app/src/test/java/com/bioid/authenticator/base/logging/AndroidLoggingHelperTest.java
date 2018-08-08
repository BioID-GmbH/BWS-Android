package com.bioid.authenticator.base.logging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class AndroidLoggingHelperTest {

    private static class ShortClassName {
    }

    private static class ClassNameIs23Characters {
    }

    private static class ClassNameLargerThan23Characters {
    }

    @Test
    public void testConstructor_UsingNameOfClassAsTag() {
        AndroidLoggingHelper loggingHelper = new AndroidLoggingHelper(ShortClassName.class);
        assertThat(loggingHelper.tag, is("ShortClassName"));
    }

    @Test
    public void testConstructor_UsingNameOfClassAsTagWithAMaxOf23Chars() {
        AndroidLoggingHelper loggingHelper = new AndroidLoggingHelper(ClassNameIs23Characters.class);
        assertThat(loggingHelper.tag, is("ClassNameIs23Characters"));
    }

    @Test
    public void testConstructor_UsingFirst23CharsOfClassNameAsTag() {
        AndroidLoggingHelper loggingHelper = new AndroidLoggingHelper(ClassNameLargerThan23Characters.class);
        assertThat(loggingHelper.tag, is("ClassNameLargerThan23Ch"));
    }
}