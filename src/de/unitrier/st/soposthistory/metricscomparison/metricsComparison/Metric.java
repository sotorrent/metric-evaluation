package de.unitrier.st.soposthistory.metricscomparison.metricsComparison;

import java.util.function.BiFunction;

class Metric {

    // to make automated process ease
    public enum Type {

        // edit based
        levenshteinStandard,
        levenshteinNormalized,

        damerauLevenshteinStandard,
        damerauLevenshteinNormalized,

        optimalAlignmentStandard,
        optimalAlignmentNormalized,

        // optimalAlignment2GramFingerprint,
        // optimalAlignment3GramFingerprint,
        // optimalAlignment4GramFingerprint,
        // optimalAlignment5GramFingerprint,

        // optimalAlignmentShingle2Fingerprint,
        // optimalAlignmentShingle3Fingerprint,

        // optimalAlignment2GramFingerprintNormalized,
        // optimalAlignment3GramFingerprintNormalized,
        // optimalAlignment4GramFingerprintNormalized,
        // optimalAlignment5GramFingerprintNormalized,

        // optimalAlignmentShingle2FingerprintNormalized,
        // optimalAlignmentShingle3FingerprintNormalized,

        longestCommonSubsequenceStandard,
        longestCommonSubsequenceNormalized,

        // longestCommonSubsequence2GramFingerprint,
        // longestCommonSubsequence3GramFingerprint,
        // longestCommonSubsequence4GramFingerprint,
        // longestCommonSubsequence5GramFingerprint,

        // longestCommonSubsequenceShingle2Fingerprint,
        // longestCommonSubsequenceShingle3Fingerprint,

        // longestCommonSubsequence2GramFingerprintNormalized,
        // longestCommonSubsequence3GramFingerprintNormalized,
        // longestCommonSubsequence4GramFingerprintNormalized,
        // longestCommonSubsequence5GramFingerprintNormalized,

        // longestCommonSubsequenceShingle2FingerprintNormalized,
        // longestCommonSubsequenceShingle3FingerprintNormalized,


        // fingerprint based
        // winnowingTokenJaccard,

        winnowing2GramJaccard,
        winnowing3GramJaccard,
        winnowing4GramJaccard,
        winnowing5GramJaccard,

        // winnowingShingle2Jaccard,
        // winnowingShingle3Jaccard,

        // winnowingNormalizedTokenJaccard,

        winnowingNormalized2GramJaccard,
        winnowingNormalized3GramJaccard,
        winnowingNormalized4GramJaccard,
        winnowingNormalized5GramJaccard,

        // winnowingNormalizedShingle2Jaccard,
        // winnowingNormalizedShingle3Jaccard,

        // winnowingTokenDice,

        winnowing2GramDice,
        winnowing3GramDice,
        winnowing4GramDice,
        winnowing5GramDice,

        // winnowingShingle2Dice,
        // winnowingShingle3Dice,

        // winnowingNormalizedTokenDice,

        winnowingNormalized2GramDice,
        winnowingNormalized3GramDice,
        winnowingNormalized4GramDice,
        winnowingNormalized5GramDice,

        // winnowingNormalizedShingle2Dice,
        // winnowingNormalizedShingle3Dice,

        // winnowingTokenDiceVariant,

        // winnowing2GramDiceVariant,
        // winnowing3GramDiceVariant,
        // winnowing4GramDiceVariant,
        // winnowing5GramDiceVariant,

        // winnowingShingle2DiceVariant,
        // winnowingShingle3DiceVariant,

        // winnowingNormalizedTokenDiceVariant,

        // winnowingNormalized2GramDiceVariant,
        // winnowingNormalized3GramDiceVariant,
        // winnowingNormalized4GramDiceVariant,
        // winnowingNormalized5GramDiceVariant,

        // winnowingNormalizedShingle2DiceVariant,
        // winnowingNormalizedShingle3DiceVariant,

        // winnowingTokenOverlap,

        winnowing2GramOverlap,
        winnowing3GramOverlap,
        winnowing4GramOverlap,
        winnowing5GramOverlap,

