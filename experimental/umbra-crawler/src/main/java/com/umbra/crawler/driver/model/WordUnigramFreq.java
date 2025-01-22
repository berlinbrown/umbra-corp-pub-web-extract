package com.umbra.crawler.driver.model;

/**
 * Word object with word and frequency from common text.
 */
public class WordUnigramFreq {
    private String word;
    private long frequency;

    public WordUnigramFreq(final String word, final long frequency) {
        this.word = word;
        this.frequency = frequency;
    }

    public String getWord() {
        return word;
    }

    public long getFrequency() {
        return frequency;
    }

    @Override
    public String toString() {
        return "WordUnigramFreq{" +
                "word='" + word + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
