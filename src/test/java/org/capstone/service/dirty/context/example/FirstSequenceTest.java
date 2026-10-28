package org.capstone.service.dirty.context.example;


import org.capstone.service.SequenceGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
public class FirstSequenceTest {

    @Autowired
    SequenceGeneratorService sequenceGeneratorService;

    @Test
    public void testIncrement() {
        int nextValue = sequenceGeneratorService.getNext();
        assertThat(nextValue).isEqualTo(1);
    }
}