        // winnowingShingle2Overlap,
        // winnowingShingle3Overlap,

        // winnowingNormalizedTokenOverlap,

        winnowingNormalized2GramOverlap,
        winnowingNormalized3GramOverlap,
        winnowingNormalized4GramOverlap,
        winnowingNormalized5GramOverlap,

        // winnowingNormalizedShingle2Overlap,
        // winnowingNormalizedShingle3Overlap,


        // profile based
        cosineNormalizedTokensBool,
        cosineNormalizedTokensTermFrequency,
        cosineNormalizedTokensNormalizedTermFrequency,

        cosineNormalized2GramsBool,
        cosineNormalized3GramsBool,
        cosineNormalized4GramsBool,
        cosineNormalized5GramsBool,

        cosineNormalized2GramsTermFrequency,
        cosineNormalized3GramsTermFrequency,
        cosineNormalized4GramsTermFrequency,
        cosineNormalized5GramsTermFrequency,

        cosineNormalized2GramsNormalizedTermFrequency,
        cosineNormalized3GramsNormalizedTermFrequency,
        cosineNormalized4GramsNormalizedTermFrequency,
        cosineNormalized5GramsNormalizedTermFrequency,

        cosineNormalizedShingle2Bool,
        cosineNormalizedShingle3Bool,
        cosineNormalizedShingle2TermFrequency,
        cosineNormalizedShingle3TermFrequency,
        cosineNormalizedShingle2NormalizedTermFrequency,
        cosineNormalizedShingle3NormalizedTermFrequency,

        manhattanNormalizedTokens,

        manhattanNormalized2Grams,
        manhattanNormalized3Grams,
        manhattanNormalized4Grams,
        manhattanNormalized5Grams,

        manhattanNormalizedShingles2,
        manhattanNormalizedShingles3,


        // set based
        jaccardTokens,
        jaccardNormalizedTokens,

        jaccard2Grams,
        jaccard3Grams,
        jaccard4Grams,
        jaccard5Grams,

        jaccardNormalized2Grams,
        jaccardNormalized3Grams,
        jaccardNormalized4Grams,
        jaccardNormalized5Grams,

        jaccardNormalizedPadding2grams,
        jaccardNormalizedPadding3grams,
        jaccardNormalizedPadding4grams,
        jaccardNormalizedPadding5grams,

        jaccardShingles2,
        jaccardShingles3,
        jaccardNormalizedShingles2,
        jaccardNormalizedShingles3,

        diceTokens,
        diceNormalizedTokens,

        dice2Grams,
        dice3Grams,
        dice4Grams,
        dice5Grams,

        diceNormalized2Grams,
        diceNormalized3Grams,
        diceNormalized4Grams,
        diceNormalized5Grams,

        diceNormalizedPadding2grams,
        diceNormalizedPadding3grams,
        diceNormalizedPadding4grams,
        diceNormalizedPadding5grams,

        diceShingles2,
        diceShingles3,
        diceNormalizedShingles2,
        diceNormalizedShingles3,

        // diceVariantTokens,
        // diceVariantNormalizedTokens,

        // diceVariant2Grams,
        // diceVariant3Grams,
        // diceVariant4Grams,
        // diceVariant5Grams,

        // diceVariantNormalized2Grams,
        // diceVariantNormalized3Grams,
        // diceVariantNormalized4Grams,
        // diceVariantNormalized5Grams,

        // diceVariantNormalizedPadding2grams,
        // diceVariantNormalizedPadding3grams,
        // diceVariantNormalizedPadding4grams,
        // diceVariantNormalizedPadding5grams,

        // diceVariantShingles2,
        // diceVariantShingles3,
        // diceVariantNormalizedShingles2,
        // diceVariantNormalizedShingles3,

        overlapTokens,
        overlapNormalizedTokens,

        overlap2Grams,
        overlap3Grams,
        overlap4Grams,
        overlap5Grams,

        overlapNormalized2Grams,
        overlapNormalized3Grams,
        overlapNormalized4Grams,
        overlapNormalized5Grams,

