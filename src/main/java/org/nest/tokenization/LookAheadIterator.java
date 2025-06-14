package org.nest.tokenization;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Special iterator that supports lookAhead without consuming tokens.
 */
public class LookAheadIterator implements Iterator<Token>
{
    private final TokenList tokens;
    private int currentIndex = 0;

    public LookAheadIterator(TokenList tokens)
    {
        this.tokens = tokens;
    }

    @Override
    public boolean hasNext()
    {
        return currentIndex < tokens.size();
    }

    @Override
    public Token next()
    {
        if (!hasNext()) throw new NoSuchElementException();
        return tokens.get(currentIndex++);
    }

    public Token lookAhead(int steps)
    {
        int idx = currentIndex + steps;
        if (idx < 0 || idx >= tokens.size())
        {
            return null;
        }
        return tokens.get(idx);
    }
}
