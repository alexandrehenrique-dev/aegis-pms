package br.com.byop.aegis.pages.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownBlockTypeExceptionTest {

    @Test
    void shouldExposeMessageWithTheUnknownType() {
        UnknownBlockTypeException exception = new UnknownBlockTypeException("carousel-3d");

        assertThat(exception.getMessage()).isEqualTo("Unknown block type: carousel-3d");
    }
}