        overlapNormalizedPadding2grams,
        overlapNormalizedPadding3grams,
        overlapNormalizedPadding4grams,
        overlapNormalizedPadding5grams,

        overlapShingles2,
        overlapShingles3,
        overlapNormalizedShingles2,
        overlapNormalizedShingles3,

        twoGramSimilarityKondrak05,
        threeGramSimilarityKondrak05,
        fourGramSimilarityKondrak05,
        fiveGramSimilarityKondrak05,


        winnowingTwoGramLongestCommonSubsequence,
        winnowingThreeGramLongestCommonSubsequence,
        winnowingFourGramLongestCommonSubsequence,
        winnowingFiveGramLongestCommonSubsequence,
        winnowingTwoGramLongestCommonSubsequenceNormalized,
        winnowingThreeGramLongestCommonSubsequenceNormalized,
        winnowingFourGramLongestCommonSubsequenceNormalized,
        winnowingFiveGramLongestCommonSubsequenceNormalized,
        winnowingTwoGramOptimalAlignment,
        winnowingThreeGramOptimalAlignment,
        winnowingFourGramOptimalAlignment,
        winnowingFiveGramOptimalAlignment,
        winnowingTwoGramOptimalAlignmentNormalized,
        winnowingThreeGramOptimalAlignmentNormalized,
        winnowingFourGramOptimalAlignmentNormalized,
        winnowingFiveGramOptimalAlignmentNormalized
    }


