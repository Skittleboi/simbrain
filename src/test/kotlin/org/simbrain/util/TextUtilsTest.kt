package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class TextUtilsTest {

    val simpleText = "This is a simple sentence. This is not hard."

    /**
     * "Cat" and "dog" are in similar contexts.  "Please" and "dog" are not in similar contexts.
     */
    val similarText = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the table."

    val harderText = "In spite of these three obstacles, the fragmentary Don Quixote of Menard is more subtle than that of Cervantes. The latter indulges in a rather coarse opposition between tales of knighthood and the meager, provincial reality of his country; Menard chooses as 'reality' the land of Carmen during the century of Lepanto and Lope. What Hispanophile would not have advised Maurice Barres or Dr. Rodrigues Larreta to make such a choice! Menard, as if it were the most natural thing in the world, eludes them."

    val windowSizeText = "Albert ran into the store, while Jean walked into the store. Jean packed all the books, after Albert read all the books."

    @Test
    fun `test sentence parsing`() {
        val sentences =  simpleText.tokenizeSentencesFromDoc()
        assertEquals(2, sentences.size)
        assertEquals(4, harderText.tokenizeSentencesFromDoc().size)
    }

    @Test
    fun `punctuation is removed correctly`() {
        var punctRemoved = "(A,B)#C:::{A_B}[D]"
        punctRemoved = punctRemoved.removePunctuation()
        assertEquals("ABCABD", punctRemoved)
    }

    @Test
    fun `correct number of words parsed from sentence`() {
        val sample = "This, is text!"
        assertEquals(3, sample.tokenizeWordsFromSentence().size)
    }

    @Test
    fun `test lowercasing`() {
        val firstCapital = "Abc"
        val middleCapital = "aBc"
        assertEquals("abc", firstCapital.lowercase())
        assertEquals("abc", middleCapital.lowercase())
    }

    @Test
    fun `tabs and newlines removed by removeSpecialCharacters`() {
        val testString = "A\t\tb\n\nc"
        assertEquals(false,testString.removeSpecialCharacters().contains("[\n\r\t]"))
        assertEquals(5,testString.removeSpecialCharacters().length)
    }

    @Test
    fun `get unique tokens from sentences`() {
        val sentence = "a A a b. B c b d c c"
        val tokenizedSentence = sentence.tokenizeWordsFromSentence()
        val uniqueTokens = tokenizedSentence.uniqueTokensFromArray()
        // println(uniqueTokens)
        assertEquals(listOf("a","b","c","d"), uniqueTokens)
    }

    @Test
    fun `outer-product computed correctly`() {
        val vectorU = doubleArrayOf(1.0, 2.0, 3.0)
        val vectorV = doubleArrayOf(4.0, 5.0, 6.0, 7.0)
        val outerProductUV = outerProduct(vectorU, vectorV)
        assertEquals(outerProductUV[2,0], outerProductUV[1,2]) //row, col
    }

    @Test
    fun `PPMI computed correctly`() {
        val A = arrayOf(
            doubleArrayOf(0.0, 3.0, 2.0),
            doubleArrayOf(1.0, 4.0, 0.0),
            doubleArrayOf(1.0, 0.0, 0.0)
        )
        val temporaryMatrix = Matrix(A)
        val adjustedMatrix = manualPPMI(temporaryMatrix, true)
        assertTrue(temporaryMatrix[0,1] > adjustedMatrix[0,0])
        assertEquals(temporaryMatrix[0,0], adjustedMatrix[0,0])
    }

    @Test
    fun `co-occurrence matrix is correct size`() {
        val tokens = simpleText.tokenizeWordsFromSentence().uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(simpleText, 2, true).second
        assertEquals(tokens.size, cooccurrenceMatrix.nrows())
        assertEquals(tokens.size, cooccurrenceMatrix.ncols())
    }

    @Test
    fun `word embedding have correct size`() {
        val tokenizedSentence = harderText.tokenizeWordsFromSentence()
        val tokens = tokenizedSentence.uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(harderText, 2, true).second
        assertEquals(tokens.size, wordEmbeddingQuery("obstacles",tokens,cooccurrenceMatrix).size)
        assertEquals(tokens.size, wordEmbeddingQuery("Quixote",tokens,cooccurrenceMatrix).size) // issue was capital Q
    }

    @Test
    fun `co-occurence matrix window size correctly affects similarity`() {
        val tokens = windowSizeText.tokenizeWordsFromSentence().uniqueTokensFromArray()
        val cooccurrenceMatrixShort = generateCooccurrenceMatrix(windowSizeText, 1, true).second
        val cooccurrenceMatrixLong = generateCooccurrenceMatrix(windowSizeText, 4, true).second
        val smallWindowSimilarity = embeddingSimilarity(
                wordEmbeddingQuery("Jean", tokens, cooccurrenceMatrixShort),
                wordEmbeddingQuery("Albert", tokens, cooccurrenceMatrixShort),
            )
        val longWindowSimilarity = embeddingSimilarity(
                wordEmbeddingQuery("Jean", tokens, cooccurrenceMatrixLong),
                wordEmbeddingQuery("Albert", tokens, cooccurrenceMatrixLong),
            )
        assertTrue(smallWindowSimilarity < longWindowSimilarity)
    }


    @Test
    fun `computes cosine similarity between two vectors`() {
        val tokenizedSentence = similarText.tokenizeWordsFromSentence()
        val tokens = tokenizedSentence.uniqueTokensFromArray()
        val cooccurrenceMatrix = generateCooccurrenceMatrix(similarText, 2, true).second
        val vectorA = wordEmbeddingQuery("cat",tokens,cooccurrenceMatrix)
        val vectorB = wordEmbeddingQuery("dog",tokens,cooccurrenceMatrix)
        val vectorC = wordEmbeddingQuery("table",tokens,cooccurrenceMatrix)
        assertTrue(embeddingSimilarity(vectorA, vectorB) > embeddingSimilarity(vectorB, vectorC) )
    }

}

