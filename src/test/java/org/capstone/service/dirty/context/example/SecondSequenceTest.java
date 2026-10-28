package org.capstone.service.dirty.context.example;


import org.capstone.service.SequenceGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
public class SecondSequenceTest {

    @Autowired
    SequenceGeneratorService sequenceGeneratorService;

    @Test
    void testInitialState() {

        assertThat(sequenceGeneratorService.getCurrent())
                .withFailMessage("Expected initial counter to be 0, but context was dirty!")
                .isEqualTo(0);
    }
}