    static BiFunction<String, String, Double> getBiFunctionMetric(Type metric) {
        switch (metric) {

            case levenshteinStandard:
                return levenshtein;
            case levenshteinNormalized:
                return levenshteinNormalized;

            case damerauLevenshteinStandard:
                return damerauLevenshtein;
            case damerauLevenshteinNormalized:
                return damerauLevenshteinNormalized;

            case optimalAlignmentStandard:
                return optimalAlignment;
            case optimalAlignmentNormalized:
                return optimalAlignmentNormalized;

            case longestCommonSubsequenceStandard:
                return longestCommonSubsequence;

            case longestCommonSubsequenceNormalized:
                return longestCommonSubsequenceNormalized;

            case winnowing2GramJaccard:
                return winnowingTwoGramJaccard;
            case winnowing3GramJaccard:
                return winnowingThreeGramJaccard;
            case winnowing4GramJaccard:
                return winnowingFourGramJaccard;
            case winnowing5GramJaccard:
                return winnowingFiveGramJaccard;
            case winnowingNormalized2GramJaccard:
                return winnowingTwoGramJaccardNormalized;
            case winnowingNormalized3GramJaccard:
                return winnowingThreeGramJaccardNormalized;
            case winnowingNormalized4GramJaccard:
                return winnowingFourGramJaccardNormalized;
            case winnowingNormalized5GramJaccard:
                return winnowingFiveGramJaccardNormalized;
            case winnowing2GramDice:
                return winnowingTwoGramDice;
            case winnowing3GramDice:
                return winnowingThreeGramDice;
            case winnowing4GramDice:
                return winnowingFourGramDice;
            case winnowing5GramDice:
                return winnowingFiveGramDice;
            case winnowingNormalized2GramDice:
                return winnowingTwoGramDiceNormalized;
            case winnowingNormalized3GramDice:
                return winnowingThreeGramDiceNormalized;
            case winnowingNormalized4GramDice:
                return winnowingFourGramDiceNormalized;
            case winnowingNormalized5GramDice:
                return winnowingFiveGramDiceNormalized;
            case winnowing2GramOverlap:
                return winnowingTwoGramOverlap;
            case winnowing3GramOverlap:
                return winnowingThreeGramOverlap;
            case winnowing4GramOverlap:
                return winnowingFourGramOverlap;
            case winnowing5GramOverlap:
                return winnowingFiveGramOverlap;
            case winnowingNormalized2GramOverlap:
                return winnowingTwoGramOverlapNormalized;
            case winnowingNormalized3GramOverlap:
                return winnowingThreeGramOverlapNormalized;
            case winnowingNormalized4GramOverlap:
                return winnowingFourGramOverlapNormalized;
            case winnowingNormalized5GramOverlap:
                return winnowingFiveGramOverlapNormalized;


            case cosineNormalizedTokensBool:
                return cosineTokenNormalizedBool;
            case cosineNormalizedTokensTermFrequency:
                return cosineTokenNormalizedTermFrequency;
            case cosineNormalizedTokensNormalizedTermFrequency:
                return cosineTokenNormalizedNormalizedTermFrequency;
            case cosineNormalized2GramsBool:
                return cosineTwoGramNormalizedBool;
            case cosineNormalized3GramsBool:
                return cosineThreeGramNormalizedBool;
            case cosineNormalized4GramsBool:
                return cosineFourGramNormalizedBool;
            case cosineNormalized5GramsBool:
                return cosineFiveGramNormalizedBool;
            case cosineNormalized2GramsTermFrequency:
                return cosineTwoGramNormalizedTermFrequency;
            case cosineNormalized3GramsTermFrequency:
                return cosineThreeGramNormalizedTermFrequency;
            case cosineNormalized4GramsTermFrequency:
                return cosineFourGramNormalizedTermFrequency;
            case cosineNormalized5GramsTermFrequency:
                return cosineFiveGramNormalizedTermFrequency;
            case cosineNormalized2GramsNormalizedTermFrequency:
                return cosineTwoGramNormalizedNormalizedTermFrequency;
            case cosineNormalized3GramsNormalizedTermFrequency:
                return cosineThreeGramNormalizedNormalizedTermFrequency;
            case cosineNormalized4GramsNormalizedTermFrequency:
                return cosineFourGramNormalizedNormalizedTermFrequency;
            case cosineNormalized5GramsNormalizedTermFrequency:
                return cosineFiveGramNormalizedNormalizedTermFrequency;
            case cosineNormalizedShingle2Bool:
                return cosineTwoShingleNormalizedBool;
            case cosineNormalizedShingle3Bool:
                return cosineThreeShingleNormalizedBool;
            case cosineNormalizedShingle2TermFrequency:
                return cosineTwoShingleNormalizedTermFrequency;
            case cosineNormalizedShingle3TermFrequency:
                return cosineThreeShingleNormalizedTermFrequency;
            case cosineNormalizedShingle2NormalizedTermFrequency:
                return cosineTwoShingleNormalizedNormalizedTermFrequency;
            case cosineNormalizedShingle3NormalizedTermFrequency:
                return cosineThreeShingleNormalizedNormalizedTermFrequency;

            case manhattanNormalizedTokens:
                return manhattanTokenNormalized;
            case manhattanNormalized2Grams:
                return manhattanTwoGramNormalized;
            case manhattanNormalized3Grams:
                return manhattanThreeGramNormalized;
            case manhattanNormalized4Grams:
                return manhattanFourGramNormalized;
            case manhattanNormalized5Grams:
                return manhattanFiveGramNormalized;
            case manhattanNormalizedShingles2:
                return manhattanTwoShingleNormalized;
            case manhattanNormalizedShingles3:
                return manhattanThreeShingleNormalized;

            case jaccardTokens:
                return tokenJaccard;
            case jaccardNormalizedTokens:
                return tokenJaccardNormalized;
            case jaccard2Grams:
                return twoGramJaccard;
            case jaccard3Grams:
                return threeGramJaccard;
            case jaccard4Grams:
                return fourGramJaccard;
            case jaccard5Grams:
                return fiveGramJaccard;
            case jaccardNormalized2Grams:
                return twoGramJaccardNormalized;
            case jaccardNormalized3Grams:
                return threeGramJaccardNormalized;
            case jaccardNormalized4Grams:
                return fourGramJaccardNormalized;
            case jaccardNormalized5Grams:
                return fiveGramJaccardNormalized;
            case jaccardNormalizedPadding2grams:
                return twoGramJaccardNormalizedPadding;
            case jaccardNormalizedPadding3grams:
                return threeGramJaccardNormalizedPadding;
            case jaccardNormalizedPadding4grams:
                return fourGramJaccardNormalizedPadding;
            case jaccardNormalizedPadding5grams:
                return fiveGramJaccardNormalizedPadding;
            case jaccardShingles2:
                return twoShingleJaccard;
            case jaccardShingles3:
                return threeShingleJaccard;
            case jaccardNormalizedShingles2:
                return twoShingleJaccardNormalized;
            case jaccardNormalizedShingles3:
                return threeShingleJaccardNormalized;

            case diceTokens:
                return tokenDice;
            case diceNormalizedTokens:
                return tokenDiceNormalized;
            case dice2Grams:
                return twoGramDice;
            case dice3Grams:
                return threeGramDice;
            case dice4Grams:
                return fourGramDice;
            case dice5Grams:
                return fiveGramDice;
            case diceNormalized2Grams:
                return twoGramDiceNormalized;
            case diceNormalized3Grams:
                return threeGramDiceNormalized;
            case diceNormalized4Grams:
                return fourGramDiceNormalized;
            case diceNormalized5Grams:
                return fiveGramDiceNormalized;
            case diceNormalizedPadding2grams:
                return twoGramDiceNormalizedPadding;
            case diceNormalizedPadding3grams:
                return threeGramDiceNormalizedPadding;
            case diceNormalizedPadding4grams:
                return fourGramDiceNormalizedPadding;
            case diceNormalizedPadding5grams:
                return fiveGramDiceNormalizedPadding;
            case diceShingles2:
                return twoShingleDice;
            case diceShingles3:
                return threeShingleDice;
            case diceNormalizedShingles2:
                return twoShingleDiceNormalized;
            case diceNormalizedShingles3:
                return threeShingleDiceNormalized;

            case overlapTokens:
                return tokenOverlap;
            case overlapNormalizedTokens:
                return tokenOverlapNormalized;
            case overlap2Grams:
                return twoGramOverlap;
            case overlap3Grams:
                return threeGramOverlap;
            case overlap4Grams:
                return fourGramOverlap;
            case overlap5Grams:
                return fiveGramOverlap;
            case overlapNormalized2Grams:
                return twoGramOverlapNormalized;
            case overlapNormalized3Grams:
                return threeGramOverlapNormalized;
            case overlapNormalized4Grams:
                return fourGramOverlapNormalized;
            case overlapNormalized5Grams:
                return fiveGramOverlapNormalized;
            case overlapNormalizedPadding2grams:
                return twoGramOverlapNormalizedPadding;
            case overlapNormalizedPadding3grams:
                return threeGramOverlapNormalizedPadding;
            case overlapNormalizedPadding4grams:
                return fourGramOverlapNormalizedPadding;
            case overlapNormalizedPadding5grams:
                return fiveGramOverlapNormalizedPadding;
            case overlapShingles2:
                return twoShingleOverlap;
            case overlapShingles3:
                return threeShingleOverlap;
            case overlapNormalizedShingles2:
                return twoShingleOverlapNormalized;
            case overlapNormalizedShingles3:
                return threeShingleOverlapNormalized;

            case twoGramSimilarityKondrak05:
                return twoGramSimilarityKondrak05;
            case threeGramSimilarityKondrak05:
                return threeGramSimilarityKondrak05;
            case fourGramSimilarityKondrak05:
                return fourGramSimilarityKondrak05;
            case fiveGramSimilarityKondrak05:
                return fiveGramSimilarityKondrak05;


            case winnowingTwoGramLongestCommonSubsequence:
                return winnowingTwoGramLongestCommonSubsequence;
            case winnowingThreeGramLongestCommonSubsequence:
                return winnowingThreeGramLongestCommonSubsequence;
            case winnowingFourGramLongestCommonSubsequence:
                return winnowingFourGramLongestCommonSubsequence;
            case winnowingFiveGramLongestCommonSubsequence:
                return winnowingFiveGramLongestCommonSubsequence;
            case winnowingTwoGramLongestCommonSubsequenceNormalized:
                return winnowingTwoGramLongestCommonSubsequenceNormalized;
            case winnowingThreeGramLongestCommonSubsequenceNormalized:
                return winnowingThreeGramLongestCommonSubsequenceNormalized;
            case winnowingFourGramLongestCommonSubsequenceNormalized:
                return winnowingFourGramLongestCommonSubsequenceNormalized;
            case winnowingFiveGramLongestCommonSubsequenceNormalized:
                return winnowingFiveGramLongestCommonSubsequenceNormalized;
            case winnowingTwoGramOptimalAlignment:
                return winnowingTwoGramOptimalAlignment;
            case winnowingThreeGramOptimalAlignment:
                return winnowingThreeGramOptimalAlignment;
            case winnowingFourGramOptimalAlignment:
                return winnowingFourGramOptimalAlignment;
            case winnowingFiveGramOptimalAlignment:
                return winnowingFiveGramOptimalAlignment;
            case winnowingTwoGramOptimalAlignmentNormalized:
                return winnowingTwoGramOptimalAlignmentNormalized;
            case winnowingThreeGramOptimalAlignmentNormalized:
                return winnowingThreeGramOptimalAlignmentNormalized;
            case winnowingFourGramOptimalAlignmentNormalized:
                return winnowingFourGramOptimalAlignmentNormalized;
            case winnowingFiveGramOptimalAlignmentNormalized:
                return winnowingFiveGramOptimalAlignmentNormalized;


            default:
                return null;
        }
    }


