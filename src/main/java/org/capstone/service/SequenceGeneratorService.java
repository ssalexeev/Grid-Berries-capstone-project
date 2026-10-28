package org.capstone.service;

import org.springframework.stereotype.Service;

@Service
public class SequenceGeneratorService {

    private int counter = 0;

    public int getNext() {
        return ++counter;
    }

    public int getCurrent() {
        return counter;
    }
}