    // ****** Edit based *****
    private static BiFunction<String, String, Double> levenshtein = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein;
    private static BiFunction<String, String, Double> levenshteinNormalized = de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized;

    private static BiFunction<String, String, Double> damerauLevenshtein = de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein;
    private static BiFunction<String, String, Double> damerauLevenshteinNormalized = de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized;

    private static BiFunction<String, String, Double> optimalAlignment = de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment;
    private static BiFunction<String, String, Double> optimalAlignmentNormalized = de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized;

    private static BiFunction<String, String, Double> longestCommonSubsequence = de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence;
    private static BiFunction<String, String, Double> longestCommonSubsequenceNormalized = de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized;

    private static BiFunction<String, String, Double> winnowingTwoGramJaccard = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard;
    private static BiFunction<String, String, Double> winnowingThreeGramJaccard = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard;
    private static BiFunction<String, String, Double> winnowingFourGramJaccard = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard;
    private static BiFunction<String, String, Double> winnowingFiveGramJaccard = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard;

    private static BiFunction<String, String, Double> winnowingTwoGramJaccardNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized;
    private static BiFunction<String, String, Double> winnowingThreeGramJaccardNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized;
    private static BiFunction<String, String, Double> winnowingFourGramJaccardNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized;
    private static BiFunction<String, String, Double> winnowingFiveGramJaccardNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized;

    private static BiFunction<String, String, Double> winnowingTwoGramDice = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice;
    private static BiFunction<String, String, Double> winnowingThreeGramDice = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice;
    private static BiFunction<String, String, Double> winnowingFourGramDice = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice;
    private static BiFunction<String, String, Double> winnowingFiveGramDice = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice;

    private static BiFunction<String, String, Double> winnowingTwoGramDiceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized;
    private static BiFunction<String, String, Double> winnowingThreeGramDiceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized;
    private static BiFunction<String, String, Double> winnowingFourGramDiceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized;
    private static BiFunction<String, String, Double> winnowingFiveGramDiceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized;

    private static BiFunction<String, String, Double> winnowingTwoGramOverlap = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap;
    private static BiFunction<String, String, Double> winnowingThreeGramOverlap = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap;
    private static BiFunction<String, String, Double> winnowingFourGramOverlap = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap;
    private static BiFunction<String, String, Double> winnowingFiveGramOverlap = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap;

    private static BiFunction<String, String, Double> winnowingTwoGramOverlapNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized;
    private static BiFunction<String, String, Double> winnowingThreeGramOverlapNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized;
    private static BiFunction<String, String, Double> winnowingFourGramOverlapNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized;
    private static BiFunction<String, String, Double> winnowingFiveGramOverlapNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized;


    // ****** Profile based
    private static BiFunction<String, String, Double> cosineTokenNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool;
    private static BiFunction<String, String, Double> cosineTokenNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineTokenNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineTwoGramNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool;
    private static BiFunction<String, String, Double> cosineThreeGramNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool;
    private static BiFunction<String, String, Double> cosineFourGramNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool;
    private static BiFunction<String, String, Double> cosineFiveGramNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool;
    private static BiFunction<String, String, Double> cosineTwoGramNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineThreeGramNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineFourGramNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineFiveGramNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineTwoGramNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineThreeGramNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineFourGramNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineFiveGramNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineTwoShingleNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool;
    private static BiFunction<String, String, Double> cosineThreeShingleNormalizedBool = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool;
    private static BiFunction<String, String, Double> cosineTwoShingleNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineThreeShingleNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineTwoShingleNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency;
    private static BiFunction<String, String, Double> cosineThreeShingleNormalizedNormalizedTermFrequency = de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency;

    private static BiFunction<String, String, Double> manhattanTokenNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized;
    private static BiFunction<String, String, Double> manhattanTwoGramNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized;
    private static BiFunction<String, String, Double> manhattanThreeGramNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized;
    private static BiFunction<String, String, Double> manhattanFourGramNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized;
    private static BiFunction<String, String, Double> manhattanFiveGramNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized;
    private static BiFunction<String, String, Double> manhattanTwoShingleNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized;
    private static BiFunction<String, String, Double> manhattanThreeShingleNormalized = de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized;

    // ****** Set based
    private static BiFunction<String, String, Double> tokenJaccard = de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard;
    private static BiFunction<String, String, Double> tokenJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized;

    private static BiFunction<String, String, Double> twoGramJaccard = de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard;
    private static BiFunction<String, String, Double> threeGramJaccard = de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard;
    private static BiFunction<String, String, Double> fourGramJaccard = de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard;
    private static BiFunction<String, String, Double> fiveGramJaccard = de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard;

    private static BiFunction<String, String, Double> twoGramJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized;
    private static BiFunction<String, String, Double> threeGramJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized;
    private static BiFunction<String, String, Double> fourGramJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized;
    private static BiFunction<String, String, Double> fiveGramJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized;

    private static BiFunction<String, String, Double> twoGramJaccardNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding;
    private static BiFunction<String, String, Double> threeGramJaccardNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding;
    private static BiFunction<String, String, Double> fourGramJaccardNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding;
    private static BiFunction<String, String, Double> fiveGramJaccardNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding;

    private static BiFunction<String, String, Double> twoShingleJaccard = de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard;
    private static BiFunction<String, String, Double> threeShingleJaccard = de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard;

    private static BiFunction<String, String, Double> twoShingleJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized;
    private static BiFunction<String, String, Double> threeShingleJaccardNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized;

    private static BiFunction<String, String, Double> tokenDice = de.unitrier.st.stringsimilarity.set.Variants::tokenDice;
    private static BiFunction<String, String, Double> tokenDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized;

    private static BiFunction<String, String, Double> twoGramDice = de.unitrier.st.stringsimilarity.set.Variants::twoGramDice;
    private static BiFunction<String, String, Double> threeGramDice = de.unitrier.st.stringsimilarity.set.Variants::threeGramDice;
    private static BiFunction<String, String, Double> fourGramDice = de.unitrier.st.stringsimilarity.set.Variants::fourGramDice;
    private static BiFunction<String, String, Double> fiveGramDice = de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice;

    private static BiFunction<String, String, Double> twoGramDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized;
    private static BiFunction<String, String, Double> threeGramDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized;
    private static BiFunction<String, String, Double> fourGramDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized;
    private static BiFunction<String, String, Double> fiveGramDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized;

    private static BiFunction<String, String, Double> twoGramDiceNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding;
    private static BiFunction<String, String, Double> threeGramDiceNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding;
    private static BiFunction<String, String, Double> fourGramDiceNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding;
    private static BiFunction<String, String, Double> fiveGramDiceNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding;

    private static BiFunction<String, String, Double> twoShingleDice = de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice;
    private static BiFunction<String, String, Double> threeShingleDice = de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice;

    private static BiFunction<String, String, Double> twoShingleDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized;
    private static BiFunction<String, String, Double> threeShingleDiceNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized;

    private static BiFunction<String, String, Double> tokenOverlap = de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap;
    private static BiFunction<String, String, Double> tokenOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized;

    private static BiFunction<String, String, Double> twoGramOverlap = de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap;
    private static BiFunction<String, String, Double> threeGramOverlap = de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap;
    private static BiFunction<String, String, Double> fourGramOverlap = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap;
    private static BiFunction<String, String, Double> fiveGramOverlap = de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap;

    private static BiFunction<String, String, Double> twoGramOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized;
    private static BiFunction<String, String, Double> threeGramOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized;
    private static BiFunction<String, String, Double> fourGramOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized;
    private static BiFunction<String, String, Double> fiveGramOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized;

    private static BiFunction<String, String, Double> twoGramOverlapNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding;
    private static BiFunction<String, String, Double> threeGramOverlapNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding;
    private static BiFunction<String, String, Double> fourGramOverlapNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding;
    private static BiFunction<String, String, Double> fiveGramOverlapNormalizedPadding = de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding;

    private static BiFunction<String, String, Double> twoShingleOverlap = de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap;
    private static BiFunction<String, String, Double> threeShingleOverlap = de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap;

    private static BiFunction<String, String, Double> twoShingleOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized;
    private static BiFunction<String, String, Double> threeShingleOverlapNormalized = de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized;


    private static BiFunction<String, String, Double> twoGramSimilarityKondrak05 = de.unitrier.st.stringsimilarity.set.Variants::twoGramSimilarityKondrak05;
    private static BiFunction<String, String, Double> threeGramSimilarityKondrak05 = de.unitrier.st.stringsimilarity.set.Variants::threeGramSimilarityKondrak05;
    private static BiFunction<String, String, Double> fourGramSimilarityKondrak05 = de.unitrier.st.stringsimilarity.set.Variants::fourGramSimilarityKondrak05;
    private static BiFunction<String, String, Double> fiveGramSimilarityKondrak05 = de.unitrier.st.stringsimilarity.set.Variants::fiveGramSimilarityKondrak05;


    private static BiFunction<String, String, Double> winnowingTwoGramLongestCommonSubsequence = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence;
    private static BiFunction<String, String, Double> winnowingThreeGramLongestCommonSubsequence = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence;
    private static BiFunction<String, String, Double> winnowingFourGramLongestCommonSubsequence = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence;
    private static BiFunction<String, String, Double> winnowingFiveGramLongestCommonSubsequence = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence;
    private static BiFunction<String, String, Double> winnowingTwoGramLongestCommonSubsequenceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized;
    private static BiFunction<String, String, Double> winnowingThreeGramLongestCommonSubsequenceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized;
    private static BiFunction<String, String, Double> winnowingFourGramLongestCommonSubsequenceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized;
    private static BiFunction<String, String, Double> winnowingFiveGramLongestCommonSubsequenceNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized;
    private static BiFunction<String, String, Double> winnowingTwoGramOptimalAlignment = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment;
    private static BiFunction<String, String, Double> winnowingThreeGramOptimalAlignment = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment;
    private static BiFunction<String, String, Double> winnowingFourGramOptimalAlignment = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment;
    private static BiFunction<String, String, Double> winnowingFiveGramOptimalAlignment = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment;
    private static BiFunction<String, String, Double> winnowingTwoGramOptimalAlignmentNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized;
    private static BiFunction<String, String, Double> winnowingThreeGramOptimalAlignmentNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized;
    private static BiFunction<String, String, Double> winnowingFourGramOptimalAlignmentNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized;
    private static BiFunction<String, String, Double> winnowingFiveGramOptimalAlignmentNormalized = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized;

}
